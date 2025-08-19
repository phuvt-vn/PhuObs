package com.multithread3.section7MultithreadConcept;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DeadlockFix {

    private Lock lock1 = new ReentrantLock(true);
    private Lock lock2 = new ReentrantLock(true);

    public static void main(String[] args) {
        DeadlockFix noDeadlock = new DeadlockFix();

        new Thread(noDeadlock::worker1, "worker1").start();
        new Thread(noDeadlock::worker2, "worker2").start();
    }

    public void worker1() {
        // ✅ GIẢI PHÁP: Cả 2 worker đều lấy lock theo THỨ TỰ GIỐNG NHAU
        // Luôn lấy lock1 trước, sau đó mới lấy lock2

        System.out.println("Worker1 đang chờ lock1...");
        lock1.lock();
        System.out.println("Worker1 đã lấy lock1 ✅");

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Worker1 đang chờ lock2...");
        lock2.lock();
        System.out.println("Worker1 đã lấy lock2 ✅");

        // Thực hiện công việc
        System.out.println("🔥 Worker1 đang làm việc...");

        // Giải phóng lock theo thứ tự ngược lại (LIFO)
        lock2.unlock();
        System.out.println("Worker1 đã trả lock2");

        lock1.unlock();
        System.out.println("Worker1 đã trả lock1");

        System.out.println("✅ Worker1 hoàn thành!");
    }

    public void worker2() {
        // ✅ QUAN TRỌNG: Worker2 cũng lấy lock theo THỨ TỰ GIỐNG Worker1
        // lock1 trước -> lock2 sau (không đảo ngược!)

        System.out.println("Worker2 đang chờ lock1...");
        lock1.lock();
        System.out.println("Worker2 đã lấy lock1 ✅");

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Worker2 đang chờ lock2...");
        lock2.lock();
        System.out.println("Worker2 đã lấy lock2 ✅");

        // Thực hiện công việc
        System.out.println("🔥 Worker2 đang làm việc...");

        // Giải phóng lock theo thứ tự ngược lại (LIFO)
        lock2.unlock();
        System.out.println("Worker2 đã trả lock2");

        lock1.unlock();
        System.out.println("Worker2 đã trả lock1");

        System.out.println("✅ Worker2 hoàn thành!");
    }
}

/*
=== TẠI SAO KHÔNG CÒN DEADLOCK? ===

Bằng cách cả 2 worker đều lấy lock theo cùng thứ tự:
1. Worker nào chạy trước sẽ lấy được lock1
2. Worker còn lại phải chờ đến khi worker đầu tiên trả lock1
3. Không có tình huống "tôi giữ A đợi B, bạn giữ B đợi A"

Timeline mới:
- Worker1 lấy lock1 ✅
- Worker2 chờ lock1 ⏳
- Worker1 lấy lock2 ✅
- Worker1 làm việc xong, trả lock2, trả lock1
- Worker2 lấy được lock1 ✅
- Worker2 lấy được lock2 ✅
- Worker2 làm việc xong, trả lock2, trả lock1
- HOÀN THÀNH! 🎉

=== NGUYÊN TẮC TRÁNH DEADLOCK ===
1. **Thứ tự cố định**: Luôn lấy lock theo cùng một thứ tự
2. **Timeout**: Dùng tryLock() với thời gian chờ
3. **Tránh nested lock**: Hạn chế lấy nhiều lock cùng lúc
4. **Lock-free**: Sử dụng CAS (Compare-And-Swap) thay vì lock
*/

PriorityBlockingQueue

Collections synchronization

linkbl
CountDownLatch

arrayblockingqueue
linkedblockingqueue
LinkedBlockingDeque

        BlockingPriorityQueue

Concurrent maps
exchanger
        CopyOnWriteArrays

tạo cho tôi 1 file .md nội dung  đầy đủ nhất về CopyOnWriteArrays.
nội dung giải thích rõ ràng chi tiết cho người chưa biết gì cũng hiểu được.
comment code tiếng việt từng step quan trọng và tại sao phải làm như vậy. so sánh kết quả giữa việc có dùng và không dùng.
kèm kết quả benchmark nếu có.
và best practice những use case cực kỳ phù hợp để dùng CopyOnWriteArrays mà không phải là những cái khác.
lưu ý . comment code và giải thích bằng tiếng việt chi tiết. ko dùng english.