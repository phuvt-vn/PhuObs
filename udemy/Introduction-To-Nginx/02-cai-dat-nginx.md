# 2. Cài Đặt Nginx

Trong chương này, chúng ta sẽ học cách cài đặt Nginx trên các hệ điều hành phổ biến và thiết lập cấu hình cơ bản.

## Cài Đặt Trên Ubuntu/Debian

### Phương Pháp 1: Từ Repository Chính Thức

```bash
# Cập nhật package list
sudo apt update

# Cài đặt Nginx
sudo apt install nginx

# Kiểm tra phiên bản
nginx -v
```

### Phương Pháp 2: Từ Nginx Repository (Khuyến nghị)

```bash
# Cài đặt prerequisites
sudo apt install curl gnupg2 ca-certificates lsb-release

# Thêm Nginx signing key
curl -fsSL https://nginx.org/keys/nginx_signing.key | sudo apt-key add -

# Thêm repository
echo "deb http://nginx.org/packages/ubuntu `lsb_release -cs` nginx" \
    | sudo tee /etc/apt/sources.list.d/nginx.list

# Cập nhật và cài đặt
sudo apt update
sudo apt install nginx
```

## Cài Đặt Trên CentOS/RHEL

### Sử Dụng YUM/DNF

```bash
# CentOS 7
sudo yum install epel-release
sudo yum install nginx

# CentOS 8/RHEL 8
sudo dnf install nginx

# Khởi động và enable service
sudo systemctl start nginx
sudo systemctl enable nginx
```

### Từ Nginx Repository

```bash
# Tạo file repo
sudo vim /etc/yum.repos.d/nginx.repo
```

Thêm nội dung:
```ini
[nginx-stable]
name=nginx stable repo
baseurl=http://nginx.org/packages/centos/$releasever/$basearch/
gpgcheck=1
enabled=1
gpgkey=https://nginx.org/keys/nginx_signing.key
module_hotfixes=true
```

```bash
# Cài đặt
sudo yum install nginx
```

## Cài Đặt Trên macOS

### Sử Dụng Homebrew

```bash
# Cài đặt Homebrew (nếu chưa có)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Cài đặt Nginx
brew install nginx

# Khởi động Nginx
brew services start nginx
```

## Cài Đặt Bằng Docker

### Docker Run

```bash
# Chạy Nginx container
docker run -d \
  --name my-nginx \
  -p 80:80 \
  nginx:latest

# Kiểm tra container
docker ps
```

### Docker Compose

Tạo file `docker-compose.yml`:

```yaml
version: '3.8'
services:
  nginx:
    image: nginx:latest
    container_name: my-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./html:/usr/share/nginx/html
    restart: unless-stopped
```

```bash
# Khởi động
docker-compose up -d
```

## Cài Đặt Trên Windows

### Download và Cài Đặt

1. Truy cập http://nginx.org/en/download.html
2. Download phiên bản Windows
3. Giải nén vào thư mục (ví dụ: `C:\nginx`)
4. Mở Command Prompt as Administrator

```cmd
cd C:\nginx
nginx.exe
```

### Sử Dụng Chocolatey

```powershell
# Cài đặt Chocolatey (nếu chưa có)
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))

# Cài đặt Nginx
choco install nginx
```

## Kiểm Tra Cài Đặt

### Kiểm Tra Phiên Bản

```bash
nginx -v
# Output: nginx version: nginx/1.20.1

nginx -V
# Hiển thị thông tin chi tiết và modules
```

### Kiểm Tra Cấu Hình

```bash
# Test cấu hình
sudo nginx -t

# Output nếu OK:
# nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
# nginx: configuration file /etc/nginx/nginx.conf test is successful
```

### Kiểm Tra Service Status

```bash
# Ubuntu/Debian/CentOS
sudo systemctl status nginx

# macOS (với Homebrew)
brew services list | grep nginx
```

## Quản Lý Nginx Service

### Linux (systemd)

```bash
# Khởi động
sudo systemctl start nginx

# Dừng
sudo systemctl stop nginx

# Khởi động lại
sudo systemctl restart nginx

# Reload cấu hình (không downtime)
sudo systemctl reload nginx

# Enable auto-start
sudo systemctl enable nginx

# Disable auto-start
sudo systemctl disable nginx
```

### macOS

```bash
# Khởi động
brew services start nginx

# Dừng
brew services stop nginx

# Khởi động lại
brew services restart nginx
```

### Windows

```cmd
# Khởi động
nginx.exe

# Dừng
nginx.exe -s quit

# Reload
nginx.exe -s reload
```

## Cấu Trúc Thư Mục Sau Cài Đặt

### Ubuntu/Debian

```
/etc/nginx/
├── nginx.conf              # File cấu hình chính
├── sites-available/        # Các site có sẵn
├── sites-enabled/          # Các site đang hoạt động
├── conf.d/                 # Cấu hình bổ sung
└── snippets/               # Các đoạn cấu hình tái sử dụng

/var/log/nginx/             # Log files
├── access.log              # Access logs
└── error.log               # Error logs

/var/www/html/              # Document root mặc định
└── index.nginx-debian.html # Trang mặc định
```

### CentOS/RHEL

```
/etc/nginx/
├── nginx.conf              # File cấu hình chính
└── conf.d/                 # Cấu hình bổ sung

/var/log/nginx/             # Log files
/usr/share/nginx/html/      # Document root mặc định
```

### macOS (Homebrew)

```
/usr/local/etc/nginx/       # Cấu hình
/usr/local/var/log/nginx/   # Logs
/usr/local/var/www/         # Document root
```

## Kiểm Tra Nginx Hoạt Động

### Truy Cập Web Browser

Mở trình duyệt và truy cập:
- `http://localhost`
- `http://your-server-ip`

Bạn sẽ thấy trang "Welcome to nginx!" nếu cài đặt thành công.

### Sử Dụng curl

```bash
curl http://localhost

# Hoặc kiểm tra headers
curl -I http://localhost
```

## Cấu Hình Firewall

### Ubuntu (UFW)

```bash
# Cho phép HTTP
sudo ufw allow 'Nginx HTTP'

# Cho phép HTTPS
sudo ufw allow 'Nginx HTTPS'

# Cho phép cả HTTP và HTTPS
sudo ufw allow 'Nginx Full'

# Kiểm tra status
sudo ufw status
```

### CentOS (firewalld)

```bash
# Cho phép HTTP
sudo firewall-cmd --permanent --add-service=http

# Cho phép HTTPS
sudo firewall-cmd --permanent --add-service=https

# Reload firewall
sudo firewall-cmd --reload

# Kiểm tra
sudo firewall-cmd --list-services
```

## Gỡ Cài Đặt Nginx

### Ubuntu/Debian

```bash
# Gỡ cài đặt
sudo apt remove nginx nginx-common

# Gỡ hoàn toàn (bao gồm cấu hình)
sudo apt purge nginx nginx-common

# Xóa dependencies không cần thiết
sudo apt autoremove
```

### CentOS/RHEL

```bash
sudo yum remove nginx
# hoặc
sudo dnf remove nginx
```

### macOS

```bash
brew uninstall nginx
```

## Troubleshooting

### Lỗi Port 80 Đã Được Sử Dụng

```bash
# Kiểm tra process nào đang sử dụng port 80
sudo netstat -tlnp | grep :80
# hoặc
sudo lsof -i :80

# Dừng Apache nếu đang chạy
sudo systemctl stop apache2
```

### Lỗi Permission Denied

```bash
# Kiểm tra SELinux (CentOS/RHEL)
sudo setsebool -P httpd_can_network_connect 1

# Kiểm tra ownership
sudo chown -R nginx:nginx /var/www/html
```

### Nginx Không Khởi Động

```bash
# Kiểm tra logs
sudo journalctl -u nginx

# Kiểm tra cấu hình
sudo nginx -t

# Kiểm tra port conflicts
sudo netstat -tlnp | grep nginx
```

## Thực Hành

1. **Cài đặt Nginx** trên hệ điều hành của bạn
2. **Kiểm tra** Nginx hoạt động bằng cách truy cập `http://localhost`
3. **Thử các lệnh** quản lý service (start, stop, restart, reload)
4. **Khám phá** cấu trúc thư mục Nginx
5. **Xem logs** để hiểu cách Nginx ghi nhận hoạt động

---

**Tiếp theo**: [Cấu Trúc File Config](./03-cau-truc-config.md)