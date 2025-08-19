package com.multithread2.section8InterThreadCommunication;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Demo cơ bản về Semaphore ("vé" – permit) để GIỚI HẠN mức độ song song.
 * 
 * Kỹ thuật chính:
 * - Khởi tạo: new Semaphore(permits, fair)
 * - acquire()/release() luôn đi theo cặp; nên đặt release trong finally để tránh "rò vé".
 * - acquire(n)/release(n): xin/trả nhiều vé cùng lúc.
 * - fair=true giúp hạn chế chen lấn (ưu tiên thread chờ lâu hơn).
 */
public class Step01_SemaphoreBasicsDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Semaphore cơ bản: giới hạn mức độ song song ===");

        // Mô phỏng bãi đỗ có 3 chỗ -> tối đa 3 thread vào vùng tài nguyên cùng lúc
        final int permits = 3;
        Semaphore parking = new Semaphore(permits, true); // fair=true để tránh starvation

        Runnable carTask = () -> {
            String name = Thread.currentThread().getName();
            try {
                log(name + " xin vé đỗ (acquire)...");
                parking.acquire(); // nếu hết chỗ -> chờ (block)
                log(name + " đã vào bãi. Permits còn: " + parking.availablePermits());

                // Giả lập sử dụng tài nguyên (đỗ xe) trong 300~600ms
                Thread.sleep(300 + ThreadLocalRandom.current().nextInt(300));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // bảo toàn cờ interrupt
            } finally {
                parking.release(); // TRẢ VÉ trong finally để không bị rò
                log(name + " rời bãi (release). Permits còn: " + parking.availablePermits());
            }
        };

        Thread[] cars = new Thread[6];
        for (int i = 0; i < cars.length; i++) {
            cars[i] = new Thread(carTask, "Car-" + (i + 1));
            cars[i].start();
        }
        // Chờ tất cả xe xong
        for (Thread t : cars) t.join();

        System.out.println("\n=== acquire(n) / release(n) (xin/trả nhiều vé) ===");
        Semaphore sem = new Semaphore(2, true);
        log("Main thử acquire(2)...");
        sem.acquire(2); // xin cùng lúc 2 vé => không còn vé cho thread khác
        try {
            log("Giữ 2 permit để thực hiện tác vụ lớn trong 200ms...");
            Thread.sleep(200);
        } finally {
            sem.release(2); // trả lại cả 2 vé
            log("Trả lại 2 permit.");
        }

        System.out.println("\nHoàn tất demo Semaphore cơ bản.");
    }


    private static void log(String msg) {
        System.out.printf("[%s][%d] %s%n", Thread.currentThread().getName(),
                System.nanoTime(), msg);
    }
}
