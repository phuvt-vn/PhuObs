# Bài 20: Cài đặt Apache và Trình kết nối Mod_Jk

- **Thời lượng**: 08:58
- **Bài trước**: [[19 - Loadbalancing Tomcat using Nginx - Linux Platform]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[21 - Distributing Traffic with Apache LB - Part1]]

---

## Nội dung

Ngoài Nginx, **Apache HTTP Server** (thường gọi là Apache hoặc httpd) cũng là một lựa chọn rất phổ biến để làm bộ cân bằng tải cho Tomcat. Bài học này giới thiệu về **Mod_Jk**, một trình kết nối (connector) được tối ưu hóa cho việc giao tiếp giữa Apache và Tomcat.

### Tại sao dùng Apache + Mod_Jk?

-   **Giao thức AJP**: Mod_Jk sử dụng giao thức **AJP (Apache JServ Protocol)** thay vì HTTP. AJP là một giao thức nhị phân, được thiết kế để có hiệu năng cao hơn HTTP khi chuyển tiếp request từ một máy chủ web đến một máy chủ ứng dụng. Nó có thể truyền nhiều thông tin hơn (ví dụ: thông tin về SSL) một cách hiệu quả.
-   **Tính năng nâng cao**: Mod_Jk cung cấp các tùy chọn cấu hình cân bằng tải và kiểm tra sức khỏe (health check) rất linh hoạt.
-   **Tích hợp sâu**: Là một module được phát triển đặc biệt cho việc tích hợp Apache và Tomcat.

### Các thành phần chính

1.  **Apache HTTP Server**: Đóng vai trò là máy chủ web front-end, nhận request từ client.
2.  **Mod_Jk Module**: Là một module được tải vào Apache, có nhiệm vụ giao tiếp với các node Tomcat qua giao thức AJP.
3.  **Tomcat AJP Connector**: Mỗi node Tomcat cần phải kích hoạt AJP Connector (thường ở cổng 8009) để lắng nghe các kết nối từ Mod_Jk.

### Bước 1: Kích hoạt AJP Connector trên Tomcat

-   Trên mỗi node Tomcat trong cụm, mở file `server.xml`.
-   Tìm và đảm bảo rằng AJP Connector đã được kích hoạt (bỏ comment).
    ```xml
    <!-- Define an AJP 1.3 Connector on port 8009 -->
    <Connector protocol="AJP/1.3"
               address="::1"
               port="8009"
               redirectPort="8443"
               secretRequired="false" /> 
    ```
-   **Lưu ý**: Thuộc tính `address="::1"` chỉ cho phép kết nối từ localhost. Trong một cụm ngang, bạn cần thay đổi nó thành `address="0.0.0.0"` để cho phép kết nối từ các máy chủ khác trong mạng.
-   Thuộc tính `secretRequired="false"` được dùng để đơn giản hóa việc cài đặt. Trong môi trường production, bạn nên đặt một `secret` để tăng cường bảo mật.

### Bước 2: Cài đặt Apache và Mod_Jk

(Hướng dẫn chi tiết cho việc cài đặt và cấu hình sẽ có trong các bài học tiếp theo. Bài này tập trung vào việc giới thiệu các thành phần.)

-   **Cài đặt Apache**: Cài đặt Apache HTTP Server trên một máy chủ riêng (máy chủ cân bằng tải).
    -   Trên Linux: `sudo apt install apache2` hoặc `sudo yum install httpd`.
-   **Tải và Cài đặt Mod_Jk**:
    -   Bạn cần tải về file module `mod_jk.so` đã được biên dịch sẵn, phù hợp với phiên bản Apache và hệ điều hành của bạn.
    -   Đặt file `mod_jk.so` vào thư mục modules của Apache.
    -   Cấu hình Apache để tải module này.

### Bước 3: Cấu hình Mod_Jk

-   Mod_Jk yêu cầu các file cấu hình riêng để hoạt động.
-   **`workers.properties`**: Đây là file cấu hình quan trọng nhất. Nó định nghĩa các "worker" (chính là các node Tomcat) và các thuộc tính của chúng (host, port, loại kết nối). Nó cũng định nghĩa một "load balancer worker" để nhóm các worker lại.
-   Bạn sẽ cần thêm các chỉ thị vào file cấu hình của Apache (`httpd.conf` hoặc một file virtual host) để yêu cầu Mod_Jk xử lý các request và chuyển chúng đến Tomcat.

Các bài học tiếp theo sẽ đi vào chi tiết cách cấu hình các file này để phân phối lưu lượng và quản lý session.
