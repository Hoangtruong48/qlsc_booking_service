package com.qlsc.qlsc_booking_service.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class MultiThreadWithAIV2 {

    public static boolean isPrime(long n) {
        return MultiThreadWithAI.isPrime(n);
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        int n = 5100000;

        int batchSize = 100000;

        Set<Integer> set = ConcurrentHashMap.newKeySet();

        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(cores);

        System.out.println("Core = " + cores);

        AtomicLong count = new AtomicLong(0L);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < n; i += batchSize) {
            int startBatch = i;
            int endBatch = Math.min(i + batchSize, n);

            futures.add(CompletableFuture.runAsync(() -> {
                for (int j = startBatch; j < endBatch; j++) {
                    if (!set.add(j)) continue;
                    if (isPrime(j)) {
                        count.incrementAndGet();
                    }
                }
            }, executorService));
        }

        System.out.println("Break for test thread run ");

        // 1. Gộp tất cả các task lại thành 1 cục
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        // 2. GÀI CALLBACK (Làm việc ngầm): Dù Main Thread có timeout đi nữa,
        // khi nào các luồng worker làm xong hết, đoạn code trong thenRun sẽ tự kích hoạt.
        allTasks.thenRun(() -> {
            long end = System.currentTimeMillis();
            System.out.println("\n--- [BACKGROUND THREAD] THÔNG BÁO HOÀN TẤT ---");
            System.out.println("Số lượng số nguyên tố: " + count.get());
            System.out.println("Tổng thời gian thực tế = " + (end - start) + " ms");
            System.out.println("----------------------------------------------\n");
        });

        // 3. LUỒNG CHÍNH ĐỨNG CHỜ (Tối đa 15 giây)
        try {
            // Thay vì join() chờ vô tận, ta bắt nó chỉ được chờ 15s
            allTasks.get(15, TimeUnit.SECONDS);

            // Nếu xử lý xong trước 15s, nó sẽ chạy tiếp xuống dòng này
            System.out.println("[RESPONSE API]: Tính toán xong sớm! Kết quả là: " + count.get());

        } catch (TimeoutException e) {
            // Nếu quá 15s mà chưa xong, nó ném ngoại lệ và văng vào đây
            System.out.println("[RESPONSE API]: Hệ thống đang thực hiện tính toán. Vui lòng kiểm tra lại sau!");
            // LƯU Ý: Mình KHÔNG gọi hàm cancel() ở đây, nên các luồng worker vẫn tiếp tục chạy ngầm.

        } catch (InterruptedException | ExecutionException e) {
            System.out.println("[RESPONSE API]: Có lỗi xảy ra trong quá trình tính toán.");
        }

        // 4. Giải phóng ThreadPool
        // shutdown() CHỈ từ chối nhận thêm task mới,
        // nó VẪN CHO PHÉP các task đang chạy dở được chạy cho đến khi hoàn thành.
        executorService.shutdown();
    }
}
