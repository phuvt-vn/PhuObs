package com.multithread2.section3ThreadCoordination;

/**
 * Ví dụ này minh họa cách một thread có thể bị "kẹt" trong một tác vụ chặn (blocking task)
 * và cách chúng ta có thể ngắt (interrupt) nó từ một thread khác.
 * Chủ đề: Dừng Thread (Thread Termination) & Thread Daemon.
 */
public class Main1Interrupt {
    public static void main(String [] args) {
        // Tạo một thread mới với một tác vụ có khả năng bị chặn (BlockingTask).
        Thread thread = new Thread(new BlockingTask());

        // Bắt đầu thread.
        thread.start();
        
        // (Trong ví dụ gốc, chương trình sẽ chỉ chờ thread con kết thúc.
        // Để làm cho nó hữu ích hơn, người ta thường sẽ thêm logic để ngắt thread này,
        // ví dụ: thread.interrupt(); sau một khoảng thời gian nhất định.)

    }

    /**
     * Một tác vụ Runnable mô phỏng một công việc mất nhiều thời gian hoặc đang chờ một tài nguyên nào đó.
     */
    private static class BlockingTask implements Runnable {

        @Override
        public void run() {
            // Giả sử thread đang thực hiện một công việc gì đó ở đây.
            try {
                // Thread.sleep() là một phương thức "blocking".
                // Nó sẽ tạm dừng thread hiện tại trong một khoảng thời gian dài (500 giây).
                // Thread sẽ ở trạng thái WAITING hoặc TIMED_WAITING.
                Thread.sleep(500000);
            } catch (InterruptedException e) {
                // Khối catch này sẽ được thực thi nếu một thread khác gọi phương thức `interrupt()` 
                // trên thread đang chạy tác vụ này trong khi nó đang sleep.
                // Đây là cơ chế chuẩn để xử lý yêu cầu dừng từ bên ngoài một cách nhẹ nhàng.
                System.out.println("Thread đang bị chặn đã được ngắt và thoát ra.");
            }
        }
    }
}