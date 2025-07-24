# Bài 40: Ghi log trong Tomcat - Phần 2: Cấu hình `logging.properties`

- **Thời lượng**: 12:26
- **Bài trước**: [[39 - Tomcat Logging Part1]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[41 - Tomcat Logging Part3]]

---

## Nội dung

Bài học này đi sâu vào cách tùy chỉnh file `$CATALINA_HOME/conf/logging.properties` để kiểm soát việc ghi log của Tomcat (sử dụng framework JULI).

### Cấu trúc của `logging.properties`

File này bao gồm ba loại cấu hình chính, tương tự như các framework logging khác:

1.  **Handlers (Appenders)**: Đích đến của các thông điệp log (ví dụ: console, file).
2.  **Loggers**: Nguồn phát sinh các thông điệp log (ví dụ: một package hoặc class cụ thể trong Tomcat). Mỗi logger có một cấp độ (level) và có thể được gán một hoặc nhiều handler.
3.  **Formatters**: Định dạng (layout) của mỗi dòng log.

### 1. Handlers

Handlers định nghĩa **nơi** log sẽ được ghi.

**Cú pháp:**
`handlers = <handler_name_1>, <handler_name_2>, ...`

**Ví dụ mặc định:**
```properties
# Liệt kê tất cả các handler sẽ được sử dụng
handlers = 1catalina.org.apache.juli.AsyncFileHandler, \
           2localhost.org.apache.juli.AsyncFileHandler, \
           3manager.org.apache.juli.AsyncFileHandler, \
           4host-manager.org.apache.juli.AsyncFileHandler, \
           java.util.logging.ConsoleHandler
```

Sau khi liệt kê, bạn cần cấu hình chi tiết cho từng handler.

**Ví dụ cấu hình cho `1catalina` handler:**
```properties
# Cấu hình cho handler có tiền tố là "1catalina"
1catalina.org.apache.juli.AsyncFileHandler.level = FINE
1catalina.org.apache.juli.AsyncFileHandler.directory = ${catalina.base}/logs
1catalina.org.apache.juli.AsyncFileHandler.prefix = catalina.
1catalina.org.apache.juli.AsyncFileHandler.formatter = org.apache.juli.OneLineFormatter
```
-   `.level`: Cấp độ log tối thiểu mà handler này sẽ xử lý (FINE, INFO, WARNING, SEVERE).
-   `.directory`: Thư mục để lưu file log.
-   `.prefix`: Tên tiền tố của file log.
-   `.formatter`: Lớp formatter sẽ được sử dụng để định dạng output.

### 2. Loggers

Loggers định nghĩa **cái gì** sẽ được ghi log.

**Cú pháp:**
` <logger_name>.level = <LEVEL>`
` <logger_name>.handlers = <handler_name_1>, <handler_name_2>, ...`

**Ví dụ mặc định:**
```properties
# Logger gốc (root logger)
.level = INFO
.handlers = java.util.logging.ConsoleHandler

# Logger cho package org.apache.catalina
# Ghi log từ cấp INFO trở lên vào handler 1catalina và ConsoleHandler
org.apache.catalina.core.ContainerBase.[Catalina].[localhost].level = INFO
org.apache.catalina.core.ContainerBase.[Catalina].[localhost].handlers = 2localhost.org.apache.juli.AsyncFileHandler

# Logger cho ứng dụng Manager
org.apache.catalina.core.ContainerBase.[Catalina].[localhost].[/manager].level = INFO
org.apache.catalina.core.ContainerBase.[Catalina].[localhost].[/manager].handlers = 3manager.org.apache.juli.AsyncFileHandler
```
-   Tên của logger thường tương ứng với tên package trong Java.
-   Logger có tính kế thừa. Nếu bạn không định nghĩa handler cho một logger cụ thể, nó sẽ sử dụng handler của logger cha.
-   `.level`: Cấp độ log tối thiểu cho logger này.
-   `.handlers`: Danh sách các handler mà logger này sẽ gửi thông điệp đến.

### 3. Formatters

Formatters định nghĩa **như thế nào** một dòng log sẽ được hiển thị.

**Cú pháp:**
`<handler_name>.formatter = <fully_qualified_class_name>`
`<handler_name>.formatter.format = <pattern>`

**Ví dụ:**
```properties
# Sử dụng OneLineFormatter cho ConsoleHandler
java.util.logging.ConsoleHandler.formatter = org.apache.juli.OneLineFormatter

# Định dạng mẫu cho OneLineFormatter
# %1: date/time, %2: source, %3: logger name, %4: level, %5: message, %6: throwable
org.apache.juli.OneLineFormatter.format = %1$tF %1$tT.%1$tL %4$s [%3$s] %5$s %6$s%n
```

### Các cấp độ Log (từ cao đến thấp)

-   `SEVERE` (cao nhất)
-   `WARNING`
-   `INFO`
-   `CONFIG`
-   `FINE`
-   `FINER`
-   `FINEST` (thấp nhất)

Khi bạn đặt level cho một logger là `INFO`, nó sẽ ghi lại tất cả các thông điệp có cấp độ từ `INFO` trở lên (`INFO`, `WARNING`, `SEVERE`).

### Tùy chỉnh phổ biến

-   **Thay đổi cấp độ log để gỡ lỗi**:
    -   Để xem thêm thông tin chi tiết về một thành phần cụ thể (ví dụ: clustering), bạn có thể tạm thời hạ thấp level của logger tương ứng xuống `FINE`.
        ```properties
        org.apache.catalina.ha.level = FINE
        ```
-   **Tạo một file log riêng cho ứng dụng**:
    1.  Định nghĩa một handler mới (ví dụ: `5myapp.org.apache.juli.AsyncFileHandler`).
    2.  Cấu hình `directory`, `prefix` cho handler mới.
    3.  Định nghĩa một logger cho package của ứng dụng bạn.
    4.  Gán handler mới cho logger đó.
        ```properties
        # Logger cho package của ứng dụng
        com.mycompany.myapp.level = INFO
        com.mycompany.myapp.handlers = 5myapp.org.apache.juli.AsyncFileHandler
        ```

Hiểu rõ cách hoạt động của `logging.properties` cho phép bạn kiểm soát hoàn toàn output log của Tomcat, giúp việc quản trị và gỡ lỗi hiệu quả hơn rất nhiều.
