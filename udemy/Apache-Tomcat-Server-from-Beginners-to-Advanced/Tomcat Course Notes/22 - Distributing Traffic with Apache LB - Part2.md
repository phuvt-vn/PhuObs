# Bài 22: Phân phối lưu lượng với Apache LB - Phần 2

- **Thời lượng**: 11:58
- **Bài trước**: [[21 - Distributing Traffic with Apache LB - Part1]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[23 - Sticky Sessions - Concept]]

---

## Nội dung

Sau khi đã cấu hình `workers.properties`, bài học này tập trung vào việc cấu hình máy chủ Apache HTTP để tải module `mod_jk` và sử dụng bộ cân bằng tải (`mybalancer`) mà chúng ta đã định nghĩa.

### Cấu hình Apache HTTP Server

Các chỉ thị (directives) của `mod_jk` cần được thêm vào file cấu hình chính của Apache (`httpd.conf`) hoặc tốt hơn là trong một file cấu hình riêng cho virtual host.

#### Bước 1: Tải Module Mod_Jk

Đầu tiên, bạn cần yêu cầu Apache tải file module `mod_jk.so`.

```apache
# Tải module mod_jk
LoadModule jk_module /path/to/your/modules/mod_jk.so
```
-   Đường dẫn `/path/to/your/modules/mod_jk.so` phải là đường dẫn tuyệt đối đến file `mod_jk.so` trên máy chủ của bạn.

#### Bước 2: Cấu hình Mod_Jk

Tiếp theo, bạn cần cung cấp các chỉ thị cấu hình cơ bản cho `mod_jk`.

```apache
# Chỉ định vị trí của file workers.properties
JkWorkersFile /etc/apache2/workers.properties

# Chỉ định vị trí của file log cho mod_jk
JkLogFile /var/log/apache2/mod_jk.log

# Đặt mức độ chi tiết của log (debug, info, error)
JkLogLevel info

# (Tùy chọn) Định dạng của log
JkLogStampFormat "[%a %b %d %H:%M:%S %Y] "
```

#### Bước 3: Ánh xạ Request tới Tomcat (JkMount)

Chỉ thị `JkMount` là quan trọng nhất. Nó ra lệnh cho `mod_jk` chuyển tiếp các request khớp với một mẫu URL cụ thể đến một worker đã được định nghĩa trong `workers.properties`.

Để chuyển tiếp tất cả các request đến bộ cân bằng tải `mybalancer`, bạn có thể thêm cấu hình sau vào trong khối `<VirtualHost>` của mình:

```apache
<VirtualHost *:80>
    ServerName www.your-domain.com
    
    # ... các cấu hình khác của VirtualHost ...
    
    # Chuyển tiếp TẤT CẢ request (/*) đến worker 'mybalancer'
    JkMount /* mybalancer
    
</VirtualHost>
```

**Ví dụ cấu hình hoàn chỉnh (trên Linux):**

Tạo file `/etc/apache2/sites-available/tomcat-lb.conf`:
```apache
<VirtualHost *:80>
    ServerAdmin webmaster@localhost
    ServerName your-domain.com

    ErrorLog ${APACHE_LOG_DIR}/error.log
    CustomLog ${APACHE_LOG_DIR}/access.log combined

    # Cấu hình Mod_Jk
    JkWorkersFile /etc/apache2/workers.properties
    JkLogFile ${APACHE_LOG_DIR}/mod_jk.log
    JkLogLevel info

    # Gửi tất cả các request đến bộ cân bằng tải Tomcat
    JkMount /* mybalancer

    # (Tùy chọn) Không gửi một số loại file tĩnh (hình ảnh, css, js) đến Tomcat
    # Apache sẽ tự phục vụ các file này để giảm tải cho Tomcat
    JkUnMount /*.jpg mybalancer
    JkUnMount /*.jpeg mybalancer
    JkUnMount /*.gif mybalancer
    JkUnMount /*.png mybalancer
    JkUnMount /*.css mybalancer
    JkUnMount /*.js mybalancer
</VirtualHost>
```
-   `JkUnMount` được sử dụng để loại trừ một số mẫu URL khỏi việc chuyển tiếp. Đây là một kỹ thuật tối ưu hóa phổ biến.

### Bước 4: Khởi động lại và Kiểm tra

1.  Lưu các file cấu hình.
2.  Kích hoạt trang (nếu cần): `sudo a2ensite tomcat-lb`
3.  Kiểm tra cú pháp cấu hình Apache: `sudo apache2ctl configtest`
4.  Nếu không có lỗi, khởi động lại Apache: `sudo systemctl restart apache2`
5.  Truy cập vào tên miền của bạn qua trình duyệt. Apache sẽ nhận request và `mod_jk` sẽ chuyển tiếp nó đến một trong các node Tomcat trong cụm. Tải lại trang để thấy sự cân bằng tải.
