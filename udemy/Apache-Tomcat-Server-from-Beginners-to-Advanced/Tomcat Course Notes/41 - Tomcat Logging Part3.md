# Bài 41: Ghi log trong Tomcat - Phần 3: Logging trong Ứng dụng

- **Thời lượng**: 10:08
- **Bài trước**: [[40 - Tomcat Logging Part2]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[42 - Tomcat Logging Part4]]

---

## Nội dung

Trong khi `logging.properties` quản lý log của bản thân Tomcat, các ứng dụng web thường có nhu cầu logging riêng. Việc trộn lẫn log của ứng dụng vào file `catalina.out` khổng lồ sẽ gây khó khăn cho việc theo dõi và gỡ lỗi. Bài học này trình bày cách thiết lập logging riêng cho ứng dụng của bạn, sử dụng các framework phổ biến như **SLF4J** và **Log4j2**.

### Tại sao không dùng `System.out.println()`?

-   **Không có cấp độ (No Levels)**: Bạn không thể phân biệt được đâu là thông điệp gỡ lỗi (debug), thông tin (info), hay lỗi nghiêm trọng (error).
-   **Không linh hoạt**: Output luôn được gửi đến console (và được `catalina.out` bắt lại). Bạn không thể dễ dàng chuyển hướng output sang file, email, hay cơ sở dữ liệu.
-   **Khó kiểm soát**: Không thể bật/tắt logging cho từng phần của ứng dụng một cách linh hoạt mà không sửa code.
-   **Hiệu năng kém**: `System.out` là một luồng được đồng bộ hóa (synchronized), có thể gây tắc nghẽn trong môi trường đa luồng.

### Giải pháp: Sử dụng Logging Framework

Các framework logging hiện đại cung cấp giải pháp cho tất cả các vấn đề trên. Kiến trúc phổ biến nhất hiện nay là sử dụng **SLF4J** làm facade (lớp mặt tiền) và một implementation (lớp triển khai) cụ thể như **Log4j2** hoặc **Logback**.

-   **SLF4J (Simple Logging Facade for Java)**: Là một API trừu tượng. Code của bạn sẽ chỉ phụ thuộc vào SLF4J. Điều này cho phép bạn thay đổi framework logging triển khai (Log4j2, Logback, JUL) ở phía sau mà không cần sửa một dòng code nào.
-   **Log4j2 / Logback**: Là các framework logging thực sự, chịu trách nhiệm xử lý và ghi log. Chúng rất mạnh mẽ, hiệu năng cao, và có nhiều tùy chọn cấu hình.

### Các bước tích hợp SLF4J và Log4j2

1.  **Thêm các thư viện (Dependencies)**:
    -   Thêm các file JAR cần thiết vào thư mục `WEB-INF/lib` của ứng dụng, hoặc khai báo chúng trong file quản lý build (như `pom.xml` của Maven).
    -   Bạn sẽ cần:
        -   `slf4j-api.jar`: API của SLF4J.
        -   `log4j-slf4j-impl.jar`: Cầu nối (bridge) để SLF4J sử dụng Log4j2 làm implementation.
        -   `log4j-api.jar`: API của Log4j2.
        -   `log4j-core.jar`: Phần lõi triển khai của Log4j2.

2.  **Tạo file cấu hình Log4j2**:
    -   Tạo một file có tên `log4j2.xml` và đặt nó vào thư mục `WEB-INF/classes` của ứng dụng (đối với Maven, đặt trong `src/main/resources`).
    -   Log4j2 sẽ tự động tìm và tải file này khi ứng dụng khởi động.

    **Ví dụ file `log4j2.xml`:**
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <Configuration status="WARN">
        <Appenders>
            <!-- Appender để ghi log ra Console -->
            <Console name="Console" target="SYSTEM_OUT">
                <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
            </Console>

            <!-- Appender để ghi log ra file, có chính sách xoay vòng (rolling) -->
            <RollingFile name="RollingFile" 
                         fileName="C:/tomcat/logs/my-app.log"
                         filePattern="C:/tomcat/logs/my-app-%d{yyyy-MM-dd}-%i.log.gz">
                <PatternLayout>
                    <Pattern>%d %p %c{1.} [%t] %m%n</Pattern>
                </PatternLayout>
                <Policies>
                    <!-- Xoay vòng file log theo thời gian (hàng ngày) -->
                    <TimeBasedTriggeringPolicy />
                    <!-- Xoay vòng file log theo kích thước (ví dụ: 10 MB) -->
                    <SizeBasedTriggeringPolicy size="10 MB"/>
                </Policies>
                <!-- Giữ lại tối đa 10 file log cũ -->
                <DefaultRolloverStrategy max="10"/>
            </RollingFile>
        </Appenders>

        <Loggers>
            <!-- Logger cho package của ứng dụng -->
            <Logger name="com.mycompany.myapp" level="debug" additivity="false">
                <AppenderRef ref="RollingFile"/>
                <AppenderRef ref="Console"/>
            </Logger>

            <!-- Logger gốc, bắt tất cả các log khác -->
            <Root level="error">
                <AppenderRef ref="Console"/>
            </Root>
        </Loggers>
    </Configuration>
    ```

3.  **Sử dụng trong Code**:
    -   Trong các lớp Java của bạn, lấy một instance của `Logger` và sử dụng nó để ghi log.

    ```java
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    public class MyServlet extends HttpServlet {
        // Lấy một logger cho lớp hiện tại
        private static final Logger logger = LoggerFactory.getLogger(MyServlet.class);

        protected void doGet(HttpServletRequest request, HttpServletResponse response) {
            // Ghi log ở các cấp độ khác nhau
            logger.trace("Đây là thông điệp trace.");
            logger.debug("Bắt đầu xử lý request cho user: {}", request.getParameter("user"));
            
            try {
                // ... logic nghiệp vụ ...
                logger.info("Xử lý request thành công.");
            } catch (Exception e) {
                // Ghi lại exception
                logger.error("Đã có lỗi xảy ra khi xử lý request", e);
            }
        }
    }
    ```

Bằng cách này, log của ứng dụng sẽ được ghi vào file `my-app.log` (và các file xoay vòng của nó) một cách độc lập với log của Tomcat, giúp việc quản lý trở nên dễ dàng và chuyên nghiệp hơn.
