# Bài 47: Kích hoạt Nén Phản hồi HTTP (HTTP Response Compression)

- **Thời lượng**: 05:14
- **Bài trước**: [[46 - Memory Optimization - Heap & Metaspace]]
- **[[_Index|Về lại mục lục]]**

---

## Nội dung

Một trong những cách đơn giản và hiệu quả nhất để cải thiện thời gian tải trang cho người dùng cuối là **nén (compress)** nội dung của các phản hồi HTTP trước khi gửi chúng qua mạng. Bài học này hướng dẫn cách kích hoạt tính năng nén GZIP trong Tomcat.

### Tại sao cần nén?

-   Các tài nguyên dựa trên văn bản (text-based) như HTML, CSS, JavaScript, JSON, và XML thường chứa rất nhiều sự lặp lại, khiến chúng có thể được nén lại với tỷ lệ rất cao.
-   Việc nén response làm **giảm kích thước dữ liệu** cần được truyền từ server đến trình duyệt của client.
-   Kích thước nhỏ hơn đồng nghĩa với thời gian truyền tải nhanh hơn, đặc biệt là đối với những người dùng có kết nối mạng chậm.
-   Kết quả là trang web của bạn sẽ được hiển thị nhanh hơn, cải thiện đáng kể trải nghiệm người dùng.

### Cách hoạt động

1.  Trình duyệt của client gửi một request đến server, kèm theo header `Accept-Encoding` để báo cho server biết các thuật toán nén mà nó hỗ trợ (ví dụ: `Accept-Encoding: gzip, deflate, br`).
2.  Tomcat nhận request. Nếu tính năng nén được kích hoạt, nó sẽ kiểm tra header này.
3.  Tomcat tạo ra response như bình thường.
4.  **Trước khi gửi đi**, Tomcat sẽ nén phần body của response bằng thuật toán được cả hai bên hỗ trợ (thường là GZIP).
5.  Tomcat thêm header `Content-Encoding: gzip` vào response để báo cho trình duyệt biết rằng nội dung đã được nén.
6.  Trình duyệt nhận response, thấy header `Content-Encoding` và tự động giải nén nội dung trước khi hiển thị cho người dùng.

### Cấu hình Nén trong Tomcat

Tính năng nén được cấu hình trực tiếp trên **Connector** trong file `server.xml`.

**Ví dụ cấu hình:**

```xml
<Connector port="8080" 
           protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443"

           -- -- Các thuộc tính nén -- --
           compression="on"
           compressionMinSize="2048"
           noCompressionUserAgents="gozilla, traviata"
           compressableMimeType="text/html,text/xml,text/plain,text/css,text/javascript,application/json,application/javascript" 
/>
```

**Giải thích các thuộc tính:**

-   **`compression="on"`**: Bật tính năng nén. Các giá trị khác:
    -   `off` (mặc định): Tắt nén.
    -   `force`: Bắt buộc nén ngay cả khi trình duyệt không gửi header `Accept-Encoding`. **Không nên dùng.**
-   **`compressionMinSize="2048"`**:
    -   Kích thước tối thiểu (tính bằng byte) của một response để nó được nén.
    -   Việc nén các response rất nhỏ có thể không hiệu quả, thậm chí còn làm tăng kích thước do overhead của việc nén.
    -   Giá trị `2048` (2 KB) là một điểm khởi đầu tốt.
-   **`noCompressionUserAgents="gozilla, traviata"`**:
    -   Một danh sách các User-Agent của các trình duyệt cũ hoặc có lỗi không xử lý đúng việc nén.
    -   Tomcat sẽ không nén response nếu User-Agent của client khớp với một trong các giá trị này.
-   **`compressableMimeType`**:
    -   **Quan trọng**: Đây là danh sách các kiểu MIME (MIME types) mà Tomcat sẽ nén.
    -   Bạn chỉ nên nén các loại nội dung dựa trên văn bản.
    -   **Không bao giờ nén** các định dạng đã được nén sẵn như hình ảnh (JPEG, PNG), video (MP4), hay file PDF, vì việc nén lại sẽ không có tác dụng, chỉ lãng phí CPU.

### Lưu ý

-   Việc nén sẽ tiêu tốn thêm một ít tài nguyên CPU trên server. Tuy nhiên, trong hầu hết các trường hợp, lợi ích về tốc độ truyền tải vượt xa chi phí CPU này, vì I/O mạng thường là nút thắt cổ chai lớn hơn nhiều so với CPU.
-   Nếu bạn đang sử dụng một reverse proxy (như Nginx) ở phía trước Tomcat, bạn cũng có thể cấu hình việc nén ở tầng proxy. Việc này có thể giúp giảm tải cho Tomcat. Tuy nhiên, việc cấu hình trực tiếp trên Tomcat vẫn là một lựa chọn hoàn toàn hợp lệ và hiệu quả.
