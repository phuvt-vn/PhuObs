# Bài 45: Tối ưu Connector và Executor (Thread Pool)

- **Thời lượng**: 08:05
- **Bài trước**: [[44 - Setting Up Monitoring]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[46 - Memory Optimization - Heap & Metaspace]]

---

## Nội dung

Một trong những khu vực quan trọng nhất ảnh hưởng trực tiếp đến khả năng xử lý request đồng thời của Tomcat là **Connector** và **Thread Pool** (vùng chứa luồng) của nó. Bài học này giải thích các tham số chính và cách tinh chỉnh chúng để đạt hiệu năng tối ưu.

### Connector và Executor

-   **Connector**: Là thành phần lắng nghe các kết nối mạng đến từ client trên một cổng cụ thể (ví dụ: HTTP trên cổng 8080).
-   **Executor (Thread Pool)**: Là thành phần quản lý một nhóm các luồng (threads). Khi Connector nhận một request, nó sẽ lấy một luồng từ Executor để xử lý request đó.

Tomcat cho phép bạn định nghĩa một **Executor dùng chung (shared Executor)** và nhiều Connector có thể cùng sử dụng nó, hoặc mỗi Connector có thể có một thread pool nội bộ riêng. Sử dụng Executor dùng chung thường là lựa chọn tốt hơn để quản lý tài nguyên tập trung.

### Cấu hình trong `server.xml`

**Ví dụ về một Executor dùng chung và một Connector sử dụng nó:**

```xml
<Service name="Catalina">

    <!-- Định nghĩa một Executor (Thread Pool) dùng chung -->
    <Executor name="tomcatThreadPool" 
              namePrefix="catalina-exec-"
              maxThreads="200" 
              minSpareThreads="25"
              maxIdleTime="60000"
    />

    <!-- Connector sử dụng Executor đã định nghĩa ở trên -->
    <Connector executor="tomcatThreadPool"
               port="8080" 
               protocol="HTTP/1.1"
               acceptCount="100"
               connectionTimeout="20000"
    />
    
    ...
</Service>
```
-   Nếu bạn không định nghĩa `Executor` và không có thuộc tính `executor` trong `Connector`, `Connector` sẽ tự tạo một thread pool nội bộ. Các thuộc tính như `maxThreads` sẽ được đặt trực tiếp trong thẻ `<Connector>`.

### Các tham số tối ưu quan trọng

1.  **`maxThreads`** (trong `<Executor>` hoặc `<Connector>`)
    -   **Ý nghĩa**: Số lượng luồng xử lý request tối đa có thể được tạo ra. Đây là số lượng request đồng thời tối đa mà Tomcat có thể xử lý tại một thời điểm.
    -   **Mặc định**: 200.
    -   **Tối ưu**:
        -   Giá trị này phụ thuộc rất nhiều vào loại ứng dụng (I/O-bound hay CPU-bound) và tài nguyên của server (số lõi CPU, bộ nhớ).
        -   **Không có một con số hoàn hảo cho tất cả mọi người.**
        -   Nếu đặt quá thấp, các request sẽ phải xếp hàng chờ trong khi server vẫn còn tài nguyên, làm giảm thông lượng (throughput).
        -   Nếu đặt quá cao, nó sẽ lãng phí bộ nhớ (mỗi luồng tốn một lượng bộ nhớ nhất định) và có thể làm tăng chi phí chuyển đổi ngữ cảnh (context switching) của CPU, thậm chí làm giảm hiệu năng.
        -   **Cách tiếp cận**: Bắt đầu với giá trị mặc định (200), sử dụng JConsole để giám sát `currentThreadsBusy`. Nếu chỉ số này thường xuyên đạt gần đến `maxThreads`, hãy tăng dần giá trị này (ví dụ: lên 300, 400) và đo lường lại hiệu năng.

2.  **`minSpareThreads`** (trong `<Executor>` hoặc `<Connector>`)
    -   **Ý nghĩa**: Số lượng luồng tối thiểu luôn được giữ trong pool, ngay cả khi chúng đang rảnh rỗi.
    -   **Mặc định**: 25.
    -   **Tối ưu**: Giữ một số lượng luồng "nóng" sẵn sàng giúp Tomcat phản hồi nhanh hơn với các đợt request đột ngột, vì nó không tốn thời gian để tạo luồng mới. Tăng giá trị này có thể cải thiện độ trễ (latency) ban đầu, nhưng sẽ tốn bộ nhớ hơn.

3.  **`acceptCount`** (chỉ trong `<Connector>`)
    -   **Ý nghĩa**: Kích thước của hàng đợi kết nối. Khi tất cả các luồng trong pool đều đang bận, các kết nối đến mới sẽ được xếp vào hàng đợi này.
    -   **Mặc định**: 100.
    -   **Tối ưu**: Nếu hàng đợi này đầy, các kết nối mới sẽ bị từ chối ngay lập tức ("Connection refused"). Tăng giá trị này cho phép server "chịu đựng" được các đợt tải đột biến ngắn hạn, nhưng có thể làm tăng thời gian chờ của người dùng.

4.  **`maxConnections`** (chỉ trong `<Connector>`)
    -   **Ý nghĩa**: Số lượng kết nối tối đa mà Tomcat sẽ chấp nhận và xử lý tại một thời điểm. Nó bao gồm cả các kết nối đang được xử lý bởi một luồng và các kết nối đang chờ trong hàng đợi.
    -   **Mặc định**: Phụ thuộc vào loại Connector, thường là 8192 (NIO) hoặc 10000 (APR/OpenSSL).
    -   **Tối ưu**: Giá trị này nên luôn lớn hơn `maxThreads`. Nó hoạt động như một cơ chế bảo vệ để ngăn server bị quá tải bởi một số lượng kết nối quá lớn.

**Quy trình xử lý request:**
Request đến -> `maxConnections` chưa đạt? -> Lấy luồng từ pool (`maxThreads` chưa đạt?) -> Xử lý.
Nếu `maxThreads` đã đạt -> Request vào hàng đợi (`acceptCount` chưa đạt?) -> Chờ.
Nếu `acceptCount` đã đạt -> Request bị từ chối.

Việc tinh chỉnh các tham số này đòi hỏi sự hiểu biết về ứng dụng của bạn và phải dựa trên kết quả đo lường thực tế.
