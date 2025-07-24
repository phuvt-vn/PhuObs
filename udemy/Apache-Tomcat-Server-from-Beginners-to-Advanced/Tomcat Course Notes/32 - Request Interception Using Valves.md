# Bài 32: Chặn Request bằng cách sử dụng Valves

- **Thời lượng**: 06:01
- **Bài trước**: [[31 - Customizing Error Pages - Part2]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[33 - Request Interception at Application Layer]]

---

## Nội dung

Bài học này giới thiệu về **Valves**, một thành phần mạnh mẽ và đặc trưng của Tomcat, cho phép bạn chèn các logic xử lý tùy chỉnh vào trong pipeline xử lý request của container.

### Valve là gì?

-   Trong kiến trúc của Tomcat, một request đến sẽ đi qua một chuỗi các thành phần xử lý (gọi là **pipeline**), bắt đầu từ `Engine`, qua `Host`, `Context`, và cuối cùng đến `Wrapper` (nơi chứa servlet).
-   **Valve** là một lớp Java mà bạn có thể chèn vào bất kỳ điểm nào trong pipeline này.
-   Nó cho phép bạn "chặn" (intercept) và xử lý request **trước khi** nó đến thành phần tiếp theo trong chuỗi, hoặc xử lý response **trước khi** nó được gửi về cho client.
-   Khái niệm này tương tự như `Filter` trong Servlet API, nhưng Valve hoạt động ở cấp độ thấp hơn (cấp độ container) và có thể được áp dụng cho toàn bộ Engine, một Host cụ thể, hoặc một Context (ứng dụng) cụ thể.

### So sánh Valve và Filter

| Tiêu chí | Valve | Filter |
| :--- | :--- | :--- |
| **Phạm vi** | Cấp container (Engine, Host, Context) | Cấp ứng dụng web (được định nghĩa trong `web.xml`) |
| **Phụ thuộc** | Đặc thù của Tomcat (không phải là một phần của chuẩn Servlet) | Chuẩn Servlet (hoạt động trên bất kỳ servlet container nào) |
| **Khả năng** | Có thể truy cập các đối tượng nội bộ của Tomcat. Có thể chặn request trước cả khi nó đến bất kỳ ứng dụng nào. | Chỉ hoạt động trong ngữ cảnh của một ứng dụng web. |
| **Tính di động** | Không thể di chuyển sang các container khác (như Jetty, WildFly). | Hoàn toàn có thể di chuyển được. |

### Các loại Valve tích hợp sẵn

Tomcat đi kèm với nhiều Valve hữu ích được xây dựng sẵn. Bạn có thể khai báo chúng trong file `server.xml` (cho Engine, Host) hoặc `context.xml` (cho Context).

-   **`AccessLogValve`**: Ghi lại log truy cập. Chúng ta đã thấy valve này khi cấu hình virtual host.
    ```xml
    <Valve className="org.apache.catalina.valves.AccessLogValve" ... />
    ```
-   **`RemoteAddrValve` / `RemoteHostValve`**: Cho phép hoặc từ chối các request dựa trên địa chỉ IP hoặc tên host của client. Rất hữu ích để tạo danh sách trắng/đen (whitelist/blacklist).
    ```xml
    <!-- Chỉ cho phép request từ localhost -->
    <Valve className="org.apache.catalina.valves.RemoteAddrValve" allow="127\.\d+\.\d+\.\d+|::1|0:0:0:0:0:0:0:1" />
    ```
-   **`RequestDumperValve`**: Một công cụ gỡ lỗi cực kỳ hữu ích. Nó sẽ "dump" (in ra) tất cả thông tin chi tiết về request và response (headers, parameters, cookies, ...) vào file log. **Không bao giờ sử dụng trên môi trường production** vì nó làm lộ thông tin nhạy cảm và ảnh hưởng nghiêm trọng đến hiệu năng.
    ```xml
    <Valve className="org.apache.catalina.valves.RequestDumperValve" />
    ```
-   **`StuckThreadDetectionValve`**: Giám sát các luồng xử lý request. Nếu một luồng bị "kẹt" (stuck) quá một khoảng thời gian nhất định (mặc định là 600 giây), nó sẽ ghi lại một cảnh báo vào log, giúp bạn phát hiện các vấn đề về hiệu năng hoặc deadlock.
    ```xml
    <Valve className="org.apache.catalina.valves.StuckThreadDetectionValve" threshold="60" />
    ```

### Khi nào nên sử dụng Valve?

-   Khi bạn cần thực hiện một hành động trên tất cả các request đến một Host hoặc toàn bộ Engine (ví dụ: ghi log, kiểm tra IP).
-   Khi bạn cần truy cập vào các chức năng nội bộ của Tomcat mà Filter không thể làm được.
-   Trong hầu hết các trường hợp khác liên quan đến logic của ứng dụng, **Servlet Filter là lựa chọn tốt hơn** vì tính di động của nó.

Các bài học tiếp theo sẽ khám phá các cách chặn request khác ở các cấp độ khác nhau.
