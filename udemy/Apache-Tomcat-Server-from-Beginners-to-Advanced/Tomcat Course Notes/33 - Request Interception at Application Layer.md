# Bài 33: Chặn Request ở Tầng Ứng dụng (Application Layer)

- **Thời lượng**: 08:22
- **Bài trước**: [[32 - Request Interception Using Valves]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[34 - HTTP Request Interception - Host Layer]]

---

## Nội dung

Sau khi đã tìm hiểu về Valve ở cấp độ container, bài học này tập trung vào cơ chế chặn request chuẩn và phổ biến nhất: **Servlet Filter**. Filter là một phần của đặc tả Java Servlet API, hoạt động ở tầng ứng dụng và là công cụ chính để thực hiện các tác vụ xử lý chéo (cross-cutting concerns).

### Servlet Filter là gì?

-   **Filter** là một đối tượng Java có khả năng chặn các request đến một tài nguyên (như một servlet hoặc một trang JSP) và/hoặc chặn các response trả về từ tài nguyên đó.
-   Nó cho phép bạn kiểm tra, sửa đổi, hoặc xử lý request và response trước khi chúng được xử lý bởi servlet hoặc gửi về cho client.
-   Nhiều filter có thể được kết hợp thành một **chuỗi filter (filter chain)**. Request sẽ đi qua từng filter trong chuỗi theo thứ tự đã định nghĩa.

### Các ứng dụng phổ biến của Filter

Filters rất lý tưởng cho các tác vụ cần được thực hiện trên nhiều servlet hoặc JSP, giúp tránh lặp lại code.

1.  **Xác thực và Ủy quyền (Authentication & Authorization)**: Kiểm tra xem người dùng đã đăng nhập chưa và có quyền truy cập vào tài nguyên được yêu cầu không. Nếu không, chuyển hướng họ đến trang đăng nhập.
2.  **Ghi log và Giám sát (Logging & Auditing)**: Ghi lại thông tin về các request (như IP, thời gian, tham số) cho mục đích phân tích hoặc giám sát.
3.  **Nén dữ liệu (Compression)**: Nén nội dung của response (ví dụ: dùng GZIP) trước khi gửi về trình duyệt để giảm băng thông và tăng tốc độ tải trang.
4.  **Chuyển đổi dữ liệu (Data Transformation)**: Sửa đổi nội dung của request hoặc response, ví dụ như mã hóa/giải mã dữ liệu.
5.  **Quản lý bộ nhớ đệm (Caching)**: Thêm các header vào response để kiểm soát việc cache trang ở phía trình duyệt.
6.  **Thiết lập mã hóa ký tự (Character Encoding)**: Đảm bảo tất cả các request đều được xử lý với một bộ mã hóa ký tự nhất quán (như UTF-8) để tránh lỗi hiển thị.

### Cách triển khai một Filter

1.  **Tạo lớp Java**:
    -   Tạo một lớp Java implement interface `javax.servlet.Filter`.
    -   Interface này có 3 phương thức cần được triển khai:
        -   `init(FilterConfig config)`: Được gọi một lần khi filter được khởi tạo. Dùng để đọc các tham số cấu hình.
        -   `doFilter(ServletRequest request, ServletResponse response, FilterChain chain)`: Đây là phương thức chính, được gọi cho mỗi request. Logic chặn request được viết ở đây.
        -   `destroy()`: Được gọi một lần khi filter bị hủy. Dùng để giải phóng tài nguyên.

2.  **Logic trong `doFilter`**:
    -   **Code xử lý trước khi gọi servlet**: Bất kỳ code nào bạn viết trước dòng `chain.doFilter(...)` sẽ được thực thi trước khi request đến servlet.
    -   **`chain.doFilter(request, response)`**: Dòng này cực kỳ quan trọng. Nó chuyển quyền điều khiển cho filter tiếp theo trong chuỗi, hoặc cho servlet nếu đây là filter cuối cùng. **Nếu bạn không gọi dòng này, request sẽ bị dừng lại và không bao giờ đến được servlet.**
    -   **Code xử lý sau khi servlet đã thực thi**: Bất kỳ code nào bạn viết sau dòng `chain.doFilter(...)` sẽ được thực thi sau khi servlet đã xử lý xong và response đang trên đường quay về.

3.  **Khai báo trong `web.xml`**:
    -   Bạn cần khai báo filter và ánh xạ nó tới một hoặc nhiều URL pattern trong file `WEB-INF/web.xml`.

    ```xml
    <!-- 1. Khai báo Filter -->
    <filter>
        <filter-name>MyLoggingFilter</filter-name>
        <filter-class>com.myapp.filters.LoggingFilter</filter-class>
    </filter>

    <!-- 2. Ánh xạ Filter tới một URL Pattern -->
    <filter-mapping>
        <filter-name>MyLoggingFilter</filter-name>
        <!-- Áp dụng filter này cho tất cả các request -->
        <url-pattern>/*</url-pattern>
    </filter-mapping>
    ```

### Kết luận

-   Sử dụng **Filter** cho các logic liên quan đến ứng dụng (xác thực, ghi log, mã hóa).
-   Sử dụng **Valve** cho các logic liên quan đến container hoặc cần thực hiện ở cấp độ thấp hơn (quản lý IP, dump request cho toàn bộ server).
-   Ưu tiên sử dụng Filter vì nó là chuẩn, giúp ứng dụng của bạn có thể di động giữa các servlet container khác nhau.
