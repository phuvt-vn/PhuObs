# Bài 30: Tùy chỉnh trang lỗi - Phần 1

- **Thời lượng**: 04:34
- **Bài trước**: [[29 - Creating JNDI and JDBC Connection Pooling - By Example]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[31 - Customizing Error Pages - Part2]]

---

## Nội dung

Khi một lỗi xảy ra trong ứng dụng web của bạn (ví dụ: người dùng truy cập một trang không tồn tại - lỗi 404, hoặc có lỗi server nội bộ - lỗi 500), Tomcat sẽ hiển thị một trang lỗi mặc định. Trang lỗi này thường chứa các thông tin kỹ thuật (như stack trace) không thân thiện với người dùng và có thể tiết lộ các thông tin nhạy cảm về cấu trúc hệ thống.

Bài học này giới thiệu cách thay thế các trang lỗi mặc định đó bằng các trang lỗi tùy chỉnh của riêng bạn, cung cấp trải nghiệm người dùng tốt hơn và tăng cường bảo mật.

### Tại sao cần tùy chỉnh trang lỗi?

1.  **Trải nghiệm người dùng (User Experience)**: Hiển thị một trang lỗi thân thiện, có thương hiệu, và hướng dẫn người dùng những việc cần làm tiếp theo (ví dụ: quay lại trang chủ, liên hệ hỗ trợ) tốt hơn nhiều so với một trang trắng với thông báo lỗi khó hiểu.
2.  **Bảo mật (Security)**: Các trang lỗi mặc định có thể để lộ thông tin chi tiết về phiên bản server, cấu trúc ứng dụng, và các đoạn mã nguồn, tạo điều kiện cho kẻ tấn công khai thác.
3.  **Thương hiệu (Branding)**: Đảm bảo tính nhất quán về giao diện của trang web, ngay cả khi có lỗi xảy ra.

### Cách cấu hình trang lỗi tùy chỉnh

Việc cấu hình được thực hiện trong file mô tả triển khai của ứng dụng: `WEB-INF/web.xml`.

Bạn sử dụng thẻ `<error-page>` để ánh xạ một mã lỗi HTTP hoặc một loại exception của Java tới một trang web (HTML, JSP, v.v.) trong ứng dụng của bạn.

#### 1. Tùy chỉnh dựa trên Mã lỗi HTTP (HTTP Error Code)

Đây là cách phổ biến nhất. Bạn có thể định nghĩa các trang riêng cho các lỗi thường gặp như 404 (Not Found), 403 (Forbidden), 500 (Internal Server Error).

**Cú pháp:**

```xml
<error-page>
    <error-code>404</error-code>
    <location>/path/to/your/404-error-page.html</location>
</error-page>

<error-page>
    <error-code>500</error-code>
    <location>/path/to/your/500-error-page.jsp</location>
</error-page>
```

-   `<error-code>`: Chứa mã trạng thái HTTP (ví dụ: 404, 500).
-   `<location>`: Đường dẫn (bắt đầu bằng `/`) đến trang lỗi tùy chỉnh của bạn, tính từ thư mục gốc của ứng dụng web.

#### 2. Tùy chỉnh dựa trên Loại Exception (Java Exception Type)

Bạn cũng có thể hiển thị một trang lỗi cụ thể khi một loại exception nhất định được ném ra từ servlet mà không được xử lý (unhandled).

**Cú pháp:**

```xml
<error-page>
    <exception-type>java.lang.Throwable</exception-type>
    <location>/path/to/your/generic-error-page.html</location>
</error-page>

<error-page>
    <exception-type>com.myapp.CustomException</exception-type>
    <location>/path/to/your/custom-exception-page.jsp</location>
</error-page>
```

-   `<exception-type>`: Tên đầy đủ (fully qualified name) của lớp exception.
-   `java.lang.Throwable` là lớp cha của tất cả các Error và Exception trong Java, vì vậy việc ánh xạ nó sẽ bắt được gần như tất cả các lỗi phía server.
-   `<location>`: Đường dẫn đến trang lỗi.

Trong phần tiếp theo, chúng ta sẽ xem xét cách tạo các trang lỗi động bằng JSP để có thể hiển thị thêm thông tin hữu ích.
