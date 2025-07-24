# Bài 16: Thiết lập Cluster trên Linux

- **Thời lượng**: 12:30
- **Bài trước**: [[15 - Cluster Setup - On Windows]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[17 - Loadbalancing using Nginx in Vertical Cluster - Windows Platform]]

---

## Nội dung

Bài học này mô tả cách thiết lập một **cụm ngang (horizontal cluster)** trên Linux, nơi mỗi node Tomcat chạy trên một máy chủ (vật lý hoặc ảo) riêng biệt. Đây là kịch bản phổ biến trong môi trường production.

### Mô hình

-   **Máy chủ 1 (IP: 192.168.1.101)**: Chạy Node 1 của Tomcat.
-   **Máy chủ 2 (IP: 192.168.1.102)**: Chạy Node 2 của Tomcat.
-   Cả hai máy chủ đều nằm trong cùng một mạng con (subnet).

### Bước 1: Cài đặt và Chuẩn bị trên mỗi Máy chủ

Thực hiện các bước sau trên **cả hai máy chủ**:

1.  **Cài đặt Java và Tomcat**:
    -   Cài đặt JDK.
    -   Tải và giải nén Tomcat vào `/opt/tomcat`.
    -   Tạo người dùng `tomcat` và gán quyền sở hữu thư mục `/opt/tomcat` cho người dùng đó.
    -   Tạo file dịch vụ systemd (`/etc/systemd/system/tomcat.service`) để quản lý Tomcat.
    -   (Tham khảo lại [[Bài 4: Yêu cầu và Cài đặt trên Linux]])

2.  **Triển khai ứng dụng**:
    -   Sao chép ứng dụng web (đã có `<distributable/>` trong `web.xml`) vào thư mục `/opt/tomcat/webapps/` trên cả hai máy chủ.

3.  **Mở cổng Firewall**:
    -   Các node trong cụm cần giao tiếp với nhau qua mạng. Bạn cần mở các cổng cần thiết trên firewall. Tomcat cluster mặc định sử dụng một cổng TCP (ví dụ: 4000) và một cổng UDP cho multicast.
    -   Ví dụ với `ufw`:
        ```bash
        # Cổng cho receiver của cluster
        sudo ufw allow 4000:4100/tcp 
        # Cổng cho multicast
        sudo ufw allow 45564/udp
        ```
    -   Kiểm tra tài liệu của Tomcat để biết các cổng mặc định chính xác cho phiên bản bạn đang dùng.

### Bước 2: Cấu hình `server.xml`

#### Cấu hình cho Node 1 (trên Máy chủ 1)

Mở file `/opt/tomcat/conf/server.xml`:

1.  **Đặt `jvmRoute`**:
    ```xml
    <Engine name="Catalina" defaultHost="localhost" jvmRoute="node1">
    ```

2.  **Kích hoạt Cluster**:
    -   Bỏ comment khối `<Cluster>`.
    -   Để các node tự tìm thấy nhau, chúng cần sử dụng cùng một địa chỉ multicast và cổng.
        ```xml
        <Cluster className="org.apache.catalina.ha.tcp.SimpleTcpCluster"
                 channelSendOptions="8">
          <Channel className="org.apache.catalina.tribes.group.GroupChannel">
            <Membership className="org.apache.catalina.tribes.membership.McastService"
                        address="228.0.0.4"
                        port="45564"
                        frequency="500"
                        dropTime="3000"/>
            ...
          </Channel>
        </Cluster>
        ```
    -   `address` và `port` trong `<Membership>` phải giống hệt nhau trên tất cả các node.

#### Cấu hình cho Node 2 (trên Máy chủ 2)

Mở file `/opt/tomcat/conf/server.xml`:

1.  **Đặt `jvmRoute`**:
    ```xml
    <Engine name="Catalina" defaultHost="localhost" jvmRoute="node2">
    ```

2.  **Kích hoạt Cluster**:
    -   Sử dụng cấu hình `<Cluster>` y hệt như trên Node 1.

### Bước 3: Khởi động và Kiểm tra

1.  Trên **Máy chủ 1**, khởi động Tomcat:
    ```bash
    sudo systemctl start tomcat
    ```
2.  Trên **Máy chủ 2**, khởi động Tomcat:
    ```bash
    sudo systemctl start tomcat
    ```
3.  Kiểm tra file log `catalina.out` trên cả hai máy chủ (`/opt/tomcat/logs/catalina.out`).
    -   Bạn sẽ thấy các thông báo như `INFO ... SimpleTcpCluster.memberAdded` và `INFO ... Replication member was added` trên cả hai node, xác nhận rằng chúng đã hình thành một cụm.

Giờ đây, bạn đã có một cụm Tomcat hoạt động trên hai máy chủ riêng biệt, sẵn sàng để được đặt sau một bộ cân bằng tải.
