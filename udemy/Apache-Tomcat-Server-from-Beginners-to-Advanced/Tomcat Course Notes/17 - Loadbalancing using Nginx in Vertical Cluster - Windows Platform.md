# Bài 17: Cân bằng tải với Nginx cho Cụm dọc trên Windows

- **Thời lượng**: 09:07
- **Bài trước**: [[16 - Cluster Setup - Linux Platform]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[18 - Loadbalancing using Nginx in Horizontal Cluster - Windows Platform]]

---

## Nội dung

Sau khi đã thiết lập một cụm Tomcat dọc (vertical cluster) trên Windows ([[Bài 15]]), bài học này hướng dẫn cách sử dụng Nginx làm bộ cân bằng tải (load balancer) để phân phối các yêu cầu đến các node trong cụm.

### Bước 1: Cài đặt Nginx trên Windows

1.  **Tải xuống Nginx**:
    -   Truy cập trang chủ của Nginx: [https://nginx.org/en/download.html](https://nginx.org/en/download.html)
    -   Tải về phiên bản "Mainline version" cho Windows (dạng file `.zip`).
2.  **Giải nén**:
    -   Giải nén file zip vào một thư mục, ví dụ: `C:\nginx-1.21.3`.

### Bước 2: Cấu hình Nginx làm Load Balancer

1.  Mở file cấu hình chính của Nginx: `C:\nginx-1.21.3\conf\nginx.conf`.
2.  Thay thế toàn bộ nội dung file bằng cấu hình sau:

    ```nginx
    # Chạy Nginx với một worker process
    worker_processes  1;

    events {
        worker_connections  1024;
    }

    http {
        # Định nghĩa một nhóm các server (Tomcat nodes) gọi là "tomcat_cluster"
        upstream tomcat_cluster {
            # Danh sách các server trong cụm
            # Tomcat Node 1 chạy trên cổng 8080
            server 127.0.0.1:8080;
            # Tomcat Node 2 chạy trên cổng 8081
            server 127.0.0.1:8081;
        }

        # Định nghĩa một server ảo lắng nghe trên cổng 80
        server {
            listen       80;
            server_name  localhost;

            # Tất cả các request đến server này (/) sẽ được xử lý bởi khối location này
            location / {
                # Chuyển tiếp (proxy) request đến nhóm server "tomcat_cluster"
                proxy_pass http://tomcat_cluster;

                # (Tùy chọn nhưng khuyến khích) Thêm các header để Tomcat biết thông tin gốc của request
                proxy_set_header Host $host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto $scheme;
            }
        }
    }
    ```

**Giải thích cấu hình:**

-   **`upstream tomcat_cluster`**: Khai báo một nhóm các máy chủ backend. `tomcat_cluster` là tên do bạn tự đặt.
-   **`server 127.0.0.1:8080;`**: Khai báo máy chủ Tomcat Node 1, đang chạy trên cổng 8080.
-   **`server 127.0.0.1:8081;`**: Khai báo máy chủ Tomcat Node 2, đang chạy trên cổng 8081.
-   **`server { listen 80; }`**: Cấu hình Nginx để lắng nghe các request đến trên cổng 80 (cổng HTTP mặc định).
-   **`location / { ... }`**: Áp dụng cho tất cả các request.
-   **`proxy_pass http://tomcat_cluster;`**: Đây là chỉ thị quan trọng nhất. Nó yêu cầu Nginx chuyển tiếp mọi request mà nó nhận được đến một trong các server trong nhóm `tomcat_cluster`. Theo mặc định, Nginx sẽ sử dụng thuật toán **Round Robin** để phân phối (luân phiên gửi request đến từng server).

### Bước 3: Khởi động và Kiểm tra

1.  **Đảm bảo các node Tomcat đang chạy**: Khởi động Node 1 (cổng 8080) và Node 2 (cổng 8081) như trong [[Bài 15]].
2.  **Khởi động Nginx**:
    -   Mở Command Prompt.
    -   Di chuyển đến thư mục cài đặt Nginx: `cd C:\nginx-1.21.3`
    -   Chạy lệnh: `start nginx`
3.  **Kiểm tra cân bằng tải**:
    -   Mở trình duyệt và truy cập `http://localhost` (không cần chỉ định cổng, vì Nginx đang nghe ở cổng 80).
    -   Bạn sẽ thấy trang ứng dụng của mình.
    -   Nhấn F5 (tải lại trang) nhiều lần. Nếu ứng dụng của bạn có cách để hiển thị `jvmRoute` (ID của session thường chứa thông tin này), bạn sẽ thấy ID thay đổi giữa `node1` và `node2`, chứng tỏ Nginx đang phân phối request đến cả hai node.
4.  **Dừng Nginx**:
    -   Chạy lệnh: `nginx -s stop`

Bây giờ bạn đã có một hệ thống hoàn chỉnh với Nginx làm bộ cân bằng tải cho cụm Tomcat trên Windows.
