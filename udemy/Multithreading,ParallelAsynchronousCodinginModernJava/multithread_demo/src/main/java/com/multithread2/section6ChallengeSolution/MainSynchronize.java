package com.multithread2.section6ChallengeSolution;

/**
 * Chủ đề: Vùng Găng (Critical Section) & Đồng bộ hóa (Synchronization)
 * ---------------------------------------------------------------------
 * Đây là phiên bản SỬA LỖI cho ví dụ về Race Condition ở phần 5.
 * Nó sử dụng một "khối được đồng bộ hóa" (synchronized block) để bảo vệ vùng găng,
 * đảm bảo rằng tại một thời điểm, chỉ có một luồng duy nhất được phép thực thi đoạn code đó.
 */
public class MainSynchronize {
    public static void main(String[] args) throws InterruptedException {
        InventoryCounter inventoryCounter = new InventoryCounter();
        IncrementingThread incrementingThread = new IncrementingThread(inventoryCounter);
        DecrementingThread decrementingThread = new DecrementingThread(inventoryCounter);

        // Bắt đầu các luồng chạy song song.
        incrementingThread.start();
        decrementingThread.start();

        // Chờ cả hai luồng hoàn thành.
        incrementingThread.join();
        decrementingThread.join();

        // **KẾT QUẢ MONG ĐỢI VÀ THỰC TẾ:**
        // Nhờ có `synchronized`, race condition đã được loại bỏ.
        // Kết quả cuối cùng BÂY GIỜ sẽ luôn luôn là 0, bất kể bạn chạy bao nhiêu lần.
        System.out.println("Số lượng hàng tồn kho hiện tại là: " + inventoryCounter.getItems());
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
     * Lớp InventoryCounter đã được sửa đổi để trở nên "an toàn luồng" (thread-safe).
     */
    private static class InventoryCounter {
        private int items = 0;

        // **CƠ CHẾ KHÓA (LOCKING MECHANISM)**
        // Chúng ta tạo một đối tượng `Object` để hoạt động như một "ổ khóa" (lock).
        // Bất kỳ đối tượng Java nào cũng có thể được dùng làm khóa.
        // Việc tạo một đối tượng khóa riêng biệt là một thực hành tốt, giúp code rõ ràng hơn.
        private final Object lock = new Object();

        public void increment() {
            // **GIẢI PHÁP: KHỐI ĐỒNG BỘ HÓA (SYNCHRONIZED BLOCK)**
            // `synchronized (this.lock)` hoạt động như một cánh cổng.
            // 1. Trước khi một luồng muốn vào khối này, nó phải "giành được ổ khóa" (`this.lock`).
            // 2. Nếu khóa đang tự do, luồng sẽ lấy khóa và đi vào thực thi code bên trong.
            // 3. Nếu một luồng khác đang giữ khóa, luồng hiện tại sẽ bị chặn (blocked) và phải chờ
            //    cho đến khi khóa được "nhả" ra.
            // 4. Sau khi thực thi xong code bên trong, luồng sẽ tự động nhả khóa, cho phép các luồng khác đang chờ có cơ hội vào.
            synchronized (this.lock) {
                // Do được bảo vệ bởi khóa, chuỗi thao tác đọc-sửa-ghi của `items++`
                // bây giờ trở thành NGUYÊN TỬ (ATOMIC). Không có luồng nào khác có thể xen vào giữa chừng.
                items++;
            }
        }

        public void decrement() {
            // Sử dụng cùng một đối tượng khóa (`this.lock`) để bảo vệ thao tác giảm.
            // Điều này đảm bảo rằng `increment()` và `decrement()` không thể chạy cùng một lúc.
            synchronized (this.lock) {
                items--;
            }
        }

        // Việc đọc cũng cần được đồng bộ hóa để đảm bảo rằng chúng ta đọc được giá trị mới nhất
        // và không đọc phải một giá trị "dở dang" trong khi một luồng khác đang cập nhật.
        public int getItems() {
            synchronized (this.lock) {
                return items;
            }
        }
    }
}