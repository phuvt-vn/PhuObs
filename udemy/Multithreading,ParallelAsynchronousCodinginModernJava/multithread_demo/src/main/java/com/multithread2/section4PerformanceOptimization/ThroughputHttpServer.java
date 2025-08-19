package com.multithread2.section4PerformanceOptimization;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Chủ đề: Tối ưu hóa Thông lượng (Throughput) - Phần 2: Máy chủ HTTP + JMeter.
 * Ví dụ này tạo ra một máy chủ HTTP đơn giản có khả năng xử lý nhiều yêu cầu cùng lúc
 * bằng cách sử dụng một "bể luồng" (thread pool).
 * Mục tiêu là để tối đa hóa số lượng yêu cầu có thể phục vụ trong một khoảng thời gian nhất định.
 */
public class ThroughputHttpServer {
    // Đường dẫn đến file văn bản lớn (ví dụ: "Chiến tranh và Hòa bình").
    private static final String INPUT_FILE = "./resources/war_and_peace.txt";
    // **ĐIỂM MẤU CHỐT:** Số lượng luồng cố định trong thread pool.
    // Con số này nên được điều chỉnh (tune) để phù hợp với phần cứng và loại tác vụ.
    // Một điểm khởi đầu tốt thường là số lõi CPU của máy.
    private static final int NUMBER_OF_THREADS = 4;

    public static void main(String[] args) throws IOException {
        // Đọc toàn bộ nội dung của file văn bản vào một chuỗi String.
        // Việc này được làm một lần duy nhất khi máy chủ khởi động.
        String text = new String(Files.readAllBytes(Paths.get(INPUT_FILE)));
        // Khởi động máy chủ.
        startServer(text);
    }

    /**
     * Khởi tạo và cấu hình máy chủ HTTP.
     * @param text Nội dung văn bản mà máy chủ sẽ tìm kiếm.
     * @throws IOException
     */
    public static void startServer(String text) throws IOException {
        // Tạo một máy chủ HTTP lắng nghe trên cổng 8000.
        // Tham số thứ hai là `backlog`, số 0 có nghĩa là sử dụng giá trị mặc định của hệ thống.
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        // Tạo một "context" hay một đường dẫn (endpoint) mà máy chủ sẽ xử lý.
        // Ở đây, mọi yêu cầu đến "/search" sẽ được xử lý bởi `WordCountHandler`.
        server.createContext("/search", new WordCountHandler(text));

        // **TỐI ƯU HÓA THÔNG LƯỢNG:**
        // Tạo một Executor với một số lượng luồng cố định (FixedThreadPool).
        // `Executor` này sẽ quản lý một nhóm các luồng đã được tạo sẵn.
        Executor executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

        // Gán `Executor` này cho máy chủ.
        // Thay vì tạo một luồng mới cho mỗi yêu cầu (rất tốn kém), máy chủ sẽ lấy một luồng có sẵn
        // từ `executor` để xử lý yêu cầu. Sau khi xử lý xong, luồng đó được trả lại bể để tái sử dụng.
        // Việc này giúp giảm độ trễ do tạo luồng và tăng thông lượng tổng thể.
        server.setExecutor(executor);
        
        // Bắt đầu máy chủ. Nó sẽ chạy ở chế độ nền để lắng nghe các yêu cầu đến.
        server.start();
        System.out.println("Máy chủ đã khởi động trên cổng 8000...");
    }

    /**
     * Lớp xử lý logic cho các yêu cầu đến endpoint "/search".
     */
    private static class WordCountHandler implements HttpHandler {
        private String text;

        public WordCountHandler(String text) {
            this.text = text;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            // Lấy phần query của URL. Ví dụ: "word=book".
            String query = httpExchange.getRequestURI().getQuery();
            String[] keyValue = query.split("=");
            String action = keyValue[0];
            String word = keyValue[1];

            // Kiểm tra xem tham số có phải là "word" hay không.
            if (!action.equals("word")) {
                // Nếu không hợp lệ, gửi mã lỗi 400 (Bad Request).
                httpExchange.sendResponseHeaders(400, 0);
                return;
            }

            // Thực hiện công việc chính: đếm số lần từ xuất hiện.
            long count = countWord(word);

            // Chuẩn bị phản hồi cho client.
            byte[] response = Long.toString(count).getBytes();
            // Gửi header phản hồi với mã 200 (OK) và độ dài của nội dung.
            httpExchange.sendResponseHeaders(200, response.length);
            // Lấy stream để ghi nội dung phản hồi.
            OutputStream outputStream = httpExchange.getResponseBody();
            // Ghi nội dung phản hồi.
            outputStream.write(response);
            // Đóng stream.
            outputStream.close();
        }

        /**
         * Đếm số lần một từ xuất hiện trong văn bản.
         * Đây là một tác vụ tốn nhiều CPU.
         */
        private long countWord(String word) {
            long count = 0;
            int index = 0;
            while (index >= 0) {
                // Tìm vị trí xuất hiện tiếp theo của từ, bắt đầu từ `index`.
                index = text.indexOf(word, index);

                if (index >= 0) {
                    count++;
                    // Di chuyển index tới vị trí tiếp theo để tránh tìm thấy cùng một từ lặp lại.
                    index++;
                }
            }
            return count;
        }
    }
}