# Bài 23: Khái niệm về Sticky Sessions (Session bền vững)

- **Thời lượng**: 08:18
- **Bài trước**: [[22 - Distributing Traffic with Apache LB - Part2]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[24 - Configuring Sticky Sessions on Nginx & Apache LB]]

---

## Nội dung

Bài học này giải thích về khái niệm **Sticky Sessions** (còn gọi là **session affinity** hoặc **session persistence**) và tại sao nó lại quan trọng trong một môi trường cân bằng tải.

### Vấn đề với Cân bằng tải và Session

-   Khi một người dùng bắt đầu một phiên làm việc (session) với ứng dụng của bạn (ví dụ: sau khi đăng nhập, thêm hàng vào giỏ), thông tin về session đó được lưu trữ trong bộ nhớ của một node Tomcat cụ thể.
-   Trong một môi trường cân bằng tải (ví dụ: Round Robin), request tiếp theo của cùng người dùng đó có thể được bộ cân bằng tải chuyển đến một node Tomcat khác.
-   Node Tomcat mới này không hề biết gì về session của người dùng (vì session đó đang nằm trên node ban đầu). Kết quả là người dùng sẽ bị mất session (ví dụ: bị yêu cầu đăng nhập lại, giỏ hàng trống rỗng).

### Giải pháp: Sticky Sessions

-   **Sticky Sessions** là một tính năng của bộ cân bằng tải, đảm bảo rằng tất cả các request từ một người dùng cụ thể (trong cùng một session) sẽ luôn được chuyển đến **cùng một node Tomcat**.
-   Bộ cân bằng tải "gắn" (sticks) session của người dùng vào một node cụ thể.

### Cách hoạt động

1.  **Request đầu tiên**: Khi một người dùng gửi request đầu tiên, bộ cân bằng tải sẽ chọn một node Tomcat (ví dụ: Node 1) để xử lý và chuyển request đến đó.
2.  **Tomcat tạo Session**: Node 1 tạo một session mới cho người dùng và trả về một Session ID cho trình duyệt (thường qua cookie, ví dụ `JSESSIONID`).
3.  **Định danh Node trong Session ID**: Điều quan trọng là Tomcat (khi được cấu hình trong cụm) sẽ tự động thêm **tên định danh của node (`jvmRoute`)** vào cuối Session ID.
    -   Ví dụ: Session ID có thể trông như thế này: `A1B2C3D4E5F6.node1`.
4.  **Các request tiếp theo**:
    -   Người dùng gửi request tiếp theo, kèm theo cookie `JSESSIONID=A1B2C3D4E5F6.node1`.
    -   Bộ cân bằng tải (Nginx hoặc Apache với Mod_Jk) sẽ đọc Session ID này.
    -   Nó nhận ra hậu tố `.node1` và biết rằng session này thuộc về `node1`.
    -   Thay vì chọn một node ngẫu nhiên, nó sẽ chuyển tiếp request này thẳng đến `node1`.

### Ưu và Nhược điểm

#### Ưu điểm:

-   **Đơn giản**: Dễ cấu hình trên bộ cân bằng tải.
-   **Hiệu quả**: Không cần phải sao chép dữ liệu session giữa các node, giúp tiết kiệm băng thông mạng và tài nguyên CPU. Dữ liệu session chỉ tồn tại trên một node.

#### Nhược điểm:

-   **Không có tính sẵn sàng cao (No High Availability)**: Nếu node mà người dùng đang "gắn" vào bị lỗi, session của người dùng đó sẽ bị mất. Bộ cân bằng tải sẽ chuyển họ sang một node khác, nhưng node mới này không có dữ liệu session của họ.
-   **Phân phối tải không đều**: Nếu một số người dùng có session "nặng" (sử dụng nhiều tài nguyên) và tất cả họ đều bị "gắn" vào cùng một node, node đó có thể bị quá tải trong khi các node khác lại rảnh rỗi.

### Khi nào nên sử dụng?

-   Sticky Sessions là một giải pháp tốt cho nhiều ứng dụng, đặc biệt là khi việc mất session không quá nghiêm trọng hoặc khi hiệu năng là ưu tiên hàng đầu.
-   Để khắc phục nhược điểm về tính sẵn sàng cao, Sticky Sessions thường được kết hợp với **Session Replication**, sẽ được đề cập trong các bài sau.
