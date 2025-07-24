# Bài 46: Tối ưu Bộ nhớ - Heap & Metaspace

- **Thời lượng**: 07:42
- **Bài trước**: [[45 - Connectors and Executor Thread Optimization]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[47 - Enabling HTTP Response Compression]]

---

## Nội dung

Tomcat là một ứng dụng Java, và việc quản lý bộ nhớ của Máy ảo Java (JVM) là yếu tố then chốt quyết định sự ổn định và hiệu năng của nó. Bài học này tập trung vào hai vùng nhớ quan trọng nhất: **Heap** và **Metaspace**.

### Cách cấu hình bộ nhớ JVM cho Tomcat

-   Các tham số bộ nhớ được truyền cho JVM thông qua các "flags" (cờ) khi khởi động.
-   Cách tốt nhất để thiết lập các cờ này cho Tomcat là sử dụng file `setenv.bat` (Windows) hoặc `setenv.sh` (Linux) trong thư mục `$CATALINA_HOME/bin`.
-   Các cờ này được đặt vào biến môi trường `CATALINA_OPTS`.

**Ví dụ file `setenv.sh` (Linux):**
```sh
# Cấu hình kích thước Heap, Metaspace và một số cờ tối ưu khác
export CATALINA_OPTS="-server -Xms2g -Xmx2g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=256m"
```
- `-Xms2g`: Thiết lập kích thước **ban đầu** của Heap là 2 Gigabytes.
- `-Xmx2g`: Thiết lập kích thước **tối đa** của Heap là 2 Gigabytes.
- `-XX:MetaspaceSize=256m`: Kích thước ban đầu của Metaspace.
- `-XX:MaxMetaspaceSize=256m`: Kích thước tối đa của Metaspace.
- `-server`: Báo cho JVM rằng đây là một ứng dụng server, bật các tối ưu hóa cho các ứng dụng chạy dài hạn.

### 1. Vùng nhớ Heap

-   **Heap** là vùng nhớ chính nơi tất cả các đối tượng (objects) được tạo ra bởi ứng dụng của bạn được lưu trữ.
-   Đây là khu vực được quản lý bởi **Garbage Collector (GC)**.

#### Tối ưu Heap Size (`-Xms` và `-Xmx`)

-   **Nguyên tắc vàng**: Trong môi trường production, hãy đặt kích thước ban đầu (`-Xms`) bằng với kích thước tối đa (`-Xmx`).
    -   **Lý do**: Việc này ngăn cản JVM phải liên tục thay đổi kích thước heap trong quá trình chạy, một hoạt động có thể gây ra các đợt tạm dừng ngắn (minor pauses) và làm giảm hiệu năng. Bằng cách cấp phát toàn bộ bộ nhớ ngay từ đầu, bạn có được hiệu năng ổn định hơn.
-   **Xác định kích thước phù hợp**:
    -   Không có "con số ma thuật". Kích thước heap cần thiết phụ thuộc hoàn toàn vào ứng dụng của bạn: số lượng người dùng đồng thời, độ phức tạp của các đối tượng, khối lượng dữ liệu được xử lý, ...
    -   **Cách tiếp cận**:
        1.  Bắt đầu với một giá trị hợp lý (ví dụ: 2GB, 4GB).
        2.  Sử dụng các công cụ giám sát như JConsole hoặc VisualVM để theo dõi "Heap Memory Usage" dưới tải thực tế.
        3.  Quan sát biểu đồ bộ nhớ. Nếu bộ nhớ đã sử dụng (used memory) sau mỗi lần Full GC liên tục tăng, đó là dấu hiệu của **rò rỉ bộ nhớ (memory leak)**. Bạn cần sửa code ứng dụng thay vì chỉ tăng heap.
        4.  Nếu ứng dụng chạy ổn định nhưng thường xuyên gần chạm ngưỡng tối đa, hãy xem xét việc tăng heap.
    -   Cung cấp quá ít heap sẽ gây ra lỗi `java.lang.OutOfMemoryError: Java heap space`.
    -   Cung cấp quá nhiều heap cũng không tốt, vì nó sẽ làm tăng thời gian tạm dừng khi Full GC chạy (GC phải quét một vùng nhớ lớn hơn).

### 2. Vùng nhớ Metaspace (Từ Java 8 trở đi)

-   **Metaspace** là nơi JVM lưu trữ **metadata** về các lớp (class) đã được tải, chẳng hạn như cấu trúc của lớp, các phương thức, các trường (fields).
-   Nó thay thế cho vùng nhớ **PermGen (Permanent Generation)** ở các phiên bản Java 7 trở về trước.
-   Khác với PermGen có kích thước cố định, Metaspace theo mặc định có thể tự động tăng kích thước và chỉ bị giới hạn bởi bộ nhớ vật lý còn lại của hệ điều hành.

#### Tối ưu Metaspace (`-XX:MetaspaceSize` và `-XX:MaxMetaspaceSize`)

-   Mặc dù Metaspace có thể tự động tăng, việc này có thể gây ra các đợt Full GC không mong muốn.
-   **Nguyên tắc**: Giám sát mức sử dụng Metaspace của ứng dụng khi khởi động và hoạt động ổn định, sau đó đặt `-XX:MetaspaceSize` và `-XX:MaxMetaspaceSize` thành một giá trị cao hơn một chút so với mức sử dụng tối đa quan sát được.
    -   Ví dụ: Nếu ứng dụng của bạn sử dụng khoảng 180MB Metaspace, đặt cả hai giá trị thành `256m` là một lựa chọn an toàn.
    -   Điều này giúp tránh việc Metaspace phải thay đổi kích thước và giảm khả năng xảy ra Full GC do Metaspace.
-   Nếu Metaspace bị đầy, bạn sẽ gặp lỗi `java.lang.OutOfMemoryError: Metaspace`.

Tối ưu bộ nhớ là một quá trình cân bằng tinh tế, đòi hỏi sự giám sát cẩn thận và hiểu biết về hành vi của ứng dụng.
