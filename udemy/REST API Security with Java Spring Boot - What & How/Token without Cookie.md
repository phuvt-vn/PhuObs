# Xác thực bằng Token qua Header (Không dùng Cookie)

## 1. Khái niệm

Đây là một mô hình xác thực rất phổ biến cho các ứng dụng hiện đại như **Single Page Applications (SPAs)** (React, Vue, Angular) và **ứng dụng di động**. Thay vì dựa vào cookie do trình duyệt quản lý tự động, client (frontend) sẽ chủ động lưu trữ token và gửi nó trong mỗi yêu cầu đến API.

### Luồng hoạt động cơ bản:

1.  **Đăng nhập:** Người dùng gửi username và password.
2.  **Nhận Token:** Server xác thực và trả về một **Access Token** (thường là JWT) trong phần body của response.
3.  **Lưu trữ Token:** Client nhận và lưu trữ Access Token này (thường là trong **Local Storage**, **Session Storage**, hoặc trong bộ nhớ của ứng dụng).
4.  **Gửi Token:** Với mỗi yêu cầu tiếp theo đến các API được bảo vệ, client sẽ đính kèm token vào header `Authorization`.
    ```
    Authorization: Bearer <your_access_token>
    ```
5.  **Xác thực phía Server:** Server nhận yêu cầu, trích xuất token từ header, xác thực nó (kiểm tra chữ ký, ngày hết hạn,...) và cấp quyền truy cập.

## 2. So sánh: Header (Local Storage) vs. Cookie

| Tiêu chí | ✅ Header (Local Storage) | ❌ Cookie (HttpOnly) |
|---|---|---|
| **Bảo mật XSS** | **Thấp.** Token lưu trong Local Storage có thể bị đánh cắp bởi bất kỳ đoạn script độc hại nào chạy trên trang (tấn công XSS). | **Cao.** Cờ `HttpOnly` ngăn JavaScript truy cập, bảo vệ token khỏi XSS. |
| **Bảo mật CSRF** | **Cao.** Vì token không được gửi tự động, phương pháp này miễn nhiễm với tấn công CSRF. | **Thấp (cần phòng chống).** Cần cờ `SameSite` để giảm thiểu. |
| **Cross-Domain** | **Dễ dàng.** Hoạt động tốt với các API ở domain khác mà không cần cấu hình CORS phức tạp cho credentials. | **Khó khăn.** Yêu cầu cấu hình CORS chặt chẽ và phức tạp. |
| **Triển khai** | **Phức tạp hơn ở phía client.** Lập trình viên phải tự viết logic để lưu, gửi, và làm mới token. | **Đơn giản hơn.** Trình duyệt xử lý việc gửi cookie một cách tự động. |

**Kết luận so sánh:**
- Dùng **Cookie** nếu Frontend và Backend của bạn ở **cùng một domain (hoặc các subdomain)**. Đây là lựa chọn an toàn hơn về mặt XSS.
- Dùng **Header (Local Storage)** nếu Frontend và Backend của bạn ở **các domain hoàn toàn khác nhau**, hoặc khi bạn xây dựng API cho ứng dụng di động. Tuy nhiên, bạn phải có các biện pháp phòng chống XSS cực kỳ nghiêm ngặt.

## 3. Hướng dẫn triển khai với Spring Boot 3.x

### 3.1. Bước 1: Setup và Dịch vụ Token

(Giả sử bạn đã có `JwtService` và cấu hình JWT như trong các bài trước).

Chúng ta cần một `TokenService` để quản lý việc tạo, làm mới (refresh), và thu hồi (revoke) token.

```java
// File: service/TokenService.java
@Service
public class TokenService {
    private final JwtService jwtService;
    private final RedisTemplate<String, String> redisTemplate; // Dùng Redis để lưu refresh token và blacklist

    // ... constructor ...

    // Tạo cả access và refresh token
    public TokenResponse generateTokens(UserDetails userDetails) {
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        // Lưu refresh token vào Redis với key là username
        redisTemplate.opsForValue().set("refresh_token:" + userDetails.getUsername(), refreshToken, Duration.ofDays(7));
        return new TokenResponse(accessToken, refreshToken);
    }

    // Làm mới access token bằng refresh token
    public String refreshAccessToken(String refreshToken) {
        if (jwtService.isTokenExpired(refreshToken)) throw new RuntimeException("Refresh token đã hết hạn");
        
        String username = jwtService.extractUsername(refreshToken);
        String savedToken = redisTemplate.opsForValue().get("refresh_token:" + username);
        
        if (savedToken == null || !savedToken.equals(refreshToken)) {
            throw new RuntimeException("Refresh token không hợp lệ hoặc đã bị thu hồi");
        }
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return jwtService.generateAccessToken(userDetails);
    }
    
    // Thu hồi token bằng cách thêm vào blacklist trong Redis
    public void revokeToken(String token) {
        String jti = jwtService.extractClaim(token, claims -> claims.getId()); // Cần có 'jti' claim
        long ttl = jwtService.extractExpiration(token).getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            redisTemplate.opsForValue().set("blacklist:" + jti, "revoked", Duration.ofMillis(ttl));
        }
    }
    
    public record TokenResponse(String accessToken, String refreshToken) {}
}
```

### 3.2. Bước 2: Filter xác thực Token từ Header

Filter này sẽ trích xuất token từ header `Authorization` và xác thực nó.

```java
// File: filter/TokenAuthenticationFilter.java
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
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
            // Logic xác thực token tương tự như với cookie
            // ...
            // Nếu hợp lệ, thiết lập SecurityContext
            // ...
        } catch (JwtException e) {
            logger.debug("Invalid JWT token: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
```

### 3.3. Bước 3: Controller xác thực

Controller này cung cấp các endpoint cho luồng đăng nhập, refresh, và logout.

```java
// File: controller/AuthController.java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    // ... constructor ...

    @PostMapping("/login")
    public ResponseEntity<TokenService.TokenResponse> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(tokenService.generateTokens(userDetails));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody RefreshRequest refreshRequest) {
        String newAccessToken = tokenService.refreshAccessToken(refreshRequest.refreshToken());
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest logoutRequest) {
        tokenService.revokeToken(logoutRequest.accessToken());
        // Có thể thu hồi cả refresh token nếu cần
        return ResponseEntity.ok().build();
    }
    
    // ... DTOs cho request ...
}
```

### 3.4. Bước 4: Triển khai phía Client (JavaScript)

Client sẽ chịu trách nhiệm lưu và quản lý token.

```javascript
// File: auth.js
class AuthService {
    constructor() {
        this.accessToken = null;
        this.refreshToken = localStorage.getItem('refreshToken');
    }

    async login(username, password) {
        const response = await fetch('/api/auth/login', { /* ... */ });
        const data = await response.json();
        this.accessToken = data.accessToken;
        this.refreshToken = data.refreshToken;
        localStorage.setItem('refreshToken', data.refreshToken);
    }

    async makeApiCall(url, options = {}) {
        if (!this.accessToken) { // Hoặc nếu token hết hạn
            await this.refresh();
        }

        const headers = {
            ...options.headers,
            'Authorization': `Bearer ${this.accessToken}`
        };
        
        return fetch(url, { ...options, headers });
    }

    async refresh() {
        if (!this.refreshToken) throw new Error("No refresh token available");
        const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: this.refreshToken })
        });
        const data = await response.json();
        this.accessToken = data.accessToken;
    }
    
    logout() {
        // Gọi API logout để server blacklist token
        fetch('/api/auth/logout', { /* ... */ });
        this.accessToken = null;
        this.refreshToken = null;
        localStorage.removeItem('refreshToken');
    }
}
```

## 4. Best Practices

1.  **Chống XSS là ưu tiên số 1:** Vì token được lưu ở nơi JavaScript có thể truy cập, bạn phải áp dụng mọi biện pháp có thể để chống XSS:
    *   **Content Security Policy (CSP):** Thiết lập CSP header chặt chẽ để hạn chế các nguồn script được phép thực thi.
    *   **Sanitize User Input:** Làm sạch và mã hóa mọi dữ liệu do người dùng nhập trước khi hiển thị ra trang.
    *   **Sử dụng các framework hiện đại:** React, Vue, Angular có các cơ chế tích hợp để chống XSS.
2.  **Sử dụng Access Token ngắn hạn:** Giảm thiểu thiệt hại nếu token bị lộ.
3.  **Bảo mật Refresh Token:** Refresh token là chìa khóa để duy trì đăng nhập. Lưu nó ở nơi an toàn nhất có thể (ví dụ: cookie `HttpOnly` chỉ cho endpoint refresh) hoặc chấp nhận rủi ro khi lưu trong Local Storage.
4.  **Luôn dùng HTTPS:** Để bảo vệ token trên đường truyền.
