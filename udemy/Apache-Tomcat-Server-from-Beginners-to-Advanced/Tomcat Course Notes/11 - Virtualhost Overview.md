# Bài 11: Tổng quan về Virtual Host (Máy chủ ảo)

- **Thời lượng**: 01:38
- **Bài trước**: [[10 - Enabling SSL - Linux Platform]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[12 - Virtual Host Creation - Windows Platform]]

---

## Nội dung

Bài học này giới thiệu khái niệm **Virtual Host** (máy chủ ảo) trong Apache Tomcat và lợi ích của việc sử dụng chúng.

### Virtual Host là gì?

-   **Virtual Host** là một tính năng cho phép một máy chủ Tomcat duy nhất phục vụ nhiều tên miền (domain) hoặc tên máy chủ (hostname) khác nhau.
-   Mỗi virtual host có thể có bộ ứng dụng web, file log, và cấu hình riêng của nó.
-   Đối với người dùng cuối, mỗi virtual host trông giống như một máy chủ web riêng biệt.

**Ví dụ:**

Bạn có một máy chủ Tomcat. Bạn có thể cấu hình nó để:
-   Khi người dùng truy cập `http://www.domain-one.com`, nó sẽ phục vụ các ứng dụng từ một thư mục cụ thể (ví dụ: `/webapps/domain-one`).
-   Khi người dùng truy cập `http://www.domain-two.com`, nó sẽ phục vụ các ứng dụng từ một thư mục khác (ví dụ: `/webapps/domain-two`).

### Tại sao cần sử dụng Virtual Host?

1.  **Tiết kiệm chi phí và tài nguyên**: Thay vì phải thiết lập nhiều máy chủ vật lý hoặc máy ảo cho mỗi trang web, bạn có thể chạy tất cả chúng trên một phiên bản Tomcat duy nhất.
2.  **Quản lý tập trung**: Dễ dàng quản lý nhiều trang web từ một điểm trung tâm.
3.  **Cô lập ứng dụng**: Mỗi virtual host có thư mục `appBase` riêng, giúp cô lập các ứng dụng của các tên miền khác nhau.
4.  **Linh hoạt**: Cho phép cấu hình các thiết lập khác nhau (như file log, context) cho từng host.

### Cách Tomcat xử lý Virtual Host

-   Trong file `server.xml`, thành phần `<Engine>` chứa một hoặc nhiều thành phần `<Host>`.
-   Mỗi thành phần `<Host>` đại diện cho một virtual host.
-   Thuộc tính `name` của thẻ `<Host>` xác định tên miền mà host đó sẽ xử lý.
-   Tomcat xác định virtual host nào sẽ xử lý một request đến dựa trên header `Host` trong yêu cầu HTTP.
-   Luôn có một **host mặc định (default host)** được định nghĩa trong `<Engine>` (thuộc tính `defaultHost`). Nếu không có virtual host nào khớp với header `Host` của request, request đó sẽ được chuyển đến host mặc định.

Trong các bài học tiếp theo, chúng ta sẽ tìm hiểu cách cấu hình chi tiết một virtual host trên cả Windows và Linux.
