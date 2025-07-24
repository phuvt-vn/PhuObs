# Bài 12: Tạo Virtual Host trên Windows

- **Thời lượng**: 12:38
- **Bài trước**: [[11 - Virtualhost Overview]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[13 - Virtual Host Creation - Linux Platform]]

---

## Nội dung

Bài học này hướng dẫn chi tiết cách cấu hình một virtual host mới trên Apache Tomcat chạy trên hệ điều hành Windows. Chúng ta sẽ cấu hình Tomcat để phục vụ một tên miền giả lập, ví dụ `www.test-domain.com`.

### Bước 1: Giả lập DNS bằng file `hosts`

Vì chúng ta không sở hữu tên miền `www.test-domain.com`, chúng ta cần "đánh lừa" máy tính của mình để nó trỏ tên miền này về máy cục bộ (localhost).

1.  Mở Notepad **với quyền Administrator**.
2.  Mở file `hosts` tại đường dẫn: `C:\Windows\System32\drivers\etc\hosts`.
3.  Thêm dòng sau vào cuối file và lưu lại:
    ```
    127.0.0.1   www.test-domain.com
    ```
    Dòng này báo cho Windows biết rằng mỗi khi bạn truy cập `www.test-domain.com`, nó sẽ được phân giải thành địa chỉ IP `127.0.0.1` (chính là máy của bạn).

### Bước 2: Tạo thư mục cho Virtual Host

1.  Trong thư mục cài đặt Tomcat (`$CATALINA_HOME`), tạo một thư mục mới để chứa các ứng dụng cho virtual host này. Ví dụ:
    ```
    C:\apache-tomcat-9.0.54\webapps-test-domain
    ```
2.  Bên trong thư mục này, tạo một ứng dụng web gốc. Tạo một thư mục có tên `ROOT` (viết hoa).
    ```
    C:\apache-tomcat-9.0.54\webapps-test-domain\ROOT
    ```
3.  Bên trong thư mục `ROOT`, tạo một file `index.html` đơn giản để kiểm tra.
    ```html
    <!-- C:\apache-tomcat-9.0.54\webapps-test-domain\ROOT\index.html -->
    <h1>Welcome to www.test-domain.com!</h1>
    ```

### Bước 3: Cấu hình `server.xml`

1.  Mở file `$CATALINA_HOME/conf/server.xml`.
2.  Tìm đến thẻ `<Engine>`. Bên trong nó, bạn sẽ thấy một thẻ `<Host>` đã có sẵn cho `localhost`.
3.  Thêm một thẻ `<Host>` mới ngay bên dưới thẻ `<Host>` của `localhost`.

    ```xml
    <Engine name="Catalina" defaultHost="localhost">
    
      <!-- Host mặc định -->
      <Host name="localhost"  appBase="webapps"
            unpackWARs="true" autoDeploy="true">
        ...
      </Host>

      <!-- Virtual Host mới -->
      <Host name="www.test-domain.com" appBase="webapps-test-domain"
            unpackWARs="true" autoDeploy="true">
            
        <!-- (Tùy chọn) Ghi log truy cập riêng cho host này -->
        <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
               prefix="test-domain_access_log" suffix=".txt"
               pattern="%h %l %u %t "%r" %s %b" />
               
      </Host>
      
    </Engine>
    ```

**Giải thích các thuộc tính của `<Host>` mới:**

-   `name="www.test-domain.com"`: Đây là tên miền mà Host này sẽ xử lý. Nó phải khớp chính xác với tên miền bạn đã cấu hình trong file `hosts`.
-   `appBase="webapps-test-domain"`: Đây là thư mục gốc chứa các ứng dụng cho virtual host này. Đường dẫn này là tương đối so với `$CATALINA_HOME`.
-   `unpackWARs="true"` và `autoDeploy="true"`: Cho phép tự động giải nén và triển khai các file `.war` được đặt trong `appBase`.
-   Thẻ `<Valve>` (tùy chọn) cho phép bạn cấu hình một file log truy cập riêng cho virtual host này, giúp việc theo dõi dễ dàng hơn.

### Bước 4: Khởi động lại và Kiểm tra

1.  Lưu file `server.xml`.
2.  Khởi động lại Tomcat.
3.  Mở trình duyệt và truy cập:
    -   `http://localhost:8080` -> Bạn sẽ thấy trang Tomcat mặc định.
    -   `http://www.test-domain.com:8080` -> Bạn sẽ thấy nội dung file `index.html` bạn đã tạo: "Welcome to www.test-domain.com!".

Nếu kết quả đúng như trên, bạn đã tạo thành công một virtual host trên Windows.
