# 7. Reverse Proxy

Trong chương cuối này, chúng ta sẽ học cách sử dụng Nginx làm Reverse Proxy để chuyển tiếp requests đến các backend servers.

## Khái Niệm Reverse Proxy

### Reverse Proxy Là Gì?

Reverse Proxy là một server đứng giữa clients và backend servers:
- **Nhận requests** từ clients
- **Chuyển tiếp** đến backend servers
- **Trả về response** cho clients
- **Ẩn backend servers** khỏi clients

### Forward Proxy vs Reverse Proxy

```
Forward Proxy:
Client → Proxy → Internet → Server
(Proxy đại diện cho Client)

Reverse Proxy:
Client → Internet → Proxy → Backend Server
(Proxy đại diện cho Server)
```

### Lợi Ích của Reverse Proxy

1. **Load Balancing**: Phân tải giữa nhiều servers
2. **SSL Termination**: Xử lý SSL/TLS tập trung
3. **Caching**: Cache responses để tăng tốc
4. **Security**: Ẩn backend infrastructure
5. **Compression**: Nén responses
6. **Rate Limiting**: Giới hạn requests

## Cấu Hình Cơ Bản

### Proxy Đơn Giản

```nginx
server {
    listen 80;
    server_name example.com;
    
    location / {
        proxy_pass http://localhost:3000;
    }
}
```

### Proxy với Headers

```nginx
server {
    listen 80;
    server_name example.com;
    
    location / {
        proxy_pass http://localhost:3000;
        
        # Preserve original request info
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Connection settings
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_cache_bypass $http_upgrade;
    }
}
```

### Giải Thích Headers

- **Host**: Domain name từ request gốc
- **X-Real-IP**: IP thực của client
- **X-Forwarded-For**: Chain of proxy IPs
- **X-Forwarded-Proto**: Protocol gốc (http/https)
- **Upgrade**: Cho WebSocket connections
- **Connection**: Connection type

## Load Balancing

### Upstream Blocks

```nginx
# Định nghĩa backend servers
upstream backend {
    server 192.168.1.10:3000;
    server 192.168.1.11:3000;
    server 192.168.1.12:3000;
}

server {
    listen 80;
    server_name example.com;
    
    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### Load Balancing Methods

#### Round Robin (Default)

```nginx
upstream backend {
    server server1.example.com;
    server server2.example.com;
    server server3.example.com;
}
```

#### Weighted Round Robin

```nginx
upstream backend {
    server server1.example.com weight=3;
    server server2.example.com weight=2;
    server server3.example.com weight=1;
}
```

#### IP Hash

```nginx
upstream backend {
    ip_hash;
    server server1.example.com;
    server server2.example.com;
    server server3.example.com;
}
```

#### Least Connections

```nginx
upstream backend {
    least_conn;
    server server1.example.com;
    server server2.example.com;
    server server3.example.com;
}
```

### Health Checks

```nginx
upstream backend {
    server server1.example.com max_fails=3 fail_timeout=30s;
    server server2.example.com max_fails=3 fail_timeout=30s;
    server server3.example.com backup;  # Backup server
    server server4.example.com down;    # Temporarily disabled
}
```

**Parameters:**
- **max_fails**: Số lần fail trước khi mark server down
- **fail_timeout**: Thời gian server bị mark down
- **backup**: Server chỉ dùng khi tất cả servers khác down
- **down**: Tạm thời disable server

## Ví Dụ Thực Tế

### Node.js Application

```nginx
# Backend Node.js servers
upstream nodejs_backend {
    server 127.0.0.1:3000;
    server 127.0.0.1:3001;
    server 127.0.0.1:3002;
}

server {
    listen 80;
    server_name app.example.com;
    
    # Static files served by Nginx
    location /static/ {
        alias /var/www/app/static/;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # API requests to Node.js
    location /api/ {
        proxy_pass http://nodejs_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # All other requests to Node.js
    location / {
        proxy_pass http://nodejs_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Microservices Architecture

```nginx
# User service
upstream user_service {
    server user1.internal:8080;
    server user2.internal:8080;
}

# Order service
upstream order_service {
    server order1.internal:8081;
    server order2.internal:8081;
}

# Payment service
upstream payment_service {
    server payment1.internal:8082;
    server payment2.internal:8082;
}

server {
    listen 80;
    server_name api.example.com;
    
    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    
    # User service
    location /api/users/ {
        limit_req zone=api burst=20 nodelay;
        proxy_pass http://user_service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    # Order service
    location /api/orders/ {
        limit_req zone=api burst=20 nodelay;
        proxy_pass http://order_service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    # Payment service (more restrictive)
    location /api/payments/ {
        limit_req zone=api burst=5 nodelay;
        proxy_pass http://payment_service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # Additional security
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_ssl_verify on;
    }
    
    # Health check endpoint
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
```

### WordPress với PHP-FPM

```nginx
server {
    listen 80;
    server_name blog.example.com;
    root /var/www/wordpress;
    index index.php index.html;
    
    # Static files
    location ~* \.(css|js|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public";
        try_files $uri =404;
    }
    
    # WordPress uploads
    location /wp-content/uploads/ {
        expires 1y;
        add_header Cache-Control "public";
    }
    
    # WordPress admin
    location /wp-admin/ {
        try_files $uri $uri/ /index.php?$args;
        
        # Basic security
        allow 192.168.1.0/24;
        deny all;
    }
    
    # PHP processing
    location ~ \.php$ {
        try_files $uri =404;
        fastcgi_split_path_info ^(.+\.php)(/.+)$;
        
        # Proxy to PHP-FPM
        fastcgi_pass unix:/var/run/php/php8.1-fpm.sock;
        fastcgi_index index.php;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
        include fastcgi_params;
        
        # Security
        fastcgi_param HTTP_PROXY "";
    }
    
    # WordPress permalinks
    location / {
        try_files $uri $uri/ /index.php?$args;
    }
    
    # Deny access to sensitive files
    location ~ /\.(ht|git) {
        deny all;
    }
}
```

## SSL Termination

### HTTPS to HTTP Backend

```nginx
server {
    listen 443 ssl http2;
    server_name secure.example.com;
    
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
        # Proxy to HTTP backend
        proxy_pass http://backend_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;  # Important!
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name secure.example.com;
    return 301 https://$server_name$request_uri;
}
```

## Caching

### Proxy Caching

```nginx
http {
    # Cache path và settings
    proxy_cache_path /var/cache/nginx/proxy 
                     levels=1:2 
                     keys_zone=my_cache:10m 
                     max_size=10g 
                     inactive=60m 
                     use_temp_path=off;
}

server {
    listen 80;
    server_name cached.example.com;
    
    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        
        # Cache settings
        proxy_cache my_cache;
        proxy_cache_valid 200 302 10m;
        proxy_cache_valid 404 1m;
        proxy_cache_use_stale error timeout invalid_header updating;
        proxy_cache_lock on;
        
        # Cache headers
        add_header X-Cache-Status $upstream_cache_status;
    }
    
    # Don't cache API calls
    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_no_cache 1;
        proxy_cache_bypass 1;
    }
    
    # Cache purge endpoint
    location ~ /purge(/.*) {
        allow 127.0.0.1;
        allow 192.168.1.0/24;
        deny all;
        proxy_cache_purge my_cache "$scheme$request_method$host$1";
    }
}
```

### Microcaching

```nginx
server {
    listen 80;
    server_name dynamic.example.com;
    
    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        
        # Very short cache for dynamic content
        proxy_cache my_cache;
        proxy_cache_valid 200 1s;
        proxy_cache_use_stale updating;
        proxy_cache_background_update on;
        proxy_cache_lock on;
    }
}
```

## WebSocket Support

### WebSocket Proxy

```nginx
map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
}

upstream websocket {
    server 127.0.0.1:3000;
    server 127.0.0.1:3001;
}

server {
    listen 80;
    server_name ws.example.com;
    
    location / {
        proxy_pass http://websocket;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # WebSocket specific timeouts
        proxy_read_timeout 86400;
        proxy_send_timeout 86400;
    }
}
```

## Advanced Configuration

### Custom Error Pages

```nginx
server {
    listen 80;
    server_name example.com;
    
    # Custom error pages
    error_page 502 503 504 /maintenance.html;
    error_page 404 /404.html;
    
    location = /maintenance.html {
        root /var/www/error-pages;
        internal;
    }
    
    location = /404.html {
        root /var/www/error-pages;
        internal;
    }
    
    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        
        # Custom error handling
        proxy_intercept_errors on;
    }
}
```

### Conditional Proxying

```nginx
server {
    listen 80;
    server_name example.com;
    
    # Route based on user agent
    location / {
        if ($http_user_agent ~* "(bot|crawler|spider)") {
            proxy_pass http://seo_backend;
            break;
        }
        
        if ($http_user_agent ~* "Mobile") {
            proxy_pass http://mobile_backend;
            break;
        }
        
        proxy_pass http://desktop_backend;
    }
    
    # Route based on geography
    location /api/ {
        if ($geoip_country_code = "US") {
            proxy_pass http://us_backend;
            break;
        }
        
        if ($geoip_country_code = "EU") {
            proxy_pass http://eu_backend;
            break;
        }
        
        proxy_pass http://global_backend;
    }
}
```

### Sticky Sessions

```nginx
upstream backend {
    ip_hash;  # Simple sticky sessions
    server server1.example.com;
    server server2.example.com;
    server server3.example.com;
}

# Or with custom hash
upstream backend_custom {
    hash $cookie_sessionid consistent;
    server server1.example.com;
    server server2.example.com;
    server server3.example.com;
}
```

## Monitoring và Debugging

### Logging

```nginx
http {
    # Custom log format for proxy
    log_format proxy '$remote_addr - $remote_user [$time_local] '
                    '"$request" $status $body_bytes_sent '
                    '"$http_referer" "$http_user_agent" '
                    'rt=$request_time uct="$upstream_connect_time" '
                    'uht="$upstream_header_time" urt="$upstream_response_time"';
}

server {
    listen 80;
    server_name example.com;
    
    access_log /var/log/nginx/proxy.access.log proxy;
    error_log /var/log/nginx/proxy.error.log;
    
    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        
        # Add timing headers
        add_header X-Response-Time $request_time;
        add_header X-Upstream-Response-Time $upstream_response_time;
    }
}
```

### Health Check Endpoint

```nginx
server {
    listen 80;
    server_name example.com;
    
    # Health check cho load balancer
    location /nginx-health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
    
    # Backend health check
    location /backend-health {
        proxy_pass http://backend/health;
        proxy_set_header Host $host;
        access_log off;
    }
    
    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
    }
}
```

## Performance Tuning

### Connection Pooling

```nginx
upstream backend {
    server 192.168.1.10:3000;
    server 192.168.1.11:3000;
    
    # Connection pooling
    keepalive 32;
    keepalive_requests 100;
    keepalive_timeout 60s;
}

server {
    listen 80;
    server_name example.com;
    
    location / {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
    }
}
```

### Buffer Optimization

```nginx
server {
    listen 80;
    server_name example.com;
    
    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        
        # Buffer settings
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
        proxy_busy_buffers_size 8k;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

## Security

### Rate Limiting

```nginx
http {
    limit_req_zone $binary_remote_addr zone=login:10m rate=1r/s;
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
}

server {
    listen 80;
    server_name example.com;
    
    # Strict rate limiting for login
    location /login {
        limit_req zone=login burst=3 nodelay;
        proxy_pass http://backend;
        proxy_set_header Host $host;
    }
    
    # Normal rate limiting for API
    location /api/ {
        limit_req zone=api burst=20 nodelay;
        proxy_pass http://backend;
        proxy_set_header Host $host;
    }
    
    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
    }
}
```

### IP Filtering

```nginx
server {
    listen 80;
    server_name admin.example.com;
    
    # Chỉ cho phép admin IPs
    allow 192.168.1.100;
    allow 10.0.0.0/8;
    deny all;
    
    location / {
        proxy_pass http://admin_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Troubleshooting

### Common Issues

#### 502 Bad Gateway

```bash
# Kiểm tra backend server
curl -I http://localhost:3000/

# Kiểm tra firewall
sudo ufw status
sudo iptables -L

# Kiểm tra SELinux
sudo setsebool -P httpd_can_network_connect 1

# Kiểm tra logs
sudo tail -f /var/log/nginx/error.log
```

#### 504 Gateway Timeout

```nginx
# Tăng timeout values
location / {
    proxy_pass http://backend;
    proxy_connect_timeout 300s;
    proxy_send_timeout 300s;
    proxy_read_timeout 300s;
}
```

#### Connection Refused

```bash
# Kiểm tra backend đang chạy
sudo netstat -tlnp | grep :3000

# Test connection
telnet localhost 3000

# Kiểm tra DNS resolution
nslookup backend-server.com
```

### Debug Commands

```bash
# Test proxy configuration
curl -v -H "Host: example.com" http://nginx-server/

# Check upstream status
curl -s http://nginx-server/nginx_status

# Monitor connections
sudo netstat -an | grep :80
sudo ss -tuln | grep :80

# Check proxy cache
sudo ls -la /var/cache/nginx/proxy/
```

## Best Practices

### 1. Security
- ✅ Always set proper headers (X-Real-IP, X-Forwarded-For)
- ✅ Use rate limiting cho sensitive endpoints
- ✅ Implement proper SSL termination
- ✅ Hide backend server information
- ✅ Use IP filtering cho admin interfaces

### 2. Performance
- ✅ Enable connection pooling
- ✅ Use appropriate buffer sizes
- ✅ Implement caching strategy
- ✅ Monitor response times
- ✅ Use health checks

### 3. Reliability
- ✅ Configure multiple backend servers
- ✅ Set proper timeouts
- ✅ Use backup servers
- ✅ Implement graceful error handling
- ✅ Monitor backend health

### 4. Monitoring
- ✅ Log proxy metrics
- ✅ Monitor upstream response times
- ✅ Set up alerts cho failures
- ✅ Track cache hit rates
- ✅ Monitor connection counts

## Thực Hành

1. **Setup basic reverse proxy** cho Node.js app
2. **Configure load balancing** với multiple backends
3. **Implement SSL termination** với Let's Encrypt
4. **Setup proxy caching** cho static content
5. **Configure WebSocket proxy** cho real-time apps
6. **Monitor performance** và troubleshoot issues

---

**Kết thúc series**: Bạn đã hoàn thành toàn bộ hướng dẫn Nginx từ cơ bản đến nâng cao! 🎉

**Tài liệu tham khảo**:
- [Nginx Official Documentation](https://nginx.org/en/docs/)
- [Nginx Admin Guide](https://docs.nginx.com/nginx/admin-guide/)
- [Nginx Security Best Practices](https://nginx.org/en/docs/http/securing_http.html)