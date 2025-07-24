# Bài 7: Tổng quan về Bảo mật (Security Overview)

- **Thời lượng**: 02:14
- **Bài trước**: [[06 - Configuration files and binaries walkthrough]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[08 - Enabling SSL - Generating Certificate]]

---

## Nội dung

Bài học này cung cấp một cái nhìn tổng quan về các khía cạnh bảo mật trong Apache Tomcat và tầm quan trọng của việc cấu hình chúng một cách chính xác.

### Các lĩnh vực bảo mật chính trong Tomcat

1.  **Bảo mật Truyền tải (Transport Layer Security - SSL/TLS)**:
    -   Mã hóa dữ liệu được truyền giữa trình duyệt của người dùng và máy chủ Tomcat.
    -   Đảm bảo tính bí mật và toàn vẹn của dữ liệu, ngăn chặn nghe lén (eavesdropping) và tấn công "man-in-the-middle".
    -   Việc kích hoạt SSL/TLS (HTTPS) là một trong những bước bảo mật cơ bản và quan trọng nhất.

2.  **Xác thực và Ủy quyền (Authentication & Authorization)**:
    -   **Xác thực (Authentication)**: Quá trình xác minh danh tính của người dùng (ai là bạn?).
    -   **Ủy quyền (Authorization)**: Quá trình xác định quyền truy cập của người dùng đã được xác thực vào các tài nguyên cụ thể (bạn được phép làm gì?).
    -   Tomcat quản lý việc này thông qua **Realms**. Realms là các "cơ sở dữ liệu" chứa thông tin người dùng, mật khẩu và vai trò.

3.  **Bảo mật ứng dụng Manager**:
    -   Ứng dụng Manager của Tomcat là một công cụ mạnh mẽ, cho phép triển khai và quản lý ứng dụng từ xa.
    -   Việc bảo vệ nó bằng mật khẩu mạnh và chỉ cấp quyền truy cập cho những người dùng tin cậy là cực kỳ quan trọng.
    -   Nên thay đổi mật khẩu mặc định và xem xét việc giới hạn truy cập dựa trên địa chỉ IP.

4.  **Security Manager**:
    -   Tomcat có thể chạy dưới một Java Security Manager, giúp giới hạn các hành động mà các ứng dụng web có thể thực hiện (ví dụ: truy cập hệ thống tệp, mở kết nối mạng).
    -   Cung cấp một lớp bảo vệ mạnh mẽ, cô lập các ứng dụng web với nhau và với hệ điều hành.

5.  **Các cấu hình bảo mật khác**:
    -   Chạy Tomcat bằng một tài khoản người dùng có quyền hạn thấp (không phải `root`).
    -   Tắt các dịch vụ và ứng dụng không cần thiết (ví dụ: gỡ bỏ các ứng dụng web mặc định như `examples` trên môi trường production).
    -   Giữ cho Tomcat và Java luôn được cập nhật phiên bản mới nhất để vá các lỗ hổng bảo mật.
