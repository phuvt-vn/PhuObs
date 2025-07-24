# Bài 10: Kích hoạt SSL trên Nền tảng Linux

- **Thời lượng**: 07:12
- **Bài trước**: [[09 - Enabling SSL - Part2]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[11 - Virtualhost Overview]]

---

## Nội dung

Bài học này mô tả các bước để kích hoạt SSL/TLS trên Tomcat chạy trên hệ điều hành Linux. Quá trình này về cơ bản tương tự như trên Windows, nhưng có một vài điểm khác biệt liên quan đến đường dẫn và quyền sở hữu file.

### Các bước thực hiện

1.  **Tạo Keystore và Chứng chỉ**:
    -   Sử dụng `keytool` giống hệt như trên Windows. Bạn cần đảm bảo đã cài đặt JDK.
    -   Chạy lệnh sau từ terminal:
        ```bash
        keytool -genkey -alias tomcat -keyalg RSA -keystore /path/to/your/keystore.jks
        ```
    -   **Quan trọng**: Nơi lưu trữ keystore trên Linux nên là một thư mục an toàn. Một lựa chọn tốt là tạo một thư mục riêng trong thư mục cấu hình của Tomcat, ví dụ: `/opt/tomcat/conf/keys`.
        ```bash
        # Tạo thư mục
        sudo mkdir /opt/tomcat/conf/keys

        # Tạo keystore trong thư mục đó
        sudo keytool -genkey -alias tomcat -keyalg RSA -keystore /opt/tomcat/conf/keys/keystore.jks
        ```
    -   Khi được hỏi "What is your first and last name?", hãy nhập tên miền của server hoặc địa chỉ IP.

2.  **Cập nhật Quyền sở hữu**:
    -   Vì Tomcat đang chạy dưới người dùng `tomcat`, nó cần có quyền đọc file keystore.
    -   Thay đổi quyền sở hữu của file keystore cho người dùng `tomcat`:
        ```bash
        sudo chown tomcat:tomcat /opt/tomcat/conf/keys/keystore.jks
        ```

3.  **Cấu hình `server.xml`**:
    -   Mở file `/opt/tomcat/conf/server.xml`.
    -   Tìm và kích hoạt (bỏ comment) SSL/TLS Connector, tương tự như đã làm trên Windows.
    -   Cập nhật thuộc tính `certificateKeystoreFile` để trỏ đến đường dẫn của keystore trên Linux.

        ```xml
        <Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol"
                   maxThreads="150" SSLEnabled="true" scheme="https" secure="true">
            <SSLHostConfig>
                <Certificate certificateKeystoreFile="conf/keys/keystore.jks"
                             certificateKeystorePassword="your_keystore_password"
                             type="RSA" />
            </SSLHostConfig>
        </Connector>
        ```
        *Lưu ý: Ở đây, đường dẫn `conf/keys/keystore.jks` là đường dẫn tương đối so với `$CATALINA_HOME`. Bạn cũng có thể sử dụng đường dẫn tuyệt đối `/opt/tomcat/conf/keys/keystore.jks`.*

4.  **Mở cổng Firewall**:
    -   Nếu bạn đang sử dụng tường lửa (firewall) như `ufw` hoặc `firewalld`, bạn cần cho phép lưu lượng truy cập qua cổng HTTPS (mặc định là 8443).
    -   Với `ufw` (trên Ubuntu/Debian):
        ```bash
        sudo ufw allow 8443/tcp
        ```
    -   Với `firewalld` (trên CentOS/RHEL):
        ```bash
        sudo firewall-cmd --permanent --add-port=8443/tcp
        sudo firewall-cmd --reload
        ```

5.  **Khởi động lại và Kiểm tra**:
    -   Khởi động lại dịch vụ Tomcat:
        ```bash
        sudo systemctl restart tomcat
        ```
    -   Mở trình duyệt từ một máy tính khác và truy cập `https://<your_server_ip>:8443`.
    -   Chấp nhận cảnh báo bảo mật (do chứng chỉ tự ký) và xác nhận trang Tomcat hiển thị thành công.
