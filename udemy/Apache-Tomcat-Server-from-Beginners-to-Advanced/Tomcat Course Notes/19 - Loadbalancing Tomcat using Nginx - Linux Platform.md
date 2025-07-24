# Bài 19: Cân bằng tải Tomcat với Nginx trên Linux

- **Thời lượng**: 12:47
- **Bài trước**: [[18 - Loadbalancing using Nginx in Horizontal Cluster - Windows Platform]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[20 - Setting up Apache and Mod_Jk Connector]]

---

## Nội dung

Bài học này hướng dẫn cách thiết lập Nginx làm bộ cân bằng tải cho một cụm Tomcat ngang (horizontal cluster) trong môi trường Linux.

### Mô hình kiến trúc

-   **Máy chủ Nginx (IP: 192.168.1.100)**: Chạy Nginx.
-   **Máy chủ Tomcat Node 1 (IP: 192.168.1.101)**: Chạy Tomcat node 1.
-   **Máy chủ Tomcat Node 2 (IP: 192.168.1.102)**: Chạy Tomcat node 2.

### Bước 1: Chuẩn bị Cụm Tomcat

-   Đảm bảo bạn đã thiết lập thành công một cụm Tomcat ngang trên hai máy chủ Linux như trong [[Bài 16: Thiết lập Cluster trên Linux]].
-   Cả hai node Tomcat đều đang chạy và đã hình thành một cụm.
-   Firewall trên các máy chủ Tomcat đã được cấu hình để cho phép truy cập vào cổng 8080.

### Bước 2: Cài đặt và Cấu hình Nginx trên Linux

Thực hiện các bước sau trên **máy chủ Nginx (192.168.1.100)**:

1.  **Cài đặt Nginx**:
    -   Trên Ubuntu/Debian:
        ```bash
        sudo apt update
        sudo apt install nginx
        ```
    -   Trên CentOS/RHEL:
        ```bash
        sudo yum install epel-release
        sudo yum install nginx
        ```

2.  **Cấu hình Nginx làm Load Balancer**:
    -   Tạo một file cấu hình mới cho trang web của bạn trong `/etc/nginx/sites-available/`.
        ```bash
        sudo nano /etc/nginx/sites-available/tomcat-lb
        ```
    -   Thêm nội dung sau vào file:
        ```nginx
        # Định nghĩa một nhóm các máy chủ backend
        upstream tomcat_cluster {
            # Thuật toán cân bằng tải (tùy chọn)
            # least_conn; # Gửi request đến server có ít kết nối nhất
            # ip_hash;    # Đảm bảo request từ một client luôn đến cùng một server

            # Danh sách các node Tomcat
            server 192.168.1.101:8080;
            server 192.168.1.102:8080;
        }

        server {
            listen 80;
            # server_name your_domain.com;

            location / {
                proxy_pass http://tomcat_cluster;
                proxy_set_header Host $host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto $scheme;
            }
        }
        ```
    -   **Lưu ý**: Khối `upstream` được đặt ở cấp `http`, bên ngoài khối `server`.

3.  **Kích hoạt trang (site)**:
    -   Tạo một liên kết tượng trưng (symbolic link) từ file cấu hình của bạn sang thư mục `sites-enabled`.
        ```bash
        sudo ln -s /etc/nginx/sites-available/tomcat-lb /etc/nginx/sites-enabled/
        ```
    -   (Tùy chọn) Xóa liên kết của trang mặc định để tránh xung đột:
        ```bash
        sudo rm /etc/nginx/sites-enabled/default
        ```

4.  **Kiểm tra cú pháp và khởi động lại Nginx**:
    -   Kiểm tra xem file cấu hình có lỗi không: `sudo nginx -t`
    -   Nếu không có lỗi, khởi động lại Nginx để áp dụng thay đổi: `sudo systemctl restart nginx`

5.  **Mở cổng Firewall trên máy chủ Nginx**:
    -   Cho phép lưu lượng truy cập HTTP (cổng 80) và HTTPS (cổng 443).
        ```bash
        sudo ufw allow 'Nginx Full'
        ```

### Bước 3: Kiểm tra

-   Từ trình duyệt, truy cập vào địa chỉ IP của máy chủ Nginx: `http://192.168.1.100`.
-   Tải lại trang nhiều lần và kiểm tra log truy cập trên cả hai node Tomcat để xác nhận rằng request đang được phân phối đều.

Bạn đã hoàn thành việc thiết lập Nginx làm bộ cân bằng tải cho cụm Tomcat trên Linux.
