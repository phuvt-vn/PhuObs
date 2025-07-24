# Bài 3: Yêu cầu và Cài đặt trên Windows

- **Thời lượng**: 05:03
- **Bài trước**: [[02 - Overview of Servlet Container]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[04 - Setup Requirements & Installation - Linux Platform]]

---

## Nội dung

Bài học này hướng dẫn các bước cần thiết để chuẩn bị môi trường và cài đặt Apache Tomcat trên hệ điều hành Windows.

### Yêu cầu hệ thống:

1.  **Java Development Kit (JDK)**:
    -   Tomcat được viết bằng Java, vì vậy bạn cần cài đặt JDK trước tiên.
    -   Kiểm tra phiên bản Java tương thích với phiên bản Tomcat bạn định cài đặt trên trang chủ của Tomcat.
    -   Sau khi cài đặt JDK, bạn cần cấu hình biến môi trường `JAVA_HOME`.
        -   `JAVA_HOME`: trỏ đến thư mục cài đặt JDK (ví dụ: `C:\Program Files\Java\jdk-11.0.12`).
        -   Thêm `%JAVA_HOME%\bin` vào biến `Path` của hệ thống.
    -   Kiểm tra cài đặt bằng lệnh: `java -version` và `javac -version`.

### Các bước cài đặt Tomcat:

1.  **Tải xuống Tomcat**:
    -   Truy cập trang web chính thức của Apache Tomcat: [https://tomcat.apache.org/](https://tomcat.apache.org/)
    -   Chọn phiên bản Tomcat bạn muốn sử dụng (ví dụ: Tomcat 9, Tomcat 10).
    -   Trong phần "Binary Distributions", tải về file **zip** (ví dụ: `apache-tomcat-9.0.54.zip`).

2.  **Giải nén**:
    -   Giải nén file zip vừa tải về vào một thư mục bạn muốn (ví dụ: `C:\apache-tomcat-9.0.54`).

3.  **Cấu hình biến môi trường (Tùy chọn nhưng khuyến khích)**:
    -   Tạo biến môi trường `CATALINA_HOME` trỏ đến thư mục cài đặt Tomcat (ví dụ: `C:\apache-tomcat-9.0.54`).
    -   Thêm `%CATALINA_HOME%\bin` vào biến `Path`.

4.  **Khởi động Tomcat**:
    -   Mở Command Prompt (CMD) hoặc PowerShell.
    -   Di chuyển đến thư mục `bin` của Tomcat: `cd C:\apache-tomcat-9.0.54\bin`
    -   Chạy file `startup.bat` để khởi động server:
        ```bash
        startup.bat
        ```
    -   Một cửa sổ terminal mới sẽ hiện ra và hiển thị các log khởi động.

5.  **Kiểm tra**:
    -   Mở trình duyệt web và truy cập vào địa chỉ: `http://localhost:8080`.
    -   Nếu bạn thấy trang chào mừng của Apache Tomcat, điều đó có nghĩa là bạn đã cài đặt thành công.

6.  **Tắt Tomcat**:
    -   Để dừng server, quay lại thư mục `bin` và chạy file `shutdown.bat`:
        ```bash
        shutdown.bat
