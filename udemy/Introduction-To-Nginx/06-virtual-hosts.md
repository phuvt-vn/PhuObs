# 6. Virtual Hosts (Server Blocks)

Trong chương này, chúng ta sẽ học cách cấu hình Virtual Hosts để host nhiều website trên cùng một server.

## Khái Niệm Virtual Hosts

### Virtual Hosts Là Gì?

Virtual Hosts (trong Nginx gọi là Server Blocks) cho phép bạn:
- **Host nhiều website** trên cùng một server
- **Sử dụng cùng IP** cho nhiều domain
- **Tiết kiệm tài nguyên** server
- **Quản lý dễ dàng** nhiều dự án

### Cách Hoạt Động

Nginx sử dụng **HTTP Host header** để xác định website nào cần phục vụ:

```
GET / HTTP/1.1
Host: example.com
```

Dựa vào `Host: example.com`, Nginx sẽ chọn server block phù hợp.

## Cấu Hình Cơ Bản

### Server Block Đơn Giản

```nginx
# /etc/nginx/sites-available/example.com
server {
    listen 80;
    server_name example.com www.example.com;
    root /var/www/example.com;
    index index.html index.php;
    
    location / {
        try_files $uri $uri/ =404;
    }
}
```

### Nhiều Virtual Hosts

```nginx
# Website 1: example.com
server {
    listen 80;
    server_name example.com www.example.com;
    root /var/www/example.com;
    index index.html;
    
    access_log /var/log/nginx/example.com.access.log;
    error_log /var/log/nginx/example.com.error.log;
    
    location / {
        try_files $uri $uri/ =404;
    }
}

# Website 2: blog.example.com
server {
    listen 80;
    server_name blog.example.com;
    root /var/www/blog;
    index index.html;
    
    access_log /var/log/nginx/blog.access.log;
    error_log /var/log/nginx/blog.error.log;
    
    location / {
        try_files $uri $uri/ =404;
    }
}

# Website 3: api.example.com
server {
    listen 80;
    server_name api.example.com;
    
    access_log /var/log/nginx/api.access.log;
    error_log /var/log/nginx/api.error.log;
    
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Quản Lý Sites

### Cấu Trúc Thư Mục Ubuntu/Debian

```
/etc/nginx/
├── nginx.conf
├── sites-available/     # Tất cả cấu hình sites
│   ├── default
│   ├── example.com
│   ├── blog.example.com
│   └── api.example.com
├── sites-enabled/       # Sites đang hoạt động (symlinks)
│   ├── default -> ../sites-available/default
│   └── example.com -> ../sites-available/example.com
└── conf.d/             # Cấu hình bổ sung
```

### Enable/Disable Sites

```bash
# Tạo site mới
sudo nano /etc/nginx/sites-available/example.com

# Enable site (tạo symlink)
sudo ln -s /etc/nginx/sites-available/example.com /etc/nginx/sites-enabled/

# Hoặc sử dụng a2ensite (nếu có)
sudo a2ensite example.com

# Disable site (xóa symlink)
sudo rm /etc/nginx/sites-enabled/example.com

# Hoặc sử dụng a2dissite
sudo a2dissite example.com

# Test cấu hình
sudo nginx -t

# Reload Nginx
sudo nginx -s reload
```

### Cấu Trúc Thư Mục CentOS/RHEL

```
/etc/nginx/
├── nginx.conf
└── conf.d/             # Tất cả cấu hình sites
    ├── default.conf
    ├── example.com.conf
    ├── blog.example.com.conf
    └── api.example.com.conf
```

## Server Name Matching

### Exact Match

```nginx
server {
    listen 80;
    server_name example.com;  # Chỉ match chính xác
}
```

### Wildcard Match

```nginx
server {
    listen 80;
    server_name *.example.com;  # Match subdomain.example.com
}

server {
    listen 80;
    server_name example.*;      # Match example.com, example.net
}
```

### Regex Match

```nginx
server {
    listen 80;
    server_name ~^(www\.)?(.+)$;  # Capture groups
    return 301 http://$2$request_uri;  # Redirect www to non-www
}

server {
    listen 80;
    server_name ~^(?<subdomain>.+)\.example\.com$;
    root /var/www/$subdomain;
}
```

### Default Server

```nginx
# Server mặc định khi không match server_name nào
server {
    listen 80 default_server;
    server_name _;
    return 444;  # Đóng connection
}

# Hoặc redirect đến main site
server {
    listen 80 default_server;
    server_name _;
    return 301 http://example.com$request_uri;
}
```

### Thứ Tự Ưu Tiên Server Name

1. **Exact match**: `example.com`
2. **Wildcard bắt đầu với ***: `*.example.com`
3. **Wildcard kết thúc với ***: `example.*`
4. **Regex match**: `~^(.+)$`
5. **Default server**: `default_server`

## Ví Dụ Thực Tế

### Website Portfolio

```nginx
# /etc/nginx/sites-available/portfolio
server {
    listen 80;
    server_name johndoe.com www.johndoe.com;
    root /var/www/portfolio;
    index index.html;
    
    # Custom error pages
    error_page 404 /404.html;
    error_page 500 502 503 504 /50x.html;
    
    # Static assets caching
    location ~* \.(css|js|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # Security headers
    add_header X-Frame-Options "SAMEORIGIN";
    add_header X-Content-Type-Options "nosniff";
    add_header X-XSS-Protection "1; mode=block";
    
    location / {
        try_files $uri $uri/ =404;
    }
}
```

### Blog WordPress

```nginx
# /etc/nginx/sites-available/blog
server {
    listen 80;
    server_name blog.johndoe.com;
    root /var/www/blog;
    index index.php index.html;
    
    # WordPress specific
    location / {
        try_files $uri $uri/ /index.php?$args;
    }
    
    # PHP processing
    location ~ \.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/var/run/php/php8.1-fpm.sock;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
        include fastcgi_params;
    }
    
    # Deny access to sensitive files
    location ~ /\.(ht|git) {
        deny all;
    }
    
    # WordPress uploads
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public";
    }
}
```

### API Server

```nginx
# /etc/nginx/sites-available/api
server {
    listen 80;
    server_name api.johndoe.com;
    
    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    
    location / {
        limit_req zone=api burst=20 nodelay;
        
        # Proxy to Node.js app
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
    
    # Health check endpoint
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
```

### Development Environment

```nginx
# /etc/nginx/sites-available/dev
server {
    listen 80;
    server_name ~^(?<project>.+)\.dev\.local$;
    root /var/www/dev/$project;
    index index.html index.php;
    
    # Auto-create document root
    location / {
        try_files $uri $uri/ @fallback;
    }
    
    location @fallback {
        return 404 "Project $project not found";
    }
    
    # PHP for development
    location ~ \.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/var/run/php/php8.1-fpm.sock;
    }
    
    # Show PHP errors in development
    fastcgi_param PHP_VALUE "error_reporting=E_ALL";
    fastcgi_param PHP_VALUE "display_errors=On";
}
```

## SSL/HTTPS Virtual Hosts

### Basic HTTPS Setup

```nginx
# HTTP (redirect to HTTPS)
server {
    listen 80;
    server_name example.com www.example.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS
server {
    listen 443 ssl http2;
    server_name example.com www.example.com;
    root /var/www/example.com;
    
    # SSL certificates
    ssl_certificate /etc/ssl/certs/example.com.crt;
    ssl_certificate_key /etc/ssl/private/example.com.key;
    
    # SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
    ssl_prefer_server_ciphers off;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=63072000" always;
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    
    location / {
        try_files $uri $uri/ =404;
    }
}
```

### Let's Encrypt với Certbot

```bash
# Cài đặt Certbot
sudo apt install certbot python3-certbot-nginx

# Tạo certificate
sudo certbot --nginx -d example.com -d www.example.com

# Auto-renewal
sudo crontab -e
# Thêm dòng:
0 12 * * * /usr/bin/certbot renew --quiet
```

## Subdomain Wildcards

### Dynamic Subdomains

```nginx
server {
    listen 80;
    server_name ~^(?<subdomain>.+)\.example\.com$;
    root /var/www/subdomains/$subdomain;
    index index.html;
    
    # Fallback nếu subdomain không tồn tại
    location / {
        try_files $uri $uri/ @fallback;
    }
    
    location @fallback {
        return 301 http://example.com;
    }
}
```

### User Subdomains

```nginx
server {
    listen 80;
    server_name ~^(?<username>[a-zA-Z0-9]+)\.users\.example\.com$;
    root /var/www/users/$username/public;
    index index.html;
    
    # Kiểm tra user directory tồn tại
    location / {
        try_files $uri $uri/ @user_not_found;
    }
    
    location @user_not_found {
        return 404 "User $username not found";
    }
}
```

## Load Balancing với Virtual Hosts

### Multiple Backend Servers

```nginx
upstream app_servers {
    server 192.168.1.10:3000;
    server 192.168.1.11:3000;
    server 192.168.1.12:3000;
}

server {
    listen 80;
    server_name app.example.com;
    
    location / {
        proxy_pass http://app_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### Environment-based Routing

```nginx
# Production
server {
    listen 80;
    server_name example.com www.example.com;
    
    location / {
        proxy_pass http://prod_servers;
    }
}

# Staging
server {
    listen 80;
    server_name staging.example.com;
    
    # Basic auth cho staging
    auth_basic "Staging Environment";
    auth_basic_user_file /etc/nginx/.htpasswd;
    
    location / {
        proxy_pass http://staging_servers;
    }
}

# Development
server {
    listen 80;
    server_name dev.example.com;
    
    # Chỉ cho phép internal IPs
    allow 192.168.1.0/24;
    allow 10.0.0.0/8;
    deny all;
    
    location / {
        proxy_pass http://dev_servers;
    }
}
```

## Monitoring và Logging

### Separate Logs cho từng Virtual Host

```nginx
server {
    listen 80;
    server_name example.com;
    
    # Custom log format
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                   '$status $body_bytes_sent "$http_referer" '
                   '"$http_user_agent" "$http_x_forwarded_for"';
    
    access_log /var/log/nginx/example.com.access.log main;
    error_log /var/log/nginx/example.com.error.log warn;
    
    location / {
        try_files $uri $uri/ =404;
    }
}
```

### Log Rotation

```bash
# /etc/logrotate.d/nginx-vhosts
/var/log/nginx/*.log {
    daily
    missingok
    rotate 52
    compress
    delaycompress
    notifempty
    create 644 nginx adm
    postrotate
        if [ -f /var/run/nginx.pid ]; then
            kill -USR1 `cat /var/run/nginx.pid`
        fi
    endscript
}
```

## Testing và Debugging

### Test Virtual Hosts

```bash
# Test với curl
curl -H "Host: example.com" http://server-ip/
curl -H "Host: blog.example.com" http://server-ip/

# Test với hosts file
echo "192.168.1.100 example.com" | sudo tee -a /etc/hosts
curl http://example.com/

# Debug server name matching
sudo nginx -T | grep -A 10 -B 5 server_name
```

### Debug Configuration

```bash
# Kiểm tra syntax
sudo nginx -t

# Test cấu hình cụ thể
sudo nginx -t -c /etc/nginx/sites-available/example.com

# Xem cấu hình được load
sudo nginx -T | grep -A 20 "server {"
```

## Best Practices

### 1. Naming Convention

```
sites-available/
├── 00-default          # Default server
├── example.com         # Main domain
├── www.example.com     # WWW redirect
├── blog.example.com    # Subdomain
└── api.example.com     # API subdomain
```

### 2. Security

- ✅ Sử dụng default server để handle unknown hosts
- ✅ Separate logs cho từng virtual host
- ✅ Rate limiting cho API endpoints
- ✅ Basic auth cho staging environments
- ✅ SSL certificates cho production

### 3. Performance

- ✅ Cache static assets appropriately
- ✅ Use HTTP/2 cho HTTPS sites
- ✅ Optimize proxy settings cho backend apps
- ✅ Monitor logs và performance metrics

### 4. Maintenance

- ✅ Use symlinks để enable/disable sites
- ✅ Regular backup của cấu hình
- ✅ Document server blocks
- ✅ Test cấu hình trước khi deploy

## Troubleshooting

### Virtual Host Không Hoạt Động

```bash
# Kiểm tra DNS
nslookup example.com

# Kiểm tra server_name matching
sudo nginx -T | grep -A 5 -B 5 "server_name.*example.com"

# Test với Host header
curl -v -H "Host: example.com" http://server-ip/

# Kiểm tra logs
sudo tail -f /var/log/nginx/error.log
```

### SSL Issues

```bash
# Test SSL certificate
openssl s_client -connect example.com:443 -servername example.com

# Kiểm tra certificate expiry
echo | openssl s_client -connect example.com:443 2>/dev/null | openssl x509 -noout -dates

# Test SSL configuration
sudo nginx -t
```

## Thực Hành

1. **Setup multiple virtual hosts** cho các domain khác nhau
2. **Configure subdomain wildcards** cho user sites
3. **Setup SSL certificates** với Let's Encrypt
4. **Create staging environment** với basic auth
5. **Monitor logs** cho từng virtual host
6. **Test failover** với multiple backend servers

---

**Tiếp theo**: [Reverse Proxy](./07-reverse-proxy.md)