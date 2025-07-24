# Bài 39: Ghi log trong Tomcat - Phần 1: Giới thiệu

- **Thời lượng**: 18:56
- **Bài trước**: [[38 - Example - Configuring JDBC Realm for User Authentication]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[40 - Tomcat Logging Part2]]

---

## Nội dung

Ghi log (logging) là một trong những khía cạnh quan trọng nhất của việc quản trị và vận hành bất kỳ ứng dụng nào. Log cung cấp thông tin chi tiết về những gì đang xảy ra bên trong hệ thống, giúp cho việc gỡ lỗi (debugging), giám sát (monitoring), và phân tích bảo mật (security auditing) trở nên khả thi.

Bài học này giới thiệu về hệ thống logging trong Tomcat và các loại log khác nhau mà nó tạo ra.

### Tại sao Logging lại quan trọng?

-   **Gỡ lỗi (Debugging)**: Khi có lỗi xảy ra, log là nơi đầu tiên bạn tìm đến để xem nguyên nhân gốc rễ, stack trace, và ngữ cảnh của lỗi.
-   **Giám sát (Monitoring)**: Phân tích log giúp bạn theo dõi sức khỏe của ứng dụng, phát hiện các vấn đề về hiệu năng, và hiểu được hành vi của người dùng.
-   **Bảo mật (Security Auditing)**: Log ghi lại các sự kiện quan trọng như các lần đăng nhập thành công/thất bại, các nỗ lực truy cập trái phép, giúp bạn phát hiện và điều tra các hoạt động đáng ngờ.
-   **Phân tích nghiệp vụ (Business Analytics)**: Dữ liệu từ log có thể được sử dụng để phân tích xu hướng, hành vi người dùng, và các chỉ số kinh doanh khác.

### Các loại Log chính trong Tomcat

Tomcat tạo ra nhiều loại log khác nhau, mỗi loại phục vụ một mục đích riêng. Tất cả chúng theo mặc định đều nằm trong thư mục `$CATALINA_HOME/logs`.

1.  **Catalina Logs (`catalina.out` hoặc `catalina.YYYY-MM-DD.log`)**
    -   Đây là file log chính của server Tomcat.
    -   Nó ghi lại tất cả các output được gửi đến `System.out` và `System.err` bởi Tomcat và các ứng dụng web chạy trên nó.
    -   Chứa các thông điệp về quá trình khởi động, dừng của server, các lỗi khi triển khai ứng dụng, các exception không được xử lý, và các thông tin trạng thái quan trọng khác.
    -   Trên Linux, tất cả output thường được gộp vào một file duy nhất là `catalina.out`. Trên Windows, log thường được xoay vòng hàng ngày thành các file có dạng `catalina.YYYY-MM-DD.log`.

2.  **localhost Logs (`localhost.YYYY-MM-DD.log`)**
    -   File log này dành riêng cho virtual host `localhost`.
    -   Nó ghi lại các sự kiện liên quan đến host đó, chẳng hạn như khi một ứng dụng trên host đó được bắt đầu hoặc dừng lại.
    -   Nếu bạn có nhiều virtual host, mỗi host có thể có file log riêng của nó (ví dụ: `site1.com.YYYY-MM-DD.log`).

3.  **Access Logs (`localhost_access_log.YYYY-MM-DD.txt`)**
    -   Ghi lại một dòng cho **mỗi request HTTP** đến server.
    -   Cực kỳ hữu ích để phân tích lưu lượng truy cập.
    -   Mỗi dòng log chứa thông tin như:
        -   Địa chỉ IP của client.
        -   Thời gian request.
        -   Phương thức HTTP (GET, POST, ...).
        -   URL được yêu cầu.
        -   Mã trạng thái HTTP trả về (200, 404, 500, ...).
        -   Kích thước của response.
        -   User-Agent của trình duyệt.
    -   Được cấu hình bởi `AccessLogValve`.

4.  **Manager / Host Manager Logs (`manager.YYYY-MM-DD.log`, `host-manager.YYYY-MM-DD.log`)**
    -   Ghi lại các hoạt động được thực hiện thông qua các ứng dụng quản trị Manager và Host Manager.
    -   Ví dụ: ghi lại khi nào một ứng dụng được triển khai, gỡ bỏ, hoặc khi một virtual host mới được tạo. Rất quan trọng cho việc kiểm toán bảo mật.

### Framework Logging của Tomcat

-   Bản thân Tomcat sử dụng một framework logging riêng gọi là **JULI**, là một phiên bản mở rộng của `java.util.logging` (JUL) - framework logging chuẩn của Java.
-   JULI cho phép cấu hình logging một cách linh hoạt cho từng ứng dụng web, thay vì chỉ có một cấu hình logging toàn cục.
-   Cấu hình chính cho JULI nằm ở file `$CATALINA_HOME/conf/logging.properties`.

Các phần tiếp theo sẽ đi sâu vào cách tùy chỉnh file `logging.properties` và cách tích hợp các framework logging phổ biến khác như Log4j2 vào ứng dụng của bạn.
