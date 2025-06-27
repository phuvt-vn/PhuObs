# Multi-Factor Authentication (MFA)

## 1. Khái niệm

**Multi-Factor Authentication (MFA - Xác thực đa yếu tố)** là một lớp bảo mật bổ sung cho quá trình đăng nhập. Thay vì chỉ cần mật khẩu, MFA yêu cầu người dùng cung cấp thêm ít nhất một bằng chứng xác thực khác để chứng minh danh tính.

Mục tiêu của MFA là tạo ra một hệ thống phòng thủ nhiều lớp, khiến việc truy cập trái phép trở nên khó khăn hơn đáng kể, ngay cả khi mật khẩu của người dùng bị lộ.

### Ba yếu tố xác thực chính:

1.  **Thứ bạn biết (Knowledge Factor):** Mật khẩu, mã PIN, câu trả lời cho câu hỏi bí mật.
2.  **Thứ bạn có (Possession Factor):** Điện thoại (nhận SMS/push), token phần cứng (YubiKey), thẻ thông minh.
3.  **Thứ bạn là (Inherence Factor):** Dấu vân tay, khuôn mặt, giọng nói (sinh trắc học).

MFA là sự kết hợp của ít nhất hai trong ba yếu tố trên.

### Các phương thức MFA phổ biến:
- **TOTP (Time-based One-Time Password):** Mã 6 số thay đổi mỗi 30 giây từ các ứng dụng như Google Authenticator, Authy. Đây là phương thức rất phổ biến và an toàn.
- **SMS/Email OTP:** Mã sử dụng một lần được gửi qua tin nhắn hoặc email.
- **Push Notifications:** Một thông báo đẩy đến ứng dụng di động yêu cầu phê duyệt đăng nhập.

## 2. Liên kết với các chủ đề khác:
- **[[JWT]]**: Sau khi xác thực thành công cả mật khẩu và MFA, hệ thống sẽ cấp JWT cho người dùng.
- **[[Basic Authentication]]**: MFA là một cách tuyệt vời để tăng cường bảo mật cho các hệ thống vẫn đang sử dụng xác thực cơ bản.
- **[[Audit Log]]**: Mọi nỗ lực xác thực MFA (thành công, thất bại) đều phải được ghi lại để giám sát an ninh.
- **[[HTTPS Importance]]**: Việc truyền mã OTP hoặc các yếu tố xác thực khác bắt buộc phải được thực hiện qua kênh HTTPS.

## 3. Hướng dẫn triển khai MFA với TOTP (Google Authenticator)

Chúng ta sẽ tập trung vào việc triển khai phương thức TOTP, một trong những phương thức phổ biến và cân bằng nhất giữa bảo mật và sự tiện lợi.

### 3.1. Bước 1: Setup Dependencies và Cấu hình

**Dependencies (`pom.xml`):**
```xml
<!-- Thư viện để xử lý logic TOTP -->
<dependency>
    <groupId>dev.samstevens.totp</groupId>
    <artifactId>totp-spring-boot-starter</artifactId>
    <version>1.7.1</version>
</dependency>
<!-- Thư viện để tạo QR Code -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.2</version>
</dependency>
```

**Cấu hình (`application.yml`):**
```yaml
totp:
  # Tên hiển thị trong ứng dụng Authenticator của người dùng
  issuer: "MyAwesomeApp"
  # Cấu hình QR code
  qr-code:
    width: 250
    height: 250
```

### 3.2. Bước 2: Cập nhật `User` Entity

Chúng ta cần thêm các trường vào `User` entity (hoặc một bảng cài đặt riêng) để lưu trạng thái MFA.

```java
// Trong User.java hoặc một lớp UserMfaSettings.java riêng
@Column
private boolean mfaEnabled = false;

@Column
private String mfaSecret; // Khóa bí mật để sinh mã TOTP, phải được mã hóa
```

### 3.3. Bước 3: Dịch vụ MFA (`MfaService`)

Lớp này sẽ xử lý logic chính: tạo secret, tạo QR code, và xác thực mã.

```java
// File: service/MfaService.java
@Service
public class MfaService {
    private final SecretGenerator secretGenerator;
    private final QrDataFactory qrDataFactory;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;
    private final EncryptionService encryptionService; // Dịch vụ mã hóa đã tạo ở bài trước
    private final UserRepository userRepository;

    // ... constructor ...

    // Khi người dùng muốn bật MFA
    public String setupDevice(String username) {
        // 1. Tạo một secret key mới
        String secret = secretGenerator.generate();

        // 2. Lưu secret đã được mã hóa vào DB cho user
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setMfaEnabled(true);
        user.setMfaSecret(encryptionService.encrypt(secret)); // Quan trọng: Mã hóa secret
        userRepository.save(user);

        // 3. Tạo dữ liệu cho QR code
        QrData qrData = qrDataFactory.create(secret, "MyAwesomeApp", username);

        // 4. Sinh ảnh QR code (dưới dạng Base64 để hiển thị trên web)
        try {
            byte[] qrCodeImage = qrGenerator.generate(qrData);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCodeImage);
        } catch (QrGenerationException e) {
            throw new RuntimeException("Lỗi khi tạo QR code", e);
        }
    }

    // Khi người dùng nhập mã từ app để xác thực
    public boolean verifyCode(String username, String code) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (!user.isMfaEnabled()) {
            return false;
        }
        // Giải mã secret đã lưu để xác thực
        String secret = encryptionService.decrypt(user.getMfaSecret());
        return codeVerifier.isValidCode(secret, code);
    }
}
```

### 3.4. Bước 4: Tích hợp vào luồng đăng nhập

Luồng đăng nhập bây giờ sẽ có 2 bước:
1.  **Bước 1: Xác thực mật khẩu.** Nếu thành công, kiểm tra xem người dùng có bật MFA không.
2.  **Bước 2 (Nếu có MFA):** Yêu cầu người dùng nhập mã TOTP. Nếu mã đúng, hoàn tất đăng nhập và cấp token (ví dụ: JWT).

**Ví dụ trong `AuthController.java`:**
```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
    // 1. Xác thực username & password
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
    );
    SecurityContextHolder.getContext().setAuthentication(authentication);
    
    // 2. Kiểm tra MFA
    String username = loginRequest.username();
    User user = userRepository.findByUsername(username).orElseThrow();
    
    if (user.isMfaEnabled()) {
        // Nếu bật MFA, trả về thông báo yêu cầu bước 2
        return ResponseEntity.ok(new MfaRequiredResponse(true));
    }
    
    // Nếu không có MFA, cấp token luôn
    String jwt = jwtService.generateAccessToken((UserDetails) authentication.getPrincipal());
    return ResponseEntity.ok(new JwtResponse(jwt));
}

@PostMapping("/verify-mfa")
public ResponseEntity<?> verifyMfa(@RequestBody MfaVerifyRequest verifyRequest) {
    // Lấy username từ user đã xác thực ở bước 1
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    
    if (mfaService.verifyCode(username, verifyRequest.code())) {
        // Nếu mã MFA đúng, cấp token
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String jwt = jwtService.generateAccessToken(userDetails);
        return ResponseEntity.ok(new JwtResponse(jwt));
    }
    
    // Nếu sai, trả về lỗi
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Mã MFA không hợp lệ.");
}
```

## 5. Best Practices

1.  **Mã hóa Secret Key:** Không bao giờ lưu trữ `mfaSecret` dưới dạng văn bản gốc trong cơ sở dữ liệu. Luôn mã hóa nó.
2.  **Cung cấp Mã dự phòng (Backup Codes):** Khi người dùng bật MFA, hãy cung cấp cho họ một danh sách các mã sử dụng một lần để họ có thể dùng trong trường hợp mất thiết bị xác thực. Các mã này cũng phải được **hash** trước khi lưu vào DB.
3.  **Rate Limiting:** Áp dụng giới hạn số lần thử sai cho endpoint xác thực mã MFA để chống lại tấn công brute-force.
4.  **Hỗ trợ nhiều phương thức:** Cho phép người dùng chọn lựa giữa TOTP, SMS, Email... để tăng tính tiện lợi.
5.  **Ghi Audit Log:** Ghi lại tất cả các sự kiện liên quan đến MFA: bật/tắt MFA, các lần xác thực thành công/thất bại.

## 6. Testing

```java
@SpringBootTest
class MfaServiceTest {
    @Autowired private MfaService mfaService;
    @Autowired private UserRepository userRepository;
    @Autowired private EncryptionService encryptionService;

    @Test
    void shouldEnableMfaAndVerifyCodeSuccessfully() {
        // Given: Tạo một user mẫu
        User user = new User("testmfa", "password");
        userRepository.save(user);

        // When: Bật MFA và xác thực mã
        String qrCode = mfaService.setupDevice("testmfa");
        assertThat(qrCode).startsWith("data:image/png;base64,");

        // Lấy secret đã mã hóa từ DB để tạo mã OTP cho việc test
        User updatedUser = userRepository.findByUsername("testmfa").get();
        String secret = encryptionService.decrypt(updatedUser.getMfaSecret());
        
        // Tạo mã OTP hợp lệ từ secret
        TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();
        String validCode = totp.generateOneTimePassword(secret, Instant.now());
        
        // Then: Việc xác thực phải thành công
        boolean isVerified = mfaService.verifyCode("testmfa", validCode);
        assertThat(isVerified).isTrue();
    }
}
