package com.qlsc.qlsc_booking_service.thread;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadWithLock {

    @Getter
    @Setter
    public static class User {
        Long id;
        boolean buySuccess = false;
    }
    public static void main(String[] args) {
        AtomicInteger total = new AtomicInteger();
        total.set(10);
        CopyOnWriteArrayList<Long> lst = new CopyOnWriteArrayList<>();

        ExecutorService service = Executors.newFixedThreadPool(12);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        List<User> users = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setId((long) i + 1);
            users.add(user);
        }

//        Collections.shuffle(users);

        List<CompletableFuture<Void>> futuresList = new ArrayList<>();

        for (User user : users) {
            futuresList.add(CompletableFuture.runAsync(() -> {
                try {
                    countDownLatch.await();
                    // Ép luồng thực hiện trừ đi 1 ngay lập tức và lấy kết quả về

                    int remain = total.decrementAndGet();

                    if (remain >= 0) {
                        // Trừ xong mà số dư >= 0 nghĩa là mua thành công
                        user.setBuySuccess(true);
                        lst.add(user.getId());
                        System.out.println("User " + user.getId() + " mua thành công. Kho còn: " + remain);
                    } else {
                        // Trừ xong mà ra số âm (< 0) nghĩa là đến lượt mình thì đã hết hàng
                        // Phải cộng trả lại số lượng vừa lỡ trừ để kho không bị âm
                        total.incrementAndGet();
                        System.out.println("User " + user.getId() + " mua thất bại do hết hàng.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, service));
        }

        countDownLatch.countDown();
        CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[0])).join();



        service.shutdown();
        System.out.println(total.get());
        Collections.sort(lst);
        System.out.println(lst);

    }

    private static void userMuaHang(User user, AtomicInteger total) {
        user.buySuccess = true;
        total.decrementAndGet();
    }


}
