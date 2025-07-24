# Bài 18: Cân bằng tải với Nginx cho Cụm ngang trên Windows

- **Thời lượng**: 14:38
- **Bài trước**: [[17 - Loadbalancing using Nginx in Vertical Cluster - Windows Platform]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[19 - Loadbalancing Tomcat using Nginx - Linux Platform]]

---

## Nội dung

Bài học này mở rộng khái niệm cân bằng tải cho một **cụm ngang (horizontal cluster)**, nơi các node Tomcat và máy chủ Nginx chạy trên các máy Windows riêng biệt trong cùng một mạng.

### Mô hình kiến trúc

-   **Máy chủ Nginx (IP: 192.168.1.100)**: Chạy Nginx, đóng vai trò là điểm vào (entry point) cho tất cả người dùng.
-   **Máy chủ Tomcat Node 1 (IP: 192.168.1.101)**: Chạy phiên bản Tomcat thứ nhất.
-   **Máy chủ Tomcat Node 2 (IP: 192.168.1.102)**: Chạy phiên bản Tomcat thứ hai.

### Bước 1: Chuẩn bị các máy chủ Tomcat

1.  Trên mỗi máy chủ Tomcat (`192.168.1.101` và `192.168.1.102`):
    -   Cài đặt Java và một phiên bản Tomcat.
    -   Triển khai ứng dụng web của bạn (đã bật `<distributable/>`).
    -   Mở file `server.xml` và kích hoạt `<Cluster>`.
    -   Đặt `jvmRoute` cho mỗi node (`node1` cho máy chủ thứ nhất, `node2` cho máy chủ thứ hai).
    -   **Quan trọng**: Vì mỗi node chạy trên một máy riêng, bạn không cần thay đổi các cổng mặc định. Cả hai node đều có thể chạy trên cổng 8080.
    -   Đảm bảo firewall trên cả hai máy chủ Tomcat cho phép kết nối đến cổng 8080 từ máy chủ Nginx.
2.  Khởi động cả hai node Tomcat và xác nhận từ log rằng chúng đã tạo thành một cụm.

### Bước 2: Cài đặt và Cấu hình Nginx

Thực hiện các bước sau trên **máy chủ Nginx (192.168.1.100)**:

1.  **Cài đặt Nginx**: Tải và giải nén Nginx cho Windows như trong bài trước.
2.  **Cấu hình `nginx.conf`**: Mở file `conf\nginx.conf` và chỉnh sửa lại khối `http`.

    ```nginx
    http {
        # Định nghĩa nhóm server backend
        upstream tomcat_cluster {
            # Trỏ đến địa chỉ IP và cổng của từng node Tomcat
            server 192.168.1.101:8080;
            server 192.168.1.102:8080;
        }

        server {
            listen       80;
            # Có thể đặt server_name là tên miền thật của bạn
            server_name  www.your-real-domain.com; 

            location / {
                proxy_pass http://tomcat_cluster;
                proxy_set_header Host $host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto $scheme;
            }
        }
    }
    ```

**Điểm khác biệt chính so với cụm dọc:**

-   Trong khối `upstream`, thay vì `127.0.0.1`, chúng ta sử dụng **địa chỉ IP thực** của các máy chủ Tomcat trong mạng.
-   `server_name` có thể được cấu hình với tên miền thực tế mà người dùng sẽ sử dụng để truy cập dịch vụ. (Bạn cần cấu hình DNS công khai để trỏ tên miền này về địa chỉ IP của máy chủ Nginx).

### Bước 3: Khởi động và Kiểm tra

1.  Đảm bảo cả hai node Tomcat đang chạy trên các máy chủ tương ứng.
2.  Trên máy chủ Nginx, khởi động Nginx: `start nginx`.
3.  Từ một máy tính client bất kỳ trong cùng mạng (hoặc từ internet nếu đã cấu hình DNS), mở trình duyệt và truy cập `http://192.168.1.100` (IP của máy chủ Nginx) hoặc `http://www.your-real-domain.com`.
4.  Tải lại trang nhiều lần để xem Nginx phân phối request đến các node Tomcat khác nhau.
5.  Bạn có thể kiểm tra log truy cập trên cả hai node Tomcat để xác nhận rằng chúng đều nhận được request từ Nginx.

Đây là mô hình kiến trúc tiêu chuẩn cho các ứng dụng web có tính sẵn sàng cao và khả năng mở rộng trong môi trường thực tế.
