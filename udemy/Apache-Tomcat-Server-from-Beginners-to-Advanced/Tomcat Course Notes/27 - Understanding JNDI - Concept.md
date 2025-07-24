# Bài 27: Tìm hiểu về JNDI - Khái niệm

- **Thời lượng**: 04:19
- **Bài trước**: [[26 - Configuring Session Replication]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[28 - Understanding Connection Pooling]]

---

## Nội dung

Bài học này giới thiệu về **JNDI (Java Naming and Directory Interface)**, một API quan trọng của Java cho phép các ứng dụng tìm kiếm (look up) các đối tượng và tài nguyên thông qua một cái tên.

### JNDI là gì?

-   **JNDI** là một API (giao diện lập trình ứng dụng) chuẩn của Java.
-   Nó cung cấp một cơ chế **thống nhất** để các thành phần ứng dụng (như servlets, JSPs) có thể định vị và truy cập các tài nguyên khác nhau.
-   Các tài nguyên này có thể là:
    -   Đối tượng `DataSource` để kết nối cơ sở dữ liệu.
    -   Các đối tượng Enterprise JavaBeans (EJB).
    -   Các dịch vụ message (JMS).
    -   Các dịch vụ thư mục (LDAP, Active Directory).
    -   Các tham số cấu hình tùy chỉnh.

### Tại sao cần JNDI? - Tách biệt cấu hình khỏi mã nguồn

Hãy xem xét kịch bản kết nối cơ sở dữ liệu **không** sử dụng JNDI:

```java
// Trong code của một Servlet
String dbUrl = "jdbc:mysql://localhost:3306/mydatabase";
String user = "myuser";
String password = "mypassword";
Connection conn = DriverManager.getConnection(dbUrl, user, password);
```

**Vấn đề của cách tiếp cận này:**

1.  **Cấu hình bị mã hóa cứng (Hard-coded)**: Thông tin kết nối (URL, user, password) nằm trực tiếp trong mã nguồn.
2.  **Khó thay đổi**: Nếu bạn muốn thay đổi cơ sở dữ liệu (ví dụ: từ môi trường development sang production), bạn phải sửa đổi mã nguồn, biên dịch lại và triển khai lại toàn bộ ứng dụng.
3.  **Kém bảo mật**: Mật khẩu được lưu dưới dạng văn bản thuần trong code.
4.  **Không thể tái sử dụng**: Mỗi phần của ứng dụng cần kết nối DB đều phải tự định nghĩa lại các thông tin này.

### Giải pháp của JNDI

Với JNDI, chúng ta chuyển việc quản lý tài nguyên ra khỏi ứng dụng và giao cho **container** (chính là Tomcat) quản lý.

**Quy trình hoạt động:**

1.  **Quản trị viên (Administrator)**:
    -   Định nghĩa tài nguyên (ví dụ: một `DataSource` kết nối đến cơ sở dữ liệu) trong file cấu hình của Tomcat (`context.xml` hoặc `server.xml`).
    -   Gán cho tài nguyên đó một cái **tên JNDI** duy nhất (ví dụ: `jdbc/myAppDB`).
2.  **Lập trình viên (Developer)**:
    -   Trong mã nguồn ứng dụng, thay vì tạo kết nối một cách thủ công, họ chỉ cần thực hiện một thao tác "look up" (tra cứu) để yêu cầu container cung cấp tài nguyên thông qua tên JNDI của nó.
        ```java
        // Yêu cầu container cung cấp tài nguyên có tên "jdbc/myAppDB"
        Context initContext = new InitialContext();
        // "java:/comp/env" là context chuẩn cho các tài nguyên của ứng dụng
        Context envContext  = (Context)initContext.lookup("java:/comp/env");
        DataSource ds = (DataSource)envContext.lookup("jdbc/myAppDB");
        Connection conn = ds.getConnection(); // Lấy kết nối từ DataSource
        ```

**Lợi ích của JNDI:**

-   **Tách biệt (Decoupling)**: Mã nguồn ứng dụng không còn chứa thông tin cấu hình chi tiết. Nó chỉ biết tên của tài nguyên.
-   **Dễ dàng thay đổi**: Quản trị viên có thể thay đổi cấu hình kết nối DB (đổi server, user, password) trong file cấu hình của Tomcat mà **không cần chạm vào mã nguồn ứng dụng**. Chỉ cần khởi động lại Tomcat là đủ.
-   **Tính di động (Portability)**: Ứng dụng có thể được triển khai trên các môi trường khác nhau (dev, test, prod) mà không cần sửa đổi. Chỉ cần thay đổi cấu hình JNDI trên mỗi môi trường tương ứng.
-   **Quản lý tập trung**: Các tài nguyên được quản lý tại một nơi duy nhất (bởi container).

Trong các bài học tiếp theo, chúng ta sẽ xem xét cách JNDI được sử dụng kết hợp với **Connection Pooling** để quản lý kết nối cơ sở dữ liệu một cách hiệu quả.
