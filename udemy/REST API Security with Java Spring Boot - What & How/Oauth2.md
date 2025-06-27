# OAuth 2.0 - Authorization Framework

## 1. Khái niệm

**OAuth 2.0** là một framework **ủy quyền (authorization)**, không phải là một giao thức xác thực (authentication). Nó cho phép một ứng dụng của bên thứ ba (gọi là **Client**) có được quyền truy cập hạn chế vào tài nguyên của người dùng trên một dịch vụ khác (gọi là **Resource Server**), mà không cần phải chia sẻ mật khẩu của người dùng.

**Ví dụ thực tế:**
Bạn muốn dùng một ứng dụng chỉnh sửa ảnh (Client) để đăng ảnh trực tiếp lên tài khoản Google Photos (Resource Server) của bạn.
- **Cách làm cũ (không an toàn):** Bạn đưa mật khẩu Google của mình cho ứng dụng chỉnh sửa ảnh. -> **Rất nguy hiểm!** Ứng dụng này có thể làm mọi thứ với tài khoản Google của bạn.
- **Cách làm với OAuth 2.0:**
    1. Ứng dụng chỉnh sửa ảnh chuyển hướng bạn đến trang đăng nhập của Google.
    2. Bạn đăng nhập vào Google và Google hỏi: "Ứng dụng chỉnh sửa ảnh muốn có quyền xem và tải ảnh lên Google Photos của bạn. Bạn có đồng ý không?"
    3. Nếu bạn đồng ý, Google sẽ cấp cho ứng dụng chỉnh sửa ảnh một "vé" (gọi là **Access Token**) có thời hạn.
    4. Ứng dụng chỉnh sửa ảnh dùng "vé" này để thực hiện đúng các hành động đã được cho phép. Nó hoàn toàn không biết mật khẩu Google của bạn.

### Các vai trò trong OAuth 2.0:

1.  **Resource Owner**: Là bạn, người dùng sở hữu dữ liệu.
2.  **Client**: Là ứng dụng chỉnh sửa ảnh, muốn truy cập dữ liệu của bạn.
3.  **Resource Server**: Là Google Photos, nơi lưu trữ dữ liệu của bạn.
4.  **Authorization Server**: Là hệ thống của Google, chịu trách nhiệm xác thực bạn và cấp "vé" (Access Token) cho Client.

### Các luồng cấp quyền (Grant Types) phổ biến:
- **Authorization Code Grant (with PKCE):** Luồng an toàn và phổ biến nhất, dành cho các ứng dụng web truyền thống và ứng dụng di động/desktop.
- **Client Credentials Grant:** Dành cho giao tiếp giữa các máy chủ (machine-to-machine), không có sự tham gia của người dùng.

## 2. Liên kết với các chủ đề bảo mật khác
- **[[JWT]]**: OAuth 2.0 thường sử dụng JWT làm định dạng cho Access Token.
- **[[HTTPS Importance]]**: Toàn bộ luồng OAuth 2.0 **bắt buộc** phải chạy trên HTTPS để đảm bảo an toàn.
- **[[CORS]]**: Cần cấu hình CORS trên Authorization Server để cho phép các Client (frontend) tương tác.
- **[[MFA]]**: Có thể tích hợp MFA vào bước đăng nhập của người dùng trên Authorization Server để tăng cường bảo mật.

## 3. Hướng dẫn triển khai với Spring Boot 3.x

Chúng ta sẽ xây dựng một hệ thống OAuth 2.0 hoàn chỉnh, bao gồm cả Authorization Server và Resource Server.

### 3.1. Bước 1: Setup Dependencies

Thêm các thư viện cần thiết vào `pom.xml`:
```xml
<!-- Spring Security và OAuth2 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

### 3.2. Bước 2: Xây dựng Authorization Server

Đây là server chịu trách nhiệm xác thực người dùng và cấp token.

**Cấu hình `AuthorizationServerConfig.java`:**
```java
@Configuration
public class AuthorizationServerConfig {

    // Cấu hình các Client được phép yêu cầu token
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("my-client")
            .clientSecret("{noop}secret") // {noop} chỉ dùng cho demo, production phải hash
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .redirectUri("http://127.0.0.1:8080/login/oauth2/code/my-client-oidc")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope("api.read")
            .scope("api.write")
            .build();

        return new InMemoryRegisteredClientRepository(registeredClient);
    }

    // Cấu hình khóa để ký JWT
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() { /* ... logic tạo key ... */ }

    // Cấu hình các endpoint của Authorization Server
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }
}
```

**Cấu hình bảo mật cho Authorization Server:**
```java
@Configuration
public class SecurityConfig {
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults()); // Bật OpenID Connect
        
        http.exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
        );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            .formLogin(Customizer.withDefaults()); // Trang đăng nhập mặc định
        return http.build();
    }
    
    // ... bean UserDetailsService và PasswordEncoder ...
}
```

### 3.3. Bước 3: Xây dựng Resource Server

Đây là server chứa các API được bảo vệ. Nó cần phải xác thực Access Token được gửi đến từ Client.

**Cấu hình `ResourceServerConfig.java`:**
```java
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**") // Chỉ áp dụng cho các đường dẫn /api/**
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/api/messages").hasAuthority("SCOPE_api.read")
                .requestMatchers(HttpMethod.POST, "/api/messages").hasAuthority("SCOPE_api.write")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        
        return http.build();
    }
}
```

**Cấu hình `application.yml` để Resource Server biết cách xác thực JWT:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Địa chỉ của Authorization Server để lấy public key
          jwk-set-uri: http://auth-server:9000/oauth2/jwks 
```

### 3.4. API Controller trên Resource Server
```java
@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/messages")
    public String[] getMessages() {
        return new String[] {"Message 1", "Message 2", "Message 3"};
    }

    @PostMapping("/messages")
    public String createMessage(@RequestBody String message) {
        return "Created message: " + message;
    }
}
```
Bây giờ, để truy cập `GET /api/messages`, Client phải gửi một Access Token hợp lệ có scope là `api.read`.

## 4. Best Practices

1.  **Luôn dùng HTTPS:** Toàn bộ giao tiếp OAuth 2.0 phải được mã hóa.
2.  **Sử dụng Authorization Code Grant với PKCE:** Đây là luồng an toàn nhất cho hầu hết các loại ứng dụng.
3.  **Giới hạn Scope:** Chỉ yêu cầu và cấp phát những quyền (scope) tối thiểu cần thiết.
4.  **Sử dụng `state` parameter:** Chống lại tấn công Cross-Site Request Forgery (CSRF) trong luồng ủy quyền.
5.  **Access Token có thời hạn ngắn:** Giảm thiểu rủi ro nếu token bị lộ. Sử dụng Refresh Token để duy trì đăng nhập.
6.  **Xác thực Redirect URI:** Luôn kiểm tra `redirect_uri` để đảm bảo Authorization Code được gửi đến đúng Client.
