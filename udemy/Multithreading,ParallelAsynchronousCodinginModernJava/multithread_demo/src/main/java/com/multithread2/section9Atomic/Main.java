package com.multithread2.section9Atomic;/*
 * Copyright (c) 2019-2023. Michael Pogrebinsky - Top Developer Academy
 * https://topdeveloperacademy.com
 * All rights reserved
 */

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demo về AtomicInteger: Giải pháp cho Race Condition không cần Lock.
 *
 * Vấn đề:
 * Khi nhiều luồng cùng lúc sửa đổi một biến chung (ví dụ: `int counter`), có thể xảy ra "race condition".
 * Thao tác `counter++` hoặc `counter--` không phải là một hành động đơn lẻ mà gồm 3 bước: đọc giá trị, thay đổi giá trị, ghi lại giá trị.
 * Nếu luồng A đọc giá trị, sau đó luồng B cũng đọc giá trị (trước khi luồng A ghi lại), cả hai sẽ cùng thấy giá trị cũ và kết quả cuối cùng sẽ bị sai.
 *
 * Giải pháp:
 * `java.util.concurrent.atomic.AtomicInteger` cung cấp các thao tác (như tăng, giảm) được đảm bảo là "nguyên tử" (atomic).
 * Một thao tác nguyên tử diễn ra như một hành động duy nhất, không thể bị chia cắt, do đó không bị các luồng khác xen vào.
 * Nó thường sử dụng cơ chế phần cứng gọi là Compare-And-Swap (CAS), hiệu quả hơn việc dùng `synchronized` hoặc `Lock` vì nó tránh được việc tạm dừng các luồng (thread suspension).
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Tạo một đối tượng đếm tồn kho, đây là tài nguyên sẽ được chia sẻ giữa các luồng.
        InventoryCounter inventoryCounter = new InventoryCounter();

        // Tạo một luồng để tăng số lượng tồn kho.
        IncrementingThread incrementingThread = new IncrementingThread(inventoryCounter);
        // Tạo một luồng để giảm số lượng tồn kho.
        DecrementingThread decrementingThread = new DecrementingThread(inventoryCounter);

        // Khởi chạy cả hai luồng. Từ thời điểm này, chúng sẽ chạy song song
        // và cùng lúc truy cập vào đối tượng `inventoryCounter`.
        incrementingThread.start();
        decrementingThread.start();

        // Yêu cầu luồng chính (main) chờ cho đến khi luồng `incrementingThread` hoàn thành.
        incrementingThread.join();
        // Tương tự, chờ cho luồng `decrementingThread` hoàn thành.
        // Lệnh `join()` rất quan trọng để đảm bảo chúng ta đọc kết quả cuối cùng sau khi tất cả các thao tác đã xong.
        decrementingThread.join();

        System.out.println("We currently have " + inventoryCounter.getItems() + " items");
    }

    public static class DecrementingThread extends Thread {

        private InventoryCounter inventoryCounter;

        public DecrementingThread(InventoryCounter inventoryCounter) {
            this.inventoryCounter = inventoryCounter;
        }

        @Override
        public void run() {
            for (int i = 0; i < 10000; i++) {
                inventoryCounter.decrement();
            }
        }
    }

    public static class IncrementingThread extends Thread {

        private InventoryCounter inventoryCounter;

        public IncrementingThread(InventoryCounter inventoryCounter) {
            this.inventoryCounter = inventoryCounter;
        }

        @Override
        public void run() {
            for (int i = 0; i < 10000; i++) {
                inventoryCounter.increment();
            }
        }
    }

    /**
     * Lớp `InventoryCounter` quản lý số lượng hàng tồn kho.
     * Đây là đối tượng chia sẻ (shared object) mà nhiều luồng sẽ cùng truy cập.
     */
    private static class InventoryCounter {
        // Sử dụng AtomicInteger thay vì `int` đơn thuần để tránh race condition.
        // Mọi thao tác trên `AtomicInteger` đều là thread-safe.
        private AtomicInteger items = new AtomicInteger(0);

        // `incrementAndGet()` là một thao tác nguyên tử.
        // Nó sẽ tăng giá trị lên 1 và trả về giá trị mới, tất cả trong một bước duy nhất.
        public void increment() {
            items.incrementAndGet();
        }

        // `decrementAndGet()` cũng là một thao tác nguyên tử.
        public void decrement() {
            items.decrementAndGet();
        }

        // `get()` trả về giá trị hiện tại một cách an toàn.
        public int getItems() {
            return items.get();
        }
    }
}
