package com.multithread2.section3ThreadCoordination;

import java.math.BigInteger;

/**
 * Ví dụ này minh họa về Thread Daemon.
 * Thread Daemon là một thread chạy ở chế độ nền và sẽ không ngăn cản chương trình kết thúc.
 * Nếu tất cả các thread không phải daemon (non-daemon/user threads) đã hoàn thành,
 * JVM sẽ tự động dừng các thread daemon còn lại và thoát.
 * Chủ đề: Dừng Thread (Thread Termination) & Thread Daemon.
 */
public class Main3Daemon {

    public static void main(String[] args) throws InterruptedException {
        // Tạo một thread để thực hiện một tác vụ tính toán dài.
        Thread thread = new Thread(new LongComputationTask(new BigInteger("200000"), new BigInteger("100000000")));

        // **ĐIỂM MẤU CHỐT:** Thiết lập thread này thành một thread daemon.
        // Lời gọi này phải được thực hiện trước khi thread bắt đầu (trước `thread.start()`).
        thread.setDaemon(true);
        
        // Bắt đầu thread daemon.
        thread.start();
        
        // Tạm dừng thread `main` trong 100 mili giây.
        // Mục đích là để cho thread daemon có một chút thời gian để chạy.
        Thread.sleep(100);
        
        // Sau 100ms, thread `main` (là một user thread) sẽ kết thúc.
        // Vì không còn user thread nào khác đang chạy, JVM sẽ thoát.
        // Do đó, thread daemon tính toán sẽ bị dừng đột ngột, bất kể nó đã tính toán xong hay chưa.
        // Lưu ý: ví dụ này không gọi `interrupt()` nữa, mà dựa vào tính chất của daemon thread để dừng.
    }

    /**
     * Một tác vụ tính toán dài, tương tự như ví dụ trước,
     * nhưng lần này nó không kiểm tra cờ ngắt.
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

        private BigInteger pow(BigInteger base, BigInteger power) {
            BigInteger result = BigInteger.ONE;

            for (BigInteger i = BigInteger.ZERO; i.compareTo(power) != 0; i = i.add(BigInteger.ONE)) {
                result = result.multiply(base);
            }

            return result;
        }
    }
}