# Bài 9: Kích hoạt SSL - Cấu hình Tomcat (Phần 2)

- **Thời lượng**: 07:42
- **Bài trước**: [[08 - Enabling SSL - Generating Certificate]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[10 - Enabling SSL - Linux Platform]]

---

## Nội dung

Sau khi đã tạo keystore và chứng chỉ ở bài trước, bài học này hướng dẫn cách cấu hình file `server.xml` của Tomcat để sử dụng chúng, qua đó kích hoạt kết nối HTTPS.

### Chỉnh sửa file `server.xml`

1.  **Mở file cấu hình**:
    -   File `server.xml` nằm trong thư mục `$CATALINA_HOME/conf/`. Mở file này bằng một trình soạn thảo văn bản.

2.  **Tìm SSL Connector**:
    -   Tìm kiếm trong file một đoạn mã trông giống như sau, thường đã được comment (vô hiệu hóa):
        ```xml
        <!--
        <Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol"
                   maxThreads="150" SSLEnabled="true">
            <SSLHostConfig>
                <Certificate certificateKeystoreFile="conf/localhost-rsa.jks"
                             type="RSA" />
            </SSLHostConfig>
        </Connector>
        -->
        ```

3.  **Bỏ comment và cấu hình**:
    -   Bỏ các dấu comment `<!--` và `-->` để kích hoạt Connector này.
    -   Chỉnh sửa các thuộc tính của nó để trỏ đến keystore bạn đã tạo:

        ```xml
        <Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol"
                   maxThreads="150" SSLEnabled="true" scheme="https" secure="true">
            <SSLHostConfig>
                <Certificate certificateKeystoreFile="C:\path\to\your\keystore.jks"
                             certificateKeystorePassword="your_keystore_password"
                             type="RSA" />
            </SSLHostConfig>
        </Connector>
        ```

**Giải thích các thuộc tính quan trọng:**

-   `port="8443"`: Cổng mà Tomcat sẽ lắng nghe các kết nối HTTPS. 8443 là cổng mặc định phổ biến.
-   `SSLEnabled="true"`: Bật chức năng SSL/TLS cho Connector này.
-   `scheme="https"`: Chỉ định scheme là `https`. Điều này giúp các ứng dụng web biết rằng kết nối là an toàn.
-   `secure="true"`: Tương tự, báo hiệu cho các servlet rằng đây là một kết nối an toàn.
-   `certificateKeystoreFile`: **Đường dẫn tuyệt đối** đến file keystore (`.jks`) bạn đã tạo ở bài trước.
-   `certificateKeystorePassword`: **Mật khẩu** của file keystore mà bạn đã đặt.
-   `type="RSA"`: Loại chứng chỉ.

### Tự động chuyển hướng từ HTTP sang HTTPS (Tùy chọn)

Để buộc tất cả người dùng phải sử dụng kết nối an toàn, bạn có thể cấu hình Tomcat để tự động chuyển hướng từ cổng HTTP (8080) sang cổng HTTPS (8443).

-   Tìm đến HTTP Connector (thường ở port 8080) trong file `server.xml`.
-   Thêm thuộc tính `redirectPort="8443"`:

    ```xml
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />
    ```

-   Sau đó, bạn cần thêm một ràng buộc bảo mật trong file `web.xml` của ứng dụng (`/WEB-INF/web.xml`) để yêu cầu kết nối an toàn cho toàn bộ hoặc một phần ứng dụng.

    ```xml
    <security-constraint>
        <web-resource-collection>
            <web-resource-name>Entire Application</web-resource-name>
            <url-pattern>/*</url-pattern>
        </web-resource-collection>
        <user-data-constraint>
            <transport-guarantee>CONFIDENTIAL</transport-guarantee>
        </user-data-constraint>
    </security-constraint>
    ```
    -   `CONFIDENTIAL` yêu cầu kết nối phải được thực hiện qua SSL/TLS.

### Khởi động lại và Kiểm tra

1.  Lưu file `server.xml` sau khi chỉnh sửa.
2.  Khởi động lại Tomcat.
3.  Mở trình duyệt và truy cập `https://localhost:8443`.
4.  Bạn sẽ thấy một cảnh báo bảo mật vì đang sử dụng chứng chỉ tự ký. Đây là điều bình thường. Chọn "Proceed" hoặc "Advanced" -> "Accept the risk" để tiếp tục.
5.  Nếu trang chủ Tomcat hiện ra, bạn đã cấu hình SSL thành công.
