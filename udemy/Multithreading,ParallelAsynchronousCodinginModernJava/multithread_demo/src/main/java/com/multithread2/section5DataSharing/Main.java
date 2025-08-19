package com.multithread2.section5DataSharing;

/**
 * Chủ đề: Chia sẻ tài nguyên và Vùng Găng (Critical Sections)
 * ----------------------------------------------------------
 * Đây là một ví dụ kinh điển để minh họa lỗi **Race Condition** (Tình trạng tranh chấp),
 * một trong những lỗi phổ biến và khó lường nhất trong lập trình song song.
 */
public class Main {

    /**
     * Phương thức chính của chương trình, nơi chúng ta thiết lập và chạy các luồng.
     */
    public static void main(String[] args) throws InterruptedException {
        // 1. TẠO TÀI NGUYÊN CHIA SẺ (SHARED RESOURCE)
        // `inventoryCounter` là một đối tượng duy nhất được cả hai luồng cùng sử dụng.
        // Biến `items` bên trong nó chính là dữ liệu mà các luồng sẽ tranh chấp nhau để đọc và ghi.
        InventoryCounter inventoryCounter = new InventoryCounter();

        // 2. TẠO CÁC LUỒNG (THREADS)
        // Tạo một luồng để tăng số lượng hàng tồn kho.
        IncrementingThread incrementingThread = new IncrementingThread(inventoryCounter);
        // Tạo một luồng để giảm số lượng hàng tồn kho.
        DecrementingThread decrementingThread = new DecrementingThread(inventoryCounter);

        // 3. KHỞI CHẠY CÁC LUỒNG
        // `start()` sẽ yêu cầu hệ điều hành cho các luồng này chạy. 
        // Chúng sẽ chạy song song và thứ tự thực thi các lệnh của chúng là không thể đoán trước.
        incrementingThread.start();
        decrementingThread.start();

        // 4. CHỜ CÁC LUỒNG HOÀN THÀNH
        // `join()` sẽ bắt luồng `main` phải đợi cho đến khi `incrementingThread` kết thúc công việc.
        incrementingThread.join();
        // Tương tự, đợi `decrementingThread` kết thúc.
        decrementingThread.join();

        // 5. IN KẾT QUẢ
        // Về mặt logic, nếu `items` bắt đầu từ 0, được tăng 10,000 lần và giảm 10,000 lần,
        // kết quả cuối cùng phải là 0. 
        // Tuy nhiên, do Race Condition, kết quả bạn thấy sẽ gần như luôn khác 0.
        System.out.println("Kết quả cuối cùng của số lượng hàng là: " + inventoryCounter.getItems());
    }

    /**
     * Luồng này có nhiệm vụ GIẢM giá trị của bộ đếm.
     */
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

    /**
     * Luồng này có nhiệm vụ TĂNG giá trị của bộ đếm.
     */
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
     * Lớp chứa dữ liệu được chia sẻ.
     */
    private static class InventoryCounter {
        // `items` là trạng thái (state) được chia sẻ, là nguồn gốc của vấn đề.
        private int items = 0;

        // **VÙNG GĂNG (CRITICAL SECTION) - NGUY HIỂM!**
        // Một "vùng găng" là một đoạn code truy cập đến tài nguyên được chia sẻ.
        // Vấn đề ở đây là các thao tác trong vùng găng này không phải là "nguyên tử" (atomic).
        public void increment() {
            // Thao tác `items++` không phải là một lệnh duy nhất.
            // Nó được biên dịch thành 3 bước:
            //   1. READ: Đọc giá trị của `items` từ bộ nhớ chính.
            //   2. MODIFY: Tăng giá trị đã đọc lên 1.
            //   3. WRITE: Ghi giá trị mới trở lại bộ nhớ chính.
            // Race condition xảy ra khi một luồng khác xen vào giữa 3 bước này.
            items++;
        }

        public void decrement() {
            // Tương tự, `items--` cũng là một thao tác không nguyên tử.
            items--;
        }

        // Phương thức này chỉ đọc, nên ít nguy hiểm hơn, nhưng vẫn có thể đọc phải dữ liệu "dở dang"
        // nếu không được đồng bộ hóa đúng cách.
        public int getItems() {
            return items;
        }
    }
}