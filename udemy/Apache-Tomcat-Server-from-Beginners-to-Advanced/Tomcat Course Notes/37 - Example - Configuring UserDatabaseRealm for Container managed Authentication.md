# Bài 37: Ví dụ - Cấu hình UserDatabaseRealm cho Xác thực

- **Thời lượng**: 12:21
- **Bài trước**: [[36 - Realm Concept]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[38 - Example - Configuring JDBC Realm for User Authentication]]

---

## Nội dung

Bài học này cung cấp một ví dụ hoàn chỉnh về cách sử dụng `UserDatabaseRealm` (một dạng của `MemoryRealm`) để triển khai cơ chế bảo mật do container quản lý (Container-Managed Security). Chúng ta sẽ bảo vệ một khu vực của ứng dụng web, yêu cầu người dùng đăng nhập để truy cập.

### Bước 1: Cấu hình người dùng trong `tomcat-users.xml`

`UserDatabaseRealm` sử dụng file `$CATALINA_HOME/conf/tomcat-users.xml` làm nguồn dữ liệu người dùng.

1.  Mở file `tomcat-users.xml`.
2.  Định nghĩa các vai trò (roles) và người dùng (users) cần thiết.

    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <tomcat-users xmlns="http://tomcat.apache.org/xml"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://tomcat.apache.org/xml tomcat-users.xsd"
                  version="1.0">
                  
      <!-- Định nghĩa một vai trò tên là 'manager' -->
      <role rolename="manager"/>
      
      <!-- Định nghĩa một vai trò tên là 'employee' -->
      <role rolename="employee"/>

      <!-- Tạo người dùng 'tom' với mật khẩu 'password' và gán vai trò 'manager' -->
      <user username="tom" password="password" roles="manager"/>
      
      <!-- Tạo người dùng 'jerry' với mật khẩu 'password' và gán vai trò 'employee' -->
      <user username="jerry" password="password" roles="employee"/>
      
    </tomcat-users>
    ```

### Bước 2: Cấu hình Realm trong `server.xml`

Mặc định, Tomcat đã cấu hình sẵn một `UserDatabaseRealm` trong file `server.xml`, vì vậy bạn thường không cần phải thay đổi gì ở bước này. Cấu hình đó trông giống như sau:

```xml
<!-- trong thẻ <Engine> hoặc <Host> -->
<Realm className="org.apache.catalina.realm.UserDatabaseRealm"
       resourceName="UserDatabase"/>
```
-   `resourceName="UserDatabase"` trỏ đến một JNDI resource toàn cục được định nghĩa sẵn trong `server.xml`, resource này lại trỏ đến file `conf/tomcat-users.xml`.

### Bước 3: Cấu hình bảo mật trong `web.xml`

Đây là bước quan trọng nhất, nơi bạn nói cho ứng dụng biết phải bảo vệ cái gì và bảo vệ như thế nào. Mở file `WEB-INF/web.xml` của ứng dụng.

Giả sử chúng ta có một thư mục `/admin` trong ứng dụng và chỉ muốn những người dùng có vai trò `manager` được truy cập.

```xml
<web-app ...>

    <!-- PHẦN 1: ĐỊNH NGHĨA RÀNG BUỘC BẢO MẬT -->
    <security-constraint>
        <web-resource-collection>
            <!-- Đặt tên cho nhóm tài nguyên được bảo vệ -->
            <web-resource-name>Admin Area</web-resource-name>
            <!-- Mẫu URL cần bảo vệ. Mọi thứ trong /admin/ sẽ được bảo vệ -->
            <url-pattern>/admin/*</url-pattern>
        </web-resource-collection>
        
        <auth-constraint>
            <!-- Chỉ những người dùng có vai trò 'manager' mới được truy cập -->
            <role-name>manager</role-name>
        </auth-constraint>
    </security-constraint>

    <!-- PHẦN 2: ĐỊNH NGHĨA PHƯƠNG THỨC ĐĂNG NHẬP -->
    <login-config>
        <!-- Sử dụng phương thức xác thực BASIC (hộp thoại pop-up của trình duyệt) -->
        <auth-method>BASIC</auth-method>
        <realm-name>My Application Realm</realm-name>
    </login-config>

    <!-- PHẦN 3: KHAI BÁO CÁC VAI TRÒ BẢO MẬT -->
    <!-- Khai báo tất cả các vai trò được sử dụng trong các <auth-constraint> -->
    <security-role>
        <role-name>manager</role-name>
    </security-role>
    
</web-app>
```

**Giải thích:**

-   **`<security-constraint>`**: Định nghĩa một quy tắc bảo mật.
-   **`<web-resource-collection>`**: Xác định các tài nguyên (URL) mà quy tắc này áp dụng.
-   **`<auth-constraint>`**: Liệt kê các vai trò được phép truy cập vào các tài nguyên này.
-   **`<login-config>`**:
    -   `auth-method`: Chỉ định cách Tomcat sẽ yêu cầu người dùng đăng nhập. Các lựa chọn phổ biến là `BASIC`, `DIGEST`, `FORM`, `CLIENT-CERT`. `BASIC` là đơn giản nhất, hiển thị một hộp thoại đăng nhập của trình duyệt.
    -   `realm-name`: Tên sẽ được hiển thị trên hộp thoại đăng nhập.
-   **`<security-role>`**: Một khai báo tường minh về tất cả các vai trò mà ứng dụng này nhận biết.

### Bước 4: Khởi động lại và Kiểm tra

1.  Khởi động lại Tomcat để các thay đổi có hiệu lực.
2.  **Truy cập một trang không được bảo vệ** (ví dụ: `http://localhost:8080/my-app/index.html`) -> Bạn sẽ truy cập được bình thường.
3.  **Truy cập một trang được bảo vệ** (ví dụ: `http://localhost:8080/my-app/admin/dashboard.html`).
4.  Trình duyệt sẽ hiển thị một hộp thoại pop-up yêu cầu username và password.
5.  **Thử đăng nhập với user `jerry` (vai trò `employee`)**: Bạn sẽ bị từ chối (Lỗi 403 Forbidden).
6.  **Thử đăng nhập với user `tom` (vai trò `manager`)**: Bạn sẽ được cấp quyền truy cập và thấy nội dung trang.

Bằng cách này, bạn đã ủy quyền việc xác thực và ủy quyền cho Tomcat, giúp mã nguồn ứng dụng sạch sẽ và dễ bảo trì hơn.
