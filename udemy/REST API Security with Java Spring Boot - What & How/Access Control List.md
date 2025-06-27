# Access Control List (ACL) trong Spring Boot

## 1. Giải thích cho người không chuyên (Non-technical Explanation)

### ACL là gì?

Access Control List (ACL), hay "Danh sách Kiểm soát Truy cập", là một cơ chế bảo mật giống như một danh sách khách mời cho một sự kiện đặc biệt. Thay vì mọi người đều có thể vào, chỉ những người có tên trong danh sách và có "vé" phù hợp mới được phép tham gia và thực hiện những hành động nhất định.

Trong thế giới phần mềm, ACL quyết định ai (người dùng, nhóm người dùng) được phép làm gì (xem, sửa, xóa) với một tài nguyên cụ thể (tài liệu, bài viết, hồ sơ khách hàng).

### Ví dụ thực tế

Hãy tưởng tượng bạn đang quản lý một hệ thống tài liệu nội bộ của công ty trên Google Drive:

*   **Tài nguyên (Object):** Một file Google Docs có tên là "Kế hoạch kinh doanh Q4.docx".
*   **Người dùng (Subject):**
    *   `Alice` (Giám đốc dự án)
    *   `Bob` (Thành viên nhóm Marketing)
    *   `Charlie` (Thực tập sinh)
*   **Quyền (Permission):** Xem (Read), Bình luận (Comment), Chỉnh sửa (Write), Quản lý (Admin).

Danh sách kiểm soát truy cập (ACL) cho file này có thể trông như sau:

1.  **Alice** có quyền **Chỉnh sửa (Write)**: Cô ấy có thể xem và thay đổi nội dung tài liệu.
2.  **Bob** có quyền **Bình luận (Comment)**: Anh ấy chỉ có thể xem và để lại nhận xét, không thể sửa trực tiếp.
3.  **Charlie** có quyền **Xem (Read)**: Cậu ấy chỉ được phép đọc tài liệu, không thể làm gì khác.
4.  Bất kỳ ai khác (không có trong danh sách) sẽ không thể truy cập được file này.

ACL cho phép bạn kiểm soát quyền truy cập một cách rất chi tiết và linh hoạt, đảm bảo đúng người có đúng quyền trên đúng tài nguyên.

### Ưu điểm

*   **Kiểm soát chi tiết (Fine-grained):** Cho phép cấp quyền rất cụ thể cho từng người dùng trên từng tài nguyên.
*   **Linh hoạt:** Dễ dàng thay đổi quyền khi vai trò của nhân viên thay đổi (ví dụ: Bob được thăng chức và cần quyền chỉnh sửa).
*   **Bảo mật cao:** Ngăn chặn truy cập trái phép vào dữ liệu nhạy cảm.

### Nhược điểm

*   **Phức tạp khi quản lý:** Với hàng nghìn người dùng và tài liệu, việc quản lý danh sách này có thể trở nên rất phức tạp.
*   **Hiệu suất:** Kiểm tra quyền cho mỗi yêu cầu có thể làm hệ thống chậm đi một chút.

## 2. Liên kết với các chủ đề bảo mật khác

*   **[[Authentication vs. Authorization]]**: Xác thực (Authentication) là kiểm tra "bạn là ai?" (đăng nhập), còn Phân quyền (Authorization) như ACL là kiểm tra "bạn được phép làm gì?".
*   **[[Role-Based Access Control (RBAC)]]**: RBAC đơn giản hơn, cấp quyền dựa trên vai trò (ví dụ: mọi "Quản lý" đều có quyền sửa). ACL chi tiết hơn, có thể cấp quyền cho một cá nhân cụ thể bất kể vai trò.
*   **[[Audit Log]]**: Ghi lại mọi lần ai đó cố gắng truy cập hoặc thay đổi quyền trong ACL, rất quan trọng để theo dõi và điều tra các vấn đề bảo mật.
*   **[[API Key]]**: Một API key có thể được liên kết với một danh sách quyền (ACL) để giới hạn những gì mà ứng dụng bên ngoài có thể làm.

## 3. Triển khai với Spring Boot 3.x và Java 21

Spring Security cung cấp một module `spring-security-acl` mạnh mẽ để triển khai ACL. Dưới đây là hướng dẫn chi tiết.

### 3.1. Dependencies

Thêm các dependency cần thiết vào file `pom.xml`:

```xml
<!-- Spring Boot Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Spring Security ACL -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-acl</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

### 3.2. Các lớp Entity cho ACL

Spring Security ACL yêu cầu 4 bảng trong cơ sở dữ liệu. Chúng ta sẽ định nghĩa các entity tương ứng.

```java
// File: src/main/java/com/example/acl/entity/AclEntities.java
package com.example.acl.entity;

// ... imports ...

/**
 * Đại diện cho một lớp được bảo vệ (ví dụ: com.example.acl.entity.Document).
 */
@Entity
@Table(name = "acl_class")
public class AclClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String className;

    // Constructors, Getters, Setters
}

/**
 * Đại diện cho một Security Identifier (SID), có thể là một người dùng (principal)
 * hoặc một vai trò (authority).
 */
@Entity
@Table(name = "acl_sid")
public class AclSid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean principal; // true: principal, false: authority

    @Column(nullable = false, unique = true, length = 100)
    private String sid; // Tên người dùng hoặc tên vai trò

    // Constructors, Getters, Setters
}

/**
 * Lưu trữ thông tin định danh của một đối tượng cụ thể được bảo vệ.
 */
@Entity
@Table(name = "acl_object_identity")
public class AclObjectIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_id_class", nullable = false)
    private AclClass objectIdClass;

    @Column(name = "object_id_identity", nullable = false)
    private Long objectIdIdentity; // ID của đối tượng (ví dụ: documentId)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_object")
    private AclObjectIdentity parentObject; // Hỗ trợ kế thừa quyền

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_sid")
    private AclSid ownerSid; // Chủ sở hữu của đối tượng

    @Column(nullable = false)
    private boolean entriesInheriting = true;

    // Constructors, Getters, Setters
}

/**
 * Một Access Control Entry (ACE), là một bản ghi quyền cụ thể trong ACL.
 */
@Entity
@Table(name = "acl_entry")
public class AclEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acl_object_identity", nullable = false)
    private AclObjectIdentity aclObjectIdentity;

    @Column(nullable = false)
    private int aceOrder; // Thứ tự áp dụng ACE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sid", nullable = false)
    private AclSid sid; // Người dùng/vai trò được cấp quyền

    @Column(nullable = false)
    private int mask; // Bitmask đại diện cho quyền (ví dụ: READ=1, WRITE=2)

    @Column(nullable = false)
    private boolean granting; // true: cấp quyền, false: từ chối quyền

    @Column(nullable = false)
    private boolean auditSuccess = false; // Ghi log khi kiểm tra thành công

    @Column(nullable = false)
    private boolean auditFailure = false; // Ghi log khi kiểm tra thất bại

    // Constructors, Getters, Setters
}
```

### 3.3. Các Repository

```java
// File: src/main/java/com/example/acl/repository/AclRepositories.java
package com.example.acl.repository;

// ... imports ...

@Repository
public interface AclClassRepository extends JpaRepository<AclClass, Long> {
    Optional<AclClass> findByClassName(String className);
}

@Repository
public interface AclSidRepository extends JpaRepository<AclSid, Long> {
    Optional<AclSid> findBySid(String sid);
}

@Repository
public interface AclObjectIdentityRepository extends JpaRepository<AclObjectIdentity, Long> {
    Optional<AclObjectIdentity> findByObjectIdClassAndObjectIdIdentity(AclClass objectIdClass, Long objectIdIdentity);
    void deleteByObjectIdClassAndObjectIdIdentity(AclClass objectIdClass, Long objectIdIdentity);
}

@Repository
public interface AclEntryRepository extends JpaRepository<AclEntry, Long> {
    List<AclEntry> findByAclObjectIdentityOrderByAceOrder(AclObjectIdentity aclObjectIdentity);
    void deleteByAclObjectIdentity(AclObjectIdentity aclObjectIdentity);
}
```

### 3.4. Cấu hình Spring Security và ACL

Đây là phần quan trọng để tích hợp ACL vào Spring Security.

```java
// File: src/main/java/com/example/acl/config/AclSecurityConfig.java
package com.example.acl.config;

// ... imports ...

@Configuration
@EnableMethodSecurity
public class AclSecurityConfig {

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
    
    // Cấu hình SecurityFilterChain nếu cần
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
```

### 3.5. AclService - Logic chính

`AclService` sẽ chứa logic để tạo, xóa, cấp và thu hồi quyền, tích hợp caching và audit.

```java
// File: src/main/java/com/example/acl/service/AclService.java
package com.example.acl.service;

// ... imports ...

@Service
@Transactional
public class AclService {
    // ... (dependencies: repositories, auditService, cacheManager)

    public AclService(
            AclClassRepository aclClassRepository,
            AclSidRepository aclSidRepository,
            AclObjectIdentityRepository aclObjectIdentityRepository,
            AclEntryRepository aclEntryRepository,
            AclAuditService auditService,
            CacheManager cacheManager) {
        // ... constructor body
    }
    
    @Cacheable(cacheNames = "aclPermissions", key = "#clazz.name + ':' + #objectId + ':' + #sid + ':' + #permission.name()")
    public boolean hasPermission(Class<?> clazz, Long objectId, String sid, AclPermission permission) {
        // ... implementation with logging and error handling
    }
    
    @CacheEvict(cacheNames = "aclPermissions", allEntries = true)
    public void createAcl(Class<?> clazz, Long objectId, String owner) {
        // ... implementation
    }
    
    @CacheEvict(cacheNames = "aclPermissions", allEntries = true)
    public void deleteAcl(Class<?> clazz, Long objectId) {
        // ... implementation
    }
    
    @CacheEvict(cacheNames = "aclPermissions", allEntries = true)
    public void grantPermission(Class<?> clazz, Long objectId, String sid, AclPermission permission) {
        // ... implementation
    }
    
    @CacheEvict(cacheNames = "aclPermissions", allEntries = true)
    public void revokePermission(Class<?> clazz, Long objectId, String sid, AclPermission permission) {
        // ... implementation
    }

    public List<AclPermissionInfo> getPermissions(Class<?> clazz, Long objectId) {
        // ... implementation
    }
    
    // ... private helper methods and DTOs
}
```

### 3.6. AclPermissionEvaluator và Custom Annotations

Lớp này kết nối `@PreAuthorize` của Spring Security với `AclService` của chúng ta, cùng với các annotation tùy chỉnh để code gọn hơn.

```java
// File: src/main/java/com/example/acl/security/AclAnnotations.java

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@aclPermissionEvaluator.hasPermission(authentication, #objectId, #targetType, #permission)")
public @interface RequirePermission {
    String permission();
    String objectId();
    Class<?> targetType();
}

// File: src/main/java/com/example/acl/security/AclPermissionEvaluator.java
@Component("aclPermissionEvaluator")
public class AclPermissionEvaluator implements PermissionEvaluator {

    private final AclService aclService;

    // ... constructor ...

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        // We use the version with targetType primarily
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if ((authentication == null) || (targetType == null) || !(permission instanceof String)) {
            return false;
        }
        String username = authentication.getName();
        AclPermission aclPermission = AclPermission.valueOf(((String) permission).toUpperCase());
        try {
            Class<?> clazz = Class.forName(targetType);
            return aclService.hasPermission(clazz, (Long) targetId, username, aclPermission);
        } catch (ClassNotFoundException e) {
            throw new AclException("Target class not found: " + targetType, e);
        }
    }
}
```

### 3.7. Áp dụng: Bảo vệ đối tượng `Document`

Tạo `DocumentController` để minh họa cách ACL được áp dụng.

```java
// File: src/main/java/com/example/acl/entity/Document.java
@Entity
public class Document {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String content;
    private String owner;
    // Getters, Setters
}

// File: src/main/java/com/example/acl/service/DocumentService.java
@Service
public class DocumentService {
    // ... dependencies: documentRepository, aclService
    
    @Transactional
    public Document createDocument(String title, String content, String owner) {
        // ... save document
        // ... aclService.createAcl(Document.class, savedDoc.getId(), owner);
        return savedDoc;
    }

    @PreAuthorize("hasPermission(#documentId, 'com.example.acl.entity.Document', 'READ')")
    public Optional<Document> getDocument(Long documentId) {
        return documentRepository.findById(documentId);
    }
    
    // ... other methods for update, delete with @PreAuthorize
}

// File: src/main/java/com/example/acl/controller/DocumentController.java
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    // ... dependencies: documentService, aclService

    // Endpoint to get a document, protected by ACL
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable Long id) {
        return documentService.getDocument(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // Endpoints to manage permissions on a document
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasPermission(#id, 'com.example.acl.entity.Document', 'ADMINISTRATION')")
    public ResponseEntity<Void> grantPermission(@PathVariable Long id, @RequestBody /*...*/) {
        // ... call aclService.grantPermission
        return ResponseEntity.ok().build();
    }
}
```

### 3.8. Sơ đồ luồng (Mermaid Diagram)

Sơ đồ này minh họa luồng kiểm tra quyền khi người dùng yêu cầu truy cập một tài liệu.

```mermaid
sequenceDiagram
    participant Client
    participant Spring Security
    participant DocumentController
    participant DocumentService
    participant AclPermissionEvaluator
    participant AclService
    participant Database

    Client->>+DocumentController: GET /api/documents/{id}
    DocumentController->>+Spring Security: @PreAuthorize check
    Spring Security->>+AclPermissionEvaluator: hasPermission(auth, id, '...Document', 'READ')
    AclPermissionEvaluator->>+AclService: hasPermission(Document.class, id, 'user', READ)
    AclService->>+Database: Query for ACL entries
    Database-->>-AclService: Return ACL entries
    AclService-->>-AclPermissionEvaluator: Return true/false
    AclPermissionEvaluator-->>-Spring Security: Return true/false

    alt Access Granted
        Spring Security-->>-DocumentController: Proceed
        DocumentController->>+DocumentService: getDocument(id)
        DocumentService-->>-DocumentController: Return document
        DocumentController-->>-Client: 200 OK with Document
    else Access Denied
        Spring Security-->>-DocumentController: Throw AccessDeniedException
        DocumentController-->>-Client: 403 Forbidden
    end
```

## 4. Best Practices và Testing

### 4.1. Best Practices

1.  **Quyền tối thiểu**: Chỉ cấp quyền tối thiểu cần thiết.
2.  **Kiểm toán định kỳ**: Thường xuyên xem xét và dọn dẹp các quyền không sử dụng.
3.  **Tối ưu hiệu suất**: Sử dụng cache cho các lần kiểm tra ACL thường xuyên.
4.  **Thao tác hàng loạt**: Triển khai các API để cấp/thu hồi quyền hàng loạt.
5.  **Ghi log kiểm toán**: Ghi lại tất cả các thay đổi quyền và các lần truy cập.

### 4.2. Testing

Viết unit test và integration test cho các kịch bản phân quyền là cực kỳ quan trọng.

```java
// File: AclServiceTest.java
@SpringBootTest
class AclServiceTest {
    
    @Autowired private AclService aclService;
    
    @Test
    void shouldGrantAndCheckPermissionSuccessfully() {
        // Setup
        aclService.createAcl(Document.class, 1L, "owner");
        aclService.grantPermission(Document.class, 1L, "user", AclPermission.READ);
        
        // Assert
        assertTrue(aclService.hasPermission(Document.class, 1L, "user", AclPermission.READ));
        assertFalse(aclService.hasPermission(Document.class, 1L, "user", AclPermission.WRITE));
    }
}

// File: AclControllerTest.java
@WebMvcTest(AclController.class)
class AclControllerTest {
    
    @Autowired private MockMvc mockMvc;
    @MockBean private AclService aclService;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGrantPermissionSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/acl/grant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("..."))
                .andExpect(status().isOk());
        
        verify(aclService).grantPermission(any(), any(), any(), any());
    }
}
```

## 5. Kết luận

Triển khai ACL với Spring Security, mặc dù ban đầu có vẻ phức tạp do yêu cầu về cấu trúc cơ sở dữ liệu, nhưng lại cung cấp một cơ chế phân quyền cực kỳ mạnh mẽ và linh hoạt. Bằng cách tự quản lý các entity và service, chúng ta có toàn quyền kiểm soát logic, dễ dàng tích hợp với JPA và các công cụ khác, đồng thời có thể tùy chỉnh để tối ưu hóa hiệu suất và thêm các tính năng như ghi log kiểm toán chi tiết.
