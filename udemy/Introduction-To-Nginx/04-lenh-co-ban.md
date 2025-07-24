# 4. Các Lệnh Cơ Bản Nginx

Trong chương này, chúng ta sẽ học các lệnh cần thiết để quản lý Nginx hàng ngày.

## Lệnh Nginx Cơ Bản

### Kiểm Tra Phiên Bản

```bash
# Hiển thị phiên bản ngắn gọn
nginx -v
# Output: nginx version: nginx/1.20.1

# Hiển thị phiên bản và thông tin build
nginx -V
# Output: nginx version: nginx/1.20.1
#         built by gcc 9.4.0 (Ubuntu 9.4.0-1ubuntu1~20.04.1)
#         configure arguments: --with-http_ssl_module...
```

### Kiểm Tra Cấu Hình

```bash
# Test syntax của file config
nginx -t

# Test và hiển thị file config được sử dụng
nginx -T

# Test config với file cụ thể
nginx -t -c /path/to/nginx.conf
```

**Output khi config đúng:**
```
nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
nginx: configuration file /etc/nginx/nginx.conf test is successful
```

**Output khi config sai:**
```
nginx: [emerg] unexpected "}" in /etc/nginx/nginx.conf:25
nginx: configuration file /etc/nginx/nginx.conf test failed
```

### Khởi Động và Dừng Nginx

```bash
# Khởi động Nginx
nginx
# hoặc
sudo systemctl start nginx

# Dừng Nginx (graceful shutdown)
nginx -s quit
# hoặc
sudo systemctl stop nginx

# Dừng Nginx ngay lập tức (fast shutdown)
nginx -s stop

# Khởi động lại
sudo systemctl restart nginx
```

### Reload Cấu Hình

```bash
# Reload config không downtime
nginx -s reload
# hoặc
sudo systemctl reload nginx

# Reopen log files
nginx -s reopen
```

## Quản Lý Service (systemctl)

### Các Lệnh Systemctl Cơ Bản

```bash
# Kiểm tra trạng thái
sudo systemctl status nginx

# Khởi động
sudo systemctl start nginx

# Dừng
sudo systemctl stop nginx

# Khởi động lại
sudo systemctl restart nginx

# Reload cấu hình
sudo systemctl reload nginx

# Enable auto-start khi boot
sudo systemctl enable nginx

# Disable auto-start
sudo systemctl disable nginx

# Kiểm tra xem service có enabled không
sudo systemctl is-enabled nginx

# Kiểm tra xem service có đang chạy không
sudo systemctl is-active nginx
```

### Xem Logs với journalctl

```bash
# Xem logs của Nginx
sudo journalctl -u nginx

# Xem logs realtime
sudo journalctl -u nginx -f

# Xem logs từ hôm nay
sudo journalctl -u nginx --since today

# Xem logs trong 1 giờ qua
sudo journalctl -u nginx --since "1 hour ago"

# Xem logs với priority error trở lên
sudo journalctl -u nginx -p err
```

## Quản Lý Process

### Xem Nginx Processes

```bash
# Xem tất cả processes của Nginx
ps aux | grep nginx

# Xem process tree
pstree -p nginx

# Xem với htop
htop -p $(pgrep nginx | tr '\n' ',')
```

**Output mẫu:**
```
root      1234  0.0  0.1  12345  1234 ?  Ss   10:00   0:00 nginx: master process
nginx     1235  0.0  0.2  12345  2345 ?  S    10:00   0:00 nginx: worker process
nginx     1236  0.0  0.2  12345  2345 ?  S    10:00   0:00 nginx: worker process
```

### Gửi Signals

```bash
# Lấy PID của master process
sudo cat /var/run/nginx.pid
# hoặc
pgrep -f "nginx: master"

# Gửi signal QUIT (graceful shutdown)
sudo kill -QUIT $(cat /var/run/nginx.pid)

# Gửi signal TERM (fast shutdown)
sudo kill -TERM $(cat /var/run/nginx.pid)

# Gửi signal HUP (reload config)
sudo kill -HUP $(cat /var/run/nginx.pid)

# Gửi signal USR1 (reopen log files)
sudo kill -USR1 $(cat /var/run/nginx.pid)

# Gửi signal USR2 (upgrade binary)
sudo kill -USR2 $(cat /var/run/nginx.pid)
```

### Các Signal Quan Trọng

| Signal | Ý nghĩa | Tương đương |
|--------|---------|-------------|
| TERM, INT | Fast shutdown | `nginx -s stop` |
| QUIT | Graceful shutdown | `nginx -s quit` |
| HUP | Reload config | `nginx -s reload` |
| USR1 | Reopen log files | `nginx -s reopen` |
| USR2 | Upgrade binary | - |
| WINCH | Graceful shutdown workers | - |

## Kiểm Tra Kết Nối

### Xem Ports Đang Listen

```bash
# Xem tất cả ports đang listen
sudo netstat -tlnp | grep nginx

# Sử dụng ss (modern alternative)
sudo ss -tlnp | grep nginx

# Xem port 80 và 443
sudo lsof -i :80
sudo lsof -i :443
```

**Output mẫu:**
```
tcp  0  0  0.0.0.0:80    0.0.0.0:*  LISTEN  1234/nginx: master
tcp  0  0  0.0.0.0:443   0.0.0.0:*  LISTEN  1234/nginx: master
```

### Test Kết Nối

```bash
# Test với curl
curl -I http://localhost
curl -I https://localhost

# Test với telnet
telnet localhost 80

# Test với wget
wget --spider http://localhost

# Test SSL certificate
openssl s_client -connect localhost:443 -servername localhost
```

## Xem và Phân Tích Logs

### Access Logs

```bash
# Xem access log
sudo tail -f /var/log/nginx/access.log

# Xem 100 dòng cuối
sudo tail -n 100 /var/log/nginx/access.log

# Xem logs theo thời gian thực
sudo tail -f /var/log/nginx/access.log | grep "GET"

# Đếm số requests
sudo wc -l /var/log/nginx/access.log

# Top 10 IP addresses
sudo awk '{print $1}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -10

# Top 10 requested pages
sudo awk '{print $7}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -10

# Requests by status code
sudo awk '{print $9}' /var/log/nginx/access.log | sort | uniq -c | sort -nr
```

### Error Logs

```bash
# Xem error log
sudo tail -f /var/log/nginx/error.log

# Xem errors trong 1 giờ qua
sudo grep "$(date -d '1 hour ago' '+%Y/%m/%d %H')" /var/log/nginx/error.log

# Xem critical errors
sudo grep "\[crit\]" /var/log/nginx/error.log

# Xem errors theo level
sudo grep "\[error\]" /var/log/nginx/error.log
sudo grep "\[warn\]" /var/log/nginx/error.log
```

## Backup và Restore

### Backup Cấu Hình

```bash
# Backup toàn bộ thư mục config
sudo tar -czf nginx-config-$(date +%Y%m%d).tar.gz /etc/nginx/

# Backup chỉ file chính
sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup

# Backup với timestamp
sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.$(date +%Y%m%d_%H%M%S)
```

### Restore Cấu Hình

```bash
# Restore từ backup
sudo cp /etc/nginx/nginx.conf.backup /etc/nginx/nginx.conf

# Test config sau khi restore
sudo nginx -t

# Reload nếu config OK
sudo nginx -s reload
```

## Monitoring và Debugging

### Kiểm Tra Resource Usage

```bash
# CPU và Memory usage
top -p $(pgrep nginx | tr '\n' ',')

# Memory usage chi tiết
sudo pmap $(pgrep nginx)

# Open files
sudo lsof -p $(pgrep nginx)

# Network connections
sudo netstat -anp | grep nginx
```

### Debug Mode

```bash
# Chạy Nginx ở foreground để debug
sudo nginx -g "daemon off;"

# Tăng log level để debug
# Thêm vào nginx.conf:
# error_log /var/log/nginx/error.log debug;
```

## Automation Scripts

### Script Kiểm Tra Health

```bash
#!/bin/bash
# nginx-health-check.sh

echo "=== Nginx Health Check ==="

# Check if nginx is running
if pgrep nginx > /dev/null; then
    echo "✓ Nginx is running"
else
    echo "✗ Nginx is not running"
    exit 1
fi

# Check config syntax
if nginx -t 2>/dev/null; then
    echo "✓ Config syntax is OK"
else
    echo "✗ Config syntax error"
    nginx -t
    exit 1
fi

# Check if ports are listening
if netstat -tlnp | grep :80 > /dev/null; then
    echo "✓ Port 80 is listening"
else
    echo "✗ Port 80 is not listening"
fi

# Test HTTP response
if curl -s -o /dev/null -w "%{http_code}" http://localhost | grep -q "200\|301\|302"; then
    echo "✓ HTTP response is OK"
else
    echo "✗ HTTP response error"
fi

echo "=== Health check completed ==="
```

### Script Reload An Toàn

```bash
#!/bin/bash
# safe-reload.sh

echo "Testing Nginx configuration..."
if nginx -t; then
    echo "Configuration test passed. Reloading..."
    nginx -s reload
    echo "Nginx reloaded successfully!"
else
    echo "Configuration test failed. Not reloading."
    exit 1
fi
```

## Troubleshooting Commands

### Khi Nginx Không Khởi Động

```bash
# Kiểm tra logs
sudo journalctl -u nginx --no-pager

# Kiểm tra config
sudo nginx -t

# Kiểm tra port conflicts
sudo netstat -tlnp | grep :80
sudo netstat -tlnp | grep :443

# Kiểm tra permissions
sudo ls -la /var/log/nginx/
sudo ls -la /var/run/nginx.pid

# Kiểm tra SELinux (CentOS/RHEL)
sudo setsebool -P httpd_can_network_connect 1
sudo setsebool -P httpd_can_network_relay 1
```

### Khi Website Không Load

```bash
# Test local connection
curl -v http://localhost

# Check DNS
nslookup your-domain.com

# Check firewall
sudo ufw status
sudo iptables -L

# Check document root permissions
sudo ls -la /var/www/html/
```

## Thực Hành

1. **Thử tất cả lệnh cơ bản** trên server của bạn
2. **Tạo script health check** cho Nginx
3. **Thực hành backup và restore** cấu hình
4. **Monitor logs** trong thời gian thực
5. **Test reload** cấu hình an toàn

---

**Tiếp theo**: [Phục Vụ Nội Dung Tĩnh](./05-noi-dung-tinh.md)