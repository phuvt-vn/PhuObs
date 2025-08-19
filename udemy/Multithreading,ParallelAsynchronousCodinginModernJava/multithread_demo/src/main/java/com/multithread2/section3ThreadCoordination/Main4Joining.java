package com.multithread2.section3ThreadCoordination;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Ví dụ này minh họa cách sử dụng phương thức `thread.join()`.
 * `join()` là một cơ chế cho phép một thread chờ một thread khác kết thúc công việc của nó.
 * Chủ đề: Nối Thread (Joining Threads).
 */
public class Main4Joining {
    public static void main(String[] args) throws InterruptedException {
        // Danh sách các số đầu vào để tính giai thừa.
        // Số đầu tiên rất lớn để đảm bảo việc tính toán mất nhiều thời gian.
        List<Long> inputNumbers = Arrays.asList(100000000L, 3435L, 35435L, 2324L, 4656L, 23L, 5556L);

        // Tạo một danh sách để chứa các thread tính giai thừa.
        List<FactorialThread> threads = new ArrayList<>();

        // Với mỗi số đầu vào, tạo một FactorialThread tương ứng.
        for (long inputNumber : inputNumbers) {
            threads.add(new FactorialThread(inputNumber));
        }

        // Bắt đầu tất cả các thread.
        for (Thread thread : threads) {
            // Đặt các thread này thành daemon. Nếu thread main kết thúc sớm,
            // các thread tính toán này sẽ không giữ chương trình chạy.
            thread.setDaemon(true);
            thread.start();
        }

        // **ĐIỂM MẤU CHỐT:** Chờ các thread kết thúc bằng `join()`.
        // Lặp qua từng thread và gọi `join()` trên nó.
        for (Thread thread : threads) {
            // `thread.join(2000)` sẽ làm cho thread `main` (thread đang chạy vòng lặp này)
            // phải chờ thread con (`thread`) kết thúc.
            // Tuy nhiên, nó chỉ chờ tối đa 2000 mili giây (2 giây).
            // Nếu thread con không kết thúc trong 2 giây, thread `main` sẽ tiếp tục chạy.
            // Nếu không có tham số, `join()` sẽ chờ vô thời hạn.
            thread.join(2000);
        }

        // Sau khi đã chờ (hoặc hết thời gian chờ), kiểm tra kết quả.
        for (int i = 0; i < inputNumbers.size(); i++) {
            FactorialThread factorialThread = threads.get(i);
            if (factorialThread.isFinished()) {
                // Nếu thread đã hoàn thành, in kết quả.
                System.out.println("Giai thừa của " + inputNumbers.get(i) + " là " + factorialThread.getResult());
            } else {
                // Nếu thread chưa hoàn thành (do hết thời gian chờ của join),
                // thông báo rằng việc tính toán vẫn đang diễn ra.
                System.out.println("Việc tính toán cho " + inputNumbers.get(i) + " vẫn đang được xử lý");
            }
        }
    }

    /**
     * Một lớp Thread tùy chỉnh để tính giai thừa của một số.
     */
    public static class FactorialThread extends Thread {
        private long inputNumber;
        private BigInteger result = BigInteger.ZERO;
        private boolean isFinished = false;

        public FactorialThread(long inputNumber) {
            this.inputNumber = inputNumber;
        }

        @Override
        public void run() {
            this.result = factorial(inputNumber);
            // Đặt cờ `isFinished` thành true sau khi tính toán xong.
            // Đây là một cách đơn giản để các thread khác biết được trạng thái của thread này.
            this.isFinished = true;
        }

        /**
         * Phương thức tính giai thừa.
         */
        public BigInteger factorial(long n) {
            BigInteger tempResult = BigInteger.ONE;

            for (long i = n; i > 0; i--) {
                tempResult = tempResult.multiply(new BigInteger((Long.toString(i))));
            }
            return tempResult;
        }

        public BigInteger getResult() {
            return result;
        }

        public boolean isFinished() {
            return isFinished;
        }
    }
}