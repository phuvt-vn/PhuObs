package com.multithread3.section7MultithreadConcept;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Deadlock {

    // Tạo 2 khóa (lock) riêng biệt
    // true = fair lock: thread đợi lâu nhất sẽ được ưu tiên
    private Lock lock1 = new ReentrantLock(true);
    private Lock lock2 = new ReentrantLock(true);

    public static void main(String[] args) {
        // Tạo đối tượng Deadlock
        Deadlock deadlock = new Deadlock();

        // Tạo và khởi động 2 thread đồng thời
        // Thread 1: chạy phương thức worker1()
        new Thread(deadlock::worker1, "worker1").start();

        // Thread 2: chạy phương thức worker2()
        // LƯU Ý: Tên thread nên là "worker2" chứ không phải "worker1"
        new Thread(deadlock::worker2, "worker2").start();
    }

    public void worker1() {
        // === BƯỚC 1: Worker1 lấy lock1 ===
        lock1.lock();
        System.out.println("Worker1 acquires the lock1...");

        try {
            // === BƯỚC 2: Worker1 ngủ 300ms ===
            // Trong lúc này Worker2 có thể chạy và lấy lock2
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // === BƯỚC 3: Worker1 cố gắng lấy lock2 ===
        // ⚠️ NGUY HIỂM: Nếu Worker2 đã lấy lock2 thì Worker1 sẽ bị treo ở đây!
        lock2.lock();
        System.out.println("Worker1 acquires the lock2...");

        // === BƯỚC 4: Giải phóng các lock ===
        // Chỉ chạy được đến đây nếu không bị deadlock
        lock1.unlock();
        lock2.unlock();
        System.out.println("Worker1 đã hoàn thành công việc");
    }

    public void worker2() {
        // === BƯỚC 1: Worker2 lấy lock2 ===
        lock2.lock();
        System.out.println("Worker2 acquires the lock2...");

        try {
            // === BƯỚC 2: Worker2 ngủ 300ms ===
            // Trong lúc này Worker1 có thể chạy và lấy lock1
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // === BƯỚC 3: Worker2 cố gắng lấy lock1 ===
        // ⚠️ NGUY HIỂM: Nếu Worker1 đã lấy lock1 thì Worker2 sẽ bị treo ở đây!
        lock1.lock();
        System.out.println("Worker2 acquires the lock1...");

        // === BƯỚC 4: Giải phóng các lock ===
        // Chỉ chạy được đến đây nếu không bị deadlock
        lock1.unlock();
        lock2.unlock();
        System.out.println("Worker2 đã hoàn thành công việc");
    }
}

/*
=== CÁCH DEADLOCK XẢY RA ===

Timeline thực tế khi chạy:

Thời điểm 0ms:
- Worker1 lấy lock1 ✅
- Worker2 lấy lock2 ✅

Thời điểm 300ms:
- Worker1 cố lấy lock2 ❌ (Worker2 đang giữ)
- Worker2 cố lấy lock1 ❌ (Worker1 đang giữ)

Kết quả:
- Worker1 đợi lock2 mãi mãi
- Worker2 đợi lock1 mãi mãi
- Chương trình bị treo DEADLOCK! 🔒

Output bạn sẽ thấy:
"Worker1 acquires the lock1..."
"Worker2 acquires the lock2..."
(Sau đó chương trình treo, không in gì thêm)

=== CÁCH KHẮC PHỤC ===

Giải pháp đơn giản nhất:
Cả 2 worker đều lấy lock theo cùng thứ tự (lock1 -> lock2)

*/