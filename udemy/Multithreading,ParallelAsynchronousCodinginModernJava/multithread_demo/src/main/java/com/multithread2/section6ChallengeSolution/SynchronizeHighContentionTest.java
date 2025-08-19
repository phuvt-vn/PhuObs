package com.multithread2.section6ChallengeSolution;

import java.util.concurrent.CountDownLatch;

public class SynchronizeHighContentionTest {
    private static final int THREADS = 100;
    private static final int ITERATIONS = 20_000_000;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== High Contention Test ===");

        testSynchronizedMethod();
        testSynchronizedBlock();
    }

    // ===== Cách 1: synchronized method =====
    static class CounterMethod {
        private int count = 0;

        public synchronized void increment() {
            count++;
        }

        public synchronized int getCount() {
            return count;
        }
    }

    // ===== Cách 2: synchronized block =====
    static class CounterBlock {
        private int count = 0;
        private final Object lock = new Object();

        public void increment() {
            synchronized (lock) {
                count++;
            }
        }

        public int getCount() {
            synchronized (lock) {
                return count;
            }
        }
    }

    // ===== Hàm test chung =====
    static void testSynchronizedMethod() throws InterruptedException {
        CounterMethod counter = new CounterMethod();
        runTest(counter, "Synchronized Method");
    }

    static void testSynchronizedBlock() throws InterruptedException {
        CounterBlock counter = new CounterBlock();
        runTest(counter, "Synchronized Block");
    }

    static void runTest(Object counter, String testName) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREADS);

        Runnable task = () -> {
            for (int i = 0; i < ITERATIONS; i++) {
                if (counter instanceof CounterMethod) {
                    ((CounterMethod) counter).increment();
                } else {
                    ((CounterBlock) counter).increment();
                }
            }
            latch.countDown();
        };

        long start = System.currentTimeMillis();
        for (int i = 0; i < THREADS; i++) {
            new Thread(task).start();
        }
        latch.await(); // Đợi tất cả luồng hoàn thành
        long end = System.currentTimeMillis();

        int finalValue = (counter instanceof CounterMethod)
                ? ((CounterMethod) counter).getCount()
                : ((CounterBlock) counter).getCount();

        System.out.printf("%s - Final value: %,d - Time: %,d ms%n",
                testName, finalValue, (end - start));
    }
}