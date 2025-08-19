package com.multithread2.section6ChallengeSolution;/*
 * Copyright (c) 2019-2023. Michael Pogrebinsky - Top Developer Academy
 * https://topdeveloperacademy.com
 * All rights reserved
 */

import java.util.Random;

/**
 * Ví dụ thực tế về Thao tác nguyên tử (Atomic Operations), từ khóa Volatile & Metrics.
 * Chương trình này mô phỏng một hệ thống nơi nhiều luồng (BusinessLogic)
 * thực hiện công việc và cập nhật một đối tượng số liệu (Metrics) được chia sẻ,
 * trong khi một luồng khác (MetricsPrinter) đọc và in các số liệu đó.
 *
 * Mục tiêu là để minh họa cách sử dụng `volatile` để đảm bảo tính nhất quán hiển thị (visibility)
 * và `synchronized` để đảm bảo các thao tác phức hợp được thực hiện một cách an toàn (thread-safety).
 */
public class MainVolatile {
    public static void main(String[] args) {
        // Tạo một đối tượng Metrics duy nhất. Đối tượng này sẽ được chia sẻ giữa tất cả các luồng.
        Metrics metrics = new Metrics();

        // Tạo hai luồng logic nghiệp vụ. Cả hai luồng này sẽ cùng cập nhật đối tượng `metrics`.
        BusinessLogic businessLogicThread1 = new BusinessLogic(metrics);
        BusinessLogic businessLogicThread2 = new BusinessLogic(metrics);

        // Tạo một luồng chuyên để in giá trị trung bình từ đối tượng `metrics`.
        MetricsPrinter metricsPrinter = new MetricsPrinter(metrics);

        // Khởi chạy tất cả các luồng để chúng chạy đồng thời.
        businessLogicThread1.start();
        businessLogicThread2.start();
        metricsPrinter.start();
    }

    /**
     * Luồng này chịu trách nhiệm đọc và in giá trị trung bình từ đối tượng Metrics một cách liên tục.
     */
    public static class MetricsPrinter extends Thread {
        private Metrics metrics;

        public MetricsPrinter(Metrics metrics) {
            this.metrics = metrics;
        }

        @Override
        public void run() {
            // Vòng lặp vô hạn để liên tục theo dõi và in số liệu.
            while (true) {
                try {
                    // Tạm dừng một chút để không làm quá tải CPU.
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }

                // Lấy giá trị trung bình hiện tại từ đối tượng metrics.
                // Vì biến 'average' trong Metrics là 'volatile', luồng này đảm bảo sẽ đọc được
                // giá trị mới nhất do các luồng BusinessLogic ghi vào.
                double currentAverage = metrics.getAverage();

                System.out.println("Current Average is " + currentAverage);
            }
        }
    }

    /**
     * Luồng này mô phỏng việc thực hiện một tác vụ nghiệp vụ.
     * Sau mỗi tác vụ, nó sẽ cập nhật đối tượng Metrics với thời gian thực thi.
     */
    public static class BusinessLogic extends Thread {
        private Metrics metrics;
        private Random random = new Random();

        public BusinessLogic(Metrics metrics) {
            this.metrics = metrics;
        }

        @Override
        public void run() {
            // Vòng lặp vô hạn để liên tục thực hiện công việc.
            while (true) {
                long start = System.currentTimeMillis();

                try {
                    // Tạm dừng luồng trong một khoảng thời gian ngẫu nhiên (0 hoặc 1 ms)
                    // để mô phỏng thời gian xử lý công việc.
                    Thread.sleep(random.nextInt(2));
                } catch (InterruptedException e) {
                }

                long end = System.currentTimeMillis();

                // Thêm mẫu thời gian (thời gian thực thi tác vụ) vào đối tượng metrics.
                metrics.addSample(end - start);
            }
        }
    }

    /**
     * Lớp này chứa dữ liệu số liệu và các phương thức để thao tác với nó.
     * Đây là tài nguyên được chia sẻ (shared resource) giữa các luồng.
     */
    public static class Metrics {
        private long count = 0;
        // Từ khóa 'volatile' đảm bảo rằng mọi thay đổi đối với biến 'average'
        // từ một luồng sẽ ngay lập tức được các luồng khác nhìn thấy.
        // Nó ngăn chặn các vấn đề về việc luồng đọc phải giá trị cũ (stale value) từ cache của CPU.
        private volatile double average = 0.0;

        /**
         * Thêm một mẫu thời gian mới và tính toán lại giá trị trung bình.
         * Phương thức này được đánh dấu là 'synchronized' để ngăn chặn "race condition".
         * Nếu không có 'synchronized', hai luồng BusinessLogic có thể gọi phương thức này cùng lúc,
         * dẫn đến việc tính toán sai `count` và `average`.
         * 'synchronized' đảm bảo rằng chỉ một luồng có thể thực thi mã bên trong phương thức này tại một thời điểm.
         */
        public synchronized void addSample(long sample) {
            // Chuỗi thao tác đọc-sửa-ghi (read-modify-write) này không phải là thao tác nguyên tử,
            // do đó cần phải được bảo vệ trong một khối synchronized.
            double currentSum = average * count;
            count++;
            average = (currentSum + sample) / count;
        }

        /**
         * Trả về giá trị trung bình hiện tại.
         * Không cần 'synchronized' ở đây vì việc đọc một biến 'volatile' đã là một thao tác an toàn
         * và chúng ta chỉ muốn lấy giá trị mới nhất mà không cần khóa đối tượng.
         */
        public double getAverage() {
            return average;
        }
    }
}

