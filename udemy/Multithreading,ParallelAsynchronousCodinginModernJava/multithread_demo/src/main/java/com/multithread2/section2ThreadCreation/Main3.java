package com.multithread2.section2ThreadCreation;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp này minh họa cách tạo và quản lý một nhóm các thread.
 * Nó nhận một danh sách các tác vụ (Runnable) và thực thi mỗi tác vụ trong một thread riêng biệt.
 * Mục đích là để chạy nhiều tác vụ song song, tận dụng khả năng của CPU đa lõi.
 */
public class Main3 {

  // Danh sách các tác vụ (dưới dạng đối tượng Runnable) cần được thực thi.
  // `Runnable` là một interface trong Java chỉ có một phương thức `run()`, đại diện cho một đơn vị công việc có thể chạy.
  // `final` ở đây có nghĩa là danh sách này sẽ được gán một lần duy nhất trong constructor và không thể thay đổi tham chiếu sau đó.
  private final List<Runnable> tasks;
 

    /**
     * Constructor để khởi tạo đối tượng Main3 với một danh sách các tác vụ.
     * @param tasks Danh sách các đối tượng Runnable mà chúng ta muốn thực thi.
     */
    public Main3(List<Runnable> tasks) {
        this.tasks = tasks;
    }
 

    /**
     * Thực thi tất cả các tác vụ trong danh sách.
     * Mỗi tác vụ sẽ được chạy trên một thread riêng.
     * Phương thức này sẽ không chờ các thread hoàn thành mà sẽ trở về ngay sau khi tất cả đã được bắt đầu.
     */
    public void executeAll() {
        // Tạo một danh sách để lưu trữ các đối tượng Thread.
        // Việc này hữu ích nếu sau này chúng ta muốn quản lý các thread này (ví dụ: chờ chúng hoàn thành với thread.join()).
        List<Thread> threads = new ArrayList<>(tasks.size());
        
        // Lặp qua danh sách các tác vụ (Runnable) đã được cung cấp.
        for (Runnable task : tasks) {
            // Với mỗi tác vụ `Runnable`, chúng ta tạo một đối tượng `Thread` mới.
            // `Thread` sẽ "bọc" lấy `Runnable` và chịu trách nhiệm thực thi phương thức `run()` của nó trong một luồng riêng.
            Thread thread = new Thread(task);
            // Thêm thread vừa tạo vào danh sách các thread để quản lý.
            threads.add(thread);
        }
        
        // Lặp qua danh sách các thread đã được tạo.
        for(Thread thread : threads) {
            // Bắt đầu thực thi mỗi thread. 
            // Lệnh `start()` sẽ yêu cầu JVM cấp phát một luồng thực thi mới và gọi phương thức `run()` của tác vụ tương ứng trên luồng đó.
            // Lệnh này không phải là một lời gọi hàm đồng bộ, nó sẽ trở về ngay lập tức.
            thread.start();
        }
    }

    // --- PHẦN CODE VÍ DỤ --- 
    // Phương thức `main` là điểm khởi đầu của chương trình Java.
    // Chúng ta sử dụng nó ở đây để tạo một ví dụ cụ thể về cách sử dụng lớp `Main3`.
    public static void main(String[] args) {
        // 1. Tạo một danh sách để chứa các tác vụ cần thực thi.
        System.out.println("Chuẩn bị các tác vụ...");
        List<Runnable> tasks = new ArrayList<>();

        // Tác vụ 1: Một tác vụ đơn giản mô phỏng một công việc mất thời gian (ví dụ: gọi API, truy vấn database).
        // Chúng ta sử dụng biểu thức lambda `() -> { ... }` để định nghĩa một `Runnable` một cách ngắn gọn.
        tasks.add(() -> {
            try {
                // In ra thông báo để biết tác vụ đã bắt đầu, cùng với tên của thread đang thực thi nó.
                System.out.println("Task 1: Bắt đầu... (Thread: " + Thread.currentThread().getName() + ")");
                // `Thread.sleep(1000)` tạm dừng thread hiện tại trong 1000 mili giây (1 giây).
                Thread.sleep(1000);
                System.out.println("Task 1: Kết thúc.");
            } catch (InterruptedException e) {
                // `InterruptedException` được ném ra nếu thread bị ngắt trong khi đang sleep.
                e.printStackTrace();
            }
        });

        // Tác vụ 2: Một tác vụ khác tương tự, nhưng mất nhiều thời gian hơn.
        tasks.add(() -> {
            try {
                System.out.println("Task 2: Bắt đầu... (Thread: " + Thread.currentThread().getName() + ")");
                // Tạm dừng trong 1.5 giây.
                Thread.sleep(1500);
                System.out.println("Task 2: Kết thúc.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Tác vụ 3: Một tác vụ rất nhanh, hoàn thành gần như ngay lập tức.
        tasks.add(() -> {
            System.out.println("Task 3: Bắt đầu và kết thúc ngay lập tức. (Thread: " + Thread.currentThread().getName() + ")");
        });

        // 2. Tạo một đối tượng `Main3` (executor) và truyền danh sách tác vụ vào constructor của nó.
        Main3 executor = new Main3(tasks);

        // 3. Gọi phương thức `executeAll()` để bắt đầu chạy tất cả các tác vụ trên các thread riêng biệt.
        System.out.println("Bắt đầu thực thi tất cả các tác vụ...");
        executor.executeAll();

        // Dòng này sẽ được in ra gần như ngay lập tức sau khi `executeAll()` được gọi,
        // bởi vì `executeAll()` không chờ các thread con hoàn thành.
        // Thread `main` sẽ tiếp tục chạy trong khi các tác vụ 1, 2, 3 đang được thực thi ở chế độ nền.
        System.out.println("Tất cả các tác vụ đã được khởi chạy. Thread 'main' đã hoàn thành công việc của mình.");
    }
}