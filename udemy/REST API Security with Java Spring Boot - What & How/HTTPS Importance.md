# Tầm quan trọng của HTTPS

## 1. Khái niệm

**HTTPS (HyperText Transfer Protocol Secure)** là phiên bản bảo mật của giao thức HTTP. Nó không phải là một giao thức riêng biệt, mà là sự kết hợp của HTTP và một lớp mã hóa **SSL/TLS (Secure Sockets Layer/Transport Layer Security)**.

**Tưởng tượng đơn giản:**
- **HTTP:** Gửi một tấm bưu thiếp. Bất kỳ ai trên đường đi cũng có thể đọc được nội dung.
- **HTTPS:** Đặt thư vào một phong bì thép, khóa lại bằng một ổ khóa phức tạp. Chỉ người nhận có chìa khóa phù hợp mới mở được.

HTTPS đảm bảo ba yếu tố cốt lõi cho việc truyền dữ liệu trên web:
1.  **Confidentiality (Tính bí mật):** Dữ liệu được mã hóa, ngăn chặn kẻ tấn công nghe lén (eavesdropping).
2.  **Integrity (Tính toàn vẹn):** Dữ liệu không thể bị thay đổi trên đường truyền mà không bị phát hiện.
3.  **Authentication (Tính xác thực):** Client có thể xác minh rằng mình đang nói chuyện với đúng server mà nó muốn kết nối, không phải một kẻ mạo danh (chống lại tấn công Man-in-the-Middle).

## 2. Tại sao HTTPS là bắt buộc?

Trong thế giới hiện đại, việc sử dụng HTTPS không còn là một "lựa chọn" mà là một **yêu cầu bắt buộc** đối với hầu hết các ứng dụng web.

-   **Bảo vệ dữ liệu người dùng:** Mọi thông tin nhạy cảm như mật khẩu, thông tin cá nhân, thẻ tín dụng đều được bảo vệ an toàn.
-   **Tạo dựng lòng tin:** Các trình duyệt hiện đại (Chrome, Firefox) sẽ hiển thị cảnh báo "Không an toàn" (Not Secure) đối với các trang HTTP, làm mất lòng tin của người dùng.
-   **Yêu cầu của các công nghệ mới:** Nhiều tính năng web hiện đại như Progressive Web Apps (PWA), Service Workers, và giao thức HTTP/2 chỉ hoạt động trên HTTPS.
-   **Tuân thủ quy định:** Các tiêu chuẩn như PCI-DSS (cho thanh toán) và GDPR (bảo vệ dữ liệu cá nhân) đều yêu cầu mã hóa dữ liệu trên đường truyền.
-   **Tốt cho SEO:** Google và các công cụ tìm kiếm khác ưu tiên xếp hạng các trang web sử dụng HTTPS.

## 3. Liên kết với các chủ đề bảo mật khác
- **[[Data Transmission]]**: HTTPS là phương pháp triển khai chính cho việc bảo mật truyền dữ liệu.
- **[[Basic Authentication]], [[API Key]], [[JWT]]**: Mọi cơ chế xác thực đều trở nên vô nghĩa nếu thông tin xác thực (token, password) bị gửi qua HTTP và bị đánh cắp. HTTPS là lớp bảo vệ bắt buộc cho chúng.
- **[[CORS]]**: Các header CORS cũng cần được bảo vệ bởi HTTPS để đảm bảo chúng không bị giả mạo trên đường truyền.

## 4. Hướng dẫn triển khai HTTPS trong Spring Boot

### 4.1. Bước 1: Cấu hình SSL/TLS trong `application.yml`

Đây là bước cơ bản nhất để bật HTTPS. Bạn cần có một file `keystore` chứa chứng chỉ và khóa riêng của server.

```yaml
# File: src/main/resources/application.yml
server:
  port: 8443 # Cổng HTTPS
  ssl:
    enabled: true
    key-store: classpath:keystore.p12 # Đường dẫn đến file keystore
    key-store-password: changeit       # Mật khẩu của keystore
    key-alias: myapp-dev               # Tên định danh của key trong keystore
    key-store-type: PKCS12             # Định dạng keystore (PKCS12 là tiêu chuẩn)
```
> **Lưu ý:** Để tạo file `keystore.p12` cho môi trường phát triển, bạn có thể tham khảo hướng dẫn trong file [[Data Transmission]].

### 4.2. Bước 2: Tự động chuyển hướng từ HTTP sang HTTPS

Để đảm bảo người dùng luôn sử dụng kênh bảo mật, chúng ta nên tự động chuyển hướng mọi yêu cầu từ HTTP (cổng 8080) sang HTTPS (cổng 8443).

```java
// File: config/HttpsRedirectConfig.java
@Configuration
public class HttpsRedirectConfig {

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL"); // Yêu cầu kênh bảo mật
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*"); // Áp dụng cho mọi đường dẫn
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        
        tomcat.addAdditionalTomcatConnectors(createHttpConnector());
        return tomcat;
    }

    private Connector createHttpConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080); // Cổng HTTP
        connector.setSecure(false);
        connector.setRedirectPort(8443); // Cổng HTTPS để chuyển hướng tới
        return connector;
    }
}
```

### 4.3. Bước 3: Thêm các Security Header quan trọng

Sử dụng Spring Security để thêm các header giúp tăng cường bảo mật, đặc biệt là **HTTP Strict Transport Security (HSTS)**.

```java
// File: config/SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .headers(headers -> headers
                // HSTS: Bắt buộc trình duyệt phải giao tiếp qua HTTPS trong 1 năm tới
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
                // Ngăn chặn tấn công clickjacking
                .frameOptions(frameOptions -> frameOptions.deny())
                // Ngăn trình duyệt tự đoán kiểu content
                .contentTypeOptions(Customizer.withDefaults())
            );
            
        // ... các cấu hình khác ...
        return http.build();
    }
}
```

## 5. Best Practices và Giám sát

### 5.1. Best Practices

1.  **Sử dụng chứng chỉ từ CA đáng tin cậy:** Trong production, không dùng chứng chỉ tự ký. Hãy dùng Let's Encrypt (miễn phí) hoặc các CA thương mại.
2.  **Cấu hình Cipher Suites mạnh:** Vô hiệu hóa các thuật toán mã hóa cũ và yếu (SSLv3, TLS 1.0, 1.1). Chỉ dùng TLS 1.2 và 1.3.
3.  **Tự động gia hạn chứng chỉ:** Sử dụng các công cụ như `certbot` để tự động gia hạn chứng chỉ trước khi hết hạn.
4.  **Kiểm tra cấu hình thường xuyên:** Dùng các công cụ online như **SSL Labs SSL Test** để kiểm tra và đảm bảo cấu hình của bạn đạt điểm A+.

### 5.2. Giám sát ngày hết hạn chứng chỉ

Tạo một `HealthIndicator` để Spring Boot Actuator có thể giám sát và cảnh báo khi chứng chỉ sắp hết hạn.

```java
// File: monitoring/SslCertificateHealthIndicator.java
@Component
public class SslCertificateHealthIndicator implements HealthIndicator {
    
    // ... logic để đọc thông tin chứng chỉ từ keystore ...

    @Override
    public Health health() {
        // ... lấy ngày hết hạn của chứng chỉ ...
        long daysUntilExpiry = ...;

        if (daysUntilExpiry < 7) {
            return Health.down()
                .withDetail("message", "Certificate expires in " + daysUntilExpiry + " days!")
                .build();
        } else if (daysUntilExpiry < 30) {
            return Health.up()
                .withDetail("warning", "Certificate is expiring soon (" + daysUntilExpiry + " days).")
                .build();
        }
        
        return Health.up().withDetail("status", "Certificate is valid.").build();
    }
}
```
Sau đó, bạn có thể truy cập endpoint `/actuator/health` để xem trạng thái của chứng chỉ.
