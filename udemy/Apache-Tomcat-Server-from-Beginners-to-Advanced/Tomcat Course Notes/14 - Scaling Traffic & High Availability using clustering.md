# Bài 14: Mở rộng lưu lượng và Tính sẵn sàng cao với Clustering

- **Thời lượng**: 03:39
- **Bài trước**: [[13 - Virtual Host Creation - Linux Platform]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[15 - Cluster Setup - On Windows]]

---

## Nội dung

Bài học này giới thiệu các khái niệm về **Clustering (tạo cụm)**, **Scalability (khả năng mở rộng)**, và **High Availability (tính sẵn sàng cao)** trong ngữ cảnh của Apache Tomcat.

### Vấn đề với một máy chủ duy nhất (Single Server)

-   **Single Point of Failure (Điểm lỗi duy nhất)**: Nếu máy chủ Tomcat duy nhất của bạn gặp sự cố (phần cứng, phần mềm, mạng), toàn bộ ứng dụng của bạn sẽ ngừng hoạt động.
-   **Giới hạn về hiệu năng**: Một máy chủ chỉ có thể xử lý một lượng truy cập nhất định. Khi lưu lượng tăng cao, thời gian phản hồi sẽ chậm đi và cuối cùng máy chủ có thể bị quá tải.

### Giải pháp: Clustering

**Clustering** là quá trình nhóm nhiều máy chủ Tomcat (gọi là các **nodes** hoặc **members**) lại với nhau để chúng hoạt động như một hệ thống thống nhất.

### Lợi ích của Clustering

1.  **High Availability (Tính sẵn sàng cao)**:
    -   Nếu một node trong cụm (cluster) bị lỗi, các node khác vẫn tiếp tục hoạt động và xử lý các yêu cầu của người dùng.
    -   Điều này giúp giảm thiểu thời gian chết (downtime) và đảm bảo ứng dụng luôn sẵn sàng phục vụ.
    -   Thường được kết hợp với **Session Replication** để duy trì trạng thái phiên của người dùng ngay cả khi họ được chuyển sang một node khác.

2.  **Scalability (Khả năng mở rộng)**:
    -   Cho phép hệ thống xử lý nhiều lưu lượng truy cập hơn bằng cách phân phối các yêu cầu trên nhiều máy chủ.
    -   Khi cần thêm năng lực xử lý, bạn chỉ cần thêm một node mới vào cụm.
    -   Thường đi kèm với một **Load Balancer (Bộ cân bằng tải)** ở phía trước.

### Các thành phần chính trong một kiến trúc Cluster

-   **Tomcat Nodes**: Hai hoặc nhiều máy chủ Tomcat, mỗi máy chạy một bản sao của cùng một ứng dụng.
-   **Load Balancer (Bộ cân bằng tải)**:
    -   Là một máy chủ (hoặc thiết bị) đặt ở phía trước các Tomcat node.
    -   Nó nhận tất cả các yêu cầu từ người dùng và phân phối chúng một cách thông minh đến các node trong cụm theo một thuật toán nhất định (ví dụ: Round Robin, Least Connections).
    -   Các bộ cân bằng tải phổ biến bao gồm Nginx, Apache HTTP Server (với `mod_jk` hoặc `mod_proxy`), HAProxy.
-   **Session Replication (Đồng bộ hóa phiên)**:
    -   Cơ chế để các node trong cụm chia sẻ thông tin về session của người dùng.
    -   Nếu một người dùng đang có session trên Node A và Node A bị lỗi, bộ cân bằng tải sẽ chuyển yêu cầu tiếp theo của người dùng đó đến Node B. Nhờ session replication, Node B sẽ có thông tin session của người dùng đó và có thể tiếp tục phục vụ mà không yêu cầu người dùng đăng nhập lại.

Trong các bài học tiếp theo, chúng ta sẽ đi sâu vào cách thiết lập một cụm Tomcat, cấu hình cân bằng tải và đồng bộ hóa session.
