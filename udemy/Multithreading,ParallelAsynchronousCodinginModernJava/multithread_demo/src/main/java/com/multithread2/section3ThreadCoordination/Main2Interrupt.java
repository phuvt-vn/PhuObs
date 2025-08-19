package com.multithread2.section3ThreadCoordination;

import java.math.BigInteger;

/**
 * Ví dụ này minh họa cách dừng một thread đang thực hiện một tác vụ tính toán dài (không phải tác vụ blocking)
 * bằng cách sử dụng cơ chế ngắt (interruption).
 * Chủ đề: Dừng Thread (Thread Termination) & Thread Daemon.
 */
public class Main2Interrupt  {

    public static void main(String[] args) {
        // Tạo một thread để thực hiện một tác vụ tính toán rất lớn (tính lũy thừa của số lớn).
        Thread thread = new Thread(new LongComputationTask(new BigInteger("200000"), new BigInteger("100000000")));

        // Bắt đầu thread.
        thread.start();
        // Ngay sau khi bắt đầu, chúng ta gọi `interrupt()` trên thread này.
        // Lệnh này sẽ thiết lập "cờ ngắt" (interrupt flag) của thread thành true.
        // Nó không thực sự dừng thread ngay lập tức, mà chỉ là một yêu cầu dừng.
        thread.interrupt();
    }

    /**
     * Một tác vụ Runnable mô phỏng một phép tính toán kéo dài.
     */
    private static class LongComputationTask implements Runnable {
        private BigInteger base;
        private BigInteger power;

        public LongComputationTask(BigInteger base, BigInteger power) {
            this.base = base;
            this.power = power;
        }

        @Override
        public void run() {
            System.out.println(base + "^" + power + " = " + pow(base, power));
        }

        /**
         * Phương thức tính `base` lũy thừa `power`.
         * Quan trọng là nó phải kiểm tra trạng thái ngắt một cách định kỳ.
         */
        private BigInteger pow(BigInteger base, BigInteger power) {
            BigInteger result = BigInteger.ONE;

            // Vòng lặp để thực hiện phép tính.
            for (BigInteger i = BigInteger.ZERO; i.compareTo(power) != 0; i = i.add(BigInteger.ONE)) {
                // **ĐIỂM MẤU CHỐT:** Kiểm tra cờ ngắt trong mỗi vòng lặp.
                // Nếu `Thread.currentThread().isInterrupted()` trả về true, có nghĩa là
                // một thread khác đã yêu cầu dừng thread này.
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Việc tính toán đã bị ngắt giữa chừng.");
                    // Dọn dẹp và thoát khỏi phương thức một cách nhẹ nhàng.
                    return BigInteger.ZERO;
                }
                result = result.multiply(base);
            }

            return result;
        }
    }
}