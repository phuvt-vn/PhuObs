package com.multithread2.section8InterThreadCommunication;

import java.util.concurrent.Semaphore;

/**
 * Minh hoạ các "bẫy" (pitfall) khi dùng Binary Semaphore như lock.
 *
 * Lưu ý quan trọng:
 * - Binary semaphore (permits=1) KHÔNG có "owner" và KHÔNG re-entrant.
 * - Bất kỳ thread nào cũng có thể release, kể cả thread chưa acquire -> dễ gây bug.
 * - Vì vậy, không dùng binary semaphore để thay thế lock cho đoạn găng độc quyền.
 */
public class Step02_BinarySemaphorePitfall {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Binary Semaphore Pitfall ===");
        demoNonReentrantDeadlock();
        System.out.println();
        demoReleaseByOtherThreadBug();
        System.out.println("\nHoàn tất demo pitfall.");
    }

    /**
     * MINH HOẠ 1: KHÔNG RE-ENTRANT
     * Cùng một thread acquire() 2 lần sẽ tự mắc kẹt nếu không có thread khác release().
     * Ở đây, để chương trình không treo vĩnh viễn, ta sẽ tạo một thread cứu hộ để release() hộ.
     */
    private static void demoNonReentrantDeadlock() throws InterruptedException {
        System.out.println("-- Pitfall 1: non-reentrant (cùng thread acquire 2 lần) --");
        Semaphore binary = new Semaphore(1); // 1 permit

        Thread worker = new Thread(() -> {
            try {
                log("acquire lần 1");
                binary.acquire();
                log("đã giữ permit lần 1");

                // Cố gắng acquire lần 2 trong cùng thread -> sẽ KẸT nếu không ai release hộ
                log("thử acquire lần 2 (sẽ bị chặn)...");
                binary.acquire();
                log("(không nên thấy log này nếu không có thread cứu hộ)");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // Phải release() 2 lần tương ứng với 2 lần acquire() thành công
                binary.release();
                log("release lần 1");
                binary.release();
                log("release lần 2");
            }
        }, "Worker");

        worker.start();

        // Chờ một chút để Worker bị chặn ở acquire lần 2
        Thread.sleep(300);

        // Thread khác release hộ -> minh họa việc semaphore KHÔNG có owner
        Thread rescuer = new Thread(() -> {
            log("Rescuer release() hộ -> Worker tiếp tục được");
            binary.release(); // cứu hộ cho lần acquire thứ 2
        }, "Rescuer");
        rescuer.start();

        worker.join();
        rescuer.join();
        System.out.println("Kết luận: Binary semaphore không re-entrant, không phù hợp làm lock re-entrant.");
    }

    /**
     * MINH HOẠ 2: THREAD KHÁC release() DẪN ĐẾN BUG LOGIC
     * Thread A acquire() và nghĩ rằng mình "độc quyền"; Thread B lỡ tay release() -> vô tình cho phép
     * Thread C đi vào vùng găng cùng lúc -> vi phạm độc quyền.
     * Ở đây chỉ minh hoạ in log, không tạo tranh chấp dữ liệu thật.
     */
    private static void demoReleaseByOtherThreadBug() throws InterruptedException {
        System.out.println("-- Pitfall 2: thread khác release() --");
        Semaphore binary = new Semaphore(1);

        Thread A = new Thread(() -> {
            try {
                binary.acquire();
                log("A vào vùng găng (nghĩ rằng độc quyền)");
                // Giữ lâu một chút
                Thread.sleep(300);
                log("A chuẩn bị rời vùng găng");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                binary.release();
                log("A release()");
            }
        }, "A");

        Thread B = new Thread(() -> {
            try {
                // Chờ cho A đã acquire xong
                Thread.sleep(100);
                log("B (vô tình) release() dù không acquire -> BUG TIỀM ẨN");
                binary.release();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "B");

        Thread C = new Thread(() -> {
            try {
                // Do B đã release hộ, C có thể acquire và "vào chung" với A
                binary.acquire();
                log("C cũng vào vùng găng (do B release hộ) -> vi phạm độc quyền nếu dùng như lock");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                binary.release();
                log("C release()");
            }
        }, "C");

        A.start();
        B.start();
        Thread.sleep(120); // đảm bảo B release trước khi C acquire
        C.start();

        A.join();
        B.join();
        C.join();
        System.out.println("Kết luận: Semaphore có thể release bởi thread khác -> không phù hợp thay lock.");
    }

    private static void log(String msg) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), msg);
    }
}
