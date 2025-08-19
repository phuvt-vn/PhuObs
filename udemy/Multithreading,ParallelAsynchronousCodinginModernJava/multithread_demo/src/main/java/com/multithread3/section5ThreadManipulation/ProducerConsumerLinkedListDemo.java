package com.multithread3.section5ThreadManipulation;

import java.util.LinkedList;

public class ProducerConsumerLinkedListDemo {
    private final LinkedList<Integer> queue = new LinkedList<>();
    private final int capacity = 5;
    private final Object lock = new Object();

    // Producer thêm phần tử khi chưa đầy
    public void put(int value) throws InterruptedException {
        synchronized (lock) {
            // DÙNG while (KHÔNG DÙNG if): chờ đến khi còn chỗ trống
            while (queue.size() == capacity) {
                lock.wait(); // nhả lock và chờ notify
            }
            queue.addLast(value);
            System.out.println("Produced: " + value + " | size=" + queue.size());
            // Với 1 producer/consumer, notify() là đủ; notifyAll() an toàn hơn nếu mở rộng
            lock.notifyAll();
        }
    }

    // Consumer lấy phần tử khi không rỗng
    public int take() throws InterruptedException {
        synchronized (lock) {
            // DÙNG while: chờ đến khi có dữ liệu
            while (queue.isEmpty()) {
                lock.wait(); // nhả lock và chờ notify
            }
            int v = queue.removeFirst();
            System.out.println("Consumed: " + v + " | size=" + queue.size());
            lock.notifyAll();
            return v;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ProducerConsumerLinkedListDemo pc = new ProducerConsumerLinkedListDemo();
        final int total = 10;

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= total; i++) {
                try {
                    pc.put(i);
                    Thread.sleep(100); // mô phỏng thời gian sản xuất
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= total; i++) {
                try {
                    pc.take();
                    Thread.sleep(150); // mô phỏng thời gian tiêu thụ
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("Done");
    }
}