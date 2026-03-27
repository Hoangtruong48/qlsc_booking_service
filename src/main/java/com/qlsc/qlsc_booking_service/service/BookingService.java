package com.qlsc.qlsc_booking_service.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.qlsc.qlsc_booking_service.controller.BookingController;
import com.qlsc.qlsc_booking_service.dto.*;
import com.qlsc.qlsc_booking_service.entity.BlackFridaySale;
import com.qlsc.qlsc_booking_service.entity.Booking;
import com.qlsc.qlsc_booking_service.excel.BlackFridaySaleExcelDTO;
import com.qlsc.qlsc_booking_service.feign.BadmintonCourtManagementClient;
import com.qlsc.qlsc_booking_service.producer.BookingProducer;
import com.qlsc.qlsc_booking_service.repo.jpa.BlackFridaySaleRepo;
import com.qlsc.qlsc_booking_service.repo.jpa.BookingRepoJpa;
import com.qlsc.qlsc_booking_service.request.BookingRequest;
import com.qlsc.qlsc_booking_service.request.FlashSaleRequest;
import com.qlsc.qlsc_common.constant.KafkaConstant;
import com.qlsc.qlsc_common.constant.SagaConstant;
import com.qlsc.qlsc_common.response.ApiResponse;
import com.qlsc.qlsc_common.saga.BookingCreatedEvent;
import com.qlsc.qlsc_common.saga.CreateBookingCommand;
import com.qlsc.qlsc_common.util.NumberQlscUtils;
import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingService {
    Logger LOG = LoggerFactory.getLogger(this.getClass());
    BadmintonCourtManagementClient badmintonFeign;
    BookingRepoJpa bookingRepoJpa;
    BookingProducer bookingProducer;
    BlackFridaySaleRepo blackFridaySaleRepo;
    EntityManager entityManager;

    @Autowired
    public BookingService(BadmintonCourtManagementClient badmintonFeign, BookingRepoJpa bookingRepoJpa, BookingProducer bookingProducer, BlackFridaySaleRepo blackFridaySaleRepo, EntityManager entityManager) {
        this.badmintonFeign = badmintonFeign;
        this.bookingRepoJpa = bookingRepoJpa;
        this.bookingProducer = bookingProducer;
        this.blackFridaySaleRepo = blackFridaySaleRepo;
        this.entityManager = entityManager;
    }


    public ApiResponse<?> getBadmintonFree(List<Integer> ids, Long bookingDate) {
        ApiResponse<List<CourtScheduleDTO>> response = new ApiResponse<>();
        // Lấy thông tin thời gian mở cửa và đóng cửa của 1 sân cầu
        List<BadmintonInfoTimeDTO> infoBadmintonCourts = badmintonFeign.getInfoTimeCourtDetail(ids).getData();
        if (infoBadmintonCourts == null || infoBadmintonCourts.isEmpty()) {
            LOG.info("Return because infoBadmintonCourts null");
            return response.setDataSuccess(new ArrayList<>());
        }
        Map<Integer, DetailInfoDTO> mapCourtAndDetailInfo = buildMapTimeOfCourt(infoBadmintonCourts);

        // tìm tất cả sân đã được đặt
        List<Object[]> rawData = bookingRepoJpa.findAllCourtAvailable(ids, bookingDate);
        List<ScheduleTimeAvailableDTO> lstCourtExists = ScheduleTimeAvailableDTO.convertRawDataToDTO(rawData);
        if (lstCourtExists == null || lstCourtExists.isEmpty()) {
            LOG.info(" lstCourtExists null");
            lstCourtExists = new ArrayList<>();
//            return response.setDataSuccess(new ArrayList<>());
        }

        Map<Integer, List<ScheduleTimeAvailableDTO>> mapSchedule = lstCourtExists.stream()
                .collect(
                        Collectors.groupingBy(ScheduleTimeAvailableDTO::getCourtId)
                );

        List<CourtScheduleDTO> res = new ArrayList<>();
        // chia build theo sân cầu to
        for (var x : ids) {
            res.add(buildResponse(x, mapSchedule.getOrDefault(x, new ArrayList<>()), mapCourtAndDetailInfo.get(x)));
        }
        response.setDataSuccess(res);
        return response;
    }

    /**
     * @param scheduleTimeAvailableDTOS : lưu thông tin sân số x đang được thuê từ giờ a --> giờ b, court id trong list giống nhau hết
     * @param detailInfo                : lưu thông tin sân cầu to mở từ giờ nào đến giờ nào
     * @return : xử lí build thông tin từng sân cầu chi tiết :
     * b1 : mặc định cho từ index 0 -> 23 : được mở
     * b2 : Đóng index [0 -> open time - 1], [close time -> 23]
     * b3 : build tất cả mảng đã được thuê : ví dụ thuê từ 1 --> 3 thì đóng 1, 2 : add [startTime --> endTime - 1]
     */
    // build từng sân nhỏ trong sân to
    private CourtScheduleDTO buildResponse(Integer courtId,
                                           List<ScheduleTimeAvailableDTO> scheduleTimeAvailableDTOS,
                                           DetailInfoDTO detailInfo) {
        CourtScheduleDTO courtScheduleDTO = new CourtScheduleDTO();
        Map<Integer, List<ScheduleTimeAvailableDTO>> mapByCourtNumber = scheduleTimeAvailableDTOS.stream()
                .collect(Collectors.groupingBy(ScheduleTimeAvailableDTO::getCourtNumber));
        int openTime = detailInfo.getStartTime();
        int endTime = detailInfo.getEndTime();
        courtScheduleDTO.setOpeningTime(openTime);
        courtScheduleDTO.setClosingTime(endTime);
        // đây là đoạn build từ sân 1 --> sân cuối trong 1 sân to
        List<Integer> lstCourtNumber = detailInfo.getListCourtNumber();
        List<CourtScheduleDTO.InfoSlot> lstInfos = new ArrayList<>();
        for (Integer x : lstCourtNumber) {
            lstInfos.add(buildInfoCourtDetail(openTime, endTime, mapByCourtNumber.getOrDefault(x, new ArrayList<>()), x));

        }
        courtScheduleDTO.setInfoSlots(lstInfos);
        courtScheduleDTO.setCourtId(courtId);
        return courtScheduleDTO;
    }

    private CourtScheduleDTO.InfoSlot buildInfoCourtDetail(int start, int end, List<ScheduleTimeAvailableDTO> scheduleTimeAvailableDTOS,
                                                           Integer courtNumber) {
        CourtScheduleDTO.InfoSlot infoSlot = new CourtScheduleDTO.InfoSlot();
        infoSlot.setCourtNumber(courtNumber);
        List<Integer> lstRes = new ArrayList<>();
        for (int i = 0; i <= 23; i++) {
            lstRes.add(1);
        }
        for (int i = 0; i <= start - 1; i++) {
            lstRes.set(i, 0);
        }
        for (int i = end; i <= 23; i++) {
            lstRes.set(i, 0);
        }
        List<Integer> lstIndexClose = new ArrayList<>();
        for (var x : scheduleTimeAvailableDTOS) {
            int s = x.getTimeStart();
            int n = x.getTimeEnd();
            for (int i = s; i <= n - 1; i++) {
                lstIndexClose.add(i);
            }
        }
        Collections.sort(lstIndexClose);
        for (var x : lstIndexClose) {
            lstRes.set(x, 0);
        }
        infoSlot.setSlots(lstRes);
        return infoSlot;
    }

    private Map<Integer, DetailInfoDTO> buildMapTimeOfCourt(List<BadmintonInfoTimeDTO> infoBadmintonCourts) {
        Map<Integer, DetailInfoDTO> res = new HashMap<>();
        Map<Integer, List<BadmintonInfoTimeDTO>> mapByCourtId = infoBadmintonCourts.stream()
                .collect(Collectors.groupingBy(BadmintonInfoTimeDTO::getCourtId));
        for (Map.Entry<Integer, List<BadmintonInfoTimeDTO>> entry : mapByCourtId.entrySet()) {
            DetailInfoDTO detailInfoDTO = new DetailInfoDTO();
            BadmintonInfoTimeDTO dto = entry.getValue().get(0);
            detailInfoDTO.setEndTime(dto.getClosingTime());
            detailInfoDTO.setStartTime(dto.getOpeningTime());
            detailInfoDTO.setListCourtNumber(entry.getValue()
                    .stream().map(BadmintonInfoTimeDTO::getCourtNumber).toList());
            res.put(entry.getKey(), detailInfoDTO);
        }
        return res;
    }

    public void createBooking(CreateBookingCommand cmd) {
        int status = SagaConstant.BookingSaga.BOOKING;
        String msg = "";
        Long courtId = cmd.getCourtId();
        Integer courtNumber = cmd.getCourtNumber();
        Integer startTime = cmd.getStartTime();
        Integer endTime = cmd.getEndTime();
        Long bookingDate = cmd.getBookingDate();
        String sagaId = cmd.getSagaId();
        Double price = cmd.getPrice();
        BookingCreatedEvent event = new BookingCreatedEvent();
        event.setSagaId(sagaId);
        // 1. check xem có sân đặt chưa
        boolean conflicted = bookingRepoJpa.isBookingConflicted(courtId, courtNumber,
                bookingDate, startTime, endTime);
        if (conflicted) {
            status = SagaConstant.FAILED;
            msg = "Create booking failed because duplicate booking already exists.";
            event.setStatus(status);
            event.setMsg(msg);
        } else {
            event.setStatus(status);
            Booking booking = new Booking();
            booking.setCourtId(courtId);
            booking.setCourtNumber(courtNumber);
            booking.setBookingDate(bookingDate);
            booking.setStartTime(startTime);
            booking.setEndTime(endTime);
            // TODO : xử lí lấy lại price chỗ này bằng cách tính giá tiền sân, lưu ý có cấu hình giờ vàng tăng giá
            booking.setPrice(price);
            booking.setUpdatedAt(System.currentTimeMillis());
            booking.setUpdatedBy(cmd.getUserName());
            booking.setStatus(Booking.STATUS_PENDING);
            Booking save = bookingRepoJpa.save(booking);
            event.setBookingId(save.getId());
        }

        bookingProducer.sendEvent(KafkaConstant.TOPIC_BOOKING_EVENT, event);

    }


    public ApiResponse<?> cusorPagingWithMultiThreadGetData() {
        long startTime = System.currentTimeMillis();
        ApiResponse<String> response = new ApiResponse<>();

        int threadCount = 5;
        int limit = 2000;

        DataBlackFridayDTO o = blackFridaySaleRepo.getMinMaxId();
        if (o == null || o.getNumber1() == null || o.getNumber2() == null) {
            return response.setMessageFailed("Khong co du lieu de xu li");
        }

        long minId = o.getNumber1();
        long maxId = o.getNumber2();
        long chunkSize = (maxId - minId) / threadCount;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                long start = minId + (i * chunkSize);
                long end = (i == threadCount - 1) ? maxId : (start + chunkSize - 1);

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> processTaskByRange(start, end, limit), executor)
                        .orTimeout(1, TimeUnit.MINUTES)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                LOG.error("Error processing range {}-{}: ", start, end, ex);
                            } else {
                                LOG.info("Completed range {}-{}", start, end);
                            }
                        });

                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } catch (Exception e) {
            LOG.error("Có lỗi xảy ra trong quá trình phân chia và chạy luồng: ", e);
            return response.setMessageFailed("Lỗi xử lý dữ liệu");
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                    LOG.warn("Hết thời gian chờ 10 phút, buộc ngắt các luồng đang chạy!");
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                LOG.error("Luồng dọn dẹp bị gián đoạn, ép buộc ngắt luồng ngay lập tức!");
                executor.shutdownNow();
                Thread.currentThread().interrupt(); // Khôi phục trạng thái ngắt
            }
        }

        LOG.info("Time to get data: {} ms", System.currentTimeMillis() - startTime);
        return response.setMessageSuccess("Xử lý thành công");
    }


    private void processTaskByRange(long startId, long endId, int step) {
        LOG.info("Thread {} start = {}, end = {}", Thread.currentThread().getName(), startId, endId);

        long currentStart = startId;

        while (currentStart <= endId) {
            long currentEnd = Math.min(endId + 1, currentStart + step);

            List<BlackFridaySale> batch = blackFridaySaleRepo.findAllByCusorId(currentStart, currentEnd);

            if (!batch.isEmpty()) {
                LOG.info("Thread {} processed {} records", Thread.currentThread().getName(), batch.size());
            }

            currentStart = currentEnd;
        }

        LOG.info("Thread {} completed range", Thread.currentThread().getName());
    }

    @Transactional(readOnly = true)
    public ApiResponse<?> streamDataAlibabaExcel() {
        long startTime = System.currentTimeMillis();
        int batchSize = 1000;

        String filePath = "black_friday_" + System.currentTimeMillis() + ".xlsx";
        // 1. Khởi tạo EasyExcel Writer
        File file = new File(filePath);
        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(file, BlackFridaySaleExcelDTO.class).build();
            Stream<BlackFridaySale> stream = blackFridaySaleRepo.streamAllData();
            // Khởi tạo Sheet
            WriteSheet writeSheet = EasyExcel.writerSheet("Dữ liệu Black Friday").build();

            // Buffer tạm để gom data trước khi ghi (tránh ghi từng dòng lẻ tẻ làm chậm I/O disk)
            List<BlackFridaySaleExcelDTO> buffer = new ArrayList<>(batchSize);

            // 2. Duyệt Stream
            ExcelWriter finalExcelWriter = excelWriter;
            stream.forEach(sale -> {
                // Map từ Entity sang DTO
                buffer.add(mapToDto(sale));

                // Khi buffer đầy 1000 dòng -> Ghi ra file và dọn rác
                if (buffer.size() >= batchSize) {
                    finalExcelWriter.write(buffer, writeSheet); // Ghi xuống đĩa
                    buffer.clear();                        // Xóa List tạm
                    entityManager.clear();                 // Xả First-Level Cache của Hibernate
                }
            });

            // 3. Ghi nốt phần dư (nếu tổng số dòng không chia hết cho 1000)
            if (!buffer.isEmpty()) {
                excelWriter.write(buffer, writeSheet);
                buffer.clear();
                entityManager.clear();
            }

        } catch (Exception e) {
            LOG.error("Lỗi khi xuất file Excel: ", e);
            return new ApiResponse<>().setMessageFailed("Lỗi xuất dữ liệu");
        } finally {
            if (excelWriter != null) excelWriter.finish();
        }


        LOG.info("Xuất file Excel thành công! Thời gian: {} ms", System.currentTimeMillis() - startTime);
        return new ApiResponse<>().setMessageSuccess("Xuất file thành công tại: " + filePath);
    }

    // Hàm mapping (có thể dùng MapStruct để code gọn hơn)
    private BlackFridaySaleExcelDTO mapToDto(BlackFridaySale entity) {
        return BlackFridaySaleExcelDTO.builder()
                .id(entity.getId())
                .msg(entity.getMsg())
                .userId(entity.getUserId())
                .build();
    }
}
