# Tấn công XSS (Cross-Site Scripting) và Cách phòng chống

## 1. XSS là gì?

**XSS (Cross-Site Scripting)** là một trong những lỗ hổng bảo mật web phổ biến nhất. Nó xảy ra khi kẻ tấn công có thể chèn (inject) các đoạn mã độc (thường là JavaScript) vào một trang web, và đoạn mã này sau đó sẽ được thực thi trên trình duyệt của những người dùng khác khi họ truy cập trang web đó.

**Ví dụ đơn giản:**
Hãy tưởng tượng một trang web có phần bình luận.
- **Người dùng bình thường:** Nhập một bình luận như "Bài viết rất hay!".
- **Kẻ tấn công:** Nhập một bình luận chứa mã độc, ví dụ: `<script>alert('Bạn đã bị hack!');</script>`.

Nếu trang web không xử lý tốt, nó sẽ lưu và hiển thị nguyên văn bình luận này. Khi một người dùng khác xem trang, trình duyệt của họ sẽ thực thi đoạn mã `<script>` đó, và một hộp thoại "Bạn đã bị hack!" sẽ hiện lên. Đây chỉ là một ví dụ vô hại, nhưng kẻ tấn công có thể thực hiện những việc nguy hiểm hơn nhiều.

### Hậu quả nghiêm trọng:
- **Đánh cắp Session/Token:** Kẻ tấn công có thể dùng JavaScript để đọc cookie hoặc token lưu trữ trong Local Storage, sau đó chiếm quyền đăng nhập của người dùng.
- **Keylogging:** Ghi lại mọi thứ người dùng gõ trên trang, bao gồm cả mật khẩu và thông tin thẻ tín dụng.
- **Thay đổi nội dung trang (Defacement):** Thay đổi giao diện, nội dung của trang web để lừa đảo hoặc phá hoại.
- **Chuyển hướng đến trang độc hại:** Tự động chuyển hướng người dùng đến một trang web lừa đảo (phishing).

### Các loại XSS chính:
1.  **Stored XSS (Persistent):** Mã độc được lưu trữ vĩnh viễn trên server (ví dụ: trong một bình luận, một bài đăng). Bất kỳ ai xem nội dung đó đều sẽ bị tấn công. Đây là loại nguy hiểm nhất.
2.  **Reflected XSS (Non-Persistent):** Mã độc được "phản chiếu" lại từ một yêu cầu của người dùng. Ví dụ: `http://example.com/search?q=<script>...</script>`. Kẻ tấn công phải lừa người dùng nhấp vào một liên kết đã bị chế tạo sẵn.
3.  **DOM-based XSS:** Lỗ hổng xảy ra hoàn toàn ở phía client, khi JavaScript của trang web tự thay đổi cấu trúc DOM một cách không an toàn dựa trên dữ liệu đầu vào.

## 2. Liên kết với các chủ đề khác
- **[[JWT]] / [[Token Cookie]]**: XSS là mối đe dọa chính đối với việc lưu trữ token. Nếu token bị đánh cắp qua XSS, kẻ tấn công có thể chiếm phiên đăng nhập.
- **[[CORS]]**: Cấu hình CORS chặt chẽ có thể giúp giới hạn nơi mà script độc hại có thể gửi dữ liệu đã đánh cắp đến.
- **[[HTTPS Importance]]**: Mặc dù không trực tiếp ngăn chặn XSS, HTTPS ngăn kẻ tấn công ở giữa (MITM) tiêm mã độc vào các trang không được mã hóa.

## 3. Chiến lược phòng thủ nhiều lớp trong Spring Boot

Không có một viên đạn bạc nào cho XSS. Chúng ta phải kết hợp nhiều lớp phòng thủ.

### 3.1. Lớp 1: Output Encoding (Quan trọng nhất)

**Nguyên tắc vàng:** **Luôn luôn mã hóa (escape) mọi dữ liệu không đáng tin cậy ngay tại thời điểm nó được chèn vào trang HTML, tùy theo ngữ cảnh mà nó được chèn vào.**

May mắn là các template engine hiện đại như **Thymeleaf** (mặc định trong Spring Boot) đã thực hiện việc này một cách **tự động**.

**Ví dụ với Thymeleaf:**
```html
<!-- Giả sử 'userComment' chứa <script>alert('XSS')</script> -->

<!-- ✅ AN TOÀN: Thymeleaf tự động escape -->
<p th:text="${userComment}"></p>
<!-- Kết quả HTML sẽ là: <p><script>alert('XSS')</script></p> -->
<!-- Trình duyệt sẽ hiển thị chuỗi ký tự, không thực thi script. -->

<!-- ❌ NGUY HIỂM: Tắt tính năng escape -->
<p th:utext="${userComment}"></p> <!-- 'utext' là unescaped text -->
<!-- Kết quả HTML sẽ là: <p><script>alert('XSS')</script></p> -->
<!-- Trình duyệt sẽ thực thi script! -->
```
**Bài học:** Chỉ cần bạn sử dụng `th:text` hoặc các thuộc tính chuẩn khác của Thymeleaf, bạn đã được bảo vệ ở mức độ cơ bản và hiệu quả nhất. **Tuyệt đối không sử dụng `th:utext` trừ khi bạn biết chính xác mình đang làm gì và dữ liệu đó đã được làm sạch.**

### 3.2. Lớp 2: Input Validation & Sanitization

Đây là lớp phòng thủ phía server, đảm bảo rằng dữ liệu độc hại không được lưu vào cơ sở dữ liệu ngay từ đầu.

- **Validation:** Kiểm tra xem dữ liệu đầu vào có tuân thủ một định dạng nghiêm ngặt không (ví dụ: chỉ chứa chữ và số).
- **Sanitization (Làm sạch):** Nếu bạn phải cho phép người dùng nhập HTML (ví dụ: trong một trình soạn thảo văn bản), hãy loại bỏ tất cả các thẻ và thuộc tính nguy hiểm.

**Thư viện được khuyến nghị:** **OWASP Java HTML Sanitizer**.

**Ví dụ về `XssProtectionService`:**
```java
// File: service/XssProtectionService.java
@Service
public class XssProtectionService {
    // Định nghĩa một chính sách chỉ cho phép các thẻ HTML an toàn
    private static final PolicyFactory POLICY_FACTORY = new HtmlPolicyBuilder()
        .allowElements("p", "br", "strong", "em", "ul", "ol", "li")
        .toFactory();

    // Làm sạch một chuỗi HTML, loại bỏ các thẻ nguy hiểm
    public String sanitizeHtml(String input) {
        if (input == null) return null;
        return POLICY_FACTORY.sanitize(input);
    }
}
```
**Sử dụng trong Controller:**
```java
@PostMapping("/comments")
public ResponseEntity<String> postComment(@RequestBody CommentRequest request) {
    // Làm sạch nội dung bình luận trước khi lưu vào DB
    String sanitizedContent = xssProtectionService.sanitizeHtml(request.getContent());
    commentService.save(sanitizedContent);
    return ResponseEntity.ok("Bình luận đã được đăng.");
}
```

### 3.3. Lớp 3: Content Security Policy (CSP)

CSP là một lớp phòng thủ cực kỳ mạnh mẽ. Nó là một HTTP header mà server gửi cho trình duyệt, ra lệnh cho trình duyệt chỉ được phép tải tài nguyên (scripts, styles, images) từ các nguồn đã được chỉ định.

Nếu kẻ tấn công có thể chèn một thẻ `<script src="http://malicious.com/evil.js">`, CSP sẽ ngăn trình duyệt tải và thực thi script đó vì `malicious.com` không có trong danh sách trắng.

**Cấu hình CSP chặt chẽ trong Spring Security:**
```java
// File: config/SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .contentSecurityPolicy(csp -> csp
                .policyDirectives(
                    "default-src 'self'; " + // Chỉ cho phép tải từ chính domain của mình
                    "script-src 'self'; " +    // Chỉ cho phép script từ domain của mình
                    "style-src 'self' 'unsafe-inline'; " + // Cho phép style inline (có thể thắt chặt hơn)
                    "img-src 'self' data:; " + // Cho phép ảnh từ domain của mình và data: URIs
                    "object-src 'none'; " +    // Không cho phép các plugin như Flash
                    "frame-ancestors 'none';"  // Không cho phép trang bị nhúng vào iframe
                )
            )
        );
        // ... các cấu hình khác ...
        return http.build();
    }
}
```

## 4. Best Practices tổng hợp

1.  **Mặc định tin tưởng Thymeleaf:** Sử dụng `th:text` và để Thymeleaf tự động escape dữ liệu.
2.  **Làm sạch dữ liệu đầu vào:** Sử dụng OWASP Java HTML Sanitizer nếu bạn phải cho phép người dùng nhập HTML.
3.  **Triển khai CSP:** Đây là lớp bảo vệ quan trọng giúp giảm thiểu thiệt hại ngay cả khi có lỗ hổng XSS.
4.  **Sử dụng Cookie `HttpOnly`:** Để bảo vệ token khỏi bị đánh cắp, như đã đề cập trong bài [[Token Cookie]].
5.  **Cập nhật thư viện:** Luôn giữ các thư viện (Spring, Thymeleaf,...) ở phiên bản mới nhất để được vá các lỗ hổng đã biết.
