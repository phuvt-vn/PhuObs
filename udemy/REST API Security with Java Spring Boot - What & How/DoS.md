# Phòng chống tấn công Từ chối Dịch vụ (Denial of Service - DoS)

## 1. Khái niệm

**Tấn công Từ chối Dịch vụ (DoS)** là một kiểu tấn công mạng nhằm làm cho một dịch vụ (website, ứng dụng) không thể truy cập được bởi người dùng hợp lệ. Kẻ tấn công làm điều này bằng cách làm quá tải hệ thống mục tiêu với một lượng lớn yêu cầu, khiến tài nguyên của hệ thống (CPU, bộ nhớ, băng thông) bị cạn kiệt.

**Ví dụ đơn giản:** Hãy tưởng tượng quán cà phê của bạn có một nhân viên. Bỗng nhiên, 500 người ùa vào quán cùng lúc, không mua hàng mà chỉ la hét và chặn lối đi. Kết quả là những khách hàng thật sự không thể vào mua hàng, và quán của bạn coi như "sập".

### Phân biệt DoS và DDoS

-   **DoS (Denial of Service):** Tấn công từ **một** nguồn duy nhất.
-   **DDoS (Distributed Denial of Service):** Tấn công từ **nhiều** nguồn khác nhau (một đội quân máy tính ma). DDoS khó chống đỡ hơn nhiều.

### Các loại tấn công phổ biến:
1.  **Tấn công làm cạn kiệt băng thông (Bandwidth Exhaustion):** Gửi một lượng lớn dữ liệu rác để làm nghẽn mạng.
2.  **Tấn công làm cạn kiệt tài nguyên (Resource Exhaustion):** Gửi các yêu cầu đòi hỏi nhiều xử lý từ server (CPU, memory).
3.  **Tấn công tầng ứng dụng (Application Layer Attacks):** Khai thác các API xử lý chậm hoặc các lỗ hổng logic của ứng dụng.

## 2. Liên kết với các chủ đề khác
- **[[Rate Limiting]]**: Là cơ chế cốt lõi để phòng chống DoS ở tầng ứng dụng.
- **[[Audit Log]]**: Ghi lại các sự kiện bị chặn hoặc các IP đáng ngờ để phục vụ việc điều tra.
- **[[Web Application Firewall]]**: Một lớp phòng thủ ở biên mạng, giúp chặn các cuộc tấn công trước khi chúng đến được ứng dụng của bạn.

## 3. Hướng dẫn triển khai phòng chống DoS trong Spring Boot

Chúng ta sẽ xây dựng một hệ thống phòng thủ đa lớp, bắt đầu từ lớp cơ bản nhất là Rate Limiting.

### 3.1. Bước 1: Cấu hình

Đầu tiên, định nghĩa các quy tắc trong `application.yml`.

```yaml
# File: application.yml
dos:
  protection:
    enabled: true
    # Giới hạn chung: 100 yêu cầu/phút cho mỗi IP
    global-rate-limit:
      max-requests: 100
      window: PT1M # ISO-8601 duration format (1 Phút)
    # Ngưỡng chặn IP: nếu vi phạm 15 lần trong 1 giờ, chặn IP đó trong 1 giờ
    block-threshold: 15
    block-duration-hours: 1
    # Các đường dẫn không cần bảo vệ
    whitelisted-paths:
      - "/actuator/health"
      - "/favicon.ico"

spring:
  data:
    redis:
      host: localhost
      port: 6379
```
Tạo một lớp `DoSProtectionProperties` để đọc các cấu hình này.

### 3.2. Bước 2: Dịch vụ Rate Limiting với Redis

Service này sử dụng Redis để theo dõi và giới hạn số lượng yêu cầu từ mỗi địa chỉ IP.

```java
// File: service/RateLimitingService.java
@Service
public class RateLimitingService {
    private final RedisTemplate<String, String> redisTemplate;

    public RateLimitingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Kiểm tra xem một yêu cầu có được phép hay không.
     * Sử dụng thuật toán Sliding Window Log trên Redis.
     */
    public boolean isAllowed(String key, int maxRequests, Duration window) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - window.toMillis();

        // 1. Xóa các bản ghi yêu cầu cũ
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        // 2. Đếm số yêu cầu còn lại
        Long currentRequests = redisTemplate.opsForZSet().zCard(key);
        // 3. So sánh với giới hạn
        if (currentRequests == null || currentRequests < maxRequests) {
            // 4. Nếu hợp lệ, ghi lại yêu cầu mới và cho phép
            redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
            redisTemplate.expire(key, window.plus(Duration.ofMinutes(1))); // Thêm buffer để tránh race condition
            return true;
        }
        // 5. Nếu vượt quá, từ chối
        return false;
    }
}
```

### 3.3. Bước 3: Filter - Chốt chặn chính

`Filter` là trái tim của hệ thống, nó sẽ chặn và kiểm tra mọi yêu cầu gửi đến ứng dụng.

```java
// File: filter/DoSProtectionFilter.java
public class DoSProtectionFilter extends OncePerRequestFilter {
    private final RateLimitingService rateLimitingService;
    private final DoSDetectionService dosDetectionService; // Sẽ tạo ở bước nâng cao
    private final DoSProtectionProperties properties;

    // ... constructor ...

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled() || isWhitelistedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = dosDetectionService.getClientIpAddress(request);

        // Kiểm tra IP có bị chặn không (sẽ implement ở bước nâng cao)
        if (dosDetectionService.isBlocked(clientIp)) {
            handleBlockedRequest(response, "IP_BLOCKED");
            return;
        }

        // Áp dụng global rate limit
        if (!rateLimitingService.isAllowed("global:" + clientIp, properties.getGlobalRateLimit().getMaxRequests(), properties.getGlobalRateLimit().getWindow())) {
            dosDetectionService.recordViolation(clientIp, "RATE_LIMIT_GLOBAL");
            handleBlockedRequest(response, "RATE_LIMIT_EXCEEDED");
            return;
        }

        filterChain.doFilter(request, response);
    }
    
    // ... các helper methods khác ...
}
```

### 3.4. Bước 4: Đăng ký Filter

Cuối cùng, đăng ký `Filter` vào chuỗi filter của Spring Boot với độ ưu tiên cao nhất.

```java
// File: config/DoSProtectionConfiguration.java
@Configuration
@EnableConfigurationProperties(DoSProtectionProperties.class)
public class DoSProtectionConfiguration {

    @Bean
    public FilterRegistrationBean<DoSProtectionFilter> dosProtectionFilterRegistration(DoSProtectionFilter filter) {
        FilterRegistrationBean<DoSProtectionFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/api/*"); // Chỉ áp dụng cho /api/*
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // Chạy đầu tiên
        return registration;
    }
    
    // ... các bean khác ...
}
```

## 4. Các lớp phòng thủ nâng cao

### 4.1. Tự động chặn IP và phát hiện hành vi đáng ngờ

Tạo một `DoSDetectionService` để theo dõi số lần vi phạm. Nếu một IP vi phạm quá nhiều lần, nó sẽ bị chặn tạm thời.

```java
// File: service/DoSDetectionService.java
@Service
public class DoSDetectionService {
    private final RedisTemplate<String, String> redisTemplate;
    private final DoSProtectionProperties properties;
    private final AuditService auditService; // Dịch vụ ghi log

    // ... constructor ...

    public void recordViolation(String clientIp, String violationType) {
        // Ghi lại vi phạm vào Redis
        String violationKey = "dos_violations:" + clientIp;
        redisTemplate.opsForZSet().add(violationKey, violationType + ":" + System.currentTimeMillis(), System.currentTimeMillis());
        
        // Ghi log audit
        auditService.logSecurityEvent(clientIp, "DOS_VIOLATION", "Type: " + violationType);

        // Kiểm tra xem có nên chặn IP này không
        checkAndBlockIp(clientIp);
    }

    private void checkAndBlockIp(String clientIp) {
        // Đếm số vi phạm trong cửa sổ thời gian
        Long violationCount = redisTemplate.opsForZSet().count(...);

        if (violationCount != null && violationCount >= properties.getBlockThreshold()) {
            blockIp(clientIp);
        }
    }

    private void blockIp(String clientIp) {
        String blockKey = "dos_blocked:" + clientIp;
        redisTemplate.opsForValue().set(blockKey, "blocked", Duration.ofHours(properties.getBlockDurationHours()));
        auditService.logSecurityEvent(clientIp, "IP_BLOCKED", "Blocked for " + properties.getBlockDurationHours() + " hours.");
    }
    
    public boolean isBlocked(String clientIp) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("dos_blocked:" + clientIp));
    }
    
    // ... các helper methods khác ...
}
```

## 5. Best Practices và Chiến lược tổng thể

1.  **Phòng thủ theo chiều sâu (Defense in Depth):** Kết hợp nhiều lớp bảo vệ: Firewall, CDN (như Cloudflare), WAF (Web Application Firewall), và các biện pháp ở tầng ứng dụng như đã trình bày.
2.  **Cấu hình Rate Limit hợp lý:** Phân tích lưu lượng truy cập để đặt ra giới hạn phù hợp cho từng API. API đăng nhập cần giới hạn chặt hơn API công khai.
3.  **Sử dụng Whitelist:** Cho phép các IP đáng tin cậy (dịch vụ nội bộ, đối tác) bỏ qua kiểm tra.
4.  **Giám sát và Cảnh báo:** Sử dụng Prometheus, Grafana để theo dõi các metric (số request bị chặn, số IP bị block) và cài đặt cảnh báo khi có dấu hiệu bất thường.
5.  **Tối ưu hóa ứng dụng:** Code xử lý nhanh, hiệu quả sẽ khó bị tấn công làm cạn kiệt tài nguyên hơn.

## 6. Testing

Kiểm thử là bước không thể thiếu để đảm bảo hệ thống phòng chống DoS hoạt động đúng.

```java
@SpringBootTest
@AutoConfigureMockMvc
class DoSProtectionIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private RedisTemplate<String, String> redisTemplate;
    @Autowired private DoSProtectionProperties properties;

    @BeforeEach
    void setUp() {
        // Dọn dẹp Redis trước mỗi test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void whenGlobalRateLimitExceeded_shouldReturnTooManyRequests() throws Exception {
        int maxRequests = properties.getGlobalRateLimit().getMaxRequests();
        String attackerIp = "10.0.0.1";

        // Gửi số request bằng giới hạn -> OK
        for (int i = 0; i < maxRequests; i++) {
            mockMvc.perform(get("/api/some-endpoint").with(req -> {
                req.setRemoteAddr(attackerIp);
                return req;
            })).andExpect(status().isOk());
        }

        // Gửi request thứ maxRequests + 1 -> Bị chặn
        mockMvc.perform(get("/api/some-endpoint").with(req -> {
            req.setRemoteAddr(attackerIp);
            return req;
        }))
        .andExpect(status().isTooManyRequests());
    }
}
