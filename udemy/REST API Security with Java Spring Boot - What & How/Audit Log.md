# Audit Log: Ghi lại và Theo dõi Hoạt động Hệ thống

## 1. Khái niệm

**Audit Log (Nhật ký Kiểm toán)** là một bản ghi chi tiết, theo trình tự thời gian, về tất cả các hoạt động, sự kiện và thay đổi trong một hệ thống.

**Hãy tưởng tượng:** Audit Log giống như một camera an ninh trong một tòa nhà. Nó ghi lại mọi hành động: ai vào, ai ra, họ làm gì, vào lúc nào. Khi có sự cố xảy ra (ví dụ: mất cắp), cuốn băng từ camera này là bằng chứng vô giá để điều tra và tìm ra sự thật.

Trong hệ thống phần mềm, Audit Log cung cấp một "dấu vết điện tử" (digital footprint) toàn diện về các hành động của người dùng, sự kiện hệ thống và các hoạt động liên quan đến bảo mật.

### Các thành phần chính của một bản ghi Audit:

1.  **Thời gian (Timestamp)**: Khi nào sự kiện xảy ra?
2.  **Chủ thể (Actor/User)**: Ai đã thực hiện hành động? (e.g., `user_id: 123`)
3.  **Hành động (Action)**: Đã làm gì? (e.g., `LOGIN`, `UPDATE_PRODUCT`)
4.  **Đối tượng (Object/Resource)**: Hành động tác động lên cái gì? (e.g., `product_id: 456`)
5.  **Kết quả (Outcome)**: Thành công hay thất bại? (`SUCCESS`, `FAILURE`)
6.  **Nguồn (Source)**: Hành động đến từ đâu? (e.g., `ip_address: 192.168.1.10`)
7.  **Chi tiết (Context)**: Thông tin bổ sung (e.g., dữ liệu cũ và mới).

### Ưu điểm:
- **Tuân thủ quy định**: Đáp ứng các tiêu chuẩn như GDPR, HIPAA, PCI DSS.
- **Điều tra sự cố**: Truy vết các cuộc tấn công hoặc lỗi hệ thống.
- **Phân tích pháp y**: Cung cấp bằng chứng số cho các mục đích pháp lý.
- **Giám sát hành vi**: Phát hiện các hoạt động bất thường hoặc lạm dụng.
- **Trách nhiệm giải trình**: Mọi hành động đều được ghi lại và có thể quy trách nhiệm.

### Nhược điểm:
- **Chi phí lưu trữ**: Có thể tạo ra một lượng dữ liệu khổng lồ.
- **Ảnh hưởng hiệu suất**: Việc ghi log liên tục có thể làm chậm hệ thống nếu không được tối ưu.
- **Phức tạp trong quản lý**: Cần có chiến lược lưu trữ, sao lưu, và phân tích hiệu quả.

## 2. Liên kết với các chủ đề khác:
- [[Access Control List]]: Ghi log các lần kiểm tra quyền và các truy cập bị từ chối.
- [[API Key]]: Ghi log việc sử dụng và các lần lạm dụng API key.
- [[JWT]]: Ghi log các sự kiện tạo, xác thực, và hết hạn của token.
- [[XSS]] / [[SQL Injection]]: Ghi log các nỗ lực tấn công bị phát hiện và ngăn chặn.
- [[Rate Limiting]]: Ghi log các request bị từ chối do vượt quá giới hạn.

## 3. Hướng dẫn triển khai với Spring Boot 3.x

### 3.1. Bước 1: Thiết lập nền tảng (Entity, Enums, Repository)

**Dependencies (`pom.xml`):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**Enums định nghĩa các loại sự kiện:**
```java
// File: com/example/auditlog/entity/AuditEnums.java
public enum AuditEventType { AUTHENTICATION, DATA_ACCESS, CONFIGURATION_CHANGE, SECURITY_EVENT }
public enum AuditAction { LOGIN, LOGOUT, CREATE, READ, UPDATE, DELETE, ACCESS_DENIED }
public enum AuditResult { SUCCESS, FAILURE, UNAUTHORIZED, BLOCKED }
public enum AuditSeverity { LOW, MEDIUM, HIGH, CRITICAL }
```

**Entity `AuditEvent.java`:**
Đây là cấu trúc cho bảng `audit_events` trong database.
```java
package com.example.auditlog.entity;

@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_user", columnList = "userId"),
    @Index(name = "idx_audit_event_type", columnList = "eventType")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @CreatedDate @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AuditEventType eventType;
    
    @Column(length = 100)
    private String userId;
    
    @Column(length = 45)
    private String ipAddress;
    
    @Column(length = 100)
    private String resourceType;
    
    @Column(length = 100)
    private String resourceId;
    
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AuditAction action;
    
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AuditResult result;
    
    @Column(columnDefinition = "TEXT")
    private String details; // Dữ liệu chi tiết dưới dạng JSON
    
    @Enumerated(EnumType.STRING)
    private AuditSeverity severity;
    
    @Builder.Default
    private Boolean suspicious = false;
}
```

**Repository `AuditEventJpaRepository.java`:**
```java
package com.example.auditlog.repository;

@Repository
public interface AuditEventJpaRepository extends JpaRepository<AuditEvent, Long> {
    // Cung cấp các phương thức tìm kiếm cơ bản
    Page<AuditEvent> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
    Page<AuditEvent> findByEventTypeOrderByTimestampDesc(AuditEventType eventType, Pageable pageable);
    Page<AuditEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
```

### 3.2. Bước 2: Service và Listener để ghi log tự động

**`AuditService.java`:** Service trung tâm để xử lý việc ghi log.
```java
package com.example.auditlog.service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {
    private final AuditEventJpaRepository jpaRepository;
    private final ObjectMapper objectMapper; // Dùng để chuyển đổi object thành JSON

    // Ghi log bất đồng bộ để không ảnh hưởng hiệu năng
    @Async("auditExecutor")
    public void logEvent(AuditEvent.AuditEventBuilder builder) {
        try {
            AuditEvent event = builder.build();
            // Có thể thêm logic phát hiện bất thường ở đây
            jpaRepository.save(event);
            log.debug("Audit event logged: {}", event.getEventType());
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", e.getMessage(), e);
        }
    }
    
    // Helper để tạo builder với các thông tin chung từ request
    public AuditEvent.AuditEventBuilder createBaseAuditBuilder(String userId, HttpServletRequest request) {
        return AuditEvent.builder()
            .userId(userId)
            .ipAddress(getClientIp(request))
            .details(buildDetails(request));
    }
    
    // ... các helper methods khác ...
}
```

**`SecurityEventListener.java`:** Lắng nghe các sự kiện của Spring Security để ghi log đăng nhập, đăng xuất, truy cập bị từ chối.
```java
package com.example.auditlog.config;

@Component
@RequiredArgsConstructor
public class SecurityEventListener {
    private final AuditService auditService;

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String userId = event.getAuthentication().getName();
        auditService.logEvent(
            AuditEvent.builder()
                .eventType(AuditEventType.AUTHENTICATION)
                .userId(userId)
                .action(AuditAction.LOGIN)
                .result(AuditResult.SUCCESS)
                .severity(AuditSeverity.LOW)
        );
    }

    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        // ... Log login thất bại ...
    }

    @EventListener
    public void handleAccessDenied(AuthorizationDeniedEvent event) {
        // ... Log truy cập bị từ chối ...
    }
}
```

### 3.3. Bước 3: Ghi log các API Endpoint (Sử dụng AOP)

Sử dụng Aspect-Oriented Programming (AOP) để tự động ghi log các lời gọi đến Controller mà không cần sửa code của Controller.

**Tạo Annotation `@Auditable`:**
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    AuditAction action();
    AuditEventType eventType() default AuditEventType.DATA_ACCESS;
}
```

**Tạo Aspect `AuditAspect.java`:**
```java
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditService auditService;
    private final HttpServletRequest request;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logSuccessfulAction(JoinPoint joinPoint, Auditable auditable, Object result) {
        logAuditEvent(auditable, AuditResult.SUCCESS);
    }

    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "exception")
    public void logFailedAction(JoinPoint joinPoint, Auditable auditable, Exception exception) {
        logAuditEvent(auditable, AuditResult.FAILURE);
    }

    private void logAuditEvent(Auditable auditable, AuditResult result) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.logEvent(
            auditService.createBaseAuditBuilder(userId, request)
                .eventType(auditable.eventType())
                .action(auditable.action())
                .result(result)
        );
    }
}
```

**Sử dụng trong Controller:**
```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    @PostMapping
    @Auditable(action = AuditAction.CREATE)
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        // ... logic tạo sản phẩm ...
    }
}
```

### 3.4. Bước 4: Xem và Phân tích Log

Tạo một `AuditController` để admin có thể xem, tìm kiếm và phân tích log.
```java
@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {
    private final AuditService auditService;
    // ...

    @GetMapping("/events")
    public Page<AuditEvent> searchEvents(
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) AuditEventType eventType,
        Pageable pageable
    ) {
        // ... logic tìm kiếm và trả về kết quả ...
    }
}
```

## 4. Best Practices (Thực hành tốt nhất)

1.  **Ghi log bất đồng bộ (`@Async`)**: Luôn ghi log trong một thread riêng để không làm chậm request chính của người dùng.
2.  **Ghi log có cấu trúc (Structured Logging)**: Sử dụng JSON cho trường `details` để dễ dàng truy vấn và phân tích sau này.
3.  **Bảo vệ Log**: Chính bản thân audit log cũng là dữ liệu nhạy cảm. Phân quyền truy cập log một cách chặt chẽ (chỉ `ADMIN` hoặc `AUDITOR`).
4.  **Không ghi thông tin nhạy cảm**: Tránh ghi mật khẩu, API key đầy đủ, hoặc thông tin thẻ tín dụng vào log.
5.  **Chính sách lưu trữ (Retention Policy)**: Xây dựng một tác vụ định kỳ (`@Scheduled`) để xóa hoặc lưu trữ (archive) các log cũ nhằm tiết kiệm không gian.
6.  **Tập trung hóa Log**: Trong môi trường microservices, sử dụng các công cụ như ELK Stack (Elasticsearch, Logstash, Kibana) hoặc Splunk để tập trung và phân tích log từ nhiều dịch vụ.
7.  **Giám sát và Cảnh báo**: Tích hợp với các công cụ như Prometheus và Grafana để tạo cảnh báo cho các hoạt động đáng ngờ (ví dụ: số lần đăng nhập thất bại tăng đột biến).

## 5. Testing

Kiểm thử hệ thống audit là rất quan trọng để đảm bảo nó hoạt động đúng và không bỏ sót sự kiện.

```java
@SpringBootTest
class AuditServiceTest {
    @Autowired private AuditService auditService;
    @Autowired private AuditEventJpaRepository repository;

    @Test
    void shouldLogAuditEventSuccessfully() {
        // Given
        String userId = "test-user";
        
        // When
        auditService.logEvent(
            AuditEvent.builder()
                .userId(userId)
                .eventType(AuditEventType.AUTHENTICATION)
                .action(AuditAction.LOGIN)
                .result(AuditResult.SUCCESS)
        );

        // Then
        // Chờ xử lý bất đồng bộ hoàn tất
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            var events = repository.findByUserIdOrderByTimestampDesc(userId, PageRequest.of(0, 1));
            assertThat(events.getContent()).hasSize(1);
            assertThat(events.getContent().get(0).getUserId()).isEqualTo(userId);
        });
    }
}
