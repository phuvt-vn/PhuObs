# Bài 35: Chặn Request ở Tầng Engine

- **Thời lượng**: 11:17
- **Bài trước**: [[34 - HTTP Request Interception - Host Layer]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[36 - Realm Concept]]

---

## Nội dung

Đây là cấp độ cao nhất để chặn request trong Tomcat. Một Valve được đặt ở tầng **Engine** sẽ xử lý **mọi request** đi vào phiên bản Tomcat đó, trước cả khi Tomcat quyết định request đó thuộc về virtual host nào.

### Chặn Request ở tầng Engine

-   Cấu hình được thực hiện trong file `server.xml`.
-   Một Valve được định nghĩa là con trực tiếp của thẻ `<Engine>`.

**Ví dụ trong `server.xml`:**

```xml
<Service name="Catalina">

  <Connector port="8080" ... />

  <Engine name="Catalina" defaultHost="localhost">
  
    <!-- VALVE Ở TẦNG ENGINE -->
    <!-- Valve này sẽ được áp dụng cho MỌI request đến Tomcat qua Connector ở trên -->
    <Valve className="org.apache.catalina.valves.RequestDumperValve" />
    
    <!-- Các virtual host được định nghĩa ở đây -->
    <Host name="localhost" ... >
        <!-- Valve ở tầng Host chỉ áp dụng cho localhost -->
        <Valve className="org.apache.catalina.valves.AccessLogValve" />
    </Host>
    
    <Host name="site1.com" ... >
        <!-- Valve ở tầng Host chỉ áp dụng cho site1.com -->
    </Host>
    
  </Engine>
  
</Service>
```

Trong ví dụ trên:
-   `RequestDumperValve` sẽ được thực thi cho các request đến cả `localhost` và `site1.com`.
-   `AccessLogValve` chỉ được thực thi cho các request đến `localhost`.

### Kịch bản sử dụng

Việc chặn request ở tầng Engine ít phổ biến hơn so với tầng Host hay Application, nhưng nó rất hữu ích cho các tác vụ mang tính toàn cục và cơ sở hạ tầng.

1.  **Gỡ lỗi toàn diện (Global Debugging)**:
    -   Đặt một `RequestDumperValve` ở tầng Engine là cách nhanh nhất để xem chi tiết tất cả các request đi vào server, bất kể chúng dành cho host nào. Đây là một công cụ chẩn đoán mạnh mẽ khi bạn không chắc chắn request đang được xử lý như thế nào.
    -   **Cảnh báo**: Chỉ sử dụng trong môi trường development.

2.  **Chính sách bảo mật toàn cục (Global Security Policy)**:
    -   Nếu bạn có một chính sách bảo mật cần áp dụng cho toàn bộ máy chủ Tomcat, bất kể có bao nhiêu virtual host được thêm vào trong tương lai (ví dụ: cấm tất cả các IP từ một quốc gia nào đó), việc đặt một `RemoteAddrValve` ở tầng Engine là giải pháp hiệu quả nhất. Nó đảm bảo chính sách được thực thi một cách nhất quán và không thể bị "quên" khi cấu hình một host mới.

3.  **Ghi log truy cập toàn cục (Global Access Logging)**:
    -   Nếu bạn muốn có một file log duy nhất ghi lại tất cả các request đến server, thay vì nhiều file log cho từng host, bạn có thể đặt một `AccessLogValve` ở tầng Engine.
    -   File log này sẽ chứa request cho tất cả các virtual host, giúp có một cái nhìn tổng thể về hoạt động của server.

### Tổng kết các tầng chặn Request

Đây là bức tranh toàn cảnh về các cơ chế chặn request trong Tomcat, từ cao nhất đến thấp nhất:

1.  **Tầng Engine (Engine Level)**
    -   **Cơ chế**: Valve trong `<Engine>`
    -   **Phạm vi**: Toàn bộ máy chủ Tomcat.
    -   **Mục đích**: Các quy tắc toàn cục, gỡ lỗi, bảo mật cơ sở hạ tầng.

2.  **Tầng Host (Host Level)**
    -   **Cơ chế**: Valve trong `<Host>`
    -   **Phạm vi**: Một virtual host cụ thể.
    -   **Mục đích**: Các quy tắc áp dụng cho tất cả ứng dụng trên một host (SSO, log riêng, kiểm soát truy cập cho host).

3.  **Tầng Context/Application (Application Level)**
    -   **Cơ chế**: Valve trong `<Context>` hoặc Servlet Filter trong `web.xml`.
    -   **Phạm vi**: Một ứng dụng web duy nhất.
    -   **Mục đích**: Logic nghiệp vụ của ứng dụng (xác thực, ủy quyền, nén dữ liệu, ...).
    -   **Khuyến nghị**: Ưu tiên sử dụng **Servlet Filter** ở tầng này vì tính di động.

Hiểu rõ về các tầng này cho phép bạn lựa chọn đúng công cụ và đặt logic xử lý ở đúng nơi, giúp xây dựng một hệ thống có cấu trúc tốt, dễ bảo trì và an toàn.
