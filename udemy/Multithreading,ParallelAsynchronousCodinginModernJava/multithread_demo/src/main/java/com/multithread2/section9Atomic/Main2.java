package com.multithread2.section9Atomic;/*
 * Copyright (c) 2019-2023. Michael Pogrebinsky - Top Developer Academy
 * https://topdeveloperacademy.com
 * All rights reserved
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Demo về AtomicReference và Compare-And-Set (CAS) để xây dựng cấu trúc dữ liệu Lock-Free.
 *
 * Vấn đề với Lock:
 * - Dùng `synchronized` hoặc `Lock` để bảo vệ cấu trúc dữ liệu (như Stack) là một giải pháp đơn giản nhưng có thể gây ra vấn đề về hiệu năng.
 * - Khi một luồng đang giữ lock, các luồng khác muốn truy cập phải chờ, dẫn đến tình trạng tranh chấp (contention) và giảm khả năng song song.
 *
 * Giải pháp Lock-Free (Không khóa):
 * - Sử dụng các lớp Atomic như `AtomicReference` để quản lý trạng thái mà không cần khóa.
 * - Cốt lõi của kỹ thuật này là thao tác `compareAndSet(expectedValue, newValue)` (CAS).
 * - CAS là một thao tác nguyên tử cấp phần cứng, hoạt động như sau:
 *   1. Đọc giá trị hiện tại của biến.
 *   2. So sánh nó với `expectedValue`.
 *   3. Nếu trùng khớp, cập nhật giá trị mới là `newValue` và trả về `true`.
 *   4. Nếu không trùng khớp (nghĩa là một luồng khác đã thay đổi giá trị), không làm gì cả và trả về `false`.
 * - Các luồng sẽ "lạc quan" thử thực hiện thay đổi. Nếu thất bại (do luồng khác đã nhanh tay hơn), chúng sẽ thử lại trong một vòng lặp.
 */
public class Main2 {
    public static void main(String[] args) throws InterruptedException {
        //StandardStack<Integer> stack = new StandardStack<>();
        LockFreeStack<Integer> stack = new LockFreeStack<>();
        Random random = new Random();

        for (int i = 0; i < 100000; i++) {
            stack.push(random.nextInt());
        }

        List<Thread> threads = new ArrayList<>();

        int pushingThreads = 2;
        int poppingThreads = 2;

        for (int i = 0; i < pushingThreads; i++) {
            Thread thread = new Thread(() -> {
                while (true) {
                    stack.push(random.nextInt());
                }
            });

            thread.setDaemon(true);
            threads.add(thread);
        }

        for (int i = 0; i < poppingThreads; i++) {
            Thread thread = new Thread(() -> {
                while (true) {
                    stack.pop();
                }
            });

            thread.setDaemon(true);
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        Thread.sleep(10000);

        System.out.println(String.format("%,d operations were performed in 10 seconds ", stack.getCounter()));
    }

        /**
     * `LockFreeStack` là một triển khai của cấu trúc dữ liệu Stack (ngăn xếp) mà không sử dụng `lock`.
     * Nó an toàn cho việc truy cập từ nhiều luồng (thread-safe) và có hiệu năng cao trong môi trường có độ tranh chấp cao.
     */
    public static class LockFreeStack<T> {
        // `head` là một `AtomicReference`, nó trỏ tới đỉnh của stack.
        // Việc dùng `AtomicReference` cho phép chúng ta thay đổi con trỏ `head` một cách nguyên tử.
        private AtomicReference<StackNode<T>> head = new AtomicReference<>();
        // `counter` dùng để đếm tổng số thao tác push/pop đã thực hiện thành công.
        private AtomicInteger counter = new AtomicInteger(0);

                public void push(T value) {
            StackNode<T> newHeadNode = new StackNode<>(value);

            // Vòng lặp này là trái tim của thuật toán lock-free.
            // Luồng sẽ liên tục thử cập nhật `head` cho đến khi thành công.
            while (true) {
                // 1. Đọc giá trị hiện tại của `head`.
                StackNode<T> currentHeadNode = head.get();
                // 2. Thiết lập node mới trỏ tới `head` hiện tại, chuẩn bị cho việc thêm vào đầu stack.
                newHeadNode.next = currentHeadNode;

                // 3. Thử cập nhật `head` một cách nguyên tử.
                //    - So sánh: `head` có còn là `currentHeadNode` không?
                //    - Nếu CÓ (true): Cập nhật `head` thành `newHeadNode`. Thao tác thành công, thoát vòng lặp.
                //    - Nếu KHÔNG (false): Một luồng khác đã thay đổi `head`. Vòng lặp sẽ chạy lại từ đầu.
                if (head.compareAndSet(currentHeadNode, newHeadNode)) {
                    break;
                } else {
                    // Đợi một chút trước khi thử lại để tránh busy-waiting (lãng phí CPU).
                    LockSupport.parkNanos(1);
                }
            }
            counter.incrementAndGet();
        }

                public T pop() {
            // Lấy đỉnh stack hiện tại.
            StackNode<T> currentHeadNode = head.get();
            StackNode<T> newHeadNode;

            // Vòng lặp thử lại, tương tự như push.
            // Tiếp tục lặp cho đến khi stack rỗng (`currentHeadNode` == null) hoặc pop thành công.
            while (currentHeadNode != null) {
                // 1. Node kế tiếp sẽ trở thành `head` mới sau khi pop thành công.
                newHeadNode = currentHeadNode.next;

                // 2. Thử cập nhật `head`.
                //    - So sánh: `head` có còn là `currentHeadNode` không?
                //    - Nếu CÓ (true): Cập nhật `head` thành `newHeadNode`. Pop thành công, thoát vòng lặp.
                //    - Nếu KHÔNG (false): Một luồng khác đã pop hoặc push. Cập nhật lại `currentHeadNode` và thử lại.
                if (head.compareAndSet(currentHeadNode, newHeadNode)) {
                    break;
                } else {
                    LockSupport.parkNanos(1);
                    currentHeadNode = head.get(); // Đọc lại giá trị head mới nhất để thử lại
                }
            }
            counter.incrementAndGet();
            return currentHeadNode != null ? currentHeadNode.value : null;
        }

        public int getCounter() {
            return counter.get();
        }
    }

    public static class StandardStack<T> {
        private StackNode<T> head;
        private int counter = 0;

        public synchronized void push(T value) {
            StackNode<T> newHead = new StackNode<>(value);
            newHead.next = head;
            head = newHead;
            counter++;
        }

        public synchronized T pop() {
            if (head == null) {
                counter++;
                return null;
            }

            T value = head.value;
            head = head.next;
            counter++;
            return value;
        }

        public int getCounter() {
            return counter;
        }
    }

    private static class StackNode<T> {
        public T value;
        public StackNode<T> next;

        public StackNode(T value) {
            this.value = value;
            this.next = next;
        }
    }
}
