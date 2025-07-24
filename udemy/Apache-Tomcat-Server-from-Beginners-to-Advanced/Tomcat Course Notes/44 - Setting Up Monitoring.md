# Bài 44: Thiết lập Giám sát (Monitoring)

- **Thời lượng**: 04:34
- **Bài trước**: [[43 - Performance Tuning Overview]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[45 - Connectors and Executor Thread Optimization]]

---

## Nội dung

Để có thể tối ưu hóa hiệu năng, trước tiên bạn phải **đo lường** và **giám sát** được nó. Bài học này giới thiệu cách sử dụng **JMX (Java Management Extensions)** để "nhìn" vào bên trong một máy chủ Tomcat đang chạy và thu thập các chỉ số hiệu năng quan trọng.

### JMX là gì?

-   **JMX** là một công nghệ chuẩn của Java, được thiết kế để quản lý và giám sát các ứng dụng, thiết bị, và dịch vụ.
-   Nó hoạt động bằng cách "phơi bày" (expose) các tài nguyên quản lý của một ứng dụng (ví dụ: Tomcat) dưới dạng các đối tượng gọi là **MBeans (Managed Beans)**.
-   Mỗi MBean có các **thuộc tính (attributes)** có thể đọc được (ví dụ: `currentThreadCount`, `heapMemoryUsage`) và các **thao tác (operations)** có thể được gọi (ví dụ: `gc()` để yêu cầu chạy Garbage Collector).

### Công cụ giám sát: JConsole

-   **JConsole** là một công cụ giám sát đồ họa đi kèm với mọi bộ JDK.
-   Nó sử dụng JMX để kết nối đến một tiến trình Java đang chạy (như Tomcat) và hiển thị các chỉ số hiệu năng theo thời gian thực.

### Bước 1: Kích hoạt JMX Remote trên Tomcat

Theo mặc định, JMX chỉ cho phép kết nối từ cùng một máy. Để có thể giám sát Tomcat từ xa (từ máy tính của bạn đến một server), bạn cần kích hoạt JMX Remote.

1.  Tạo một file có tên `setenv.bat` (trên Windows) hoặc `setenv.sh` (trên Linux) trong thư mục `$CATALINA_HOME/bin`. File này dùng để thiết lập các biến môi trường cho Tomcat.

2.  Thêm các thuộc tính hệ thống Java sau vào file `setenv`:

    **Trên Windows (`setenv.bat`):**
    ```bat
    set "CATALINA_OPTS=%CATALINA_OPTS% -Dcom.sun.management.jmxremote"
    set "CATALINA_OPTS=%CATALINA_OPTS% -Dcom.sun.management.jmxremote.port=9001"
    set "CATALINA_OPTS=%CATALINA_OPTS% -Dcom.sun.management.jmxremote.ssl=false"
    set "CATALINA_OPTS=%CATALINA_OPTS% -Dcom.sun.management.jmxremote.authenticate=false"
    set "CATALINA_OPTS=%CATALINA_OPTS% -Djava.rmi.server.hostname=your_server_ip"
    ```

    **Trên Linux (`setenv.sh`):**
    ```sh
    export CATALINA_OPTS="$CATALINA_OPTS -Dcom.sun.management.jmxremote"
    export CATALINA_OPTS="$CATALINA_OPTS -Dcom.sun.management.jmxremote.port=9001"
    export CATALINA_OPTS="$CATALINA_OPTS -Dcom.sun.management.jmxremote.ssl=false"
    export CATALINA_OPTS="$CATALINA_OPTS -Dcom.sun.management.jmxremote.authenticate=false"
    export CATALina_OPTS="$CATALINA_OPTS -Djava.rmi.server.hostname=your_server_ip"
    ```
    -   Thay `your_server_ip` bằng địa chỉ IP của máy chủ Tomcat.
    -   `jmxremote.port=9001`: Chỉ định cổng cho kết nối JMX.
    -   **Cảnh báo**: Cấu hình trên (`ssl=false`, `authenticate=false`) vô hiệu hóa bảo mật, chỉ nên dùng trong môi trường mạng nội bộ tin cậy. Trong môi trường production, bạn **phải** kích hoạt xác thực và SSL.

3.  Khởi động lại Tomcat.

### Bước 2: Kết nối bằng JConsole

1.  Mở Command Prompt hoặc Terminal trên máy tính của bạn (máy đã cài JDK).
2.  Gõ lệnh `jconsole`.
3.  Trong cửa sổ JConsole, chọn "Remote Process".
4.  Nhập `your_server_ip:9001` và nhấn "Connect".

### Các chỉ số quan trọng cần theo dõi trong JConsole

-   **Tab `Overview`**: Cung cấp biểu đồ tổng quan về:
    -   **Heap Memory Usage**: Mức sử dụng bộ nhớ heap. Đây là chỉ số quan trọng nhất. Nếu biểu đồ có dạng "răng cưa" (tăng dần rồi giảm đột ngột), đó là hoạt động bình thường của GC. Nếu nó cứ tăng mãi, có thể bạn đang bị rò rỉ bộ nhớ (memory leak).
    -   **Threads**: Số lượng luồng đang hoạt động.
    -   **Classes**: Số lượng lớp đã được tải.
    -   **CPU Usage**: Mức sử dụng CPU của tiến trình Tomcat.

-   **Tab `Memory`**:
    -   Cung cấp thông tin chi tiết về các vùng nhớ khác nhau (Heap, Metaspace, ...).
    -   Cho phép bạn yêu cầu chạy GC thủ công (nút "Perform GC").

-   **Tab `Threads`**:
    -   Liệt kê tất cả các luồng đang chạy.
    -   Hữu ích để phát hiện deadlock (nút "Detect Deadlock").

-   **Tab `MBeans`**:
    -   Đây là nơi bạn có thể khám phá tất cả các MBean mà Tomcat cung cấp.
    -   **`Catalina` -> `ThreadPool` -> `http-nio-8080` (hoặc tên connector của bạn)**:
        -   `maxThreads`: Số luồng tối đa đã cấu hình.
        -   `currentThreadCount`: Số luồng hiện tại trong pool.
        -   `currentThreadsBusy`: Số luồng đang bận xử lý request. **Nếu chỉ số này thường xuyên gần bằng `maxThreads`, đó là dấu hiệu bạn cần tăng `maxThreads`.**
    -   **`java.lang` -> `Memory`**:
        -   `HeapMemoryUsage`: Xem chi tiết các giá trị `init`, `used`, `committed`, `max`.

Bằng cách thường xuyên giám sát các chỉ số này, bạn có thể hiểu rõ hơn về hoạt động của Tomcat, xác định các nút thắt cổ chai và đưa ra các quyết định tối ưu hóa dựa trên dữ liệu thực tế.
