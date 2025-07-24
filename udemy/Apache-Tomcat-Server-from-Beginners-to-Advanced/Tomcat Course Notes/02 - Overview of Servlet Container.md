# Bài 2: Tổng quan về Servlet Container

- **Thời lượng**: 03:55
- **Bài trước**: [[01 - What we are going to cover]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[03 - Setup Requirements & Installation - Windows Platform]]

---

## Nội dung

Bài học này giải thích khái niệm về **Servlet Container** (còn được gọi là web container), vai trò và chức năng của nó trong kiến trúc ứng dụng web Java.

### Servlet Container là gì?

- **Servlet Container** là một thành phần của máy chủ web (web server) có nhiệm vụ tương tác với các Java Servlet.
- Về cơ bản, nó quản lý vòng đời của các servlet: từ việc tải và khởi tạo (instantiation), gọi các phương thức của servlet (như `init()`, `service()`, `destroy()`), cho đến việc hủy servlet.
- Nó chịu trách nhiệm ánh xạ một URL tới một servlet cụ thể.
- Đảm bảo rằng request và response được xử lý đúng cách.

### Chức năng chính:

1.  **Quản lý vòng đời (Lifecycle Management)**: Tự động hóa quá trình tạo, thực thi và hủy các servlet.
2.  **Giao tiếp (Communication)**: Cung cấp một môi trường dễ dàng để giao tiếp giữa máy chủ web và các thành phần ứng dụng web (servlets, JSPs).
3.  **Hỗ trợ đa luồng (Multithreading Support)**: Tự động tạo một luồng mới cho mỗi request đến servlet, giúp xử lý nhiều yêu cầu đồng thời.
4.  **Bảo mật (Security)**: Quản lý các ràng buộc bảo mật được định nghĩa trong tệp tin mô tả triển khai (`web.xml`).
5.  **Quản lý Session**: Chịu trách nhiệm tạo và quản lý các session cho các request.

**Apache Tomcat** chính là một trong những Servlet Container phổ biến và được sử dụng rộng rãi nhất.
