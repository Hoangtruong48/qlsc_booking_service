package com.qlsc.qlsc_booking_service.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class MultiThreadWithAI {

    public static boolean isPrime(long n) {
        for (int i = 2; (long) i * i <= n; i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return n > 1;
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

                    if (!set.add(j)) {
                        continue;
                    }

                    if (isPrime(j)) {
                        count.incrementAndGet();
                    }
                }
            }, executorService));
        }

        System.out.println("Break for test thread run ");

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();

        System.out.println("Số lượng số nguyên tố: " + count.get());
        long end = System.currentTimeMillis();
        System.out.println("Time = " + (end - start) + " ms");
    }
}