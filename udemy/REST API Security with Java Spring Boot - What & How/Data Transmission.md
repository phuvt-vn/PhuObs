# Data Transmission Security (Bảo mật trong quá trình truyền dữ liệu)

## 1. Khái niệm

**Data Transmission Security** là tập hợp các biện pháp kỹ thuật nhằm bảo vệ dữ liệu khi nó được truyền từ điểm này đến điểm khác qua mạng (ví dụ: từ trình duyệt của người dùng đến server).

**Tưởng tượng đơn giản:**
- **Không bảo mật (HTTP):** Gửi một tấm bưu thiếp. Bất kỳ ai trên đường đi cũng có thể đọc được nội dung.
- **Có bảo mật (HTTPS):** Đặt thư vào một phong bì đã niêm phong. Chỉ người nhận có "chìa khóa" phù hợp mới mở được. Nếu niêm phong bị rách, bạn biết thư đã bị can thiệp.

### Ba trụ cột của bảo mật truyền dữ liệu:

1.  **Encryption (Mã hóa):** Dữ liệu được xáo trộn để không thể đọc được nếu không có khóa giải mã.
2.  **Integrity (Toàn vẹn):** Đảm bảo dữ liệu không bị thay đổi trên đường truyền, thường dùng thuật toán băm (hashing).
3.  **Authentication (Xác thực):** Server chứng minh danh tính của mình cho client (và ngược lại) để đảm bảo bạn đang nói chuyện với đúng người.

### Tại sao nó quan trọng?
-   **Bảo vệ dữ liệu nhạy cảm**: Ngăn lộ mật khẩu, thông tin cá nhân, tài chính.
-   **Chống tấn công Man-in-the-Middle (MITM)**: Ngăn kẻ tấn công xen vào giữa để đọc hoặc sửa đổi dữ liệu.
-   **Tuân thủ quy định**: Đáp ứng các tiêu chuẩn như GDPR, PCI-DSS.
-   **Tạo dựng lòng tin**: Người dùng và các trình duyệt hiện đại tin tưởng các trang web sử dụng HTTPS.

## 2. Liên kết với các chủ đề bảo mật khác
-   **[[HTTPS Importance]]**: Là nền tảng của bảo mật truyền dữ liệu trên web.
-   **[[Encode,Encrypt,Hash]]**: Cung cấp các công cụ cơ bản để xây dựng Data Transmission Security.
-   **[[API Key]]**, **[[JWT]]**: Các thông tin xác thực này phải được truyền qua một kênh đã mã hóa để tránh bị đánh cắp.

## 3. Hướng dẫn triển khai với Spring Boot 3.x

### 3.1. Bước 1: Tạo chứng chỉ SSL/TLS

Để bật HTTPS, bạn cần một chứng chỉ. Trong môi trường phát triển, ta có thể tự tạo một chứng chỉ tự ký (self-signed) bằng `keytool` của Java.

**Lệnh tạo chứng chỉ (dùng trong terminal/CMD):**
```bash
# Tạo một keystore định dạng PKCS12 với khóa RSA 4096-bit
keytool -genkeypair -alias myapp-dev -keyalg RSA -keysize 4096 -storetype PKCS12 -keystore keystore.p12 -validity 365 -dname "CN=localhost, OU=Dev, O=MyCompany, C=VN" -storepass changeit -keypass changeit
```
Lệnh này sẽ tạo ra file `keystore.p12` trong thư mục hiện tại. Hãy di chuyển nó vào `src/main/resources` của dự án Spring Boot.

> **Lưu ý:** Trong môi trường production, bạn phải sử dụng chứng chỉ được cấp bởi một Certificate Authority (CA) đáng tin cậy như Let's Encrypt (miễn phí) hoặc các CA thương mại khác.

### 3.2. Bước 2: Cấu hình HTTPS trong Spring Boot

Mở file `application.yml` và thêm cấu hình để Spring Boot sử dụng keystore bạn vừa tạo.

```yaml
# File: src/main/resources/application.yml
server:
  port: 8443 # Cổng HTTPS (cổng chuẩn là 443)
  ssl:
    enabled: true
    key-store-type: PKCS12
    key-store: classpath:keystore.p12 # Đường dẫn đến keystore trong classpath
    key-store-password: changeit       # Mật khẩu của keystore
    key-alias: myapp-dev               # Alias của key trong keystore
```

Sau khi cấu hình, khởi động lại ứng dụng. Bạn sẽ có thể truy cập nó qua `https://localhost:8443`.

### 3.3. Bước 3: Tự động chuyển hướng từ HTTP sang HTTPS

Để đảm bảo mọi truy cập đều được mã hóa, chúng ta sẽ cấu hình để server tự động chuyển hướng các yêu cầu từ cổng HTTP (ví dụ: 8080) sang cổng HTTPS (8443).

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
                securityConstraint.setUserConstraint("CONFIDENTIAL"); // Yêu cầu kênh bảo mật (HTTPS)
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*"); // Áp dụng cho tất cả các đường dẫn
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        
        // Thêm một connector cho cổng HTTP và chỉ định cổng redirect là HTTPS
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

## 4. Tăng cường bảo mật (Hardening)

### 4.1. Cấu hình TLS và Cipher Suites mạnh

Vô hiệu hóa các giao thức và bộ mã hóa cũ, yếu. Chỉ cho phép TLS 1.2 và 1.3 với các bộ mã hóa hiện đại.

Thêm vào `application.yml`:
```yaml
server:
  ssl:
    # ... các cấu hình cũ ...
    protocol: TLS
    enabled-protocols: TLSv1.3, TLSv1.2 # Chỉ cho phép TLS 1.2 và 1.3
    ciphers: >- # Dấu >- cho phép viết trên nhiều dòng
      TLS_AES_256_GCM_SHA384,
      TLS_CHACHA20_POLY1305_SHA256,
      TLS_AES_128_GCM_SHA256,
      TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
      TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
```

### 4.2. Thêm các Security Header quan trọng

Sử dụng Spring Security để thêm các header bảo mật, đặc biệt là HSTS.

```java
// File: config/SecurityHeadersConfig.java
@Configuration
@EnableWebSecurity
public class SecurityHeadersConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .headers(headers -> headers
                // HTTP Strict Transport Security (HSTS)
                // Bắt buộc trình duyệt phải giao tiếp qua HTTPS trong tương lai
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)  // 1 năm
                    .includeSubdomains(true)
                )
                // X-Content-Type-Options: Ngăn trình duyệt tự ý đoán kiểu content
                .contentTypeOptions(Customizer.withDefaults())
                // X-Frame-Options: Ngăn chặn tấn công clickjacking
                .frameOptions(frameOptions -> frameOptions.deny())
                // Content-Security-Policy (CSP): Kiểm soát nguồn tài nguyên được phép tải
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
            );
            
        // ... các cấu hình khác ...
        return http.build();
    }
}
```

## 5. Testing và Monitoring

### 5.1. Kiểm tra cấu hình

Sử dụng các công cụ online như **SSL Labs SSL Test** hoặc các công cụ dòng lệnh để kiểm tra cấu hình của bạn.

```bash
# Kiểm tra phiên bản TLS và các cipher được hỗ trợ
nmap --script ssl-enum-ciphers -p 8443 localhost

# Kiểm tra header HSTS
curl -s -D- https://localhost:8443/ | grep -i "Strict-Transport-Security"
```

### 5.2. Health Check cho chứng chỉ

Tạo một `HealthIndicator` để tự động kiểm tra ngày hết hạn của chứng chỉ và cảnh báo khi nó sắp hết hạn.

```java
// File: monitoring/SslCertificateHealthIndicator.java
@Component
public class SslCertificateHealthIndicator implements HealthIndicator {
    // ... logic để kết nối đến server, lấy chứng chỉ và kiểm tra ngày hết hạn ...
    // ... trả về Health.up(), Health.down(), hoặc Health.up().withDetail("warning", "...") ...
}
```
Kích hoạt endpoint `/actuator/health` để có thể giám sát trạng thái này.

### 5.3. Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HttpsConfigurationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldHaveHstsHeader() {
        String url = "https://localhost:" + port + "/";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get("Strict-Transport-Security"))
            .isNotNull()
            .anyMatch(header -> header.contains("max-age=31536000"));
    }
}
