# Bài 15: Thiết lập Cluster trên Windows

- **Thời lượng**: 13:24
- **Bài trước**: [[14 - Scaling Traffic & High Availability using clustering]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[16 - Cluster Setup - Linux Platform]]

---

## Nội dung

Bài học này hướng dẫn cách thiết lập một cụm (cluster) Tomcat đơn giản trên một máy Windows duy nhất. Đây được gọi là **cụm dọc (vertical cluster)**, nơi nhiều phiên bản Tomcat chạy trên cùng một máy. Mục đích chính là để thực hành và hiểu các khái niệm về clustering.

### Bước 1: Chuẩn bị các phiên bản Tomcat

1.  **Tạo các thư mục riêng biệt**:
    -   Tạo hai (hoặc nhiều hơn) thư mục riêng biệt cho mỗi node Tomcat. Ví dụ:
        -   `C:\tomcat-cluster\node1`
        -   `C:\tomcat-cluster\node2`
    -   Sao chép toàn bộ nội dung của một bản cài đặt Tomcat chuẩn vào cả hai thư mục này.

2.  **Triển khai ứng dụng**:
    -   Để kiểm tra session replication, chúng ta cần một ứng dụng web có khả năng lưu trữ dữ liệu trong session.
    -   Sao chép file `.war` của ứng dụng vào thư mục `webapps` của **cả hai node**.
    -   **Quan trọng**: Ứng dụng phải được đánh dấu là "distributable" trong file `WEB-INF/web.xml` của nó để cho phép session replication.
        ```xml
        <web-app>
            <distributable/>
            ...
        </web-app>
        ```

### Bước 2: Cấu hình `server.xml` cho từng Node

Bạn cần chỉnh sửa file `server.xml` trong thư mục `conf` của **mỗi node**.

#### Cấu hình cho Node 1 (`C:\tomcat-cluster\node1\conf\server.xml`)

1.  **Thay đổi các cổng (Ports)**: Để tránh xung đột cổng trên cùng một máy, mỗi node phải sử dụng các cổng khác nhau.
    -   **Shutdown Port**: Thay đổi `port="8005"` thành `port="8006"`.
    -   **HTTP Port**: Giữ nguyên `port="8080"`.
    -   **AJP Port**: Thay đổi `port="8009"` thành `port="8010"`.

2.  **Đặt `jvmRoute`**:
    -   Trong thẻ `<Engine>`, thêm thuộc tính `jvmRoute` để định danh cho node này. Load balancer sẽ sử dụng thông tin này.
        ```xml
        <Engine name="Catalina" defaultHost="localhost" jvmRoute="node1">
        ```

3.  **Kích hoạt Cluster**:
    -   Bên trong thẻ `<Engine>`, bỏ comment hoặc thêm khối `<Cluster>`. Cấu hình mặc định của `SimpleTcpCluster` thường là đủ để bắt đầu.
        ```xml
        <Cluster className="org.apache.catalina.ha.tcp.SimpleTcpCluster"/>
        ```

#### Cấu hình cho Node 2 (`C:\tomcat-cluster\node2\conf\server.xml`)

1.  **Thay đổi các cổng**:
    -   **Shutdown Port**: Thay đổi `port="8005"` thành `port="8007"`.
    -   **HTTP Port**: Thay đổi `port="8080"` thành `port="8081"`.
    -   **AJP Port**: Thay đổi `port="8009"` thành `port="8011"`.

2.  **Đặt `jvmRoute`**:
    -   Đặt một tên định danh khác cho node này.
        ```xml
        <Engine name="Catalina" defaultHost="localhost" jvmRoute="node2">
        ```

3.  **Kích hoạt Cluster**:
    -   Tương tự Node 1, kích hoạt khối `<Cluster>`.
        ```xml
        <Cluster className="org.apache.catalina.ha.tcp.SimpleTcpCluster"/>
        ```
    -   Tomcat sử dụng multicast để các node tự động phát hiện nhau trong mạng. Nếu mạng của bạn không hỗ trợ multicast, bạn sẽ cần cấu hình static membership.

### Bước 3: Khởi động và Kiểm tra

1.  Mở hai cửa sổ Command Prompt riêng biệt.
2.  **Trong cửa sổ thứ nhất**, khởi động Node 1:
    ```bash
    cd C:\tomcat-cluster\node1\bin
    set "CATALINA_BASE=%cd%\.."
    startup.bat
    ```
3.  **Trong cửa sổ thứ hai**, khởi động Node 2:
    ```bash
    cd C:\tomcat-cluster\node2\bin
    set "CATALINA_BASE=%cd%\.."
    startup.bat
    ```
4.  Quan sát log ở cả hai cửa sổ. Bạn sẽ thấy các thông báo liên quan đến việc "cluster member added", cho thấy hai node đã tìm thấy và kết nối với nhau.

Bây giờ bạn đã có một cụm Tomcat gồm hai node đang chạy. Bước tiếp theo là thiết lập một bộ cân bằng tải (load balancer) để phân phối lưu lượng đến chúng.
