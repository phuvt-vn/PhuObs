# Bài 24: Cấu hình Sticky Sessions trên Nginx & Apache LB

- **Thời lượng**: 09:43
- **Bài trước**: [[23 - Sticky Sessions - Concept]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[25 - Session Replication - Concept]]

---

## Nội dung

Bài học này hướng dẫn cách bật tính năng sticky sessions trên hai bộ cân bằng tải phổ biến là Nginx và Apache (với `mod_jk`). Điều kiện tiên quyết là các node Tomcat trong cụm đã được cấu hình với `jvmRoute` riêng biệt.

### Cấu hình Sticky Sessions với Nginx

Nginx sử dụng chỉ thị `ip_hash` để triển khai sticky sessions. Cơ chế này hơi khác so với việc đọc `jvmRoute`. Thay vì dựa vào session cookie, `ip_hash` đảm bảo rằng các request từ cùng một địa chỉ IP của client sẽ luôn được gửi đến cùng một máy chủ backend.

**Cách cấu hình:**

-   Mở file cấu hình của Nginx (ví dụ: `/etc/nginx/sites-available/tomcat-lb`).
-   Thêm chỉ thị `ip_hash;` vào đầu khối `upstream`.

```nginx
upstream tomcat_cluster {
    # Sử dụng thuật toán IP Hash để "gắn" client vào một server
    ip_hash;

    server 192.168.1.101:8080;
    server 192.168.1.102:8080;
}
```

**Lưu ý về `ip_hash`:**
-   **Ưu điểm**: Rất đơn giản để cấu hình.
-   **Nhược điểm**: Nếu nhiều client cùng đi ra internet qua một địa chỉ IP công cộng duy nhất (ví dụ: các nhân viên trong cùng một văn phòng), tất cả họ sẽ bị gửi đến cùng một node Tomcat, có thể gây ra phân phối tải không đều. Nó cũng không hoạt động tốt nếu địa chỉ IP của client thay đổi.

> **Ghi chú**: Mặc dù Nginx có các module của bên thứ ba để có thể triển khai sticky session dựa trên cookie (giống như `mod_jk`), `ip_hash` là phương pháp được tích hợp sẵn và phổ biến nhất.

### Cấu hình Sticky Sessions với Apache và Mod_Jk

`mod_jk` được thiết kế để tích hợp chặt chẽ với Tomcat và nó hỗ trợ sticky sessions một cách tự nhiên bằng cách đọc `jvmRoute` từ `JSESSIONID`.

**Cách cấu hình:**

-   Mở file `workers.properties` (ví dụ: `/etc/apache2/workers.properties`).
-   Thêm thuộc tính `sticky_session=true` hoặc `sticky_session=1` vào định nghĩa của worker cân bằng tải.

```properties
# workers.properties

# ... (định nghĩa worker.list, worker.node1, worker.node2) ...

# --- ĐỊNH NGHĨA WORKER CÂN BẰNG TẢI ---
worker.mybalancer.type=lb
worker.mybalancer.balance_workers=node1,node2

# Bật tính năng Sticky Session
worker.mybalancer.sticky_session=true
```

**Cách hoạt động của `mod_jk` với `sticky_session=true`:**

1.  Khi một request đến, `mod_jk` sẽ kiểm tra xem có cookie `JSESSIONID` không.
2.  Nếu không có, nó sẽ chọn một node (ví dụ: `node1`) theo thuật toán cân bằng tải và gửi request đến đó.
3.  Tomcat `node1` trả về response với cookie `JSESSIONID` có dạng `...SESSION_ID...node1`.
4.  Khi request tiếp theo của cùng client đến, `mod_jk` thấy cookie, đọc phần hậu tố `.node1`, và ngay lập tức gửi request đến `worker.node1` mà không cần thực hiện cân bằng tải.
5.  Nếu `node1` bị lỗi, `mod_jk` sẽ phát hiện và chuyển request sang một node khác đang hoạt động trong `balance_workers`.

So với `ip_hash` của Nginx, phương pháp của `mod_jk` linh hoạt và hiệu quả hơn trong việc đảm bảo session persistence.
