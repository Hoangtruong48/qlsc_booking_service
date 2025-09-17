package com.qlsc.qlsc_booking_service.service;

import com.qlsc.qlsc_booking_service.controller.BookingController;
import com.qlsc.qlsc_booking_service.dto.BadmintonInfoTimeDTO;
import com.qlsc.qlsc_booking_service.dto.CourtScheduleDTO;
import com.qlsc.qlsc_booking_service.dto.DetailInfoDTO;
import com.qlsc.qlsc_booking_service.dto.ScheduleTimeAvailableDTO;
import com.qlsc.qlsc_booking_service.entity.Booking;
import com.qlsc.qlsc_booking_service.feign.BadmintonCourtManagementClient;
import com.qlsc.qlsc_booking_service.producer.BookingProducer;
import com.qlsc.qlsc_booking_service.repo.jpa.BookingRepoJpa;
import com.qlsc.qlsc_booking_service.request.BookingRequest;
import com.qlsc.qlsc_common.constant.KafkaConstant;
import com.qlsc.qlsc_common.constant.SagaConstant;
import com.qlsc.qlsc_common.response.ApiResponse;
import com.qlsc.qlsc_common.saga.BookingCreatedEvent;
import com.qlsc.qlsc_common.saga.CreateBookingCommand;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class BookingService {
    Logger LOG = LoggerFactory.getLogger(this.getClass());
    BadmintonCourtManagementClient badmintonFeign;
    BookingRepoJpa bookingRepoJpa;
    BookingProducer bookingProducer;

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
            LOG.info("Return because lstCourtExists null");
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
            booking.setPrice(price);
            booking.setUpdatedAt(System.currentTimeMillis());
            booking.setUpdatedBy(cmd.getUserName());
            bookingRepoJpa.save(booking);
        }
        bookingProducer.sendEvent(KafkaConstant.TOPIC_BOOKING_EVENT, event);

    }

//    }
}
