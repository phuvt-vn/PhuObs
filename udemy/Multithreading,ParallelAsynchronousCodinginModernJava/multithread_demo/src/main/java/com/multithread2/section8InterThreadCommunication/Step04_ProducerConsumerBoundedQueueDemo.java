package com.multithread2.section8InterThreadCommunication;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Producer–Consumer tổng quát N–M với hàng đợi giới hạn K.
 *
 * Kỹ thuật:
 * - Semaphore `slotsAvailable` (đếm ô trống) khởi tạo với K, `itemsAvailable` (đếm sản phẩm) khởi tạo với 0.
 * - `ReentrantLock` được dùng để bảo vệ queue, đảm bảo chỉ một thread được thêm/bớt phần tử tại một thời điểm.
 * - Luồng Producer: acquire(`slotsAvailable`) -> lock -> enqueue -> unlock -> release(`itemsAvailable`)
 * - Luồng Consumer: acquire(`itemsAvailable`) -> lock -> dequeue -> unlock -> release(`slotsAvailable`)
 *
 * Tác dụng:
 * - Back-pressure: khi đầy K, producer phải chờ -> không tràn bộ nhớ
 * - Cho phép nhiều producer/consumer chạy an toàn
 */
public class Step04_ProducerConsumerBoundedQueueDemo {

    // Hàng đợi chia sẻ và dung lượng
    private final Queue<String> queue = new ArrayDeque<>();
    private final int capacity;

    // `slotsAvailable` đếm số ô trống trong queue. Producer phải `acquire` nó trước khi sản xuất.
    private final Semaphore slotsAvailable;

    // `itemsAvailable` đếm số sản phẩm có sẵn trong queue. Consumer phải `acquire` nó trước khi tiêu thụ.
    private final Semaphore itemsAvailable;

    // Lock được dùng để bảo vệ các thao tác trên `queue` (offer, poll, size).
    // Bất cứ khi nào truy cập vào queue, thread phải giữ lock này để tránh race condition.
    private final ReentrantLock lock = new ReentrantLock(true); // fair=true để hạn chế starvation

    public Step04_ProducerConsumerBoundedQueueDemo(int capacity) {
        this.capacity = capacity;
        this.slotsAvailable = new Semaphore(capacity, true); // Ban đầu có `capacity` ô trống
        this.itemsAvailable = new Semaphore(0, true);      // Ban đầu chưa có sản phẩm nào
    }

    public void produce(String x) throws InterruptedException {
        // Bước 1: Xin một "vé" ô trống. Nếu queue đã đầy (slotsAvailable=0), thread sẽ bị chặn ở đây.
        // Việc này giúp giảm tải cho CPU (không bận chờ) và tạo ra back-pressure tự nhiên.
        slotsAvailable.acquire();

        // Bước 2: Sau khi chắc chắn có chỗ trống, lấy lock để thao tác với queue.
        // Đây là bước bắt buộc để đảm bảo tính toàn vẹn của queue khi có nhiều Producer cùng chạy.
        lock.lock();
        try {
            queue.offer(x);
            log("Produce: " + x + " (size=" + queue.size() + "/" + capacity + ")");
        } finally {
            // Bước 3: Luôn nhả lock trong finally để tránh deadlock.
            lock.unlock();
        }

        // Bước 4: Sau khi đã thêm sản phẩm thành công, báo hiệu rằng đã có một sản phẩm mới.
        // Tín hiệu này sẽ đánh thức một Consumer đang chờ (nếu có).
        itemsAvailable.release();
    }

    public String consume() throws InterruptedException {
        // Bước 1: Xin một "vé" sản phẩm. Nếu queue rỗng (itemsAvailable=0), thread sẽ bị chặn ở đây.
        itemsAvailable.acquire();

        // Bước 2: Sau khi chắc chắn có sản phẩm, lấy lock để thao tác với queue.
        lock.lock();
        String x;
        try {
            x = queue.poll();
            log("Consume: " + x + " (size=" + queue.size() + "/" + capacity + ")");
        } finally {
            // Bước 3: Luôn nhả lock trong finally.
            lock.unlock();
        }

        // Bước 4: Sau khi đã lấy sản phẩm thành công, báo hiệu rằng đã có một ô trống mới.
        // Tín hiệu này sẽ đánh thức một Producer đang chờ (nếu có).
        slotsAvailable.release();
        return x;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Producer–Consumer N–M với queue giới hạn ===");
        int capacity = 3;     // Kích thước queue
        int producers = 2;    // Số producer
        int consumers = 3;    // Số consumer
        int itemsPerProducer = 5; // Mỗi producer tạo 5 item

        Step04_ProducerConsumerBoundedQueueDemo demo = new Step04_ProducerConsumerBoundedQueueDemo(capacity);

        // Tạo producers
        Thread[] pThreads = new Thread[producers];
        for (int p = 0; p < producers; p++) {
            final int id = p + 1;
            pThreads[p] = new Thread(() -> {
                try {
                    for (int i = 1; i <= itemsPerProducer; i++) {
                        Thread.sleep(50 + ThreadLocalRandom.current().nextInt(120));
                        demo.produce("P" + id + "-Item" + i);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Producer-" + id);
            pThreads[p].start();
        }

        // Tạo consumers
        Thread[] cThreads = new Thread[consumers];
        for (int c = 0; c < consumers; c++) {
            final int id = c + 1;
            cThreads[c] = new Thread(() -> {
                try {
                    // Tổng số item = producers * itemsPerProducer, chia tương đối giữa consumers
                    // Ở đây để demo đơn giản, mỗi consumer sẽ tiêu thụ cho tới khi các producer xong
                    int consumed = 0;
                    while (!allFinished(pThreads) || demo.peekSize() > 0) {
                        demo.consume();
                        consumed++;
                        Thread.sleep(80 + ThreadLocalRandom.current().nextInt(150));
                    }
                    log("Consumer-" + id + " done. Total consumed: " + consumed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Consumer-" + id);
            cThreads[c].start();
        }

        // Chờ producers xong
        for (Thread t : pThreads) t.join();

        // Đợi queue được tiêu hết
        while (demo.peekSize() > 0) {
            Thread.sleep(50);
        }

        // Cho consumers thêm thời gian thoát vòng while
        Thread.sleep(200);
        for (Thread t : cThreads) t.interrupt(); // ngắt nhẹ để thoát nếu còn chờ
        for (Thread t : cThreads) t.join();

        System.out.println("Hoàn tất demo N–M.");
    }

    // Hỗ trợ: xem nhanh kích thước queue (chỉ để demo/log)
    private int peekSize() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    private static boolean allFinished(Thread[] arr) {
        for (Thread t : arr) if (t.isAlive()) return false;
        return true;
    }

    private static void log(String msg) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), msg);
    }
}
