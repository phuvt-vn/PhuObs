package com.multithread2.section2ThreadCreation;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        // Tạo một thread mới bằng cách sử dụng biểu thức lambda.
        // Đây là cách hiện đại để định nghĩa tác vụ mà thread sẽ thực thi.
        Thread thread = new Thread(() ->  {

            // In ra tên của thread hiện tại đang thực thi.
            System.out.println("Hello World in " +  Thread.currentThread().getName());
            // In ra độ ưu tiên của thread hiện tại.
            System.out.println("current priority is " +  Thread.currentThread().getPriority());
            // Ném ra một ngoại lệ để kiểm tra việc xử lý ngoại lệ.
            throw new RuntimeException("this is test exception");

        });

        // Đặt tên cho thread mới là "PHU THREAD".
        thread.setName("PHU THREAD");
        // Đặt độ ưu tiên cho thread ở mức cao nhất.
        // Lưu ý: Việc hệ điều hành có tôn trọng độ ưu tiên này hay không còn phụ thuộc vào nhiều yếu tố.
        thread.setPriority(Thread.MAX_PRIORITY);

        // Thiết lập một trình xử lý cho các ngoại lệ không được bắt trong thread.
        // Nếu có lỗi xảy ra trong thread mà không được xử lý, lambda này sẽ được gọi.
        thread.setUncaughtExceptionHandler((t, e) -> System.out.println("error happen in thread " +  Thread.currentThread().getName()+ " the error is: " + e.getMessage()));

        // In ra tên của thread chính (main thread) trước khi bắt đầu thread mới.
        System.out.println("this is start " +  Thread.currentThread().getName());
        // Bắt đầu thực thi thread mới. Lệnh này sẽ trở về ngay lập tức, và thread mới sẽ chạy song song.
        thread.start();
        // In ra tên của thread chính (main thread) sau khi đã gọi start().
        System.out.println("this is end " +  Thread.currentThread().getName());


    }


}