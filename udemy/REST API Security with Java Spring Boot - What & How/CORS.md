# CORS (Cross-Origin Resource Sharing)

## 1. Khái niệm

**CORS (Cross-Origin Resource Sharing)** là một cơ chế bảo mật được tích hợp sẵn trong trình duyệt web để kiểm soát việc một trang web ở một domain (`origin A`) có được phép yêu cầu tài nguyên từ một domain khác (`origin B`) hay không.

Nó được sinh ra để nới lỏng **Chính sách Cùng Nguồn gốc (Same-Origin Policy)** một cách có kiểm soát.

### Vấn đề: Same-Origin Policy (SOP)

Theo mặc định, trình duyệt web tuân thủ SOP, một quy tắc bảo mật nền tảng. Quy tắc này chặn các đoạn script trên một trang web thực hiện yêu cầu đến một nguồn gốc khác. Một "nguồn gốc" (origin) được định nghĩa bởi ba thành phần: **lược đồ (protocol), tên miền (domain), và cổng (port).**

-   `https://myapp.com/page` có thể gọi `https://myapp.com/api`. (Cùng nguồn gốc)
-   `http://myapp.com` **KHÔNG** thể gọi `https://myapp.com`. (Khác lược đồ)
-   `https://www.myapp.com` **KHÔNG** thể gọi `https://api.myapp.com`. (Khác tên miền)
-   `https://myapp.com` **KHÔNG** thể gọi `https://myapp.com:8080`. (Khác cổng)

SOP là một cơ chế bảo vệ quan trọng, ngăn chặn các trang web độc hại đọc dữ liệu từ các trang web khác mà bạn đã đăng nhập.

### Giải pháp: CORS

CORS cho phép server ở `origin B` thông báo cho trình duyệt rằng "Tôi cho phép các yêu cầu từ `origin A`". Điều này được thực hiện thông qua các HTTP header đặc biệt.

### Cách thức hoạt động: Preflight Request

Đối với các yêu cầu "phức tạp" (ví dụ: dùng phương thức `PUT`, `DELETE`, hoặc có header tùy chỉnh như `Authorization`), trình duyệt sẽ tự động gửi một yêu cầu "tiền kiểm" (preflight request) trước khi gửi yêu cầu thật.

1.  **Preflight Request (Yêu cầu tiền kiểm):**
    *   Trình duyệt gửi một request với phương thức `OPTIONS` đến server.
    *   Request này hỏi server: "Này server, tôi sắp gửi một request `PUT` với header `Authorization` từ domain `https://my-frontend.com`. Anh có cho phép không?"
2.  **Preflight Response (Phản hồi tiền kiểm):**
    *   Server nhận request `OPTIONS` và trả lời bằng các header CORS:
        *   `Access-Control-Allow-Origin: https://my-frontend.com` (OK, tôi cho phép domain này)
        *   `Access-Control-Allow-Methods: GET, POST, PUT, DELETE` (OK, tôi cho phép phương thức PUT)
        *   `Access-Control-Allow-Headers: Authorization, Content-Type` (OK, tôi cho phép header này)
3.  **Actual Request (Yêu cầu thật):**
    *   Nếu phản hồi tiền kiểm là "OK", trình duyệt sẽ tiến hành gửi yêu cầu `PUT` thật sự.
    *   Nếu không, trình duyệt sẽ chặn yêu cầu và báo lỗi CORS trên console.

## 2. Các Header CORS quan trọng

-   `Access-Control-Allow-Origin`: Chỉ định các domain được phép. Có thể là `*` (không khuyến khích) hoặc một domain cụ thể.
-   `Access-Control-Allow-Methods`: Các phương thức HTTP được phép (`GET`, `POST`, `PUT`, ...).
-   `Access-Control-Allow-Headers`: Các header tùy chỉnh được phép trong request.
-   `Access-Control-Allow-Credentials`: Cho biết có cho phép gửi thông tin xác thực (như cookie, token) hay không.
-   `Access-Control-Max-Age`: Thời gian (tính bằng giây) mà trình duyệt có thể cache kết quả của preflight request.

## 3. Liên kết với các chủ đề bảo mật khác

-   **[[XSS]]**: CORS giúp giảm thiểu rủi ro từ các cuộc tấn công XSS xuyên trang, vì script độc hại trên một trang không thể tùy tiện gọi API trên trang của bạn.
-   **[[HTTPS Importance]]**: CORS header cần được bảo vệ bằng HTTPS để tránh bị kẻ tấn công ở giữa (Man-in-the-Middle) sửa đổi.
-   **[[JWT]] / [[API Key]]**: CORS đảm bảo các thông tin xác thực như JWT hoặc API Key chỉ được gửi từ các frontend đáng tin cậy đến các API endpoint được phép.

## 4. Hướng dẫn triển khai với Spring Boot 3.x

Spring Boot cung cấp nhiều cách để cấu hình CORS, nhưng cách tiếp cận toàn cục và an toàn nhất là sử dụng `CorsConfigurationSource`.

### 4.1. Cấu hình CORS toàn cục

Đây là cách tiếp cận được khuyến nghị, giúp bạn quản lý CORS tại một nơi duy nhất và tích hợp liền mạch với Spring Security.

```java
// File: config/GlobalCorsConfig.java
@Configuration
public class GlobalCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Chỉ định các domain được phép.
        // KHÔNG BAO GIỜ dùng "*" trong môi trường production nếu có allowCredentials.
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",      // Cho frontend dev
            "https://app.yourdomain.com"  // Cho frontend production
        ));
        
        // Các phương thức HTTP được phép
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Các header được phép gửi trong request
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-API-Key"));
        
        // Cho phép trình duyệt gửi thông tin xác thực (cookies, tokens)
        configuration.setAllowCredentials(true);
        
        // Thời gian cache kết quả preflight (ví dụ: 1 giờ)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Áp dụng cấu hình này cho tất cả các đường dẫn
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
```

### 4.2. Tích hợp với Spring Security

Sau khi đã có bean `CorsConfigurationSource`, bạn chỉ cần "bật" nó trong cấu hình Spring Security.

```java
// File: config/SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            // Kích hoạt CORS bằng cách sử dụng bean đã cấu hình
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // Tắt CSRF nếu bạn dùng API stateless (ví dụ: với JWT)
            .csrf(csrf -> csrf.disable())
            
            // Các cấu hình bảo mật khác...
            .authorizeHttpRequests(authz -> authz
                .anyRequest().authenticated()
            );
            
        return http.build();
    }
}
```

### 4.3. Cấu hình theo môi trường

Để linh hoạt hơn, bạn có thể đọc danh sách các domain được phép từ file `application.yml`.

**`application-dev.yml` (Môi trường phát triển):**
```yaml
cors:
  allowed-origins: http://localhost:3000,http://localhost:8080
```

**`application-prod.yml` (Môi trường sản phẩm):**
```yaml
cors:
  allowed-origins: https://app.yourdomain.com,https://admin.yourdomain.com
```

**Đọc giá trị trong lớp Config:**
```java
@Configuration
public class GlobalCorsConfig {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        // ... các cấu hình khác
        // ...
        return ...;
    }
}
```

## 5. Best Practices và Xử lý lỗi

### 5.1. Các thực hành tốt nhất (Best Practices)

1.  **Không dùng `*` cho `allowedOrigins`**: Luôn chỉ định danh sách các domain cụ thể được phép. Việc dùng `*` sẽ vô hiệu hóa `allowCredentials` và mở toang cửa cho mọi trang web.
2.  **Giới hạn `allowedMethods` và `allowedHeaders`**: Chỉ cho phép những gì thực sự cần thiết.
3.  **Cache Preflight**: Sử dụng `setMaxAge` để giảm số lượng request `OPTIONS` không cần thiết, cải thiện hiệu năng.
4.  **Luôn dùng HTTPS**: Đảm bảo tất cả các origin được phép đều sử dụng HTTPS trong môi trường production.
5.  **Ghi log và Giám sát**: Ghi lại các yêu cầu bị từ chối do vi phạm CORS để phát hiện các nỗ lực tấn công.

### 5.2. Xử lý lỗi thường gặp

-   **Lỗi "No 'Access-Control-Allow-Origin' header"**:
    -   **Nguyên nhân**: Request đến từ một origin không có trong danh sách `allowedOrigins` của bạn.
    -   **Giải pháp**: Kiểm tra lại chuỗi origin trong cấu hình, đảm bảo nó khớp chính xác với origin mà trình duyệt gửi lên (bao gồm cả protocol và port).
-   **Lỗi khi `allowCredentials(true)`**:
    -   **Nguyên nhân**: Bạn không thể sử dụng `allowedOrigins("*")` khi `allowCredentials` là `true`.
    -   **Giải pháp**: Phải chỉ định một hoặc nhiều domain cụ thể.
-   **Lỗi Header không được phép**:
    -   **Nguyên nhân**: Frontend đang gửi một header tùy chỉnh (ví dụ: `X-Tenant-ID`) mà backend chưa cho phép trong `allowedHeaders`.
    -   **Giải pháp**: Bổ sung header đó vào danh sách `allowedHeaders`.

## 6. Testing

Kiểm thử cấu hình CORS là rất quan trọng để đảm bảo nó hoạt động đúng như mong đợi.

```java
@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void whenPreflightFromAllowedOrigin_thenReturnsOk() throws Exception {
        mockMvc.perform(options("/api/some-endpoint")
                .header("Origin", "http://localhost:3000") // Origin hợp lệ
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
    
    @Test
    void whenPreflightFromDisallowedOrigin_thenReturnsForbidden() throws Exception {
        mockMvc.perform(options("/api/some-endpoint")
                .header("Origin", "http://malicious-site.com") // Origin không hợp lệ
                .header("Access-Control-Request-Method", "GET"))
                // Spring Security mặc định trả về 403 Forbidden cho lỗi CORS
                .andExpect(status().isForbidden());
    }
    
    @Test
    void whenActualRequestFromAllowedOrigin_thenReturnsOk() throws Exception {
        // Giả sử /api/hello là một endpoint được bảo vệ
        mockMvc.perform(get("/api/hello")
                .header("Origin", "http://localhost:3000"))
                // Expect 401 vì chưa có auth, nhưng header CORS vẫn phải có
                .andExpect(status().isUnauthorized()) 
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}
