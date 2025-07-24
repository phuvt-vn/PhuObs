# Bài 13: Tạo Virtual Host trên Linux

- **Thời lượng**: 12:19
- **Bài trước**: [[12 - Virtual Host Creation - Windows Platform]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[14 - Scaling Traffic & High Availability using clustering]]

---

## Nội dung

Bài học này trình bày cách thiết lập một virtual host trên Tomcat chạy trên môi trường Linux. Các bước tương tự như trên Windows, nhưng có thêm phần quản lý quyền sở hữu thư mục.

### Bước 1: Cấu hình DNS (Sử dụng file `/etc/hosts`)

Trên máy client mà bạn sẽ dùng để truy cập, hoặc ngay trên chính server Linux, bạn cần chỉnh sửa file `/etc/hosts` để ánh xạ tên miền ảo về địa chỉ IP của server Tomcat.

1.  Mở file `/etc/hosts` với quyền `sudo`:
    ```bash
    sudo nano /etc/hosts
    ```
2.  Thêm dòng sau, thay `<server_ip>` bằng địa chỉ IP của máy chủ Tomcat:
    ```
    <server_ip>   www.test-domain.com
    ```
    Nếu bạn đang kiểm tra ngay trên server, có thể dùng `127.0.0.1`.

### Bước 2: Tạo thư mục và Quyền sở hữu

1.  Tạo thư mục `appBase` cho virtual host mới. Thư mục này nên nằm ngoài thư mục `webapps` mặc định để giữ cho mọi thứ có tổ chức.
    ```bash
    sudo mkdir -p /opt/tomcat/webapps-test-domain/ROOT
    ```
2.  Tạo một file `index.html` để kiểm tra:
    ```bash
    sudo nano /opt/tomcat/webapps-test-domain/ROOT/index.html
    ```
    Thêm nội dung: `<h1>Welcome to www.test-domain.com on Linux!</h1>`

3.  **Quan trọng**: Thay đổi quyền sở hữu của các thư mục và file mới tạo cho người dùng `tomcat` để Tomcat có thể đọc và ghi vào chúng.
    ```bash
    sudo chown -R tomcat:tomcat /opt/tomcat/webapps-test-domain
    ```

### Bước 3: Cấu hình `server.xml`

1.  Mở file `/opt/tomcat/conf/server.xml`:
    ```bash
    sudo nano /opt/tomcat/conf/server.xml
    ```
2.  Tương tự như trên Windows, tìm đến thẻ `<Engine>` và thêm một khối `<Host>` mới.

    ```xml
    <Engine name="Catalina" defaultHost="localhost">
    
      <!-- Host mặc định -->
      <Host name="localhost"  appBase="webapps"
            unpackWARs="true" autoDeploy="true">
        ...
      </Host>

      <!-- Virtual Host mới cho test-domain.com -->
      <Host name="www.test-domain.com" appBase="webapps-test-domain"
            unpackWARs="true" autoDeploy="true">
            
        <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
               prefix="test-domain_access_log" suffix=".txt"
               pattern="%h %l %u %t "%r" %s %b" />
               
      </Host>
      
    </Engine>
    ```

    -   `name="www.test-domain.com"`: Tên miền mà host sẽ phục vụ.
    -   `appBase="webapps-test-domain"`: Thư mục chứa ứng dụng cho host này, tương đối so với `$CATALINA_HOME`.

### Bước 4: Khởi động lại và Kiểm tra

1.  Lưu file `server.xml`.
2.  Khởi động lại dịch vụ Tomcat:
    ```bash
    sudo systemctl restart tomcat
    ```
3.  Từ máy client đã chỉnh sửa file `hosts`, mở trình duyệt và truy cập `http://www.test-domain.com:8080`.
4.  Bạn sẽ thấy trang "Welcome to www.test-domain.com on Linux!".

Như vậy, bạn đã cấu hình thành công một virtual host trên Linux.
