# 5. Phục Vụ Nội Dung Tĩnh

Trong chương này, chúng ta sẽ học cách cấu hình Nginx để phục vụ các file tĩnh như HTML, CSS, JavaScript, hình ảnh và tài liệu.

## Khái Niệm Cơ Bản

### Nội Dung Tĩnh Là Gì?

Nội dung tĩnh là những file không thay đổi dựa trên request của user:
- **HTML files**: Trang web tĩnh
- **CSS files**: Stylesheet
- **JavaScript files**: Script phía client
- **Images**: PNG, JPG, GIF, SVG, WebP
- **Documents**: PDF, DOC, TXT
- **Media files**: Video, Audio
- **Fonts**: TTF, WOFF, WOFF2

### Tại Sao Nginx Tốt Cho Static Content?

1. **Hiệu suất cao**: Nginx được tối ưu cho việc phục vụ file tĩnh
2. **Ít tài nguyên**: Sử dụng ít CPU và RAM
3. **Caching hiệu quả**: Hỗ trợ nhiều loại cache
4. **Compression**: Tự động nén file để tiết kiệm bandwidth
5. **Security**: Nhiều tính năng bảo mật tích hợp

## Cấu Hình Cơ Bản

### Server Block Đơn Giản

```nginx
server {
    listen 80;
    server_name example.com www.example.com;
    
    # Document root - thư mục chứa website
    root /var/www/html;
    
    # File index mặc định
    index index.html index.htm;
    
    # Location block chính
    location / {
        try_files $uri $uri/ =404;
    }
}
```

### Giải Thích Chi Tiết

- **root**: Thư mục gốc chứa website
- **index**: File sẽ được phục vụ khi truy cập thư mục
- **try_files**: Thử tìm file theo thứ tự, trả về 404 nếu không tìm thấy

### Ví Dụ Cấu Trúc Thư Mục

```
/var/www/html/
├── index.html
├── about.html
├── css/
│   ├── style.css
│   └── bootstrap.css
├── js/
│   ├── app.js
│   └── jquery.js
├── images/
│   ├── logo.png
│   └── banner.jpg
└── downloads/
    ├── manual.pdf
    └── software.zip
```

## Location Blocks Chi Tiết

### Location Matching

```nginx
server {
    listen 80;
    server_name example.com;
    root /var/www/html;
    
    # Exact match (cao nhất)
    location = / {
        return 200 "Homepage exact match";
    }
    
    # Prefix match (ưu tiên cao)
    location ^~ /images/ {
        # Phục vụ hình ảnh
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # Regex match (case sensitive)
    location ~ \.(css|js)$ {
        # Phục vụ CSS và JS
        expires 1M;
        add_header Cache-Control "public";
    }
    
    # Regex match (case insensitive)
    location ~* \.(jpg|jpeg|png|gif|ico|svg)$ {
        # Phục vụ hình ảnh
        expires 1y;
        add_header Cache-Control "public";
    }
    
    # Prefix match (ưu tiên thấp)
    location /api/ {
        # API endpoints
        proxy_pass http://backend;
    }
    
    # Default location
    location / {
        try_files $uri $uri/ =404;
    }
}
```

### Thứ Tự Ưu Tiên Location

1. **Exact match** (`=`)
2. **Prefix match** (`^~`)
3. **Regex match** (`~` và `~*`)
4. **Prefix match** (không có modifier)

## Cấu Hình Cho Các Loại File

### HTML Files

```nginx
location ~* \.html?$ {
    # Không cache HTML để content luôn fresh
    expires -1;
    add_header Cache-Control "no-cache, no-store, must-revalidate";
    add_header Pragma "no-cache";
    
    # Security headers
    add_header X-Frame-Options "SAMEORIGIN";
    add_header X-Content-Type-Options "nosniff";
    add_header X-XSS-Protection "1; mode=block";
}
```

### CSS và JavaScript

```nginx
location ~* \.(css|js)$ {
    # Cache lâu cho CSS/JS
    expires 1M;
    add_header Cache-Control "public";
    
    # Gzip compression
    gzip on;
    gzip_types text/css application/javascript;
    
    # CORS cho CDN
    add_header Access-Control-Allow-Origin "*";
}
```

### Hình Ảnh

```nginx
location ~* \.(jpg|jpeg|png|gif|ico|svg|webp)$ {
    # Cache rất lâu cho hình ảnh
    expires 1y;
    add_header Cache-Control "public, immutable";
    
    # Tối ưu cho hình ảnh lớn
    sendfile on;
    tcp_nopush on;
    tcp_nodelay off;
    
    # Log riêng cho hình ảnh (optional)
    access_log off;
}
```

### Fonts

```nginx
location ~* \.(ttf|ttc|otf|eot|woff|woff2)$ {
    # Cache lâu cho fonts
    expires 1y;
    add_header Cache-Control "public";
    
    # CORS cho web fonts
    add_header Access-Control-Allow-Origin "*";
    add_header Access-Control-Allow-Methods "GET";
    add_header Access-Control-Allow-Headers "Range";
}
```

### Documents và Downloads

```nginx
location /downloads/ {
    # Thư mục downloads
    alias /var/www/downloads/;
    
    # Autoindex để list files
    autoindex on;
    autoindex_exact_size off;
    autoindex_localtime on;
    
    # Force download cho một số file types
    location ~* \.(pdf|doc|docx|zip|tar|gz)$ {
        add_header Content-Disposition "attachment";
    }
}
```

## Tối Ưu Hiệu Suất

### Gzip Compression

```nginx
http {
    # Enable gzip
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_comp_level 6;
    
    # Các file types được nén
    gzip_types
        text/plain
        text/css
        text/xml
        text/javascript
        application/javascript
        application/xml+rss
        application/json
        image/svg+xml;
    
    # Không nén cho IE6
    gzip_disable "msie6";
}
```

### Sendfile và TCP Optimization

```nginx
http {
    # Tối ưu cho file tĩnh
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    
    # Buffer sizes
    client_body_buffer_size 128k;
    client_max_body_size 10m;
    client_header_buffer_size 1k;
    large_client_header_buffers 4 4k;
    output_buffers 1 32k;
    postpone_output 1460;
}
```

### File Caching

```nginx
http {
    # Cache file descriptors
    open_file_cache max=1000 inactive=20s;
    open_file_cache_valid 30s;
    open_file_cache_min_uses 2;
    open_file_cache_errors on;
}
```

## Security cho Static Content

### Ẩn Sensitive Files

```nginx
# Chặn truy cập .htaccess, .git, etc.
location ~ /\. {
    deny all;
    access_log off;
    log_not_found off;
}

# Chặn backup files
location ~ ~$ {
    deny all;
    access_log off;
    log_not_found off;
}

# Chặn config files
location ~* \.(conf|config|ini)$ {
    deny all;
}
```

### Hotlink Protection

```nginx
# Chống hotlink hình ảnh
location ~* \.(jpg|jpeg|png|gif)$ {
    valid_referers none blocked server_names
                   *.example.com example.com
                   *.google.com *.bing.com;
    
    if ($invalid_referer) {
        return 403;
        # Hoặc redirect đến hình ảnh khác
        # rewrite ^/.*$ /images/hotlink-denied.png last;
    }
}
```

### Rate Limiting

```nginx
http {
    # Định nghĩa rate limit zone
    limit_req_zone $binary_remote_addr zone=static:10m rate=10r/s;
}

server {
    # Áp dụng rate limit cho downloads
    location /downloads/ {
        limit_req zone=static burst=20 nodelay;
        # Cho phép 10 requests/second, burst tối đa 20
    }
}
```

## Ví Dụ Cấu Hình Hoàn Chỉnh

```nginx
server {
    listen 80;
    server_name static.example.com;
    root /var/www/static;
    index index.html;
    
    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    
    # Chặn sensitive files
    location ~ /\.|~$ {
        deny all;
        access_log off;
        log_not_found off;
    }
    
    # HTML files - no cache
    location ~* \.html?$ {
        expires -1;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }
    
    # CSS và JS - cache 1 tháng
    location ~* \.(css|js)$ {
        expires 1M;
        add_header Cache-Control "public";
        gzip_static on;
    }
    
    # Hình ảnh - cache 1 năm
    location ~* \.(jpg|jpeg|png|gif|ico|svg|webp)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        access_log off;
        
        # Hotlink protection
        valid_referers none blocked server_names *.example.com;
        if ($invalid_referer) {
            return 403;
        }
    }
    
    # Fonts - cache 1 năm với CORS
    location ~* \.(ttf|otf|eot|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public";
        add_header Access-Control-Allow-Origin "*";
    }
    
    # Downloads directory
    location /downloads/ {
        alias /var/www/downloads/;
        autoindex on;
        autoindex_exact_size off;
        
        # Force download
        location ~* \.(pdf|zip|tar\.gz)$ {
            add_header Content-Disposition "attachment";
        }
    }
    
    # Default location
    location / {
        try_files $uri $uri/ =404;
    }
    
    # Custom error pages
    error_page 404 /404.html;
    error_page 500 502 503 504 /50x.html;
    
    location = /404.html {
        internal;
    }
    
    location = /50x.html {
        internal;
    }
}
```

## Testing và Debugging

### Test Cấu Hình

```bash
# Test syntax
sudo nginx -t

# Reload config
sudo nginx -s reload

# Test với curl
curl -I http://static.example.com/
curl -I http://static.example.com/css/style.css
curl -I http://static.example.com/images/logo.png

# Test compression
curl -H "Accept-Encoding: gzip" -I http://static.example.com/css/style.css

# Test caching headers
curl -I http://static.example.com/images/logo.png | grep -i cache
```

### Monitoring Performance

```bash
# Monitor access logs
sudo tail -f /var/log/nginx/access.log

# Analyze response times
sudo awk '{print $NF}' /var/log/nginx/access.log | sort -n | tail -10

# Check file sizes being served
sudo awk '{print $10}' /var/log/nginx/access.log | sort -n | tail -10
```

## Best Practices

### 1. Cấu Trúc Thư Mục

```
/var/www/
├── html/                 # Main website
│   ├── index.html
│   ├── assets/
│   │   ├── css/
│   │   ├── js/
│   │   └── images/
├── static/              # Static CDN content
│   ├── css/
│   ├── js/
│   └── images/
└── downloads/           # Download files
    ├── docs/
    └── software/
```

### 2. Cache Strategy

- **HTML**: Không cache hoặc cache ngắn
- **CSS/JS**: Cache 1 tháng, sử dụng versioning
- **Images**: Cache 1 năm
- **Fonts**: Cache 1 năm với CORS
- **Documents**: Cache theo nhu cầu

### 3. Security Checklist

- ✅ Ẩn sensitive files (`.git`, `.env`, etc.)
- ✅ Hotlink protection cho media
- ✅ Rate limiting cho downloads
- ✅ Security headers
- ✅ Proper file permissions

### 4. Performance Checklist

- ✅ Enable gzip compression
- ✅ Optimize cache headers
- ✅ Use sendfile for large files
- ✅ Enable file descriptor caching
- ✅ Minimize access logging cho static files

## Troubleshooting

### File Không Tìm Thấy (404)

```bash
# Kiểm tra file permissions
sudo ls -la /var/www/html/

# Kiểm tra SELinux context (CentOS/RHEL)
sudo ls -Z /var/www/html/

# Test với absolute path
curl -I http://localhost/path/to/file.css
```

### Performance Issues

```bash
# Kiểm tra disk I/O
sudo iotop

# Kiểm tra file descriptor limits
sudo lsof | grep nginx | wc -l
ulimit -n

# Monitor nginx processes
sudo htop -p $(pgrep nginx | tr '\n' ',')
```

## Thực Hành

1. **Tạo website tĩnh** với HTML, CSS, JS
2. **Cấu hình cache headers** cho từng loại file
3. **Enable gzip compression** và test
4. **Setup hotlink protection** cho hình ảnh
5. **Tạo downloads directory** với autoindex
6. **Test performance** với các tools như GTmetrix

---

**Tiếp theo**: [Virtual Hosts](./06-virtual-hosts.md)