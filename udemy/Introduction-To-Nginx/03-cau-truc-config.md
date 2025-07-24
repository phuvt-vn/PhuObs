# 3. Cấu Trúc File Config Nginx

File cấu hình là trái tim của Nginx. Hiểu rõ cấu trúc này sẽ giúp bạn cấu hình Nginx một cách hiệu quả.

## Tổng Quan File nginx.conf

File cấu hình chính của Nginx thường nằm ở:
- **Ubuntu/Debian**: `/etc/nginx/nginx.conf`
- **CentOS/RHEL**: `/etc/nginx/nginx.conf`
- **macOS**: `/usr/local/etc/nginx/nginx.conf`
- **Windows**: `C:\nginx\conf\nginx.conf`

## Cấu Trúc Cơ Bản

```nginx
# Global context (ngữ cảnh toàn cục)
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log;
pid /run/nginx.pid;

# Events context (ngữ cảnh sự kiện)
events {
    worker_connections 1024;
    use epoll;
}

# HTTP context (ngữ cảnh HTTP)
http {
    # HTTP directives
    include /etc/nginx/mime.types;
    default_type application/octet-stream;
    
    # Server context (ngữ cảnh server)
    server {
        listen 80;
        server_name example.com;
        
        # Location context (ngữ cảnh location)
        location / {
            root /var/www/html;
            index index.html;
        }
    }
}
```

## Các Context (Ngữ Cảnh) Chính

### 1. Main Context (Ngữ Cảnh Chính)
Đây là phạm vi toàn cục, nằm ngoài tất cả các block khác.

```nginx
# Người dùng chạy Nginx
user nginx;

# Số worker processes
worker_processes auto;

# File log lỗi
error_log /var/log/nginx/error.log warn;

# File chứa process ID
pid /var/run/nginx.pid;
```

### 2. Events Context
Cấu hình cách Nginx xử lý connections.

```nginx
events {
    # Số connections mỗi worker có thể xử lý
    worker_connections 1024;
    
    # Phương thức xử lý events (Linux)
    use epoll;
    
    # Cho phép worker accept nhiều connections cùng lúc
    multi_accept on;
}
```

### 3. HTTP Context
Chứa tất cả cấu hình liên quan đến HTTP.

```nginx
http {
    # Include file MIME types
    include /etc/nginx/mime.types;
    default_type application/octet-stream;
    
    # Logging format
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';
    
    # Access log
    access_log /var/log/nginx/access.log main;
    
    # Performance settings
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    
    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    
    # Include server configs
    include /etc/nginx/conf.d/*.conf;
    include /etc/nginx/sites-enabled/*;
}
```

### 4. Server Context
Định nghĩa một virtual server.

```nginx
server {
    # Port và IP để listen
    listen 80;
    listen [::]:80;  # IPv6
    
    # Tên server
    server_name example.com www.example.com;
    
    # Document root
    root /var/www/example.com;
    
    # File index mặc định
    index index.html index.htm index.php;
    
    # Access log riêng cho server này
    access_log /var/log/nginx/example.com.access.log;
    error_log /var/log/nginx/example.com.error.log;
}
```

### 5. Location Context
Định nghĩa cách xử lý các URL cụ thể.

```nginx
server {
    listen 80;
    server_name example.com;
    root /var/www/html;
    
    # Location cho root
    location / {
        try_files $uri $uri/ =404;
    }
    
    # Location cho static files
    location ~* \.(css|js|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # Location cho API
    location /api/ {
        proxy_pass http://backend_server;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Directives (Chỉ Thị)

### Simple Directives (Chỉ thị đơn giản)
Kết thúc bằng dấu chấm phẩy (;)

```nginx
listen 80;
server_name example.com;
root /var/www/html;
index index.html;
```

### Block Directives (Chỉ thị khối)
Chứa các directives khác trong dấu ngoặc nhọn {}

```nginx
server {
    listen 80;
    server_name example.com;
    
    location / {
        root /var/www/html;
    }
}
```

## Các Directive Quan Trọng

### Worker Configuration

```nginx
# Số worker processes (thường = số CPU cores)
worker_processes auto;

# Số connections mỗi worker
worker_connections 1024;

# CPU affinity
worker_cpu_affinity auto;

# Priority của worker processes
worker_priority -10;
```

### Logging Configuration

```nginx
# Error log levels: debug, info, notice, warn, error, crit, alert, emerg
error_log /var/log/nginx/error.log warn;

# Access log format
log_format combined '$remote_addr - $remote_user [$time_local] '
                   '"$request" $status $body_bytes_sent '
                   '"$http_referer" "$http_user_agent"';

access_log /var/log/nginx/access.log combined;
```

### Performance Directives

```nginx
# Sử dụng sendfile system call
sendfile on;

# Optimize sendfile packets
tcp_nopush on;
tcp_nodelay on;

# Keep-alive timeout
keepalive_timeout 65;

# Client body size limit
client_max_body_size 100M;

# Buffer sizes
client_body_buffer_size 128k;
client_header_buffer_size 1k;
large_client_header_buffers 4 4k;
```

## Include Directive

Để tổ chức cấu hình tốt hơn, sử dụng `include`:

```nginx
http {
    # Include MIME types
    include /etc/nginx/mime.types;
    
    # Include general settings
    include /etc/nginx/conf.d/general.conf;
    
    # Include all server configs
    include /etc/nginx/sites-enabled/*;
    
    # Include SSL settings
    include /etc/nginx/conf.d/ssl.conf;
}
```

## Variables (Biến)

Nginx có nhiều biến built-in:

```nginx
server {
    listen 80;
    server_name example.com;
    
    location / {
        # Log client IP
        access_log /var/log/nginx/access.log 
                   '$remote_addr - [$time_local] "$request"';
        
        # Set custom header
        add_header X-Server-Name $hostname;
        
        # Conditional logic
        if ($request_method = POST) {
            return 405;
        }
    }
}
```

### Các Biến Thường Dùng

```nginx
$remote_addr        # IP của client
$remote_user        # Username (nếu có auth)
$time_local         # Thời gian local
$request            # Request line đầy đủ
$request_method     # GET, POST, PUT, etc.
$request_uri        # URI với query string
$uri                # URI không có query string
$args               # Query string
$host               # Host header
$server_name        # Server name
$server_port        # Server port
$scheme             # http hoặc https
$document_root      # Document root
$realpath_root      # Real path của document root
```

## Comments (Chú Thích)

Sử dụng dấu `#` để thêm comments:

```nginx
# Đây là comment
server {
    listen 80;  # Listen trên port 80
    
    # Server name configuration
    server_name example.com;
    
    location / {
        root /var/www/html;  # Document root
        index index.html;    # Default index file
    }
}
```

## Ví Dụ File Config Hoàn Chỉnh

```nginx
# /etc/nginx/nginx.conf

# Main context
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log;
pid /run/nginx.pid;

# Events context
events {
    worker_connections 1024;
    use epoll;
    multi_accept on;
}

# HTTP context
http {
    # MIME types
    include /etc/nginx/mime.types;
    default_type application/octet-stream;
    
    # Logging
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';
    
    access_log /var/log/nginx/access.log main;
    
    # Performance
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    
    # Security
    server_tokens off;
    
    # Gzip
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript;
    
    # Include server configs
    include /etc/nginx/conf.d/*.conf;
    include /etc/nginx/sites-enabled/*;
}
```

## Best Practices

### 1. Tổ Chức File
- Tách các server configs thành files riêng
- Sử dụng thư mục `sites-available` và `sites-enabled`
- Tạo file config cho từng domain

### 2. Naming Convention
```
/etc/nginx/sites-available/
├── example.com.conf
├── api.example.com.conf
└── blog.example.com.conf
```

### 3. Comments
- Thêm comments giải thích cấu hình phức tạp
- Ghi chú lý do tại sao sử dụng setting đó

### 4. Validation
Luôn test config trước khi reload:
```bash
sudo nginx -t
```

## Thực Hành

1. **Xem file config** hiện tại của bạn
2. **Tìm hiểu** từng section trong file
3. **Thêm comments** để hiểu rõ hơn
4. **Tạo backup** trước khi chỉnh sửa
5. **Test config** sau mỗi thay đổi

---

**Tiếp theo**: [Các Lệnh Cơ Bản](./04-lenh-co-ban.md)