# 1. Giới Thiệu Nginx

## Nginx Là Gì?

Nginx (phát âm "engine-x") là một web server mã nguồn mở có hiệu suất cao được phát triển bởi Igor Sysoev vào năm 2002. Ban đầu, Nginx được tạo ra để giải quyết "vấn đề C10k" - khả năng xử lý hơn 10,000 kết nối đồng thời trên một server.

## Lịch Sử Phát Triển

- **2002**: Igor Sysoev bắt đầu phát triển Nginx
- **2004**: Phiên bản đầu tiên được phát hành công khai
- **2011**: Nginx Inc. được thành lập
- **2021**: Nginx chiếm 35.3% thị phần web server toàn cầu

## Tại Sao Nginx Ra Đời?

### Vấn Đề C10k
Trước khi có Nginx, các web server truyền thống như Apache sử dụng mô hình "một thread cho mỗi kết nối". Điều này có nghĩa là:

- Mỗi người dùng truy cập = 1 thread riêng biệt
- 10,000 người dùng = 10,000 threads
- Máy chủ sẽ bị quá tải và crash

### Giải Pháp Của Nginx
Nginx sử dụng kiến trúc **event-driven** (hướng sự kiện):

- 1 worker process có thể xử lý hàng nghìn kết nối
- Không tạo thread mới cho mỗi kết nối
- Sử dụng ít RAM và CPU hơn nhiều

## Nginx vs Apache: So Sánh Đơn Giản

| Tiêu Chí | Apache | Nginx |
|----------|--------|-------|
| **Kiến trúc** | Multi-threaded | Event-driven |
| **RAM sử dụng** | Cao | Thấp |
| **Xử lý file tĩnh** | Chậm | Rất nhanh |
| **Cấu hình** | Phức tạp | Đơn giản |
| **Modules** | Nhiều | Ít hơn nhưng hiệu quả |

## Nginx Có Thể Làm Gì?

### 1. Web Server (Máy Chủ Web)
```
Người dùng → Nginx → File HTML/CSS/JS
```
Phục vụ các file tĩnh như HTML, CSS, JavaScript, hình ảnh.

### 2. Reverse Proxy (Proxy Ngược)
```
Người dùng → Nginx → Application Server (Node.js, PHP, Python)
```
Nhận request từ người dùng và chuyển tiếp đến server ứng dụng.

### 3. Load Balancer (Cân Bằng Tải)
```
Người dùng → Nginx → Server 1
                  → Server 2  
                  → Server 3
```
Phân phối traffic đến nhiều server để tránh quá tải.

### 4. API Gateway
```
Mobile App → Nginx → Microservice A
Web App   →        → Microservice B
                   → Microservice C
```
Quản lý và định tuyến các API requests.

## Ưu Điểm Của Nginx

### 1. Hiệu Suất Cao
- Xử lý 10,000+ kết nối đồng thời
- Phục vụ file tĩnh cực nhanh
- Sử dụng ít tài nguyên hệ thống

### 2. Ổn Định
- Ít crash hơn so với các web server khác
- Có thể chạy liên tục trong nhiều tháng

### 3. Cấu Hình Đơn Giản
- File config dễ đọc và hiểu
- Cú pháp rõ ràng, logic

### 4. Tính Năng Phong Phú
- SSL/TLS termination
- HTTP/2 support
- Gzip compression
- Caching
- Rate limiting

## Nhược Điểm Của Nginx

### 1. Ít Modules
- Không có nhiều modules như Apache
- Một số tính năng cần compile thêm

### 2. Không Xử Lý Dynamic Content
- Không thể chạy PHP, Python trực tiếp
- Cần kết hợp với FastCGI, uWSGI

### 3. Learning Curve
- Cần thời gian để hiểu kiến trúc event-driven
- Debugging khó hơn so với Apache

## Kiến Trúc Nginx

### Master Process
- Đọc và kiểm tra file cấu hình
- Quản lý worker processes
- Xử lý signals (start, stop, reload)

### Worker Processes
- Xử lý requests thực tế
- Mỗi worker có thể handle hàng nghìn connections
- Số lượng worker thường = số CPU cores

```
Master Process
├── Worker Process 1 (handles 1000+ connections)
├── Worker Process 2 (handles 1000+ connections)
├── Worker Process 3 (handles 1000+ connections)
└── Worker Process 4 (handles 1000+ connections)
```

## Các Công Ty Sử Dụng Nginx

- **Netflix**: Streaming video
- **Airbnb**: Platform booking
- **Pinterest**: Social media
- **GitHub**: Code repository
- **WordPress.com**: Blogging platform

## Khi Nào Nên Sử Dụng Nginx?

### ✅ Nên Dùng Khi:
- Website có traffic cao
- Cần phục vụ nhiều file tĩnh
- Muốn tiết kiệm tài nguyên server
- Cần reverse proxy hoặc load balancer
- Ưu tiên hiệu suất

### ❌ Không Nên Dùng Khi:
- Website nhỏ, ít traffic
- Cần nhiều modules đặc biệt
- Team chưa có kinh nghiệm với Nginx
- Ứng dụng cần xử lý phức tạp trong web server

## Tóm Tắt

Nginx là một web server hiện đại, hiệu suất cao được thiết kế để xử lý lượng lớn kết nối đồng thời. Với kiến trúc event-driven độc đáo, Nginx có thể:

- Phục vụ nội dung tĩnh cực nhanh
- Làm reverse proxy cho ứng dụng
- Cân bằng tải giữa nhiều server
- Tiết kiệm tài nguyên hệ thống

---

**Tiếp theo**: [Cài Đặt Nginx](./02-cai-dat-nginx.md)