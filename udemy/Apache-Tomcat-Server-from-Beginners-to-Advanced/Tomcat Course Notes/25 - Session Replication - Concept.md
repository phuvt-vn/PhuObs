# Bài 25: Khái niệm về Session Replication (Đồng bộ hóa Session)

- **Thời lượng**: 10:01
- **Bài trước**: [[24 - Configuring Sticky Sessions on Nginx & Apache LB]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[26 - Configuring Session Replication]]

---

## Nội dung

Bài học này giới thiệu về **Session Replication**, một cơ chế quan trọng để đạt được **tính sẵn sàng cao thực sự (true high availability)** cho session của người dùng trong một cụm Tomcat.

### Vấn đề của Sticky Sessions

Như đã thảo luận ở [[Bài 23]], Sticky Sessions có một nhược điểm lớn: nếu node Tomcat mà người dùng đang được "gắn" vào bị lỗi, session của người dùng đó sẽ bị mất hoàn toàn. Người dùng sẽ được chuyển hướng đến một node khác đang hoạt động, nhưng phải bắt đầu một session mới (ví dụ: phải đăng nhập lại).

### Giải pháp: Session Replication

-   **Session Replication** là quá trình sao chép (copy) dữ liệu session từ node này sang các node khác trong cụm.
-   Mục tiêu là để mỗi node trong cụm đều có một bản sao của session, hoặc ít nhất là có một bản sao dự phòng ở một nơi khác.
-   Khi một node bị lỗi, bộ cân bằng tải sẽ chuyển người dùng sang một node khác. Node mới này đã có sẵn dữ liệu session của người dùng, vì vậy nó có thể tiếp tục phục vụ người dùng một cách liền mạch mà không làm mất trạng thái của họ.

### Cách hoạt động

1.  Khi một thuộc tính trong session được tạo, cập nhật hoặc xóa trên một node (ví dụ: `session.setAttribute("username", "cline")` trên Node 1), Tomcat sẽ "bắt" sự kiện thay đổi này.
2.  Nó sẽ đóng gói (serialize) sự thay đổi đó (hoặc toàn bộ session) thành một gói tin.
3.  Gói tin này sau đó được gửi qua mạng đến một hoặc tất cả các node khác trong cụm.
4.  Các node nhận được sẽ giải nén (deserialize) gói tin và cập nhật bản sao session của chúng.

### Các loại Manager trong Tomcat Cluster

Tomcat cung cấp các trình quản lý (Manager) khác nhau để thực hiện việc đồng bộ hóa session. Hai loại chính là:

1.  **`DeltaManager` (Quản lý thay đổi - Delta)**
    -   **Cách hoạt động**: Chỉ gửi những thay đổi (deltas) trong session đến tất cả các node khác trong cụm. Ví dụ, nếu bạn chỉ thay đổi một thuộc tính, chỉ có thuộc tính đó được gửi đi.
    -   **Ưu điểm**: Có thể nhanh hơn nếu session lớn nhưng chỉ có ít thay đổi.
    -   **Nhược điểm**: Lượng lưu thông mạng (network traffic) tăng theo cấp số nhân với số lượng node (`N*(N-1)` tin nhắn cho mỗi thay đổi). Do đó, nó **không được khuyến nghị cho các cụm có nhiều hơn 3-4 node**.

2.  **`BackupManager` (Quản lý dự phòng)**
    -   **Cách hoạt động**: Chỉ gửi toàn bộ dữ liệu session đến **một node dự phòng (backup node)** duy nhất. Các node khác không nhận được bản sao.
    -   **Ưu điểm**: Hiệu quả hơn về mặt mạng so với `DeltaManager` vì dữ liệu chỉ được gửi đến một nơi. Phù hợp hơn cho các cụm lớn.
    -   **Nhược điểm**: Nếu cả node chính và node dự phòng của một session đều bị lỗi cùng lúc (hiếm nhưng có thể xảy ra), session đó vẫn sẽ bị mất.

**Lưu ý quan trọng**:
-   Tất cả các đối tượng được lưu trong session (ví dụ: các đối tượng User, Product) phải implement interface `java.io.Serializable` để Tomcat có thể đóng gói và gửi chúng qua mạng.
-   Ứng dụng web phải được đánh dấu là `<distributable/>` trong `web.xml`.

Trong bài học tiếp theo, chúng ta sẽ cấu hình `DeltaManager` để xem session replication hoạt động trong thực tế.
