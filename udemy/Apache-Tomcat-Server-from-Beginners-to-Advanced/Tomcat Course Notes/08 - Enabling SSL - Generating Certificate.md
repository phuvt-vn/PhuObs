# Bài 8: Kích hoạt SSL - Tạo Chứng chỉ (Certificate)

- **Thời lượng**: 02:49
- **Bài trước**: [[07 - Security Overview]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[09 - Enabling SSL - Part2]]

---

## Nội dung

Đây là phần đầu tiên trong chuỗi bài học về cách kích hoạt SSL/TLS (HTTPS) trên Tomcat. Bài này tập trung vào việc tạo ra một **chứng chỉ tự ký (self-signed certificate)** bằng công cụ `keytool` của Java.

### Tại sao cần Chứng chỉ (Certificate)?

-   SSL/TLS hoạt động dựa trên cơ sở hạ tầng khóa công khai (PKI).
-   Một chứng chỉ số (digital certificate) liên kết một danh tính (ví dụ: tên miền của bạn) với một cặp khóa công khai/khóa riêng.
-   Trình duyệt sử dụng chứng chỉ của máy chủ để xác thực rằng nó đang giao tiếp với đúng máy chủ mà nó muốn và để thiết lập một kênh truyền thông được mã hóa.

### Sử dụng `keytool` để tạo Keystore và Chứng chỉ

`keytool` là một tiện ích dòng lệnh đi kèm với JDK, dùng để quản lý các khóa (keys) và chứng chỉ. Chúng được lưu trữ trong một file gọi là **keystore**.

**Lệnh để tạo một keystore và một cặp khóa mới:**

```bash
keytool -genkey -alias tomcat -keyalg RSA -keystore C:\path\to\your\keystore.jks
```

**Giải thích các tham số:**

-   `-genkey`: Chỉ định hành động là tạo một cặp khóa (khóa riêng và khóa công khai).
-   `-alias tomcat`: `tomcat` là một tên định danh (bí danh) cho cặp khóa này bên trong keystore. Bạn có thể cần đến alias này khi cấu hình Tomcat.
-   `-keyalg RSA`: Chỉ định thuật toán để tạo khóa là RSA. Đây là thuật toán phổ biến và an toàn.
-   `-keystore C:\path\to\your\keystore.jks`: Chỉ định đường dẫn và tên file keystore sẽ được tạo. File này sẽ chứa khóa riêng và chứng chỉ của bạn.

**Quá trình thực thi lệnh:**

1.  **Mật khẩu Keystore**: `keytool` sẽ yêu cầu bạn nhập và xác nhận mật khẩu cho keystore. Mật khẩu này dùng để bảo vệ toàn bộ file keystore. **Hãy nhớ mật khẩu này!**
2.  **Thông tin định danh (Distinguished Name - DN)**:
    -   **What is your first and last name?**: Đây là phần quan trọng nhất. Bạn nên nhập **tên miền đầy đủ (Fully Qualified Domain Name - FQDN)** của máy chủ của bạn (ví dụ: `www.example.com` hoặc `localhost` nếu chỉ dùng cho môi trường phát triển).
    -   **What is the name of your organizational unit?**: Tên đơn vị tổ chức (ví dụ: IT Department).
    -   **What is the name of your organization?**: Tên tổ chức/công ty (ví dụ: My Company).
    -   **What is the name of your City or Locality?**: Tên thành phố (ví dụ: Hanoi).
    -   **What is the name of your State or Province?**: Tên tỉnh/bang.
    -   **What is the two-letter country code for this unit?**: Mã quốc gia gồm 2 chữ cái (ví dụ: VN).
3.  **Xác nhận**: `keytool` sẽ hiển thị lại thông tin bạn đã nhập và hỏi xác nhận. Nhập `yes` nếu tất cả đều đúng.
4.  **Mật khẩu khóa (Key password)**: `keytool` sẽ yêu cầu mật khẩu riêng cho cặp khóa `tomcat`. Bạn có thể nhấn **Enter** để sử dụng lại mật khẩu của keystore cho đơn giản.

Sau khi hoàn tất, file `keystore.jks` sẽ được tạo tại đường dẫn bạn đã chỉ định. File này đã sẵn sàng để được sử dụng trong cấu hình SSL của Tomcat.

> **Lưu ý**: Chứng chỉ tự ký (self-signed) không được các trình duyệt tin cậy mặc định và sẽ hiển thị cảnh báo bảo mật. Nó phù hợp cho môi trường phát triển (development) hoặc môi trường nội bộ. Đối với môi trường sản xuất (production), bạn nên sử dụng chứng chỉ được cấp bởi một Tổ chức phát hành chứng chỉ (Certificate Authority - CA) đáng tin cậy.
