# Bài 34: Chặn Request ở Tầng Host

- **Thời lượng**: 06:15
- **Bài trước**: [[33 - Request Interception at Application Layer]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[35 - HTTP Request Interception - Tomcat Engine Layer]]

---

## Nội dung

Bài học này khám phá cách chặn các request ở một cấp độ cao hơn tầng ứng dụng: **tầng Host**. Việc chặn request ở cấp độ này cho phép bạn áp dụng một logic chung cho tất cả các ứng dụng web được triển khai trên một virtual host cụ thể.

### Chặn Request ở tầng Host

-   Như đã tìm hiểu, các **Valves** có thể được cấu hình ở các cấp độ khác nhau trong container của Tomcat.
-   Khi một Valve được cấu hình bên trong thẻ `<Host>` trong file `server.xml`, nó sẽ được áp dụng cho **mọi request** đến virtual host đó, bất kể request đó dành cho ứng dụng web (context) nào.

### Kịch bản sử dụng

Đây là một cơ chế rất mạnh mẽ cho các tác vụ quản trị và bảo mật ở cấp độ server.

1.  **Kiểm soát truy cập tập trung**:
    -   Thay vì phải cấu hình một filter bảo mật (ví dụ: kiểm tra IP) trong file `web.xml` của từng ứng dụng, bạn có thể định nghĩa một `RemoteAddrValve` duy nhất ở tầng Host.
    -   Valve này sẽ bảo vệ tất cả các ứng dụng trên host đó một cách nhất quán.

    **Ví dụ trong `server.xml`:**
    ```xml
    <Host name="secure.my-domain.com" appBase="secure_webapps">
    
        <!-- Valve này sẽ chặn tất cả request đến host secure.my-domain.com
             nếu IP của client không nằm trong dải 192.168.1.* -->
        <Valve className="org.apache.catalina.valves.RemoteAddrValve"
               allow="192\.168\.1\.\d+"/>
               
        <!-- Tất cả các ứng dụng trong thư mục 'secure_webapps' sẽ được bảo vệ -->
        
    </Host>
    ```

2.  **Ghi log truy cập cho từng Virtual Host**:
    -   Bạn có thể tạo các file log truy cập riêng biệt cho mỗi virtual host bằng cách đặt một `AccessLogValve` bên trong mỗi thẻ `<Host>`.
    -   Điều này giúp việc phân tích lưu lượng truy cập cho từng trang web trở nên dễ dàng hơn.

    **Ví dụ trong `server.xml`:**
    ```xml
    <Host name="site1.com" appBase="webapps_site1">
        <Valve className="org.apache.catalina.valves.AccessLogValve"
               prefix="site1_access_log" ... />
    </Host>
    
    <Host name="site2.com" appBase="webapps_site2">
        <Valve className="org.apache.catalina.valves.AccessLogValve"
               prefix="site2_access_log" ... />
    </Host>
    ```

3.  **Single Sign-On (SSO)**:
    -   Tomcat cung cấp một `SingleSignOn` Valve.
    -   Khi được đặt ở tầng Host, valve này cho phép người dùng đăng nhập một lần và được tự động xác thực trên tất cả các ứng dụng web khác thuộc cùng một virtual host đó.
    -   Người dùng không cần phải đăng nhập lại khi di chuyển giữa các ứng dụng.

    **Ví dụ trong `server.xml`:**
    ```xml
    <Host name="sso.my-domain.com" appBase="sso_apps">
        <Valve className="org.apache.catalina.authenticator.SingleSignOn" />
    </Host>
    ```

### So sánh các tầng chặn request

| Tầng | Cơ chế | Phạm vi | Trường hợp sử dụng |
| :--- | :--- | :--- | :--- |
| **Application** | Servlet Filter | Một ứng dụng web duy nhất (context) | Logic nghiệp vụ, xác thực, ghi log cho ứng dụng cụ thể. |
| **Host** | Valve | Tất cả ứng dụng trên một virtual host | Kiểm soát truy cập tập trung, SSO, ghi log cho từng host. |
| **Engine** | Valve | **Tất cả** các request đến Tomcat | Các quy tắc toàn cục, bảo mật ở mức cao nhất. |

Việc lựa chọn đặt logic chặn request ở tầng nào (Application, Host, hay Engine) phụ thuộc vào phạm vi và mục đích của tác vụ bạn muốn thực hiện. Chặn ở tầng Host là một công cụ hữu ích để quản lý nhiều ứng dụng một cách nhất quán.
