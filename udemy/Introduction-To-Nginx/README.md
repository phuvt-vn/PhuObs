# Hướng Dẫn Nginx Từ Cơ Bản Đến Nâng Cao

Chào mừng bạn đến với series hướng dẫn Nginx toàn diện! Series này được thiết kế để giúp bạn hiểu rõ về Nginx từ những khái niệm cơ bản nhất đến các kỹ thuật nâng cao.

## Nginx Là Gì?

Nginx (phát âm là "engine-x") là một web server mã nguồn mở có hiệu suất cao, được phát triển bởi Igor Sysoev vào năm 2002 để giải quyết vấn đề C10k - khả năng xử lý hơn 10,000 kết nối đồng thời.

## Tại Sao Nên Học Nginx?

- **Hiệu suất cao**: Có thể xử lý hàng nghìn kết nối đồng thời
- **Tiết kiệm tài nguyên**: Sử dụng ít RAM và CPU hơn so với Apache
- **Đa năng**: Có thể làm web server, reverse proxy, load balancer
- **Phổ biến**: Được sử dụng bởi 35.3% các website trên thế giới

## 📚 Cấu Trúc Series

### 🔰 Phần 1: Kiến Thức Cơ Bản
1. [Giới Thiệu Nginx](./01-gioi-thieu-nginx.md) ✅ **(160 dòng)**
2. [Cài Đặt Nginx](./02-cai-dat-nginx.md) ✅ **(407 dòng)**
3. [Cấu Trúc File Config](./03-cau-truc-config.md) ✅ **(421 dòng)**
4. [Các Lệnh Cơ Bản](./04-lenh-co-ban.md) ✅ **(439 dòng)**

### 🌐 Phần 2: Web Server
5. [Phục Vụ Nội Dung Tĩnh](./05-noi-dung-tinh.md) ✅ **(541 dòng)**
6. [Virtual Hosts](./06-virtual-hosts.md) ✅ **(662 dòng)**

### 🔄 Phần 3: Reverse Proxy & Load Balancing
7. [Reverse Proxy](./07-reverse-proxy.md) ✅ **(867 dòng)**
8. [Layer 4 và Layer 7 Proxy](./08-layer4-layer7-proxy.md) ✅ **(543 dòng)**

---

## 📊 Thống Kê Series

- **✅ Hoàn thành**: 8/8 chương cốt lõi
- **📄 Tổng số dòng**: Hơn **4,000 dòng** nội dung chi tiết
- **🔧 Ví dụ thực tế**: Hơn 100 ví dụ cấu hình
- **💡 Best practices**: Được tích hợp trong từng chương
- **🛠️ Troubleshooting**: Hướng dẫn xử lý sự cố chi tiết

## 🎯 Nội Dung Bao Gồm

### Kiến Thức Cơ Bản
- Lịch sử và kiến trúc Nginx
- Cài đặt trên nhiều hệ điều hành
- Cấu trúc file config và directives
- Các lệnh quản lý hàng ngày

### Web Server
- Phục vụ static files với tối ưu hiệu suất
- Virtual hosts và server blocks
- Location matching và routing
- Caching và compression

### Reverse Proxy & Load Balancing
- Proxy cho Node.js, PHP, và microservices
- Load balancing algorithms
- Health checks và failover
- SSL termination
- WebSocket support

## Yêu Cầu Trước Khi Học

- Kiến thức cơ bản về Linux/Unix
- Hiểu biết về HTTP/HTTPS
- Kinh nghiệm với command line
- Kiến thức cơ bản về web development

## Cách Sử Dụng Series Này

1. **Đọc tuần tự**: Bắt đầu từ file đầu tiên và đọc theo thứ tự
2. **Thực hành**: Mỗi bài đều có ví dụ thực tế, hãy thử nghiệm trên máy của bạn
3. **Tham khảo**: Sử dụng như tài liệu tham khảo khi cần

## Lưu Ý

- Tất cả ví dụ được test trên Ubuntu/CentOS
- Code examples sử dụng cú pháp Nginx mới nhất
- Mỗi file đều có phần "Thực Hành" để bạn áp dụng ngay

---

**Bắt đầu hành trình học Nginx của bạn với [Giới Thiệu Nginx](./01-gioi-thieu-nginx.md)!**