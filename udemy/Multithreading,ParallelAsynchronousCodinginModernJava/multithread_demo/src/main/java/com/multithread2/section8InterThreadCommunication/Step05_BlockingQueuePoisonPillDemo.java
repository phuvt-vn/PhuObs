package com.multithread2.section8InterThreadCommunication;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Demo Producer–Consumer dùng BlockingQueue + "poison pill" để dừng consumer an toàn.
 *
 * Kỹ thuật:
 * - Thay vì tự điều phối bằng Semaphore + Lock, dùng thẳng BlockingQueue (đã tích hợp chặn khi đầy/rỗng).
 * - Khi tất cả producers kết thúc, main sẽ enqueue đúng bằng số consumers một phần tử đặc biệt (POISON).
 * - Consumer lấy được POISON thì thoát vòng và kết thúc thread.
 *
 * Ưu điểm so với phiên bản Semaphore + Lock:
 * - Ít mã điều phối hơn -> ít lỗi (không lo rò release/acquire, quên unlock,...)
 * - Độ rõ ràng cao: put/take chặn sẵn, fair queue có sẵn (ArrayBlockingQueue(capacity, fair)).
 * - Bảo trì dễ hơn, phù hợp thực tế.
 */
public class Step05_BlockingQueuePoisonPillDemo {

    private static final String POISON = "__POISON__"; // sentinel dừng consumer (đảm bảo không trùng item thật)

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== BlockingQueue + Poison Pill (N–M) ===");

        final int capacity = 3;     // kích thước queue
        final int producers = 2;    // số producer
        final int consumers = 3;    // số consumer
        final int itemsPerProducer = 5; // mỗi producer tạo 5 item

        // Fair=true để hạn chế starvation khi cạnh tranh nặng
        BlockingQueue<String> q = new ArrayBlockingQueue<>(capacity, true);

        // Tạo và chạy producers
        Thread[] pThreads = new Thread[producers];
        for (int p = 0; p < producers; p++) {
            final int id = p + 1;
            pThreads[p] = new Thread(() -> {
                try {
                    for (int i = 1; i <= itemsPerProducer; i++) {
                        Thread.sleep(50 + ThreadLocalRandom.current().nextInt(120));
                        String item = "P" + id + "-Item" + i;
                        q.put(item); // chặn nếu đầy
                        log("Produce: " + item + " (size=" + q.size() + "/" + capacity + ")");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Producer-" + id);
            pThreads[p].start();
        }

        // Tạo và chạy consumers
        Thread[] cThreads = new Thread[consumers];
        for (int c = 0; c < consumers; c++) {
            final int id = c + 1;
            cThreads[c] = new Thread(() -> {
                int consumed = 0;
                try {
                    while (true) {
                        String x = q.take(); // chặn nếu rỗng
                        if (POISON.equals(x)) {
                            log("Consumer-" + id + " nhận POISON -> thoát");
                            break; // kết thúc
                        }
                        consumed++;
                        log("Consume: " + x + " (size=" + q.size() + "/" + capacity + ")");
                        Thread.sleep(80 + ThreadLocalRandom.current().nextInt(150));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    log("Consumer-" + id + " done. Total consumed: " + consumed);
                }
            }, "Consumer-" + id);
            cThreads[c].start();
        }

        // Chờ producers xong
        for (Thread t : pThreads) t.join();

        // Enqueue đúng bằng số consumers "poison pill" để tất cả consumers đều nhận được và thoát
        for (int i = 0; i < consumers; i++) {
            q.put(POISON);
        }

        // Chờ consumers kết thúc
        for (Thread t : cThreads) t.join();

        System.out.println("Hoàn tất demo BlockingQueue + Poison Pill.");
    }

    private static void log(String msg) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), msg);
    }
}
