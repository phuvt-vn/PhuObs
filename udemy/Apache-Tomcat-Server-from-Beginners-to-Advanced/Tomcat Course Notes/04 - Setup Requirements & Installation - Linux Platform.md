# Bài 4: Yêu cầu và Cài đặt trên Linux

- **Thời lượng**: 07:18
- **Bài trước**: [[03 - Setup Requirements & Installation - Windows Platform]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[05 - Application Deployment]]

---

## Nội dung

Bài học này cung cấp hướng dẫn chi tiết về việc cài đặt Apache Tomcat trên các hệ điều hành dựa trên Linux (như Ubuntu, CentOS).

### Yêu cầu hệ thống:

1.  **Java Development Kit (JDK)**:
    -   Tương tự như Windows, bạn cần cài đặt JDK.
    -   Trên Ubuntu/Debian, bạn có thể cài đặt bằng lệnh:
        ```bash
        sudo apt update
        sudo apt install default-jdk
        ```
    -   Trên CentOS/RHEL, sử dụng lệnh:
        ```bash
        sudo yum install java-11-openjdk-devel
        ```
    -   Kiểm tra phiên bản Java: `java -version`.
    -   Biến môi trường `JAVA_HOME` thường được hệ thống tự động cấu hình. Bạn có thể kiểm tra bằng `echo $JAVA_HOME`.

### Các bước cài đặt Tomcat:

1.  **Tạo người dùng Tomcat (Khuyến nghị vì lý do bảo mật)**:
    -   Tạo một nhóm và người dùng mới tên là `tomcat` để chạy dịch vụ, tránh chạy bằng quyền `root`.
        ```bash
        sudo groupadd tomcat
        sudo useradd -s /bin/false -g tomcat -d /opt/tomcat tomcat
        ```

2.  **Tải xuống Tomcat**:
    -   Truy cập trang tải xuống của Tomcat. Sử dụng `curl` hoặc `wget` để tải file `tar.gz`.
        ```bash
        # Thay đổi URL cho phiên bản bạn muốn
        curl -O https://dlcdn.apache.org/tomcat/tomcat-9/v9.0.80/bin/apache-tomcat-9.0.80.tar.gz
        ```

3.  **Giải nén và di chuyển**:
    -   Tạo thư mục `/opt/tomcat` và giải nén file vào đó.
        ```bash
        sudo mkdir /opt/tomcat
        sudo tar xzvf apache-tomcat-*.tar.gz -C /opt/tomcat --strip-components=1
        ```

4.  **Cập nhật quyền sở hữu**:
    -   Thay đổi quyền sở hữu của thư mục `/opt/tomcat` cho người dùng và nhóm `tomcat`.
        ```bash
        sudo chown -R tomcat:tomcat /opt/tomcat
        sudo sh -c 'chmod +x /opt/tomcat/bin/*.sh'
        ```

5.  **Tạo file dịch vụ Systemd (Để quản lý Tomcat như một service)**:
    -   Tạo một file mới tại `/etc/systemd/system/tomcat.service`:
        ```bash
        sudo nano /etc/systemd/system/tomcat.service
        ```
    -   Thêm nội dung sau vào file (nhớ kiểm tra lại đường dẫn `JAVA_HOME` của bạn):
        ```ini
        [Unit]
        Description=Apache Tomcat Web Application Container
        After=network.target

        [Service]
        Type=forking

        User=tomcat
        Group=tomcat

        Environment="JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64"
        Environment="CATALINA_PID=/opt/tomcat/temp/tomcat.pid"
        Environment="CATALINA_HOME=/opt/tomcat"
        Environment="CATALINA_BASE=/opt/tomcat"
        Environment="CATALINA_OPTS=-Xms512M -Xmx1024M -server -XX:+UseParallelGC"
        Environment="JAVA_OPTS=-Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom"

        ExecStart=/opt/tomcat/bin/startup.sh
        ExecStop=/opt/tomcat/bin/shutdown.sh

        [Install]
        WantedBy=multi-user.target
        ```

6.  **Quản lý dịch vụ Tomcat**:
    -   Tải lại systemd để nhận diện file service mới: `sudo systemctl daemon-reload`
    -   Khởi động Tomcat: `sudo systemctl start tomcat`
    -   Kiểm tra trạng thái: `sudo systemctl status tomcat`
    -   Kích hoạt để Tomcat tự khởi động cùng hệ thống: `sudo systemctl enable tomcat`

7.  **Kiểm tra**:
    -   Mở trình duyệt và truy cập `http://<your_server_ip>:8080`.
    -   Nếu server có firewall (tường lửa), bạn cần mở port 8080: `sudo ufw allow 8080`.
