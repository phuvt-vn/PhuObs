package com.multithread2.section6ChallengeSolution;/*
 * Copyright (c) 2019-2023. Michael Pogrebinsky - Top Developer Academy
 * https://topdeveloperacademy.com
 * All rights reserved
 */

import java.util.Random;

/**
 * Ví dụ về Deadlock (Khóa chết).
 * Chương trình này mô phỏng hai đoàn tàu (TrainA, TrainB) cố gắng đi qua một giao lộ (Intersection).
 * Giao lộ có hai con đường (roadA, roadB), và mỗi con đường được bảo vệ bởi một "khóa" (lock) riêng.
 * Deadlock xảy ra khi hai (hoặc nhiều) luồng chờ đợi lẫn nhau để giải phóng tài nguyên mà chúng đang giữ,
 * tạo ra một vòng lặp chờ đợi vô tận.
 */
public class MainDeadlock {
    public static void main(String[] args) {
        // Tạo một đối tượng giao lộ duy nhất được chia sẻ.
        Intersection intersection = new Intersection();

        // Tạo hai luồng, một cho Tàu A và một cho Tàu B.
        // Cả hai luồng đều dùng chung đối tượng `intersection`.
        Thread trainAThread = new Thread(new TrainA(intersection));
        Thread trainBThread = new Thread(new TrainB(intersection));

        // Bắt đầu cả hai luồng.
        trainAThread.start();
        trainBThread.start();
    }

    /**
     * Đại diện cho Tàu B, liên tục cố gắng đi qua giao lộ bằng đường B.
     */
    public static class TrainB implements Runnable {
        private Intersection intersection;
        private Random random = new Random();

        public TrainB(Intersection intersection) {
            this.intersection = intersection;
        }

        @Override
        public void run() {
            while (true) {
                long sleepingTime = random.nextInt(5);
                try {
                    Thread.sleep(sleepingTime);
                } catch (InterruptedException e) {
                }
                // Tàu B cố gắng đi vào đường B.
                intersection.takeRoadB();
            }
        }
    }

    /**
     * Đại diện cho Tàu A, liên tục cố gắng đi qua giao lộ bằng đường A.
     */
    public static class TrainA implements Runnable {
        private Intersection intersection;
        private Random random = new Random();

        public TrainA(Intersection intersection) {
            this.intersection = intersection;
        }

        @Override
        public void run() {
            while (true) {
                long sleepingTime = random.nextInt(5);
                try {
                    Thread.sleep(sleepingTime);
                } catch (InterruptedException e) {
                }
                // Tàu A cố gắng đi vào đường A.
                intersection.takeRoadA();
            }
        }
    }

    /**
     * Lớp đại diện cho giao lộ, nơi chứa các tài nguyên (khóa) được chia sẻ.
     */
    public static class Intersection {
        // Mỗi con đường được đại diện bởi một đối tượng Object riêng biệt.
        // Những đối tượng này sẽ được sử dụng làm "khóa" (monitor lock) cho các khối synchronized.
        private Object roadA = new Object();
        private Object roadB = new Object();

        /**
         * Phương thức cho phép một đoàn tàu đi qua đường A.
         */
        public void takeRoadA() {
            // 1. Luồng (Tàu A) lấy khóa của `roadA`.
            synchronized (roadA) {
                System.out.println("Road A is locked by thread " + Thread.currentThread().getName());

                // 2. Sau khi đã khóa `roadA`, luồng này cố gắng lấy khóa của `roadB`.
                synchronized (roadB) {
                    System.out.println("Train is passing through road A");
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        /**
         * Phương thức cho phép một đoàn tàu đi qua đường B.
         * **NGUYÊN NHÂN GÂY DEADLOCK NẰM Ở ĐÂY**
         */
        public void takeRoadB() {
            // 1. Luồng (Tàu B) lấy khóa của `roadB`.
            synchronized (roadB) {
                System.out.println("Road B is locked by thread " + Thread.currentThread().getName());

                // 2. Sau khi đã khóa `roadB`, luồng này cố gắng lấy khóa của `roadA`.
                // Vấn đề: Thứ tự khóa ở đây (roadB -> roadA) ngược lại với thứ tự trong `takeRoadA` (roadA -> roadB).
                synchronized (roadA) {
                    System.out.println("Train is passing through road B");

                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        /*
         * KỊCH BẢN DEADLOCK:
         * 1. Tàu A (luồng 0) gọi `takeRoadA()` và lấy được khóa của `roadA`.
         * 2. Cùng lúc đó, Tàu B (luồng 1) gọi `takeRoadB()` và lấy được khóa của `roadB`.
         * 3. Bây giờ, Tàu A đang giữ khóa `roadA` và cố gắng lấy khóa `roadB` (bên trong `takeRoadA`).
         *    Nhưng khóa `roadB` đang bị Tàu B giữ. Vì vậy, Tàu A phải chờ.
         * 4. Đồng thời, Tàu B đang giữ khóa `roadB` và cố gắng lấy khóa `roadA` (bên trong `takeRoadB`).
         *    Nhưng khóa `roadA` đang bị Tàu A giữ. Vì vậy, Tàu B cũng phải chờ.
         * 5. Cả hai luồng đều chờ đợi nhau giải phóng khóa mà chúng cần. Không luồng nào có thể tiến triển.
         *    => DEADLOCK.
         *
         * CÁCH GIẢI QUYẾT:
         * Đảm bảo rằng tất cả các luồng khi cần lấy nhiều khóa, chúng phải lấy các khóa đó theo cùng một thứ tự.
         * Ví dụ, sửa `takeRoadB` để nó cũng khóa `roadA` trư`ớc rồi mới đến `roadB.
         */
    }
}
