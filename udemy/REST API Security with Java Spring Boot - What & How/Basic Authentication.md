# Xác thực cơ bản (Basic Authentication)

## 1. Khái niệm

**Xác thực cơ bản (Basic Authentication)** là một trong những phương thức xác thực đơn giản nhất được định nghĩa trong tiêu chuẩn của web. Nó hoạt động bằng cách yêu cầu client (ví dụ: trình duyệt) gửi tên người dùng (username) và mật khẩu (password) cùng với mỗi yêu cầu (request) đến server.

**Tưởng tượng đơn giản:** Bạn muốn vào một căn phòng bí mật. Mỗi lần muốn vào, bạn phải đọc to "khẩu lệnh" gồm tên và mật khẩu. Người bảo vệ nghe và kiểm tra, nếu đúng thì bạn được vào.

### Cách thức hoạt động:

1.  **Yêu cầu ban đầu:** Client truy cập một tài nguyên được bảo vệ mà không có thông tin xác thực.
2.  **Server từ chối:** Server trả về lỗi `401 Unauthorized` và một header `WWW-Authenticate: Basic realm="tên_vùng_bảo_vệ"`.
3.  **Client gửi lại:** Client hiển thị hộp thoại đăng nhập. Sau khi người dùng nhập username và password, client tạo chuỗi `username:password`, mã hóa nó bằng **Base64**, và gửi lại yêu cầu với header `Authorization`.
    *   Ví dụ: `admin:password123` -> `YWRtaW46cGFzc3dvcmQxMjM=`
    *   Header: `Authorization: Basic YWRtaW46cGFzc3dvcmQxMjM=`
4.  **Server xác thực:** Server nhận header, giải mã Base64 để lấy username/password và kiểm tra với database của mình.
5.  **Phản hồi cuối cùng:** Nếu hợp lệ, server trả về tài nguyên. Nếu không, tiếp tục trả về lỗi `401 Unauthorized`.

## 2. Ưu và Nhược điểm

| Ưu điểm | Nhược điểm |
| :--- | :--- |
| **Dễ triển khai:** Cả server và client đều không cần logic phức tạp. | **Bảo mật rất thấp nếu không có HTTPS:** Base64 chỉ là mã hóa để truyền dữ liệu, không phải mã hóa bảo mật. Bất kỳ ai cũng có thể giải mã ngược lại. |
| **Được hỗ trợ rộng rãi:** Hầu hết mọi trình duyệt và công cụ HTTP đều hỗ trợ sẵn. | **Gửi thông tin trên mỗi request:** Tăng nguy cơ bị lộ thông tin xác thực. |
| **Không cần quản lý trạng thái (Stateless):** Server không cần lưu trữ thông tin phiên (session). | **Không có cơ chế đăng xuất (logout) chuẩn:** Cách duy nhất là client phải tự xóa thông tin đăng nhập. |

## 3. Liên kết với các chủ đề bảo mật khác

-   **[[HTTPS Importance]]**: **BẮT BUỘC!** Đây là điều kiện tiên quyết để sử dụng Basic Auth. HTTPS sẽ mã hóa toàn bộ kênh truyền, bảo vệ username/password khỏi bị nghe lén.
-   **[[Encode,Encrypt,Hash]]**: Phân biệt rõ: Base64 là **Encoding** (mã hóa dữ liệu), không phải **Encryption** (mã hóa bảo mật). Mật khẩu trong database phải được **Hashing** (băm).
-   **[[JWT]]**: Một phương thức thay thế hiện đại, an toàn và linh hoạt hơn nhiều, cho phép đăng xuất và có thời gian hết hạn.
-   **[[Audit Log]]**: Mọi nỗ lực đăng nhập (thành công hay thất bại) qua Basic Auth đều phải được ghi lại để phát hiện các cuộc tấn công dò mật khẩu (brute-force).

## 4. Hướng dẫn triển khai với Spring Boot 3.x

### 4.1. Bước 1: Entity và Repository

Định nghĩa đối tượng `User` và cách truy vấn từ cơ sở dữ liệu.

```java
// File: entity/User.java
@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // Luôn lưu dạng đã hash!

    @Column(nullable = false)
    private String role; // Ví dụ: "ROLE_USER", "ROLE_ADMIN"

    private boolean enabled = true;
}

// File: repository/UserJpaRepository.java
@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndEnabledTrue(String username);
}
```

### 4.2. Bước 2: `UserDetailsService`

Lớp này là cầu nối giữa database người dùng của bạn và Spring Security.

```java
// File: service/CustomUserDetailsService.java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserJpaRepository userRepository;

    public CustomUserDetailsService(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndEnabledTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

        // Chuyển đổi User entity thành UserDetails của Spring Security
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true, true, true,
                Collections.singletonList(() -> user.getRole())
        );
    }
}
```

### 4.3. Bước 3: Cấu hình Spring Security

Kích hoạt Basic Authentication và định nghĩa các quy tắc truy cập.

```java
// File: config/BasicAuthSecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class BasicAuthSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF vì dùng API stateless
                .csrf(csrf -> csrf.disable())
                // Kích hoạt Basic Authentication
                .httpBasic(httpBasic -> httpBasic.realmName("My-API"))
                // Cấu hình session là STATELESS
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Định nghĩa các quy tắc ủy quyền
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
```

### 4.4. Bước 4: Controller để kiểm thử

```java
// File: controller/ApiController.java
@RestController
public class ApiController {

    @GetMapping("/public/hello")
    public String publicHello() {
        return "Chào mừng bạn đến với API công khai!";
    }

    @GetMapping("/api/user")
    public String userEndpoint(Principal principal) {
        return "Xin chào, " + principal.getName() + "! Bạn đã được xác thực.";
    }

    @GetMapping("/admin/data")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminData() {
        return "Đây là dữ liệu bí mật chỉ dành cho Admin.";
    }
}
```

## 5. Best Practices và Testing

### 5.1. Best Practices (Thực hành tốt nhất)

1.  **LUÔN LUÔN SỬ DỤNG HTTPS**: Điều quan trọng nhất.
2.  **HASH MẬT KHẨU**: Dùng BCrypt, SCrypt, hoặc Argon2 để băm mật khẩu trước khi lưu vào DB.
3.  **TRIỂN KHAI RATE LIMITING**: Chống tấn công brute-force bằng cách giới hạn số lần đăng nhập thất bại.
4.  **GHI LOG ĐẦY ĐỦ**: Ghi lại mọi nỗ lực đăng nhập (thành công và thất bại).
5.  **CÂN NHẮC PHƯƠNG THỨC THAY THẾ**: Với ứng dụng mới, ưu tiên dùng [[JWT]] hoặc [[Oauth2]].

### 5.2. Testing

Sử dụng `@WithMockUser` và `MockMvc` để kiểm thử các quy tắc bảo mật.

```java
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldDenyAnonymousAccessToProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void shouldAllowAuthenticatedUserToUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void shouldDenyUserAccessToAdminEndpoint() throws Exception {
        mockMvc.perform(get("/admin/data"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldAllowAdminAccessToAdminEndpoint() throws Exception {
        mockMvc.perform(get("/admin/data"))
                .andExpect(status().isOk());
    }
}
```

## 6. So sánh: Tự viết vs. Dùng thư viện

**Nguyên tắc vàng: "Đừng tự viết lại cơ chế xác thực - hãy dùng các framework đã được kiểm chứng"**

| Tiêu chí | ❌ Tự viết (Manual) | ✅ Dùng Spring Security |
|---|---|---|
| **Bảo mật** | Thấp (Dễ có lỗ hổng, ví dụ: timing attack, hash yếu) | Cao (Đã xử lý các best practices, dùng BCrypt) |
| **Tính năng** | Rất hạn chế (Không có quản lý role, khóa tài khoản,...) | Đầy đủ (Role-based access, method security,...) |
| **Bảo trì** | Khó (Phải tự vá lỗi, tự nâng cấp) | Dễ (Được cộng đồng hỗ trợ, cập nhật thường xuyên) |

Sử dụng Spring Security không chỉ giúp bạn viết code nhanh hơn mà còn đảm bảo ứng dụng của bạn được bảo vệ bởi các tiêu chuẩn bảo mật ngành công nghiệp đã được kiểm chứng qua thời gian.
