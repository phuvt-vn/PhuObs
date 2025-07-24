# Bài 5: Triển khai ứng dụng (Application Deployment)

- **Thời lượng**: 10:21
- **Bài trước**: [[04 - Setup Requirements & Installation - Linux Platform]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[06 - Configuration files and binaries walkthrough]]

---

## Nội dung

Bài học này trình bày các phương pháp khác nhau để triển khai một ứng dụng web lên Apache Tomcat.

### Cấu trúc ứng dụng web Java

Một ứng dụng web Java tiêu chuẩn được đóng gói dưới dạng file **WAR (Web Application Archive)** và có cấu trúc thư mục như sau:

```
my-app/
├── META-INF/
│   └── ...
├── WEB-INF/
│   ├── classes/      // Chứa các file .class của ứng dụng
│   ├── lib/          // Chứa các thư viện .jar phụ thuộc
│   └── web.xml       // Tệp tin mô tả triển khai (Deployment Descriptor)
├── index.html
├── jsp-pages/
└── images/
```

### Các cách triển khai (Deployment)

1.  **Triển khai tự động (Automatic Deployment)**:
    -   Đây là cách đơn giản nhất.
    -   Chỉ cần sao chép file `.war` của ứng dụng vào thư mục `webapps` trong thư mục cài đặt Tomcat (`$CATALINA_HOME/webapps/`).
    -   Tomcat sẽ tự động phát hiện file `.war` mới, giải nén nó ra thành một thư mục cùng tên (không có phần mở rộng `.war`) và triển khai ứng dụng.
    -   Ứng dụng sẽ có thể truy cập được tại `http://localhost:8080/<tên-ứng-dụng>/`.
    -   **Lưu ý**: Tính năng này được bật theo mặc định (`autoDeploy="true"` trong file `server.xml`).

2.  **Triển khai qua Tomcat Manager App**:
    -   Tomcat cung cấp một ứng dụng web quản trị tên là **Manager App**.
    -   Để sử dụng, bạn cần cấu hình người dùng có quyền `manager-gui` trong file `tomcat-users.xml` nằm trong thư mục `conf`.
        ```xml
        <role rolename="manager-gui"/>
        <user username="admin" password="password" roles="manager-gui"/>
        ```
    -   Truy cập vào `http://localhost:8080/manager/html`.
    -   Đăng nhập bằng tài khoản vừa tạo.
    -   Giao diện quản lý cho phép bạn:
        -   **Deploy**: Tải lên file `.war` để triển khai.
        -   **Undeploy**: Gỡ bỏ một ứng dụng.
        -   **Start/Stop/Reload**: Quản lý trạng thái của các ứng dụng.

3.  **Triển khai thủ công (Manual Deployment)**:
    -   Phương pháp này cho phép bạn kiểm soát nhiều hơn.
    -   Tạo một file context XML cho ứng dụng của bạn.
    -   File này có thể đặt tại:
        -   `$CATALINA_HOME/conf/[enginename]/[hostname]/<tên-ứng-dụng>.xml`
    -   Nội dung file context sẽ trông giống như sau, trỏ đến vị trí của file `.war` hoặc thư mục đã giải nén của ứng dụng:
        ```xml
        <Context docBase="/path/to/your/app.war" reloadable="true" />
        ```
    -   Cách này hữu ích khi bạn muốn đặt mã nguồn ứng dụng ở một nơi khác ngoài thư mục `webapps`.
