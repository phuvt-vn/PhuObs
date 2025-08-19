package com.multithread2.section8InterThreadCommunication;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Producer–Consumer 1–1 dùng 2 semaphore và 1 biến chia sẻ duy nhất (1 ô chứa).
 *
 * Ý tưởng:
 * - itemsAvailable = 0: ban đầu chưa có hàng -> consumer phải chờ
 * - slotsAvailable = 1: có 1 ô trống -> producer được phép sản xuất 1 món
 * - item: biến chia sẻ (volatile) để các thread nhìn thấy giá trị mới
 * Luồng:
 * - Consumer: acquire(itemsAvailable) -> lấy hàng -> release(slotsAvailable)
 * - Producer: acquire(slotsAvailable) -> đặt hàng -> release(itemsAvailable)
 *
 * Ưu điểm: Không bận chờ (không tốn CPU), Producer không thể vượt mặt Consumer.
 */
public class Step03_ProducerConsumerOneItemDemo {

    // 2 semaphore tín hiệu
    // `itemsAvailable` đếm số sản phẩm đang có trong kho (ban đầu là 0).
    private final Semaphore itemsAvailable = new Semaphore(0, true);

    // `slotsAvailable` đếm số ô trống trong kho (ban đầu là 1).
    private final Semaphore slotsAvailable = new Semaphore(1, true);

    // Ô chứa duy nhất
    private volatile String item = null;

    private void produce(String x) throws InterruptedException {
        slotsAvailable.acquire(); // Chờ đến khi có ô trống
        try {
            // Đặt hàng vào ô chứa
            item = x;
            log("Producer đặt: " + x);
        } finally {
            // Báo hiệu đã có sản phẩm mới
            itemsAvailable.release();
        }
    }

    private String consume() throws InterruptedException {
        itemsAvailable.acquire(); // Chờ đến khi có sản phẩm
        try {
            String x = item;
            log("Consumer lấy: " + x);
            return x;
        } finally {
            // Dọn ô chứa, báo hiệu đã có ô trống mới
            item = null;
            slotsAvailable.release();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Producer–Consumer 1–1 (1 ô chứa) ===");
        Step03_ProducerConsumerOneItemDemo demo = new Step03_ProducerConsumerOneItemDemo();

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    String x = demo.consume();
                    // Giả lập xử lý
                    Thread.sleep(100 + ThreadLocalRandom.current().nextInt(150));
                    log("Consumer xử lý xong: " + x);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    Thread.sleep(50 + ThreadLocalRandom.current().nextInt(100));
                    demo.produce("Item-" + i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        consumer.start();
        producer.start();
        consumer.join();
        producer.join();

        System.out.println("Hoàn tất demo Producer–Consumer 1–1.");
    }

    private static void log(String msg) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), msg);
    }
}
