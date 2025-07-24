# Bài 42: Ghi log trong Tomcat - Phần 4: Cấu hình Access Log

- **Thời lượng**: 12:44
- **Bài trước**: [[41 - Tomcat Logging Part3]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[43 - Performance Tuning Overview]]

---

## Nội dung

Bài học này tập trung vào **Access Log** - nhật ký truy cập. Đây là một công cụ cực kỳ quan trọng, ghi lại thông tin về mọi request HTTP được xử lý bởi Tomcat. Việc tùy chỉnh Access Log giúp bạn thu thập chính xác những thông tin cần thiết cho việc phân tích lưu lượng, giám sát hiệu năng và điều tra bảo mật.

### Access Log Valve

-   Việc ghi log truy cập trong Tomcat được điều khiển bởi một Valve đặc biệt có tên là `AccessLogValve`.
-   Bạn có thể cấu hình Valve này ở các cấp độ khác nhau (`Engine`, `Host`) trong file `server.xml`. Đặt nó trong `<Host>` sẽ tạo ra một file log truy cập riêng cho virtual host đó.

### Cấu hình `AccessLogValve`

Dưới đây là một ví dụ về cấu hình `AccessLogValve` với các thuộc tính phổ biến:

```xml
<Valve className="org.apache.catalina.valves.AccessLogValve" 
       directory="logs"
       prefix="my_app_access_log" 
       suffix=".txt"
       pattern="common" 
       resolveHosts="false"
       rotatable="true"
       fileDateFormat="yyyy-MM-dd"
/>
```

**Giải thích các thuộc tính:**

-   `className`: Phải là `org.apache.catalina.valves.AccessLogValve`.
-   `directory`: Thư mục nơi lưu các file log truy cập (tương đối so với `$CATALINA_HOME`). Mặc định là `logs`.
-   `prefix`: Tiền tố cho tên file log.
-   `suffix`: Hậu tố cho tên file log.
-   `pattern`: **Quan trọng nhất**. Xác định định dạng và nội dung của mỗi dòng log. Tomcat cung cấp hai định dạng được định nghĩa sẵn:
    -   `common`: Một định dạng phổ biến, tương thích với nhiều công cụ phân tích log.
        -   Định dạng: `%h %l %u %t "%r" %s %b`
    -   `combined`: Mở rộng của `common`, thêm thông tin về Referer và User-Agent.
        -   Định dạng: `%h %l %u %t "%r" %s %b "%{Referer}i" "%{User-Agent}i"`
    -   Bạn cũng có thể tạo **mẫu tùy chỉnh** bằng cách sử dụng các mã định dạng khác nhau (xem bên dưới).
-   `resolveHosts`: Nếu đặt là `true`, Tomcat sẽ cố gắng phân giải địa chỉ IP của client thành tên host thông qua DNS lookup. **Không khuyến khích** bật tính năng này (`false` là mặc định) vì nó ảnh hưởng tiêu cực đến hiệu năng.
-   `rotatable`: Nếu đặt là `true` (mặc định), Tomcat sẽ tự động xoay vòng (tạo file mới) cho log truy cập.
-   `fileDateFormat`: Định dạng ngày tháng sẽ được thêm vào tên file khi log được xoay vòng. Mặc định là `.yyyy-MM-dd`.

### Tùy chỉnh `pattern` (Mẫu định dạng)

Bạn có thể xây dựng một chuỗi định dạng tùy chỉnh để ghi lại chính xác những thông tin bạn cần. Một vài mã định dạng hữu ích:

| Mã | Mô tả |
| :--- | :--- |
| `%h` | Tên host hoặc địa chỉ IP của client. |
| `%l` | Tên người dùng từ `identd` (thường là `-`). |
| `%u` | Tên người dùng đã được xác thực (nếu có, nếu không là `-`). |
| `%t` | Ngày và giờ của request. |
| `%r` | Dòng request đầu tiên (ví dụ: "GET /index.html HTTP/1.1"). |
| `%s` | Mã trạng thái HTTP (200, 404, 500, ...). |
| `%b` | Kích thước của response (tính bằng byte), không bao gồm headers. |
| `%D` | Thời gian xử lý request (tính bằng **mili giây**). **Cực kỳ hữu ích để theo dõi hiệu năng.** |
| `%T` | Thời gian xử lý request (tính bằng **giây**). |
| `%{xxx}i` | Header của request đến (ví dụ: `%{User-Agent}i`, `%{Referer}i`). |
| `%{xxx}o` | Header của response trả về. |
| `%{xxx}c` | Cookie (ví dụ: `%{JSESSIONID}c`). |
| `%{xxx}r` | Thuộc tính (attribute) trong ServletRequest. |
| `%{xxx}s` | Thuộc tính (attribute) trong HttpSession. |

**Ví dụ về một `pattern` tùy chỉnh nâng cao:**

Pattern này ghi lại thông tin giống `combined`, nhưng thêm vào thời gian xử lý request (`%D`) và Session ID.
```
pattern="%h %l %u %t "%r" %s %b "%{Referer}i" "%{User-Agent}i" %D %{JSESSIONID}c"
```
-   **Lưu ý**: Dấu `"` được sử dụng thay cho `"` trong file XML.

Việc cấu hình Access Log một cách cẩn thận sẽ cung cấp cho bạn dữ liệu vô giá để hiểu rõ hơn về cách ứng dụng của bạn đang được sử dụng và hoạt động như thế nào.
