# Bài 6: Tìm hiểu các file Cấu hình và Thực thi

- **Thời lượng**: 05:23
- **Bài trước**: [[05 - Application Deployment]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[07 - Security Overview]]

---

## Nội dung

Bài học này giới thiệu về cấu trúc thư mục của Apache Tomcat, giải thích mục đích của các thư mục và các tệp tin quan trọng.

### Cấu trúc thư mục Tomcat (`$CATALINA_HOME`)

-   **`/bin`**: Chứa các file script để thực thi.
    -   `startup.bat` / `startup.sh`: Khởi động Tomcat.
    -   `shutdown.bat` / `shutdown.sh`: Dừng Tomcat.
    -   `catalina.bat` / `catalina.sh`: Script chính được sử dụng bởi các script khác.
    -   `configtest.bat` / `configtest.sh`: Kiểm tra cú pháp của các file cấu hình.
    -   `version.bat` / `version.sh`: Hiển thị phiên bản Tomcat và Java.

-   **`/conf`**: Chứa các file cấu hình XML và DTDs cho toàn bộ server.
    -   `server.xml`: File cấu hình chính và quan trọng nhất của Tomcat. Nó định nghĩa các thành phần cốt lõi như Server, Service, Connector, Engine, Host.
    -   `web.xml`: File cấu hình mặc định cho tất cả các ứng dụng web được triển khai trên server.
    -   `context.xml`: Cấu hình mặc định cho Context của các ứng dụng.
    -   `tomcat-users.xml`: Nơi định nghĩa người dùng, mật khẩu và vai trò (roles) để truy cập Manager App và Host Manager App.
    -   `catalina.properties`: Cấu hình các thuộc tính cấp cao, classloader, và các cài đặt bảo mật.
    -   `logging.properties`: Cấu hình cho việc ghi log của Tomcat (Java Util Logging).

-   **`/lib`**: Chứa các thư viện (file `.jar`) cần thiết cho Tomcat hoạt động. Các thư viện ở đây được chia sẻ cho tất cả các ứng dụng web.

-   **`/logs`**: Thư mục mặc định chứa các file log của Tomcat.
    -   `catalina.out` (trên Linux) hoặc các file `catalina.YYYY-MM-DD.log`: Log chính của server, ghi lại các sự kiện khởi động, dừng, và các lỗi.
    -   `localhost_access_log.YYYY-MM-DD.txt`: Ghi lại các request truy cập đến server.

-   **`/temp`**: Thư mục tạm thời mà Tomcat sử dụng trong quá trình hoạt động.

-   **`/webapps`**: Thư mục mặc định để triển khai các ứng dụng web. Bất kỳ file `.war` nào được đặt ở đây sẽ được tự động triển khai.
    -   Chứa các ứng dụng mặc định như `docs`, `examples`, `manager`, `host-manager`.

-   **`/work`**: Nơi Tomcat lưu trữ các servlet và trang JSP đã được biên dịch thành các file Java và class. Nếu gặp vấn đề với JSP, việc xóa nội dung thư mục này có thể hữu ích.
