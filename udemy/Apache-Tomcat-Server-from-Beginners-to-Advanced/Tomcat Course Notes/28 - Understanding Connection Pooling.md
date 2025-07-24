# Bài 28: Tìm hiểu về Connection Pooling

- **Thời lượng**: 02:55
- **Bài trước**: [[27 - Understanding JNDI - Concept]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[29 - Creating JNDI and JDBC Connection Pooling - By Example]]

---

## Nội dung

Bài học này giải thích khái niệm **Connection Pooling (tạo vùng chứa kết nối)** và tại sao nó là một kỹ thuật tối ưu hóa hiệu năng cực kỳ quan trọng cho các ứng dụng có tương tác với cơ sở dữ liệu.

### Vấn đề của việc tạo kết nối mới mỗi lần

Hãy tưởng tượng một ứng dụng không sử dụng connection pooling. Mỗi khi cần truy vấn cơ sở dữ liệu, nó sẽ thực hiện các bước sau:

1.  Mở một kết nối mạng vật lý đến máy chủ cơ sở dữ liệu.
2.  Thực hiện quá trình "bắt tay" (handshake) TCP/IP.
3.  Xác thực với cơ sở dữ liệu (gửi username/password).
4.  Thực hiện câu lệnh SQL.
5.  Đóng kết nối.

**Vấn đề:**

-   **Chi phí cao (Expensive)**: Việc thiết lập và hủy bỏ một kết nối cơ sở dữ liệu là một hoạt động tốn kém tài nguyên và thời gian. Nó liên quan đến I/O mạng, cấp phát bộ nhớ và xử lý của CPU trên cả máy chủ ứng dụng và máy chủ cơ sở dữ liệu.
-   **Hiệu năng kém**: Trong một ứng dụng có lưu lượng truy cập cao, việc liên tục tạo và đóng kết nối sẽ làm giảm đáng kể hiệu năng tổng thể. Hệ thống sẽ dành phần lớn thời gian để quản lý kết nối thay vì thực hiện logic nghiệp vụ.
-   **Không có khả năng mở rộng (Not Scalable)**: Khi số lượng người dùng đồng thời tăng lên, máy chủ cơ sở dữ liệu có thể nhanh chóng bị quá tải vì phải xử lý quá nhiều yêu cầu tạo kết nối mới.

### Giải pháp: Connection Pooling

-   **Connection Pooling** là một kỹ thuật trong đó một tập hợp các kết nối cơ sở dữ liệu (gọi là "pool" - vùng chứa) được tạo sẵn và duy trì bởi container (Tomcat).
-   Thay vì tạo một kết nối mới cho mỗi request, ứng dụng sẽ "mượn" (borrow) một kết nối có sẵn từ pool.
-   Sau khi sử dụng xong, thay vì đóng kết nối vật lý, ứng dụng sẽ "trả" (return) nó về lại cho pool để các request khác có thể tái sử dụng.

### Quy trình hoạt động

1.  **Khởi tạo**: Khi Tomcat khởi động, nó sẽ tạo ra một số lượng kết nối cơ sở dữ liệu ban đầu (dựa trên cấu hình) và đặt chúng vào trong pool. Các kết nối này luôn ở trạng thái mở và sẵn sàng.
2.  **Ứng dụng yêu cầu kết nối**:
    -   Ứng dụng thực hiện tra cứu JNDI để lấy `DataSource`.
    -   Ứng dụng gọi `dataSource.getConnection()`.
3.  **Lấy kết nối từ Pool**:
    -   Connection Pool sẽ kiểm tra xem có kết nối nào đang rảnh rỗi trong pool không.
    -   Nếu có, nó sẽ trả về một kết nối cho ứng dụng.
    -   Nếu không có và số lượng kết nối hiện tại chưa đạt mức tối đa, nó có thể tạo một kết nối mới.
    -   Nếu không có và đã đạt mức tối đa, request sẽ phải chờ cho đến khi có một kết nối được trả về pool.
4.  **Ứng dụng sử dụng kết nối**: Ứng dụng thực hiện các thao tác cơ sở dữ liệu.
5.  **Ứng dụng "đóng" kết nối**:
    -   Ứng dụng gọi `connection.close()`.
    -   **Quan trọng**: Lời gọi này không thực sự đóng kết nối vật lý. Thay vào đó, nó chỉ là một tín hiệu để trả kết nối về lại cho pool, đánh dấu nó là "rảnh rỗi" và sẵn sàng cho lần sử dụng tiếp theo.

### Lợi ích

-   **Tăng hiệu năng đáng kể**: Loại bỏ chi phí của việc tạo và đóng kết nối cho mỗi request.
-   **Tái sử dụng tài nguyên**: Các kết nối được tái sử dụng, giúp giảm tải cho cả máy chủ ứng dụng và máy chủ cơ sở dữ liệu.
-   **Quản lý tài nguyên tốt hơn**: Connection pool cho phép bạn kiểm soát và giới hạn số lượng kết nối đồng thời đến cơ sở dữ liệu, giúp ngăn ngừa tình trạng quá tải.

Tomcat cung cấp một thư viện connection pool tích hợp sẵn (Tomcat JDBC Connection Pool), rất hiệu quả và dễ cấu hình.
