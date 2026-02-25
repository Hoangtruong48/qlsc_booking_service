package com.qlsc.qlsc_booking_service.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Test {
    public static void main(String[] args) {
        BlockingQueue<Long> queue = new LinkedBlockingQueue<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 8; i <= 9; i++) {
            int finalI = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> addValueInQueue(queue, finalI), executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executor.shutdown();
        System.out.println(queue.size());
        while (!queue.isEmpty()) {
            System.out.print(queue.poll() + " ");
        }
    }

    private static void addValueInQueue(BlockingQueue<Long> queue, int i) {
        for (int j = 0; j < 10; j++) {
            boolean k = queue.offer((long) i);
        }

    }
}
