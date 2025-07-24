# Bài 36: Khái niệm về Realm

- **Thời lượng**: 04:04
- **Bài trước**: [[35 - HTTP Request Interception - Tomcat Engine Layer]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[37 - Example - Configuring UserDatabaseRealm for Container managed Authentication]]

---

## Nội dung

Bài học này giới thiệu về **Realm**, một thành phần cốt lõi trong cơ chế bảo mật của Tomcat, chịu trách nhiệm về việc **xác thực (authentication)** và **ủy quyền (authorization)**.

### Realm là gì?

-   Một **Realm** có thể được xem như một "cầu nối" (bridge) giữa Tomcat và một "cơ sở dữ liệu" chứa thông tin về người dùng, mật khẩu và vai trò (roles) của họ.
-   Khi bạn bảo vệ một tài nguyên trong ứng dụng web và người dùng cố gắng truy cập nó, Tomcat sẽ sử dụng Realm đã được cấu hình để:
    1.  **Xác thực**: Kiểm tra xem username và password mà người dùng cung cấp có hợp lệ không, bằng cách so sánh với dữ liệu trong "cơ sở dữ liệu" mà Realm trỏ tới.
    2.  **Ủy quyền**: Sau khi xác thực thành công, kiểm tra xem người dùng đó có thuộc một vai trò (role) được phép truy cập vào tài nguyên đó hay không.

### Container-Managed Security (Bảo mật do Container quản lý)

-   Việc sử dụng Realm là một phần của cơ chế **Container-Managed Security**.
-   Thay vì viết logic xác thực và ủy quyền trong code của ứng dụng (ví dụ: `if (user.equals("admin") && pass.equals("123"))`), bạn khai báo các ràng buộc bảo mật một cách **khai báo (declaratively)** trong file `web.xml`.
-   Tomcat (container) sẽ tự động thực thi các ràng buộc này cho bạn bằng cách sử dụng Realm.

**Luồng hoạt động:**

1.  **Lập trình viên**: Trong `web.xml`, định nghĩa:
    -   Các tài nguyên cần bảo vệ (`<security-constraint>`).
    -   Các vai trò được phép truy cập (`<auth-constraint>`).
    -   Phương thức xác thực sẽ được sử dụng (`<login-config>`, ví dụ: BASIC, FORM).
2.  **Quản trị viên**: Trong `server.xml` hoặc `context.xml`, cấu hình một `<Realm>` để chỉ cho Tomcat biết nơi tìm kiếm thông tin người dùng (username, password, roles).
3.  **Người dùng**: Cố gắng truy cập một trang được bảo vệ.
4.  **Tomcat**:
    -   Chặn request.
    -   Hiển thị form đăng nhập (dựa trên `<login-config>`).
    -   Nhận username/password từ người dùng.
    -   Hỏi **Realm**: "Này Realm, username/password này có đúng không?"
    -   Realm kiểm tra trong nguồn dữ liệu của nó (XML, DB, LDAP) và trả lời Tomcat.
    -   Nếu xác thực thành công, Tomcat tiếp tục hỏi Realm: "Người dùng này có vai trò 'admin' không?"
    -   Realm kiểm tra và trả lời.
    -   Tomcat dựa vào câu trả lời để quyết định cho phép hay từ chối truy cập.

### Các loại Realm tích hợp sẵn trong Tomcat

Tomcat cung cấp nhiều loại Realm khác nhau, cho phép bạn lấy thông tin người dùng từ nhiều nguồn khác nhau:

-   **`MemoryRealm`**: Đọc thông tin người dùng từ một file XML (`tomcat-users.xml`). Đơn giản, dễ cấu hình, phù hợp cho môi trường development hoặc các ứng dụng nhỏ.
-   **`JDBCRealm`**: Lấy thông tin người dùng từ một bảng trong cơ sở dữ liệu quan hệ thông qua JDBC. Rất phổ biến cho các ứng dụng thực tế.
-   **`DataSourceRealm`**: Tương tự `JDBCRealm` nhưng sử dụng JNDI DataSource, cho phép tận dụng connection pooling. Đây là lựa chọn được khuyến nghị khi dùng DB.
-   **`JNDIRealm`**: Kết nối đến một máy chủ thư mục (directory server) sử dụng JNDI, thường là LDAP hoặc Active Directory. Phù hợp cho các môi trường doanh nghiệp đã có sẵn hệ thống quản lý người dùng tập trung.
-   **`LockOutRealm`**: Một Realm đặc biệt, có thể được "bọc" bên ngoài một Realm khác (như `JDBCRealm`). Nó có chức năng khóa tài khoản của người dùng sau một số lần đăng nhập thất bại, giúp chống lại các cuộc tấn công dò mật khẩu (brute-force attack).

Trong các bài học tiếp theo, chúng ta sẽ xem xét các ví dụ cụ thể về cách cấu hình `MemoryRealm` và `JDBCRealm`.
