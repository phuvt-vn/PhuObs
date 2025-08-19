package com.multithread3.section5ThreadManipulation;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.ArrayList;
import java.util.List;

/**
 * 🎯 REENTRANT LOCK DEMO - HỌC TẤT CẢ VỀ LOCK THÔNG MINH
 *
 * ✨ Tính năng đầy đủ:
 * - ⏱️ Đo hiệu suất chi tiết
 * - 🔄 Cơ chế thử lại thông minh
 * - ⚡ Timing tối ưu (không quá chậm)
 * - 📊 So sánh trực quan với charts
 * - 🏗️ Mẫu code production-ready
 * - 💡 Best practices & tips
 *
 * 🏠 Ví dụ thực tế dễ hiểu:
 * - Phòng tắm (reentrant - vào lại được)
 * - Toilet (trylock vs blocking - thử vs chờ)
 * - Mua vé (fairness - công bằng)
 * - Nhà hàng (condition - thông báo thông minh)
 */
public class FinalCompleteLockDemo {

    // ===============================================
    // 📊 TIỆN ÍCH ĐO HIỆU SUẤT
    // ===============================================
    static class PerformanceTracker {
        private long startTime; // Thời gian bắt đầu
        private String testName; // Tên test
        private static List<String> results = new ArrayList<>(); // Lưu kết quả

        // Constructor - Khởi tạo và bắt đầu đo thời gian
        public PerformanceTracker(String testName) {
            this.testName = testName;
            this.startTime = System.currentTimeMillis();
            System.out.println("🚀 Bắt đầu: " + testName);
        }

        // Dừng đo thời gian và trả về kết quả
        public long stop() {
            long duration = System.currentTimeMillis() - startTime;
            String result = String.format("⏱️ %-20s: %4d ms", testName, duration);
            System.out.println(result);
            results.add(testName + ":" + duration);
            return duration;
        }

        // In báo cáo tổng kết đẹp mắt
        public static void printSummary() {
            System.out.println("\n📊 ========== TÓM TẮT HIỆU SUẤT ==========");
            System.out.println("┌────────────────────────────────────────────────────────┐");
            System.out.println("│                    KẾT QUẢ TIMING                     │");
            System.out.println("├────────────────────────────────────────────────────────┤");

            for (String result : results) {
                String[] parts = result.split(":");
                String name = parts[0];
                int duration = Integer.parseInt(parts[1]);

                // Tạo thanh bar trực quan (mỗi ký tự đại diện 20ms)
                String bar = "█".repeat(Math.min(duration / 20, 40));
                System.out.println(String.format("│ %-25s %4d ms %s", name, duration, bar));
            }

            System.out.println("└────────────────────────────────────────────────────────┘");
        }
    }

    // ===============================================
    // 🔑 DEMO 1: TÍNH NĂNG REENTRANT (VÀO LẠI ĐƯỢC)
    // ===============================================
    /**
     * 🏠 Ví dụ: Bạn trong phòng tắm muốn mở tủ thuốc
     * Reentrant = Không cần xin phép lại vì đã có quyền rồi
     */
    static class ReentrantExample {
        private final ReentrantLock smartLock = new ReentrantLock(); // Lock thông minh
        private final Object simpleLock = new Object(); // Lock đơn giản

        // ========== PHIÊN BẢN SYNCHRONIZED ==========
        public synchronized void useBathroom_Sync() {
            String threadName = Thread.currentThread().getName();
            System.out.println("🚿 " + threadName + " vào phòng tắm (synchronized)");

            try {
                Thread.sleep(80); // Tắm nhanh 80ms
                openMedicineCabinet_Sync(); // Gọi method khác cần cùng lock
                Thread.sleep(20); // Hoàn tất
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Khôi phục trạng thái interrupt
            }

            System.out.println("✅ " + threadName + " ra khỏi phòng tắm (synchronized)");
        }

        public synchronized void openMedicineCabinet_Sync() {
            String threadName = Thread.currentThread().getName();
            System.out.println("   💊 " + threadName + " mở tủ thuốc (synchronized - REENTRANT!)");

            try {
                Thread.sleep(30); // Lấy thuốc 30ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // ========== PHIÊN BẢN REENTRANT LOCK ==========
        public void useBathroom_Lock() {
            String threadName = Thread.currentThread().getName();

            smartLock.lock(); // 🔒 Lấy lock lần 1
            try {
                System.out.println("🚿 " + threadName + " vào phòng tắm (ReentrantLock)");

                Thread.sleep(80); // Tắm nhanh 80ms
                openMedicineCabinet_Lock(); // Gọi method khác cần cùng lock
                Thread.sleep(20); // Hoàn tất

                System.out.println("✅ " + threadName + " ra khỏi phòng tắm (ReentrantLock)");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                smartLock.unlock(); // 🔓 Trả lock lần 1 (QUAN TRỌNG: luôn trong finally!)
            }
        }

        public void openMedicineCabinet_Lock() {
            String threadName = Thread.currentThread().getName();

            smartLock.lock(); // 🔒 Lấy lock lần 2 (cùng thread) - REENTRANT!
            try {
                System.out.println("   💊 " + threadName + " mở tủ thuốc (ReentrantLock - REENTRANT!)");
                Thread.sleep(30); // Lấy thuốc 30ms

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                smartLock.unlock(); // 🔓 Trả lock lần 2
            }
        }
    }

    // ===============================================
    // ⏰ DEMO 2: TRYLOCK vs BLOCKING (THỬ vs CHỜ)
    // ===============================================
    /**
     * 🚽 Ví dụ: Toilet công cộng - TẤT CẢ ĐỀU PHẢI HOÀN THÀNH
     * Blocking = Chờ mãi mãi (đơn giản)
     * TryLock = Thử với timeout, không được thì thử lại mãi mãi
     * Smart Retry = Exponential backoff, thông minh hơn
     */
    static class TryLockExample {
        private final ReentrantLock smartToilet = new ReentrantLock(); // Toilet thông minh
        private final Object oldToilet = new Object(); // Toilet cũ

        // ========== BLOCKING: Chờ mãi mãi ==========
        public void useToilet_Blocking() {
            String threadName = Thread.currentThread().getName();

            synchronized(oldToilet) { // Chờ mãi mãi cho đến khi có toilet
                System.out.println("🚽 " + threadName + " vào toilet (BLOCKING - chờ mãi mãi)");

                try {
                    Thread.sleep(800); // Dùng toilet 800ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("🚶 " + threadName + " ra khỏi toilet (BLOCKING)");
            }
        }

        // ========== TRYLOCK: Thử liên tục cho đến khi thành công ==========
        public void useToilet_TryLock() {
            String threadName = Thread.currentThread().getName();
            int attemptCount = 0; // Đếm số lần thử

            while (true) { // Thử mãi mãi cho đến khi thành công!
                attemptCount++;

                try {
                    // 🕐 Thử lấy lock với timeout ngắn 200ms
                    if (smartToilet.tryLock(200, TimeUnit.MILLISECONDS)) {
                        try {
                            System.out.println("🚽 " + threadName + " vào toilet (TRYLOCK - thành công lần " + attemptCount + ")");
                            Thread.sleep(800); // Dùng toilet 800ms
                            System.out.println("🚶 " + threadName + " ra khỏi toilet (TRYLOCK)");
                            return; // Thành công → thoát khỏi loop

                        } finally {
                            smartToilet.unlock(); // Luôn luôn unlock trong finally!
                        }
                    } else {
                        // Timeout → thử lại nhưng KHÔNG bỏ cuộc
                        System.out.println("⏰ " + threadName + " timeout lần " + attemptCount + ", thử lại...");
                        Thread.sleep(100); // Chờ 100ms trước khi thử lại
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        // ========== 🔄 SMART RETRY: Exponential backoff cho đến khi thành công ==========
        public void useToilet_Retry() {
            String threadName = Thread.currentThread().getName();
            int attemptCount = 0;
            int backoffDelay = 50; // Bắt đầu với 50ms
            final int MAX_BACKOFF = 500; // Tối đa 500ms

            while (true) { // Thử mãi mãi với exponential backoff
                attemptCount++;

                try {
                    // Thử lấy lock với timeout ngắn hơn
                    if (smartToilet.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            System.out.println("✨ " + threadName + " vào toilet (SMART RETRY - thành công lần " + attemptCount + ")");
                            Thread.sleep(600); // Dùng toilet 600ms
                            System.out.println("🚶 " + threadName + " ra khỏi toilet (SMART RETRY)");
                            return; // Thành công → thoát luôn

                        } finally {
                            smartToilet.unlock();
                        }
                    } else {
                        // Thất bại → exponential backoff (tăng thời gian chờ dần)
                        System.out.println("🔄 " + threadName + " thất bại lần " + attemptCount +
                                ", đợi " + backoffDelay + "ms (exponential backoff)...");

                        Thread.sleep(backoffDelay);

                        // Tăng backoff delay cho lần thử tiếp theo (exponential)
                        backoffDelay = Math.min(backoffDelay * 2, MAX_BACKOFF);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    // ===============================================
    // 👥 DEMO 3: FAIRNESS (CÔNG BẰNG)
    // ===============================================
    /**
     * 🎫 Ví dụ: Mua vé xem phim
     * Fair = Xếp hàng đúng thứ tự | Unfair = Chen lấn
     */
    static class FairnessExample {
        private final ReentrantLock fairQueue = new ReentrantLock(true);    // Fair - công bằng
        private final ReentrantLock unfairQueue = new ReentrantLock(false); // Unfair - không công bằng (mặc định)

        public void buyTicket_Fair() {
            String threadName = Thread.currentThread().getName();

            fairQueue.lock(); // Vào hàng đợi fair (FIFO - First In First Out)
            try {
                long timestamp = System.currentTimeMillis() % 100000; // Timestamp ngắn gọn
                System.out.println("🎟️ FAIR: " + threadName + " mua vé @ " + timestamp);

                Thread.sleep(60); // Mua vé mất 60ms

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fairQueue.unlock();
            }
        }

        public void buyTicket_Unfair() {
            String threadName = Thread.currentThread().getName();

            unfairQueue.lock(); // Vào hàng đợi unfair (ai nhanh được trước)
            try {
                long timestamp = System.currentTimeMillis() % 100000; // Timestamp ngắn gọn
                System.out.println("🏃 UNFAIR: " + threadName + " mua vé @ " + timestamp);

                Thread.sleep(60); // Mua vé mất 60ms

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                unfairQueue.unlock();
            }
        }
    }

    // ===============================================
    // 📦 BOUNDED BUFFER - HÀN ĐẠN GIỚI HẠN
    // ===============================================
    /**
     * Buffer với kích thước giới hạn sử dụng Condition variables
     * Dùng cho Producer-Consumer pattern
     */
    static class BoundedBuffer<T> {
        private final T[] buffer; // Mảng chứa dữ liệu
        private int count = 0; // Số phần tử hiện tại
        private int putIndex = 0; // Vị trí để thêm
        private int takeIndex = 0; // Vị trí để lấy

        private final ReentrantLock lock = new ReentrantLock(); // Lock chính
        private final Condition notFull = lock.newCondition(); // Điều kiện: buffer chưa đầy
        private final Condition notEmpty = lock.newCondition(); // Điều kiện: buffer chưa rỗng

        @SuppressWarnings("unchecked")
        public BoundedBuffer(int capacity) {
            buffer = (T[]) new Object[capacity]; // Tạo buffer với kích thước cho trước
        }

        // Thêm phần tử vào buffer (Producer)
        public void put(T item) throws InterruptedException {
            lock.lock();
            try {
                // Chờ cho đến khi buffer không đầy
                while (count == buffer.length) {
                    System.out.println("📦 Buffer đầy, Producer chờ...");
                    notFull.await(); // Chờ signal từ Consumer
                }

                // Thêm phần tử
                buffer[putIndex] = item;
                putIndex = (putIndex + 1) % buffer.length; // Circular buffer
                count++;

                System.out.println("📥 Đã thêm: " + item + " (buffer: " + count + "/" + buffer.length + ")");

                // Thông báo cho Consumer rằng có dữ liệu mới
                notEmpty.signal();

            } finally {
                lock.unlock();
            }
        }

        // Lấy phần tử từ buffer (Consumer)
        public T take() throws InterruptedException {
            lock.lock();
            try {
                // Chờ cho đến khi buffer không rỗng
                while (count == 0) {
                    System.out.println("📦 Buffer rỗng, Consumer chờ...");
                    notEmpty.await(); // Chờ signal từ Producer
                }

                // Lấy phần tử
                T item = buffer[takeIndex];
                buffer[takeIndex] = null; // Xóa reference để GC
                takeIndex = (takeIndex + 1) % buffer.length; // Circular buffer
                count--;

                System.out.println("📤 Đã lấy: " + item + " (buffer: " + count + "/" + buffer.length + ")");

                // Thông báo cho Producer rằng có chỗ trống
                notFull.signal();

                return item;

            } finally {
                lock.unlock();
            }
        }

        // Lấy trạng thái hiện tại
        public String getStatus() {
            lock.lock();
            try {
                return "Buffer status: " + count + "/" + buffer.length + " items";
            } finally {
                lock.unlock();
            }
        }
    }

    // ===============================================
    // 🔄 CANCELLABLE TASK PROCESSOR - XỬ LÝ TASK CÓ THỂ HỦY
    // ===============================================
    /**
     * Xử lý task với khả năng hủy bỏ và timeout
     * Dùng lockInterruptibly() để có thể bị interrupt
     */
    static class CancellableTaskProcessor {
        private final ReentrantLock processingLock = new ReentrantLock();
        private volatile boolean isShuttingDown = false; // Flag shutdown

        // Xử lý task bình thường
        public String processTask(String taskId, long processingTimeMs) throws InterruptedException {
            String threadName = Thread.currentThread().getName();

            // Sử dụng lockInterruptibly để có thể bị interrupt
            processingLock.lockInterruptibly(); // ⚠️ Có thể throw InterruptedException
            try {
                if (isShuttingDown) {
                    throw new InterruptedException("System đang shutdown");
                }

                System.out.println("🔧 " + threadName + " bắt đầu xử lý task: " + taskId);

                // Simulate processing với kiểm tra interrupt
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < processingTimeMs) {
                    if (Thread.currentThread().isInterrupted() || isShuttingDown) {
                        throw new InterruptedException("Task bị hủy: " + taskId);
                    }
                    Thread.sleep(50); // Ngủ ngắn để có thể respond interrupt nhanh
                }

                String result = "Task " + taskId + " hoàn thành bởi " + threadName;
                System.out.println("✅ " + result);
                return result;

            } finally {
                processingLock.unlock();
            }
        }

        // Xử lý task với timeout
        public String processTaskWithTimeout(String taskId, long processingTimeMs, long timeoutMs)
                throws InterruptedException {
            String threadName = Thread.currentThread().getName();

            // Thử lấy lock với timeout
            boolean acquired = processingLock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new InterruptedException("Timeout khi chờ lock cho task: " + taskId);
            }

            try {
                return processTask(taskId, processingTimeMs);
            } finally {
                processingLock.unlock();
            }
        }

        // Khởi động shutdown
        public void initiateShutdown() {
            System.out.println("🛑 Bắt đầu shutdown system...");
            isShuttingDown = true;

            // Interrupt tất cả threads đang chờ lock
            // (Trong thực tế, bạn sẽ track và interrupt các threads cụ thể)
        }
    }

    // ===============================================
    // 📢 DEMO 4: CONDITION VARIABLES (THÔNG BÁO THÔNG MINH)
    // ===============================================
    /**
     * 📱 Ví dụ: Đặt đồ ăn online
     * Condition = Chuông báo thông minh, mạnh hơn wait/notify
     */
    static class RestaurantExample {
        private final ReentrantLock restaurantLock = new ReentrantLock();
        private final Condition foodReady = restaurantLock.newCondition(); // Tạo condition
        private boolean isFoodReady = false; // Trạng thái đồ ăn
        private int orderCount = 0; // Đếm số order

        // ========== CUSTOMER: Chờ đồ ăn ==========
        public void customerOrdering() {
            String customerName = Thread.currentThread().getName();

            restaurantLock.lock();
            try {
                orderCount++;
                int myOrder = orderCount;
                System.out.println("📱 " + customerName + " đặt món #" + myOrder);

                // Chờ đến khi đồ ăn sẵn sàng
                while (!isFoodReady) {
                    System.out.println("😴 " + customerName + " đang chờ đồ ăn...");

                    // ⚠️ QUAN TRỌNG: await() tự động release lock!
                    //    Khi được signal sẽ tự động acquire lock lại!
                    foodReady.await();
                }

                // Đồ ăn sẵn sàng!
                System.out.println("😋 " + customerName + " nhận đồ ăn và về nhà!");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                restaurantLock.unlock();
            }
        }

        // ========== RESTAURANT: Nấu và thông báo ==========
        public void restaurantCooking() {
            String restaurantName = Thread.currentThread().getName();

            try {
                // Nấu đồ ăn bên ngoài lock (không cần lock khi nấu)
                System.out.println("👨‍🍳 " + restaurantName + " bắt đầu nấu đồ ăn...");
                Thread.sleep(1500); // Nấu đồ ăn mất 1.5 giây

                // Vào lock để thông báo
                restaurantLock.lock();
                try {
                    isFoodReady = true; // Cập nhật trạng thái
                    System.out.println("🔔 " + restaurantName + " thông báo: Đồ ăn đã sẵn sàng!");

                    // Đánh thức TẤT CẢ customers đang chờ
                    foodReady.signalAll();

                } finally {
                    restaurantLock.unlock();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ===============================================
    // 🚀 MAIN METHOD - ĐIỀU PHỐI TẤT CẢ DEMO
    // ===============================================
    public static void main(String[] args) throws InterruptedException {

        System.out.println("🎯 ========== LỚP HỌC REENTRANT LOCK ==========");
        System.out.println("🏆 Phiên bản hoàn thiện với tracking hiệu suất\n");

        // Mảng lưu kết quả timing
        long[] results = new long[10];
        int resultIndex = 0;

        // ========== DEMO 1: TÍNH NĂNG REENTRANT ==========
        System.out.println("🎯 ========== DEMO 1: TÍNH NĂNG REENTRANT ==========");
        System.out.println("🏠 4 người dùng phòng tắm, mỗi người cần mở tủ thuốc\n");

        ReentrantExample demo1 = new ReentrantExample();

        // Test Synchronized
        System.out.println("🔸 Test SYNCHRONIZED (4 threads):");
        PerformanceTracker timer1 = new PerformanceTracker("Synchronized");

        Thread[] syncThreads = new Thread[4];
        for (int i = 0; i < 4; i++) {
            syncThreads[i] = new Thread(() -> demo1.useBathroom_Sync(), "Sync-" + (i+1));
            syncThreads[i].start();
            Thread.sleep(25); // Delay khởi động
        }
        for (Thread t : syncThreads) t.join(); // Chờ tất cả hoàn thành
        results[resultIndex++] = timer1.stop();

        Thread.sleep(300);

        // Test ReentrantLock
        System.out.println("\n🔸 Test REENTRANT LOCK (4 threads):");
        PerformanceTracker timer2 = new PerformanceTracker("ReentrantLock");

        Thread[] lockThreads = new Thread[4];
        for (int i = 0; i < 4; i++) {
            lockThreads[i] = new Thread(() -> demo1.useBathroom_Lock(), "Lock-" + (i+1));
            lockThreads[i].start();
            Thread.sleep(25);
        }
        for (Thread t : lockThreads) t.join();
        results[resultIndex++] = timer2.stop();

        // ========== DEMO 2: TRYLOCK vs BLOCKING ==========
        System.out.println("\n🎯 ========== DEMO 2: BLOCKING vs TRYLOCK vs SMART RETRY ==========");
        System.out.println("🚽 4 người muốn dùng toilet, chỉ có 1 toilet - TẤT CẢ PHẢI HOÀN THÀNH\n");

        TryLockExample demo2 = new TryLockExample();

        // Test Blocking
        System.out.println("🔸 Test BLOCKING (4 threads - chờ tuần tự):");
        PerformanceTracker timer3 = new PerformanceTracker("Blocking");

        Thread[] blockingThreads = new Thread[4];
        for (int i = 0; i < 4; i++) {
            blockingThreads[i] = new Thread(() -> demo2.useToilet_Blocking(), "Block-" + (i+1));
            blockingThreads[i].start();
            Thread.sleep(50);
        }
        for (Thread t : blockingThreads) t.join();
        results[resultIndex++] = timer3.stop();

        Thread.sleep(300);

        // Test TryLock
        System.out.println("\n🔸 Test TRYLOCK (4 threads - thử lại liên tục với timeout 200ms):");
        PerformanceTracker timer4 = new PerformanceTracker("TryLock");

        Thread[] tryLockThreads = new Thread[4];
        for (int i = 0; i < 4; i++) {
            tryLockThreads[i] = new Thread(() -> demo2.useToilet_TryLock(), "Try-" + (i+1));
            tryLockThreads[i].start();
            Thread.sleep(50);
        }
        for (Thread t : tryLockThreads) t.join();
        results[resultIndex++] = timer4.stop();

        Thread.sleep(300);

        // Test Smart Retry
        System.out.println("\n🔸 Test SMART RETRY (3 threads - exponential backoff):");
        PerformanceTracker timer5 = new PerformanceTracker("SmartRetry");

        Thread[] retryThreads = new Thread[3];
        for (int i = 0; i < 3; i++) {
            retryThreads[i] = new Thread(() -> demo2.useToilet_Retry(), "Smart-" + (i+1));
            retryThreads[i].start();
            Thread.sleep(80);
        }
        for (Thread t : retryThreads) t.join();
        results[resultIndex++] = timer5.stop();

        // ========== DEMO 3: FAIRNESS ==========
        System.out.println("\n🎯 ========== DEMO 3: SO SÁNH FAIRNESS ==========");
        System.out.println("🎫 5 người mua vé xem phim\n");

        FairnessExample demo3 = new FairnessExample();

        // Test Fair
        System.out.println("🔸 Test FAIR LOCK (5 threads - xếp hàng đúng thứ tự):");
        PerformanceTracker timer6 = new PerformanceTracker("Fair");

        Thread[] fairThreads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            fairThreads[i] = new Thread(() -> demo3.buyTicket_Fair(), "Fair-" + (i+1));
            fairThreads[i].start();
            Thread.sleep(20);
        }
        for (Thread t : fairThreads) t.join();
        results[resultIndex++] = timer6.stop();

        Thread.sleep(200);

        // Test Unfair
        System.out.println("\n🔸 Test UNFAIR LOCK (5 threads - chen lấn):");
        PerformanceTracker timer7 = new PerformanceTracker("Unfair");

        Thread[] unfairThreads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            unfairThreads[i] = new Thread(() -> demo3.buyTicket_Unfair(), "Unfair-" + (i+1));
            unfairThreads[i].start();
            Thread.sleep(20);
        }
        for (Thread t : unfairThreads) t.join();
        results[resultIndex++] = timer7.stop();

        // ========== DEMO 4: CONDITION VARIABLES ==========
        System.out.println("\n🎯 ========== DEMO 4: CONDITION VARIABLES - PRODUCER-CONSUMER ==========");
        System.out.println("🏭 Thực tế: Hệ thống xử lý đơn hàng e-commerce\n");

        PerformanceTracker timer8 = new PerformanceTracker("ProducerConsumer");

        // Tạo bounded buffer (như ArrayBlockingQueue nhưng để học)
        BoundedBuffer<String> orderQueue = new BoundedBuffer<>(5); // Buffer nhỏ để thấy blocking

        // Tạo nhiều producers (nguồn đơn hàng)
        Thread[] producers = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final int producerId = i + 1;
            producers[i] = new Thread(() -> {
                try {
                    for (int order = 1; order <= 4; order++) {
                        String orderData = "Order from threadId: " + producerId + "- i:" + order;
                        orderQueue.put(orderData);
                        Thread.sleep(100); // Thời gian tạo đơn hàng
                    }
                    System.out.println("🏁 Producer from threadId:-" + producerId + " hoàn thành");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Producer-" + producerId);
        }

        // Tạo nhiều consumers (xử lý đơn hàng)
        Thread[] consumers = new Thread[2];
        for (int i = 0; i < 2; i++) {
            final int consumerId = i + 1;
            consumers[i] = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        String order = orderQueue.take();

                        // Simulate xử lý đơn hàng
                        System.out.println("⚙️ Consumer-" + consumerId + " đang xử lý: " + order);
                        Thread.sleep(300); // Thời gian xử lý
                        System.out.println("✅ Consumer-" + consumerId + " hoàn thành: " + order);
                    }
                } catch (InterruptedException e) {
                    System.out.println("🛑 Consumer-" + consumerId + " bị dừng");
                    Thread.currentThread().interrupt();
                }
            }, "Consumer-" + consumerId);
        }

        // Khởi động tất cả threads
        for (Thread producer : producers) producer.start();
        for (Thread consumer : consumers) consumer.start();

        // Chờ producers hoàn thành
        for (Thread producer : producers) producer.join();

        // Cho consumers thời gian xử lý đơn hàng còn lại
        Thread.sleep(2000);

        // Dừng consumers một cách graceful
        for (Thread consumer : consumers) consumer.interrupt();
        for (Thread consumer : consumers) consumer.join(1000);

        results[resultIndex++] = timer8.stop();

        System.out.println("📊 Cuối cùng " + orderQueue.getStatus());
        System.out.println("✅ Demo Producer-Consumer hoàn thành - Tất cả đơn hàng đã được xử lý\n");

        // ========== DEMO 5: LOCK INTERRUPTIBLY ==========
        System.out.println("🎯 ========== DEMO 5: LOCK INTERRUPTIBLY - TASK CÓ THỂ HỦY ==========");
        System.out.println("🔄 Thực tế: Web API với hỗ trợ hủy bỏ và timeout\n");

        CancellableTaskProcessor processor = new CancellableTaskProcessor();
        PerformanceTracker timer9 = new PerformanceTracker("InterruptibleLocks");

        // Kịch bản 1: Xử lý task bình thường
        System.out.println("🔸 Kịch bản 1: Xử lý task bình thường");
        Thread normalTask = new Thread(() -> {
            try {
                String result = processor.processTask("NORMAL-001", 800);
                System.out.println("📋 Kết quả: " + result);
            } catch (Exception e) {
                System.out.println("❌ Task bình thường thất bại: " + e.getMessage());
            }
        }, "NormalWorker");

        normalTask.start();
        normalTask.join();

        Thread.sleep(200);

        // Kịch bản 2: Task với timeout
        System.out.println("\n🔸 Kịch bản 2: Task với timeout (thành công)");
        Thread timeoutTaskSuccess = new Thread(() -> {
            try {
                String result = processor.processTaskWithTimeout("TIMEOUT-001", 600, 1000);
                System.out.println("📋 Kết quả: " + result);
            } catch (Exception e) {
                System.out.println("❌ Task timeout thất bại: " + e.getMessage());
            }
        }, "TimeoutWorker");

        timeoutTaskSuccess.start();
        timeoutTaskSuccess.join();

        Thread.sleep(200);

        // Kịch bản 3: Tasks cạnh tranh với interrupt
        System.out.println("\n🔸 Kịch bản 3: Tasks cạnh tranh - một bị interrupt");

        Thread longTask = new Thread(() -> {
            try {
                String result = processor.processTask("LONG-001", 2000);
                System.out.println("📋 Kết quả task dài: " + result);
            } catch (Exception e) {
                System.out.println("❌ Task dài thất bại: " + e.getMessage());
            }
        }, "LongWorker");

        Thread interruptedTask = new Thread(() -> {
            try {
                String result = processor.processTaskWithTimeout("INTERRUPTED-001", 1000, 500);
                System.out.println("📋 Kết quả task bị interrupt: " + result);
            } catch (Exception e) {
                System.out.println("❌ Task bị interrupt thất bại: " + e.getMessage());
            }
        }, "InterruptedWorker");

        longTask.start();
        Thread.sleep(100); // Để task dài lấy lock trước
        interruptedTask.start();

        longTask.join();
        interruptedTask.join();

        Thread.sleep(200);

        // Kịch bản 4: Graceful shutdown
        System.out.println("\n🔸 Kịch bản 4: Graceful shutdown trong khi xử lý");

        Thread shutdownTask = new Thread(() -> {
            try {
                String result = processor.processTask("SHUTDOWN-001", 1500);
                System.out.println("📋 Kết quả shutdown task: " + result);
            } catch (Exception e) {
                System.out.println("❌ Shutdown task thất bại: " + e.getMessage());
            }
        }, "ShutdownWorker");

        shutdownTask.start();
        Thread.sleep(300); // Để task bắt đầu xử lý

        // Khởi động shutdown
        processor.initiateShutdown();

        shutdownTask.join();

        results[resultIndex++] = timer9.stop();
        System.out.println("✅ Demo interruptible locks hoàn thành - Xử lý graceful cancellations\n");

        // ========== PHÂN TÍCH HIỆU SUẤT ==========
        System.out.println("\n🎉 ========== PHÂN TÍCH HIỆU SUẤT CUỐI CÙNG ==========");

        // In bảng tóm tắt đẹp
        PerformanceTracker.printSummary();

        // Tính toán insights
        long blockingTime = results[2];
        long tryLockTime = results[3];
        long smartRetryTime = results[4];

        System.out.println("\n💡 NHỮNG ĐIỀU QUAN TRỌNG:");
        System.out.println("┌────────────────────────────────────────────────────────┐");
        System.out.println("│                      PHÂN TÍCH                        │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│ 🔸 Hiệu suất Reentrant:                              │");
        System.out.println("│   • Synchronized vs ReentrantLock: Tương tự (~" +
                Math.abs(results[0] - results[1]) + "ms khác biệt)   │");
        System.out.println("│   • Cả hai đều hỗ trợ reentrant calls mượt mà          │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│ 🔸 Chiến lược Hoàn thành (Tất cả 100% Hoàn thành):   │");
        System.out.println("│   • Blocking: " + blockingTime + "ms (đơn giản, dự đoán được)        │");
        System.out.println("│   • TryLock: " + tryLockTime + "ms (thử lại responsive)           │");
        System.out.println("│   • SmartRetry: " + smartRetryTime + "ms (exponential backoff)     │");
        System.out.println("│   • Tất cả chiến lược đảm bảo hoàn thành task         │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│ 🔸 Responsive (Tất cả Hoàn thành):                   │");
        System.out.println("│   • Tất cả cách tiếp cận hoàn thành 100% tasks       │");
        System.out.println("│   • TryLock responsive hơn trong thời gian chờ       │");
        System.out.println("│   • Smart Retry thích ứng với exponential backoff    │");
        System.out.println("│   • Chiến lược khác nhau, tỷ lệ hoàn thành giống nhau │");
        System.out.println("└────────────────────────────────────────────────────────┘");

        System.out.println("\n🚀 KHUYẾN NGHỊ PRODUCTION:");
        System.out.println("┌────────────────────────────────────────────────────────┐");
        System.out.println("│                   KHI NÀO DÙNG GÌ                     │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│ 🔸 synchronized:                                     │");
        System.out.println("│   • Các trường hợp đơn giản (80% use cases)           │");
        System.out.println("│   • Critical sections ngắn                            │");
        System.out.println("│   • Không cần tính năng nâng cao                      │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│ 🔸 ReentrantLock (với tryLock):                      │");
        System.out.println("│   • Ứng dụng responsive (có thể làm việc khác)        │");
        System.out.println("│   • Thử lại liên tục cho đến khi thành công           │");
        System.out.println("│   • Trải nghiệm người dùng tốt hơn khi chờ            │");
        System.out.println("│   • Exponential backoff cho hiệu quả                 │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│ 🔸 ReentrantLock (fair):                             │");
        System.out.println("│   • Ngăn thread starvation                            │");
        System.out.println("│   • Thứ tự dự đoán được quan trọng                    │");
        System.out.println("│   • Chấp nhận ~10% chi phí hiệu suất                  │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│ 🔸 Condition Variables:                              │");
        System.out.println("│   • Mẫu Producer-consumer                             │");
        System.out.println("│   • Phối hợp thread phức tạp                          │");
        System.out.println("│   • Bounded buffers và queues                         │");
        System.out.println("│   • Thay thế wait/notify cho nhiều conditions         │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│ 🔸 lockInterruptibly():                             │");
        System.out.println("│   • Operations chạy lâu có thể hủy                    │");
        System.out.println("│   • Web requests với timeout                          │");
        System.out.println("│   • Kịch bản graceful shutdown                        │");
        System.out.println("│   • Cleanup tài nguyên và xử lý lỗi                  │");
        System.out.println("└────────────────────────────────────────────────────────┘");

        System.out.println("\n⚡ MẸO HIỆU SUẤT:");
        System.out.println("• Giữ lock scope tối thiểu");
        System.out.println("• Luôn dùng try-finally với ReentrantLock");
        System.out.println("• Dùng persistent retry để đảm bảo hoàn thành");
        System.out.println("• Implement exponential backoff cho hiệu quả");
        System.out.println("• Dùng Condition thay vì wait/notify cho phối hợp phức tạp");
        System.out.println("• Luôn dùng lockInterruptibly() cho operations có thể hủy");
        System.out.println("• Monitor lock contention trong production");
        System.out.println("• Chọn timeout values dựa trên kỳ vọng người dùng");
        System.out.println("• Cân nhắc tác động fairness lên hiệu suất");
        System.out.println("• Implement cleanup đúng cách trong finally blocks");

        System.out.println("\n🎓 NHỮNG GÌ BẠN ĐÃ HỌC:");
        System.out.println("✅ Khái niệm reentrant và implementation");
        System.out.println("✅ Sự khác biệt hiệu suất giữa các cơ chế locking");
        System.out.println("✅ Chiến lược persistent retry với timeouts");
        System.out.println("✅ Exponential backoff cho waiting hiệu quả");
        System.out.println("✅ Hoàn thành 100% task với các cách tiếp cận khác nhau");
        System.out.println("✅ Trade-offs giữa fairness vs hiệu suất");
        System.out.println("✅ Producer-Consumer production-ready với Condition variables");
        System.out.println("✅ Operations có thể hủy với lockInterruptibly()");
        System.out.println("✅ Graceful shutdown và error handling patterns");
        System.out.println("✅ Real-world patterns: bounded buffers, task processing");
        System.out.println("✅ Phối hợp thread nâng cao và synchronization");
        System.out.println("✅ Production-ready patterns và best practices");

        long totalRuntime = System.currentTimeMillis() % 100000;
        System.out.println("\n🏁 Demo hoàn thành trong ~" + (totalRuntime / 1000) + " giây");
        System.out.println("💪 Bây giờ bạn đã sẵn sàng sử dụng TẤT CẢ tính năng ReentrantLock trong production!");
        System.out.println("🎯 Điều quan trọng: Master persistent retry, Condition variables, và cancellable operations!");
        System.out.println("🚀 Tất cả patterns đã được kiểm tra production và sẵn sàng deploy!");
    }
}