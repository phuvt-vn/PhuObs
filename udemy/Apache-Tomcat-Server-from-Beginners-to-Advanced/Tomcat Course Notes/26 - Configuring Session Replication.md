# Bài 26: Cấu hình Session Replication

- **Thời lượng**: 10:21
- **Bài trước**: [[25 - Session Replication - Concept]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[27 - Understanding JNDI - Concept]]

---

## Nội dung

Bài học này hướng dẫn cách cấu hình session replication trong một cụm Tomcat bằng cách sử dụng `DeltaManager`. Cấu hình này được thực hiện trong file `server.xml` trên tất cả các node của cụm.

### Điều kiện tiên quyết

-   Một cụm Tomcat đã được thiết lập và đang hoạt động.
-   Ứng dụng web đã được đánh dấu `<distributable/>` trong `web.xml`.
-   Tất cả các đối tượng lưu trong session phải implement `java.io.Serializable`.

### Cấu hình `server.xml`

Mở file `server.xml` trên mỗi node và tìm đến khối `<Cluster>`. Bạn cần thêm một thẻ `<Manager>` vào bên trong nó.

```xml
<Engine name="Catalina" defaultHost="localhost" jvmRoute="node1">

    <Cluster className="org.apache.catalina.ha.tcp.SimpleTcpCluster">

        <!-- Cấu hình Manager cho Session Replication -->
        <Manager className="org.apache.catalina.ha.session.DeltaManager"
                 expireSessionsOnShutdown="false"
                 notifyListenersOnReplication="true"/>
        
        <Channel className="org.apache.catalina.tribes.group.GroupChannel">
            <!-- ... cấu hình Channel, Membership, Receiver, Sender ... -->
        </Channel>

        <Valve className="org.apache.catalina.ha.tcp.ReplicationValve" 
               filter=""/>
               
        <ClusterListener className="org.apache.catalina.ha.session.ClusterSessionListener"/>
        
    </Cluster>
    
    ...
</Engine>
```

**Giải thích các thành phần chính:**

1.  **`<Manager>`**:
    -   `className="org.apache.catalina.ha.session.DeltaManager"`: Chỉ định rằng chúng ta muốn sử dụng `DeltaManager`. Đây là trình quản lý mặc định nếu bạn không chỉ định gì khác.
    -   `expireSessionsOnShutdown="false"`: Khi một node tắt một cách bình thường, các session của nó sẽ không bị hết hạn trên các node khác. Các node khác sẽ tiếp quản chúng.
    -   `notifyListenersOnReplication="true"`: Đảm bảo rằng các `HttpSessionListener` trong ứng dụng của bạn sẽ được kích hoạt khi có các thay đổi session được sao chép từ các node khác.

2.  **`<Valve>`**:
    -   `className="org.apache.catalina.ha.tcp.ReplicationValve"`: Valve này là một "interceptor" (bộ chặn). Nó chặn các request sau khi chúng được xử lý. Nếu có bất kỳ thay đổi nào trong session, valve này sẽ kích hoạt quá trình sao chép (replication).
    -   `filter=""`: Thuộc tính `filter` cho phép bạn chỉ định các mẫu URL (dạng regex) để loại trừ khỏi việc kiểm tra sao chép. Ví dụ, bạn có thể không muốn kích hoạt việc kiểm tra sao chép cho các request đến các file tĩnh (hình ảnh, css). Để trống có nghĩa là tất cả các request sẽ được kiểm tra.

3.  **`<ClusterListener>`**:
    -   `className="org.apache.catalina.ha.session.ClusterSessionListener"`: Listener này chịu trách nhiệm lắng nghe các thông điệp từ các thành viên khác trong cụm và xử lý chúng, ví dụ như tạo hoặc hết hạn một session đã được sao chép.

### Kiểm tra Session Replication

1.  **Thiết lập môi trường**:
    -   Sử dụng một cụm gồm 2 node (Node 1 và Node 2).
    -   Sử dụng một bộ cân bằng tải với **sticky sessions đã được bật**.
2.  **Bắt đầu kịch bản**:
    -   Truy cập ứng dụng qua bộ cân bằng tải. Sticky session sẽ "gắn" bạn vào một node, ví dụ **Node 1**.
    -   Thực hiện một hành động để tạo hoặc thay đổi dữ liệu session (ví dụ: đăng nhập, thêm sản phẩm vào giỏ hàng). Tại thời điểm này, dữ liệu session được tạo trên Node 1 và được sao chép sang Node 2.
3.  **Mô phỏng lỗi**:
    -   **Tắt Node 1** một cách đột ngột (ví dụ: dùng `kill -9` trên Linux, hoặc đóng cửa sổ console trên Windows).
4.  **Quan sát kết quả**:
    -   Tải lại trang trong trình duyệt.
    -   Bộ cân bằng tải sẽ phát hiện Node 1 bị lỗi và tự động chuyển request của bạn sang **Node 2**.
    -   Vì dữ liệu session đã được sao chép, Node 2 có đầy đủ thông tin về session của bạn. Bạn sẽ thấy mình vẫn đang đăng nhập, giỏ hàng vẫn còn nguyên. Trải nghiệm người dùng không bị gián đoạn.

Nếu kịch bản trên thành công, bạn đã cấu hình thành công tính năng session replication, đạt được tính sẵn sàng cao cho ứng dụng của mình.
