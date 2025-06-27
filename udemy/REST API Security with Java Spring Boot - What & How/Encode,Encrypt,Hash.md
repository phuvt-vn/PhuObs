# Phân biệt và Sử dụng: Encoding, Encryption, và Hashing

## 1. Khái niệm cốt lõi

Trong bảo mật, ba khái niệm này thường bị nhầm lẫn nhưng lại có mục đích và cách hoạt động hoàn toàn khác nhau.

### 1.1. Encoding (Mã hóa dữ liệu)
- **Mục đích:** Biến đổi dữ liệu từ định dạng này sang định dạng khác để đảm bảo việc **truyền tải hoặc lưu trữ** được an toàn và tương thích.
- **Bảo mật:** **Không có giá trị bảo mật.** Bất kỳ ai cũng có thể giải mã (decode) ngược lại mà không cần "chìa khóa".
- **Ví dụ:** Chuyển đổi ký tự đặc biệt để gửi qua URL, hoặc chuyển đổi dữ liệu nhị phân thành văn bản bằng Base64.

### 1.2. Encryption (Mã hóa bảo mật)
- **Mục đích:** Bảo vệ **tính bí mật (confidentiality)** của dữ liệu. Biến đổi dữ liệu (plaintext) thành một dạng không thể đọc được (ciphertext).
- **Bảo mật:** **Cao.** Cần có một "khóa" (key) bí mật để giải mã (decrypt) và đọc dữ liệu gốc.
- **Ví dụ:** Mã hóa thông tin thẻ tín dụng trước khi lưu vào cơ sở dữ liệu, mã hóa nội dung email.

### 1.3. Hashing (Băm)
- **Mục đích:** Đảm bảo **tính toàn vẹn (integrity)** của dữ liệu. Biến đổi một chuỗi dữ liệu có độ dài bất kỳ thành một chuỗi có độ dài cố định (hash value).
- **Bảo mật:** **Là hàm một chiều.** Không thể (về mặt lý thuyết) đảo ngược từ chuỗi hash để lấy lại dữ liệu gốc.
- **Ví dụ:** Lưu trữ mật khẩu người dùng (chỉ lưu hash, không lưu mật khẩu gốc), tạo chữ ký số để kiểm tra xem file có bị thay đổi hay không.

## 2. Bảng so sánh nhanh

| Tiêu chí | Encoding | Encryption | Hashing |
|---|---|---|---|
| **Mục đích** | Biến đổi dữ liệu | Bảo vệ bí mật | Đảm bảo toàn vẹn |
| **Có thể đảo ngược?** | **Có** (decode) | **Có** (cần key) | **Không** |
| **Cần "khóa"?** | Không | Có | Không |
| **Ví dụ phổ biến** | Base64, URL Encoding | AES, RSA | SHA-256, BCrypt |
| **Ứng dụng chính** | Truyền tải dữ liệu | Bảo mật dữ liệu nhạy cảm | Lưu trữ mật khẩu, chữ ký số |

## 3. Liên kết với các chủ đề bảo mật khác
- **[[Basic Authentication]]**: Tên người dùng và mật khẩu được **Encode** bằng Base64 (không an toàn), và mật khẩu trong DB phải được **Hash**.
- **[[JWT]]**: Token được ký (sign) bằng một thuật toán **Hashing** (như HMAC-SHA256) hoặc **Encryption** (như RSA).
- **[[HTTPS Importance]]**: Toàn bộ kênh truyền được **Encrypt** bằng TLS.

## 4. Hướng dẫn triển khai với Spring Boot 3.x

### 4.1. Encoding
Java cung cấp sẵn các lớp tiện ích cho việc encoding.

```java
// File: service/EncodingService.java
@Service
public class EncodingService {

    // Mã hóa một chuỗi sang định dạng Base64
    public String encodeBase64(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    // Giải mã một chuỗi Base64
    public String decodeBase64(String encodedInput) {
        byte[] decodedBytes = Base64.getDecoder().decode(encodedInput);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    // Mã hóa một chuỗi để an toàn khi dùng trong URL
    public String encodeUrl(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8);
    }

    // Giải mã một chuỗi từ URL
    public String decodeUrl(String encodedInput) {
        return URLDecoder.decode(encodedInput, StandardCharsets.UTF_8);
    }
}
```

### 4.2. Hashing
Spring Security là công cụ tiêu chuẩn để xử lý hashing, đặc biệt là cho mật khẩu.

```java
// File: config/SecurityConfig.java
@Configuration
public class SecurityConfig {
    // Định nghĩa bean PasswordEncoder để sử dụng trong toàn ứng dụng
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt là thuật toán được khuyến nghị mạnh mẽ cho việc hash mật khẩu
        return new BCryptPasswordEncoder();
    }
}

// File: service/HashingService.java
@Service
public class HashingService {
    private final PasswordEncoder passwordEncoder;

    public HashingService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    // Băm mật khẩu bằng BCrypt
    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    // Kiểm tra mật khẩu có khớp với chuỗi hash không
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
    
    // Băm dữ liệu thông thường (ví dụ: để kiểm tra tính toàn vẹn) bằng SHA-256
    public String hashDataWithSHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, hash).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi khi băm dữ liệu", e);
        }
    }
}
```

### 4.3. Encryption
Mã hóa phức tạp hơn do yêu cầu quản lý khóa. Dưới đây là ví dụ về mã hóa đối xứng AES, loại phổ biến nhất.

**Quan trọng:** Việc quản lý khóa (key management) là phần khó và quan trọng nhất. Trong thực tế, bạn nên sử dụng các dịch vụ quản lý khóa chuyên dụng như AWS KMS, Azure Key Vault, hoặc HashiCorp Vault.

```java
// File: service/EncryptionService.java
@Service
public class EncryptionService {
    private final SecretKey secretKey;
    private final IvParameterSpec iv;

    public EncryptionService(@Value("${encryption.secret-key}") String key,
                             @Value("${encryption.init-vector}") String initVector) {
        // Trong thực tế, key và IV không nên được hardcode hoặc lưu trong config file.
        // Chúng nên được lấy từ một hệ thống quản lý secret an toàn.
        this.secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        this.iv = new IvParameterSpec(initVector.getBytes(StandardCharsets.UTF_8));
    }

    // Mã hóa dữ liệu bằng AES
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            byte[] cipherText = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi mã hóa", e);
        }
    }

    // Giải mã dữ liệu
    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(plainText);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi giải mã", e);
        }
    }
}
```
**Cấu hình trong `application.yml` (chỉ cho mục đích minh họa):**
```yaml
encryption:
  # LƯU Ý: Key và IV phải có độ dài chính xác (ví dụ: 16, 24, 32 bytes cho AES)
  # ĐÂY LÀ CÁCH LÀM KHÔNG AN TOÀN, CHỈ DÙNG ĐỂ DEMO
  secret-key: ThisIsASecretKey1234567890123456
  init-vector: ThisIsAnInitVector123456789012
```

## 5. Best Practices

1.  **Mật khẩu:** Luôn **HASH** mật khẩu với một thuật toán mạnh, chậm, và có salt như **BCrypt** hoặc **Argon2**. Không bao giờ Encode hay Encrypt mật khẩu.
2.  **Dữ liệu nhạy cảm cần truy xuất lại:** Sử dụng **ENCRYPTION** (ví dụ: AES-256) để bảo vệ thông tin như số thẻ tín dụng, số an sinh xã hội.
3.  **Dữ liệu cần truyền tải an toàn:** Sử dụng **ENCODING** (như Base64) để đóng gói dữ liệu, nhưng phải truyền nó qua một kênh đã được **ENCRYPT** (HTTPS/TLS).
4.  **Kiểm tra tính toàn vẹn:** Sử dụng **HASHING** (như SHA-256) để tạo "chữ ký" cho file hoặc dữ liệu, giúp phát hiện nếu chúng bị thay đổi.
5.  **Quản lý khóa:** Đối với mã hóa, việc bảo vệ khóa (secret key) là tối quan trọng. Sử dụng các hệ thống quản lý khóa chuyên dụng.
