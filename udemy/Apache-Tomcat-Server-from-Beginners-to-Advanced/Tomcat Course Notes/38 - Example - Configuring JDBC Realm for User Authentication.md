# Bài 38: Ví dụ - Cấu hình JDBC Realm để Xác thực người dùng

- **Thời lượng**: 11:11
- **Bài trước**: [[37 - Example - Configuring UserDatabaseRealm for Container managed Authentication]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[39 - Tomcat Logging Part1]]

---

## Nội dung

Sử dụng file XML để quản lý người dùng không phải là giải pháp tốt cho các ứng dụng lớn. `JDBCRealm` cho phép Tomcat lấy thông tin người dùng và vai trò trực tiếp từ cơ sở dữ liệu, một giải pháp mạnh mẽ và có khả năng mở rộng hơn nhiều.

### Bước 1: Chuẩn bị Cơ sở dữ liệu

Trước tiên, bạn cần có các bảng trong cơ sở dữ liệu để lưu trữ thông tin người dùng. Cấu trúc phổ biến nhất bao gồm hai bảng: một bảng cho người dùng và một bảng cho vai trò của họ.

**Bảng `users`:**

| user_name (VARCHAR) | user_pass (VARCHAR) |
| :--- | :--- |
| tom | tom_password |
| jerry | jerry_password |

**Bảng `user_roles`:**

| user_name (VARCHAR) | role_name (VARCHAR) |
| :--- | :--- |
| tom | manager |
| tom | employee |
| jerry | employee |

-   **Quan trọng**: Mật khẩu nên được lưu trữ dưới dạng đã được băm (hashed), không bao giờ lưu dưới dạng văn bản thuần. Tomcat hỗ trợ nhiều thuật toán băm như `SHA-256`, `MD5`.

### Bước 2: Chuẩn bị JDBC Driver

-   Giống như khi cấu hình JNDI DataSource, bạn cần đặt file `.jar` của JDBC driver (ví dụ: `mysql-connector-java.jar`) vào thư mục `$CATALINA_HOME/lib`.

### Bước 3: Cấu hình `JDBCRealm`

`JDBCRealm` có thể được cấu hình trong `server.xml` (áp dụng cho toàn bộ host) hoặc trong `META-INF/context.xml` của ứng dụng (chỉ áp dụng cho ứng dụng đó). Cấu hình trong `context.xml` thường được ưa chuộng hơn.

**Ví dụ cấu hình `JDBCRealm` trong `META-INF/context.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Context>
    <!-- 
      Thay vì dùng UserDatabaseRealm, chúng ta định nghĩa một JDBCRealm.
      Realm này sẽ được sử dụng bởi ứng dụng này thay cho Realm mặc định.
    -->
    <Realm className="org.apache.catalina.realm.JDBCRealm"
           
           -- -- Cấu hình kết nối DB -- --
           driverName="com.mysql.cj.jdbc.Driver"
           connectionURL="jdbc:mysql://localhost:3306/security_db?useSSL=false"
           connectionName="db_user"
           connectionPassword="db_password"
           
           -- -- Cấu hình truy vấn người dùng -- --
           userTable="users"
           userNameCol="user_name"
           userCredCol="user_pass"
           
           -- -- Cấu hình truy vấn vai trò -- --
           userRoleTable="user_roles"
           roleNameCol="role_name"
           
           -- -- (Tùy chọn) Cấu hình băm mật khẩu -- --
           digest="SHA-256"
    />
</Context>
```

**Giải thích các thuộc tính:**

-   `className`: Chỉ định lớp `JDBCRealm`.
-   `driverName`, `connectionURL`, `connectionName`, `connectionPassword`: Các thông tin cần thiết để `JDBCRealm` tự tạo kết nối đến cơ sở dữ liệu.
-   `userTable`: Tên của bảng chứa thông tin người dùng.
-   `userNameCol`: Tên của cột chứa username trong bảng `userTable`.
-   `userCredCol`: Tên của cột chứa mật khẩu (credential) trong bảng `userTable`.
-   `userRoleTable`: Tên của bảng chứa thông tin về vai trò của người dùng.
-   `roleNameCol`: Tên của cột chứa tên vai trò trong bảng `userRoleTable`.
-   `digest`: (Tùy chọn) Chỉ định thuật toán băm được sử dụng để lưu mật khẩu trong cơ sở dữ liệu. Tomcat sẽ tự động băm mật khẩu do người dùng nhập vào bằng thuật toán này trước khi so sánh với giá trị trong DB. Điều này tăng cường bảo mật đáng kể.

> **Lựa chọn tốt hơn**: Thay vì để `JDBCRealm` tự quản lý kết nối, bạn có thể cấu hình nó để sử dụng một JNDI DataSource đã có sẵn (`DataSourceRealm`). Cách này cho phép Realm tận dụng được connection pooling, giúp cải thiện hiệu năng.

### Bước 4: Cấu hình `web.xml`

-   Phần cấu hình trong `web.xml` (`<security-constraint>`, `<login-config>`, `<security-role>`) **giữ nguyên không thay đổi** so với khi sử dụng `UserDatabaseRealm`.
-   Ứng dụng không cần biết thông tin người dùng đến từ đâu (file XML hay DB), đó là vẻ đẹp của việc tách biệt logic bằng Realm.

### Bước 5: Kiểm tra

-   Khởi động lại Tomcat.
-   Truy cập vào một trang được bảo vệ.
-   Tomcat sẽ sử dụng `JDBCRealm` để xác thực thông tin đăng nhập của bạn với dữ liệu trong cơ sở dữ liệu.

Việc sử dụng `JDBCRealm` hoặc `DataSourceRealm` là phương pháp tiêu chuẩn để quản lý người dùng trong hầu hết các ứng dụng Java web trong môi trường production.
