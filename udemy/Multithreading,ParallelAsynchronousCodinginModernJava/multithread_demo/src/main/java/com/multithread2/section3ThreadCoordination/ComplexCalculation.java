package com.multithread2.section3ThreadCoordination;

import java.math.BigInteger;

/**
 * Lớp này thực hiện một phép tính phức tạp, cụ thể là tính tổng của hai lũy thừa:
 * `base1 ^ power1 + base2 ^ power2`.
 * Mục đích là để minh họa một tác vụ tính toán dài có thể được thực thi trên một thread riêng biệt
 * và có khả năng bị ngắt giữa chừng.
 */
public class ComplexCalculation {

    /**
     * Phương thức chính để thực hiện phép tính.
     * @param base1 Cơ số thứ nhất.
     * @param power1 Số mũ thứ nhất.
     * @param base2 Cơ số thứ hai.
     * @param power2 Số mũ thứ hai.
     * @return Kết quả của `base1^power1 + base2^power2`.
     * @throws InterruptedException nếu thread bị ngắt trong quá trình tính toán.
     */
    public BigInteger calculateResult(BigInteger base1, BigInteger power1, BigInteger base2, BigInteger power2) throws InterruptedException {
        BigInteger result;
        
        // Tạo hai tác vụ con để tính toán hai lũy thừa riêng biệt.
        PowerCalculatingThread thread1 = new PowerCalculatingThread(base1, power1);
        PowerCalculatingThread thread2 = new PowerCalculatingThread(base2, power2);

        // Bắt đầu cả hai thread để chúng chạy song song.
        thread1.start();
        thread2.start();

        // **Sử dụng join() để chờ các thread con hoàn thành**
        // Thread hiện tại (đang chạy phương thức này) sẽ bị chặn tại đây cho đến khi thread1 kết thúc.
        thread1.join();
        // Tương tự, chờ thread2 kết thúc.
        thread2.join();

        // Sau khi cả hai thread đã hoàn thành, lấy kết quả của chúng và cộng lại.
        result = thread1.getResult().add(thread2.getResult());
        return result;
    }

    /**
     * Lớp nội bộ (inner class) đại diện cho một thread chỉ để tính toán lũy thừa.
     * Việc tách ra như thế này giúp quản lý code dễ dàng hơn.
     */
    private static class PowerCalculatingThread extends Thread {
        private BigInteger result = BigInteger.ONE;
        private BigInteger base;
        private BigInteger power;

        public PowerCalculatingThread(BigInteger base, BigInteger power) {
            this.base = base;
            this.power = power;
        }

        @Override
        public void run() {
            // Vòng lặp để tính toán `base^power`.
            for (BigInteger i = BigInteger.ZERO; i.compareTo(power) != 0; i = i.add(BigInteger.ONE)) {
                // **Kiểm tra ngắt:** Rất quan trọng đối với các tác vụ dài.
                // Nếu thread nhận được yêu cầu ngắt, nó sẽ dừng tính toán và thoát.
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Thread tính toán lũy thừa đã bị ngắt.");
                    // Đặt kết quả về 0 để báo hiệu có lỗi xảy ra.
                    this.result = BigInteger.ZERO;
                    return; // Kết thúc phương thức run()
                }
                this.result = this.result.multiply(base);
            }
        }

        public BigInteger getResult() {
            return result;
        }
    }

    // --- HÀM MAIN ĐỂ CHẠY VÍ DỤ ---
    public static void main(String[] args) throws InterruptedException {
        ComplexCalculation calculation = new ComplexCalculation();

        // Định nghĩa các số lớn để tính toán.
        BigInteger base1 = new BigInteger("12345");
        BigInteger power1 = new BigInteger("54321");
        BigInteger base2 = new BigInteger("54321");
        BigInteger power2 = new BigInteger("12345");

        System.out.println("Bắt đầu phép tính phức tạp...");
        
        // Thực thi phép tính trong một thread riêng để thread `main` không bị block hoàn toàn
        // và có thể thực hiện các hành động khác (như ngắt phép tính).
        Thread calculationThread = new Thread(() -> {
            try {
                BigInteger result = calculation.calculateResult(base1, power1, base2, power2);
                System.out.println("Kết quả cuối cùng là: " + result);
            } catch (InterruptedException e) {
                System.out.println("Thread tính toán chính đã bị ngắt.");
            }
        });

        // Bắt đầu thread tính toán chính.
        calculationThread.start();

        // Cho phép tính toán chạy trong 2 giây.
        Thread.sleep(2000);

        // Sau 2 giây, gửi yêu cầu ngắt đến thread tính toán.
        // Điều này sẽ làm cho các phương thức `join()` trong `calculateResult` ném ra InterruptedException
        // hoặc các vòng lặp trong `PowerCalculatingThread` dừng lại.
        System.out.println("Gửi yêu cầu dừng đến phép tính...");
        calculationThread.interrupt();
    }
}
