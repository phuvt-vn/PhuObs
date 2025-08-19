package com.multithread2.section2ThreadCreation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main2 {

        // Mật khẩu tối đa có thể có.
        public static final int MAX_PASSWORD = 99999;

        public static void main(String[] args) {
            // Tạo một đối tượng Random để sinh số ngẫu nhiên.
            Random random = new Random();

            // Tạo một "kho báu" với mật khẩu ngẫu nhiên.
            Vault vault = new Vault(random.nextInt(MAX_PASSWORD));

            // Tạo một danh sách để chứa các thread.
            List<Thread> threads = new ArrayList<>();

            // Thêm các thread "hacker" và "cảnh sát" vào danh sách.
            threads.add(new AscendingHackerThread(vault)); // Hacker đoán mật khẩu tăng dần
            threads.add(new DescendingHackerThread(vault)); // Hacker đoán mật khẩu giảm dần
            threads.add(new PoliceThread()); // Cảnh sát đếm ngược thời gian

            // Bắt đầu tất cả các thread trong danh sách.
            for (Thread thread : threads) {
                thread.start();
            }
        }

        // Lớp Vault đại diện cho kho báu cần được mở khóa.
        private static class Vault {
            private int password;

            public Vault(int password) {
                this.password = password;
            }

            // Phương thức kiểm tra xem mật khẩu đoán có đúng không.
            public boolean isCorrectPassword(int guess) {
                try {
                    // Tạm dừng thread một chút để mô phỏng thời gian cần thiết để thử mật khẩu.
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
                return this.password == guess;
            }
        }

        // Lớp trừu tượng cho các thread hacker.
        private static abstract class HackerThread extends Thread {
            protected Vault vault;

            public HackerThread(Vault vault) {
                this.vault = vault;
                // Đặt tên cho thread dựa trên tên lớp.
                this.setName(this.getClass().getSimpleName());
                // Đặt độ ưu tiên cao cho các thread hacker.
                this.setPriority(Thread.MAX_PRIORITY);
            }

            @Override
            public void start() {
                System.out.println("Starting thread " + this.getName());
                super.start();
            }
        }

        // Thread hacker đoán mật khẩu theo thứ tự tăng dần.
        private static class AscendingHackerThread extends HackerThread {

            public AscendingHackerThread(Vault vault) {
                super(vault);
            }

            @Override
            public void run() {
                for (int guess = 0; guess < MAX_PASSWORD; guess++) {
                    if (vault.isCorrectPassword(guess)) {
                        System.out.println(this.getName() + " guessed the password " + guess);
                        // Thoát chương trình ngay khi tìm thấy mật khẩu.
                        System.exit(0);
                    }
                }
            }
        }

        // Thread hacker đoán mật khẩu theo thứ tự giảm dần.
        private static class DescendingHackerThread extends HackerThread {

            public DescendingHackerThread(Vault vault) {
                super(vault);
            }

            @Override
            public void run() {
                for (int guess = MAX_PASSWORD; guess >= 0; guess--) {
                    if (vault.isCorrectPassword(guess)) {
                        System.out.println(this.getName() + " guessed the password " + guess);
                        // Thoát chương trình ngay khi tìm thấy mật khẩu.
                        System.exit(0);
                    }
                }
            }
        }

        // Thread cảnh sát đếm ngược thời gian.
        private static class PoliceThread extends Thread {
            @Override
            public void run() {
                for (int i = 20; i > 0; i--) {
                    try {
                        // Đếm ngược mỗi nửa giây.
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    System.out.println(i);
                }

                System.out.println("Game over for you hackers");
                // Hết giờ, thoát chương trình.
                System.exit(0);
            }
        }




}