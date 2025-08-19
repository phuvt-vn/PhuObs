package com.multithread2.section4PerformanceOptimization;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Chủ đề: Tối ưu hóa độ trễ (Latency) - Phần 2: Xử lý ảnh.
 * Ví dụ này so sánh hiệu năng giữa việc xử lý ảnh bằng một luồng (single-threaded)
 * và nhiều luồng (multi-threaded). 
 * Tác vụ là đổi màu các pixel trong một bức ảnh.
 */
public class MainLatency {
    // Đường dẫn đến file ảnh gốc.
    public static final String SOURCE_FILE = "./resources/many-flowers.jpg";
    // Đường dẫn để lưu file ảnh đã qua xử lý.
    public static final String DESTINATION_FILE = "./out/many-flowers.jpg";

    public static void main(String[] args) throws IOException {

        // Đọc ảnh gốc từ file vào một đối tượng BufferedImage.
        BufferedImage originalImage = ImageIO.read(new File(SOURCE_FILE));
        // Tạo một ảnh mới để chứa kết quả, có cùng kích thước và loại với ảnh gốc.
        BufferedImage resultImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        // Ghi lại thời gian bắt đầu để đo hiệu năng.
        long startTime = System.currentTimeMillis();

        // --- CHỌN PHƯƠNG THỨC ĐỂ CHẠY ---
        // Chạy phiên bản đơn luồng (để so sánh).
        // recolorSingleThreaded(originalImage, resultImage);
        
        // Chạy phiên bản đa luồng. Bạn có thể thay đổi số luồng ở đây.
        // Các giá trị tốt thường là 2, 4, 6, 8, tùy thuộc vào số lõi CPU của bạn.
        int numberOfThreads = 4;
        recolorMultithreaded(originalImage, resultImage, numberOfThreads);
        
        // Ghi lại thời gian kết thúc.
        long endTime = System.currentTimeMillis();

        // Tính toán tổng thời gian xử lý.
        long duration = endTime - startTime;

        // Tạo file đầu ra và ghi ảnh kết quả vào đó.
        File outputFile = new File(DESTINATION_FILE);
        ImageIO.write(resultImage, "jpg", outputFile);

        // In ra thời gian xử lý (tính bằng mili giây).
        System.out.println("Thời gian xử lý với " + numberOfThreads + " luồng: " + String.valueOf(duration) + " ms");
    }

    /**
     * Đổi màu ảnh bằng cách sử dụng nhiều luồng.
     * @param originalImage Ảnh gốc.
     * @param resultImage Ảnh để lưu kết quả.
     * @param numberOfThreads Số luồng sẽ được sử dụng.
     */
    public static void recolorMultithreaded(BufferedImage originalImage, BufferedImage resultImage, int numberOfThreads) {
        List<Thread> threads = new ArrayList<>();
        int width = originalImage.getWidth();
        // **CHIẾN LƯỢC PHÂN CHIA:** Chia ảnh thành các dải ngang.
        // Mỗi luồng sẽ xử lý một dải có chiều cao là `height`.
        int height = originalImage.getHeight() / numberOfThreads;

        for(int i = 0; i < numberOfThreads ; i++) {
            final int threadMultiplier = i;

            // Tạo một thread mới cho mỗi dải ảnh.
            Thread thread = new Thread(() -> {
                int xOrigin = 0 ; // Bắt đầu từ cột 0.
                // Tính toán tọa độ y bắt đầu cho dải của luồng này.
                int yOrigin = height * threadMultiplier;

                // Gọi hàm xử lý cho một phần của ảnh (dải được giao).
                recolorImage(originalImage, resultImage, xOrigin, yOrigin, width, height);
            });

            threads.add(thread);
        }

        // Bắt đầu tất cả các luồng để chúng chạy song song.
        for(Thread thread : threads) {
            thread.start();
        }

        // **QUAN TRỌNG:** Chờ tất cả các luồng hoàn thành công việc.
        // Nếu không có `join()`, luồng `main` có thể kết thúc trước khi các luồng con xử lý xong ảnh,
        // dẫn đến ảnh kết quả bị thiếu hoặc sai.
        for(Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Đổi màu ảnh chỉ bằng một luồng duy nhất.
     */
    public static void recolorSingleThreaded(BufferedImage originalImage, BufferedImage resultImage) {
        recolorImage(originalImage, resultImage, 0, 0, originalImage.getWidth(), originalImage.getHeight());
    }

    /**
     * Hàm cốt lõi, xử lý việc đổi màu cho một vùng chữ nhật của ảnh.
     * @param leftCorner Tọa độ x của góc trên bên trái vùng cần xử lý.
     * @param topCorner Tọa độ y của góc trên bên trái vùng cần xử lý.
     * @param width Chiều rộng của vùng cần xử lý.
     * @param height Chiều cao của vùng cần xử lý.
     */
    public static void recolorImage(BufferedImage originalImage, BufferedImage resultImage, int leftCorner, int topCorner,
                                    int width, int height) {
        // Lặp qua từng pixel trong vùng được chỉ định.
        for(int x = leftCorner ; x < leftCorner + width && x < originalImage.getWidth() ; x++) {
            for(int y = topCorner ; y < topCorner + height && y < originalImage.getHeight() ; y++) {
                // Đổi màu cho pixel tại tọa độ (x, y).
                recolorPixel(originalImage, resultImage, x , y);
            }
        }
    }

    /**
     * Logic đổi màu cho một pixel cụ thể.
     */
    public static void recolorPixel(BufferedImage originalImage, BufferedImage resultImage, int x, int y) {
        // Lấy giá trị màu RGB của pixel gốc.
        int rgb = originalImage.getRGB(x, y);

        // Tách các thành phần màu Red, Green, Blue từ giá trị RGB.
        int red = getRed(rgb);
        int green = getGreen(rgb);
        int blue = getBlue(rgb);

        int newRed;
        int newGreen;
        int newBlue;

        // **Logic nghiệp vụ:** Nếu pixel là một sắc thái của màu xám...
        if(isShadeOfGray(red, green, blue)) {
            // ...thì tăng thành phần màu đỏ, giảm mạnh màu xanh lá, và giảm nhẹ màu xanh dương.
            // Điều này sẽ làm cho các vùng màu xám trong ảnh có xu hướng ngả sang màu tím/hồng.
            newRed = Math.min(255, red + 10);
            newGreen = Math.max(0, green - 80);
            newBlue = Math.max(0, blue - 20);
        } else {
            // Nếu không phải màu xám, giữ nguyên màu của pixel.
            newRed = red;
            newGreen = green;
            newBlue = blue;
        }
        // Tạo lại giá trị RGB từ các thành phần màu mới.
        int newRGB = createRGBFromColors(newRed, newGreen, newBlue);
        // Gán màu mới cho pixel tương ứng trong ảnh kết quả.
        setRGB(resultImage, x, y, newRGB);
    }

    /**
     * Gán một giá trị RGB cho một pixel tại tọa độ (x, y) của ảnh.
     */
    public static void setRGB(BufferedImage image, int x, int y, int rgb) {
        image.getRaster().setDataElements(x, y, image.getColorModel().getDataElements(rgb, null));
    }

    /**
     * Kiểm tra xem một màu có phải là một sắc thái của màu xám hay không.
     * Logic ở đây là nếu các giá trị Red, Green, Blue gần bằng nhau thì đó là màu xám.
     */
    public static boolean isShadeOfGray(int red, int green, int blue) {
        return Math.abs(red - green) < 30 && Math.abs(red - blue) < 30 && Math.abs( green - blue) < 30;
    }

    /**
     * Tổng hợp lại một giá trị integer RGB từ ba thành phần màu.
     * Đây là thao tác xử lý bit (bitwise operations).
     */
    public static int createRGBFromColors(int red, int green, int blue) {
        int rgb = 0;

        rgb |= blue;          // Gán 8 bit của blue vào 8 bit cuối.
        rgb |= green << 8;    // Dịch trái green 8 bit rồi OR với kết quả.
        rgb |= red << 16;     // Dịch trái red 16 bit rồi OR với kết quả.

        // Đặt giá trị Alpha thành 255 (hoàn toàn không trong suốt).
        rgb |= 0xFF000000;

        return rgb;
    }

    /**
     * Trích xuất thành phần màu Đỏ (Red) từ một giá trị RGB integer.
     */
    public static int getRed(int rgb) {
        return (rgb & 0x00FF0000) >> 16;
    }

    /**
     * Trích xuất thành phần màu Xanh lá (Green) từ một giá trị RGB integer.
     */
    public static int getGreen(int rgb) {
        return (rgb & 0x0000FF00) >> 8;
    }

    /**
     * Trích xuất thành phần màu Xanh dương (Blue) từ một giá trị RGB integer.
     */
    public static int getBlue(int rgb) {
        return rgb & 0x000000FF;
    }
}