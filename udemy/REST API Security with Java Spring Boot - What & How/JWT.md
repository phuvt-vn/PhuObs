# JWT (JSON Web Token) - Hướng dẫn toàn diện

## 1. Khái niệm

**JWT (JSON Web Token)** là một tiêu chuẩn mở (RFC 7519) để truyền tải thông tin một cách an toàn giữa các bên dưới dạng một đối tượng JSON nhỏ gọn và khép kín (self-contained). Dữ liệu trong JWT được ký số (digitally signed) để đảm bảo tính toàn vẹn và có thể được mã hóa để đảm bảo tính bí mật.

### Cấu trúc của một JWT
Một JWT bao gồm 3 phần, được phân tách bởi dấu chấm (`.`): `header.payload.signature`

1.  **Header**: Chứa thông tin về thuật toán được sử dụng để ký token (ví dụ: `HS256`, `RS256`).
2.  **Payload**: Chứa các "claims" (tuyên bố) - là các thông tin về người dùng (như `userId`, `username`, `roles`) và các thông tin meta của token (như ngày hết hạn `exp`, ngày phát hành `iat`).
3.  **Signature**: Chữ ký được tạo ra bằng cách kết hợp header, payload, và một "khóa bí mật" (secret key), sau đó đưa qua thuật toán đã chỉ định trong header. Chữ ký này đảm bảo rằng nội dung của token không bị thay đổi trên đường truyền.

### Ưu điểm:
- **Stateless (Không trạng thái):** Server không cần lưu trữ thông tin phiên (session). Mọi thông tin cần thiết đều nằm trong token, giúp giảm tải cho server.
- **Scalable (Dễ mở rộng):** Rất phù hợp với kiến trúc microservices vì bất kỳ service nào có khóa bí mật đều có thể xác thực token mà không cần gọi đến một server xác thực trung tâm.
- **Self-contained (Khép kín):** Token chứa tất cả thông tin cần thiết (ví dụ: quyền của người dùng), giúp giảm số lần truy vấn cơ sở dữ liệu.

### Nhược điểm:
- **Không thể thu hồi ngay lập tức:** Một khi đã được cấp, JWT sẽ hợp lệ cho đến khi hết hạn. Việc thu hồi token trước hạn đòi hỏi các giải pháp phức tạp hơn (ví dụ: token blacklisting).
- **Kích thước lớn hơn:** JWT thường lớn hơn session ID, có thể ảnh hưởng đến hiệu năng nếu truyền trong mỗi request.

## 2. Liên kết với các chủ đề bảo mật khác
- **[[Basic Authentication]] / [[API Key]]**: JWT là một phương thức thay thế hiện đại, an toàn và linh hoạt hơn cho việc xác thực API.
- **[[Oauth2]]**: JWT thường được sử dụng làm access token và refresh token trong luồng OAuth2.
- **[[HTTPS Importance]]**: JWT chứa thông tin nhạy cảm, do đó **bắt buộc** phải được truyền qua kênh HTTPS đã được mã hóa.

## 3. Hướng dẫn triển khai với Spring Boot 3.x

Chúng ta sẽ xây dựng một hệ thống xác thực hoàn chỉnh sử dụng Access Token và Refresh Token.

### 3.1. Bước 1: Setup Dependencies và Cấu hình

**Dependencies (`pom.xml`):**
```xml
<dependencies>
    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <!-- JJWT Library -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
    <!-- Redis để làm blacklist -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
</dependencies>
```

**Cấu hình (`application.yml`):**
```yaml
jwt:
  # LƯU Ý: Đây phải là một chuỗi ngẫu nhiên, dài và được bảo mật, không hardcode.
  # Nên dùng biến môi trường trong production.
  secret: "DayLaMotChuoiBiMatRatDaiVaAnToanDeTestMaThoiDungDungNoTrongProduction"
  access-token-expiration: 900000    # 15 phút
  refresh-token-expiration: 604800000 # 7 ngày
  issuer: "my-app"
```

### 3.2. Bước 2: Dịch vụ JWT (`JwtService`)

Lớp này chịu trách nhiệm tạo, phân tích và xác thực token.

```java
// File: service/JwtService.java
@Service
public class JwtService {
    private final SecretKey secretKey;
    private final JwtParser jwtParser;
    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // Tạo khóa bí mật từ chuỗi cấu hình
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        // Tạo parser để tái sử dụng
        this.jwtParser = Jwts.parser().verifyWith(secretKey).build();
    }

    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(userDetails, jwtProperties.accessTokenExpiration(), "ACCESS");
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails, jwtProperties.refreshTokenExpiration(), "REFRESH");
    }

    private String generateToken(UserDetails userDetails, long expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuer(jwtProperties.issuer())
            .claim("authorities", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
            .claim("type", tokenType)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }

    public Claims extractAllClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractAllClaims(token).getSubject();
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
```

### 3.3. Bước 3: Filter xác thực (`JwtAuthenticationFilter`)

Filter này sẽ chặn mọi request, tìm JWT trong header `Authorization`, và xác thực nó.

```java
// File: filter/JwtAuthenticationFilter.java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // ... constructor ...

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            final String username = jwtService.extractAllClaims(jwt).getSubject();
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException e) {
            // Token không hợp lệ, không làm gì cả, request sẽ bị từ chối ở các bước sau
            logger.debug("Invalid JWT token: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
```

### 3.4. Bước 4: Tích hợp vào Spring Security

```java
// File: config/SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider; // Cần được định nghĩa ở nơi khác

    // ... constructor ...

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll() // Cho phép truy cập endpoint đăng nhập
                .anyRequest().authenticated() // Mọi request khác cần xác thực
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### 3.5. Bước 5: Hoàn thiện luồng (Đăng nhập, Refresh, Logout)

-   **Đăng nhập:** Tạo một `AuthController` với endpoint `/login`. Khi người dùng đăng nhập thành công, cấp cho họ cả `accessToken` và `refreshToken`.
-   **Refresh Token:** Tạo endpoint `/refresh` nhận `refreshToken`. Nếu hợp lệ, cấp một `accessToken` mới.
-   **Logout:** Để xử lý logout, chúng ta cần một **Token Blacklist**. Khi người dùng logout, `accessToken` của họ sẽ được thêm vào blacklist (ví dụ: trong Redis) cho đến khi nó hết hạn. `JwtAuthenticationFilter` phải kiểm tra blacklist trước khi xác thực token.

## 5. Best Practices

1.  **Bảo mật Khóa bí mật:** Không bao giờ hardcode khóa trong code. Sử dụng biến môi trường hoặc các hệ thống quản lý secret (như AWS Secrets Manager, HashiCorp Vault).
2.  **Thời hạn ngắn cho Access Token:** Access token nên có thời hạn ngắn (5-15 phút) để giảm thiểu rủi ro nếu bị lộ.
3.  **Sử dụng Refresh Token:** Cung cấp refresh token có thời hạn dài hơn (vài ngày hoặc vài tuần) để người dùng duy trì đăng nhập mà không cần nhập lại mật khẩu.
4.  **Luôn dùng HTTPS:** Bắt buộc để bảo vệ token trên đường truyền.
5.  **Không lưu trữ trong Local Storage:** Tránh lưu JWT trong `localStorage` của trình duyệt vì nó dễ bị tấn công XSS. Ưu tiên lưu trong cookie `HttpOnly`.
6.  **Xác thực đầy đủ các Claims:** Luôn kiểm tra chữ ký, ngày hết hạn (`exp`), nhà phát hành (`iss`), và đối tượng (`aud`).
