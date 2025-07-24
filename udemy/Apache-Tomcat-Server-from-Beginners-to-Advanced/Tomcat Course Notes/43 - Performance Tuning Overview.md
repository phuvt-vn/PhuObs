# Bài 43: Tổng quan về Tối ưu hiệu năng (Performance Tuning)

- **Thời lượng**: 05:26
- **Bài trước**: [[42 - Tomcat Logging Part4]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[44 - Setting Up Monitoring]]

---

## Nội dung

Bài học này cung cấp một cái nhìn tổng quan về các lĩnh vực chính cần quan tâm khi tối ưu hóa hiệu năng cho một máy chủ Apache Tomcat. Tối ưu hiệu năng là một quá trình liên tục nhằm mục đích làm cho ứng dụng của bạn chạy nhanh hơn, xử lý được nhiều request hơn, và sử dụng tài nguyên một cách hiệu quả hơn.

### Triết lý của Tối ưu hiệu năng

-   **"Đừng tối ưu hóa sớm" (Don't optimize prematurely)**: Đừng dành thời gian tối ưu những thứ không phải là "nút thắt cổ chai" (bottleneck).
-   **Đo lường -> Phân tích -> Tối ưu -> Đo lường lại**: Đây là một chu trình lặp đi lặp lại. Bạn không thể cải thiện cái mà bạn không thể đo lường.
-   **Tối ưu hóa là một sự đánh đổi**: Cải thiện một khía cạnh (ví dụ: tốc độ) có thể làm ảnh hưởng đến một khía cạnh khác (ví dụ: mức sử dụng bộ nhớ).

### Các lĩnh vực chính cần tối ưu trong Tomcat

1.  **JVM (Java Virtual Machine) Tuning - Tối ưu máy ảo Java**:
    -   Tomcat là một ứng dụng Java, vì vậy hiệu năng của nó phụ thuộc rất nhiều vào JVM.
    -   **Heap Size**: Cấu hình kích thước bộ nhớ heap (`-Xms` cho kích thước ban đầu, `-Xmx` cho kích thước tối đa) là một trong những tinh chỉnh quan trọng nhất. Kích thước heap quá nhỏ sẽ gây ra lỗi `OutOfMemoryError`, trong khi quá lớn có thể gây ra thời gian tạm dừng dài khi Garbage Collection (GC) chạy.
    -   **Garbage Collection (GC) Tuning**: Lựa chọn và tinh chỉnh thuật toán GC (ví dụ: G1GC, ZGC, Shenandoah) có thể làm giảm đáng kể thời gian tạm dừng của ứng dụng.
    -   Các tham số JVM khác (`Metaspace`, `Code Cache`, ...).

2.  **Connector & Thread Pool Tuning - Tối ưu Trình kết nối và Vùng chứa luồng**:
    -   **Connector** là thành phần của Tomcat nhận các kết nối từ client.
    -   **Thread Pool (Executor)** là nơi quản lý các luồng (threads) để xử lý các request.
    -   Các thuộc tính quan trọng cần tinh chỉnh:
        -   `maxThreads`: Số lượng luồng xử lý request tối đa. Nếu quá thấp, các request sẽ phải xếp hàng chờ. Nếu quá cao, nó sẽ lãng phí bộ nhớ và có thể gây ra hiện tượng "context switching" quá mức.
        -   `minSpareThreads`: Số lượng luồng tối thiểu luôn được giữ sẵn sàng.
        -   `acceptCount`: Số lượng kết nối tối đa được phép xếp hàng chờ khi tất cả các luồng đều đang bận.
        -   `connectionTimeout`: Thời gian chờ một request trước khi đóng kết nối.

3.  **Application Code - Tối ưu mã nguồn ứng dụng**:
    -   Đây thường là nơi có thể mang lại sự cải thiện hiệu năng lớn nhất.
    -   Các vấn đề phổ biến:
        -   Các câu truy vấn cơ sở dữ liệu chậm.
        -   Thuật toán không hiệu quả (vòng lặp lồng nhau không cần thiết).
        -   Tạo ra quá nhiều đối tượng không cần thiết, gây áp lực lên GC.
        -   Sử dụng các cấu trúc dữ liệu không phù hợp.
        -   Các vấn đề về đồng bộ hóa (synchronization) gây tắc nghẽn.

4.  **Database Connection Pool Tuning - Tối ưu Vùng chứa kết nối DB**:
    -   Như đã học ở [[Bài 29]], việc cấu hình đúng các tham số của connection pool (`maxTotal`, `maxIdle`, `maxWaitMillis`) là rất quan trọng để tránh tình trạng ứng dụng phải chờ kết nối DB.

5.  **HTTP Response Compression - Nén phản hồi HTTP**:
    -   Kích hoạt tính năng nén (GZIP) cho các response dạng text (HTML, CSS, JS, JSON, XML) có thể làm giảm đáng kể kích thước dữ liệu truyền qua mạng, giúp trang web tải nhanh hơn đối với người dùng.

Trong các bài học tiếp theo của phần này, chúng ta sẽ đi sâu vào cách giám sát và tinh chỉnh từng lĩnh vực này.
