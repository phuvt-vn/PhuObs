# Tấn công SQL Injection và Cách phòng chống

## 1. SQL Injection là gì?

**SQL Injection** là một trong những kỹ thuật tấn công web phổ biến và nguy hiểm nhất. Nó xảy ra khi kẻ tấn công có thể chèn (inject) các câu lệnh SQL độc hại vào các truy vấn mà ứng dụng gửi đến cơ sở dữ liệu.

**Ví dụ đơn giản:**
Hãy tưởng tượng một form đăng nhập. Thay vì nhập tên người dùng bình thường, kẻ tấn công nhập:
`' OR '1'='1' --`

Nếu ứng dụng xây dựng câu lệnh SQL bằng cách nối chuỗi một cách không an toàn:
`"SELECT * FROM users WHERE username = '" + userInput + "'"`

Câu lệnh cuối cùng sẽ trở thành:
`SELECT * FROM users WHERE username = '' OR '1'='1' --'`

Điều kiện `'1'='1'` luôn đúng, và phần còn lại của câu lệnh bị vô hiệu hóa bởi `--`. Kết quả là kẻ tấn công có thể đăng nhập mà không cần mật khẩu.

### Hậu quả nghiêm trọng:
- **Đánh cắp dữ liệu:** Lấy cắp thông tin nhạy cảm của người dùng, thông tin tài chính.
- **Thay đổi/Xóa dữ liệu:** Phá hoại cơ sở dữ liệu, làm tê liệt hoạt động kinh doanh.
- **Chiếm quyền kiểm soát:** Tạo tài khoản admin, chiếm quyền điều khiển hệ thống.

## 2. Các loại tấn công phổ biến

1.  **In-band SQLi (Tấn công trực tiếp):** Phổ biến nhất, kẻ tấn công dùng cùng một kênh để tấn công và nhận kết quả.
    *   **Error-based:** Cố tình gây lỗi để server tiết lộ thông tin về cấu trúc DB.
    *   **Union-based:** Dùng `UNION` để kết hợp kết quả của truy vấn độc hại vào kết quả hợp lệ.
2.  **Inferential SQLi (Tấn công suy luận - Blind SQLi):** Khi server không trả về lỗi, kẻ tấn công suy luận thông tin dựa trên phản ứng của hệ thống.
    *   **Boolean-based:** Gửi các câu lệnh đúng/sai và quan sát sự thay đổi của trang web.
    *   **Time-based:** Dùng các hàm delay (`SLEEP()`) để làm chậm phản hồi, từ đó suy ra thông tin.

## 3. Nguyên tắc vàng & Cách phòng chống cốt lõi

**Nguyên tắc vàng:** **Không bao giờ tin tưởng dữ liệu đầu vào của người dùng và luôn luôn sử dụng truy vấn tham số hóa (Parameterized Queries).**

May mắn là các framework hiện đại như Spring Boot đã tích hợp sẵn các cơ chế phòng chống hiệu quả.

### 3.1. Sử dụng Spring Data JPA (Cách an toàn nhất)

Đây là phương pháp được khuyến nghị hàng đầu vì nó tự động xử lý việc tham số hóa, giúp loại bỏ gần như hoàn toàn nguy cơ SQL Injection.

**Cách làm đúng:**
```java
// File: repository/ProductJpaRepository.java
@Repository
public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    // ✅ AN TOÀN: Spring Data JPA tự động tạo PreparedStatement
    // Câu lệnh thực tế: SELECT * FROM products WHERE category = ?
    // Giá trị của 'category' được bind một cách an toàn.
    List<Product> findByCategory(String category);

    // ✅ AN TOÀN: Sử dụng @Query với tham số đặt tên (:paramName)
    // JPA sẽ thay thế :categoryName bằng một tham số an toàn.
    @Query("SELECT p FROM Product p WHERE p.category = :categoryName AND p.price <= :maxPrice")
    List<Product> findByFilter(@Param("categoryName") String category, @Param("maxPrice") BigDecimal maxPrice);
}
```

**Cách làm sai (TUYỆT ĐỐI KHÔNG LÀM):**
```java
// ❌ SAI: Nối chuỗi trực tiếp trong @Query
@Query("SELECT p FROM Product p WHERE p.category = '" + category + "'") // LỖ HỔNG BẢO MẬT!
List<Product> findByCategoryUnsafe(String category);
```

### 3.2. Sử dụng MyBatis (An toàn nếu cẩn thận)

MyBatis cho phép kiểm soát câu lệnh SQL nhiều hơn, nhưng cũng đòi hỏi lập trình viên phải cẩn thận hơn.

**Nguyên tắc của MyBatis:**
- `#{}` = **AN TOÀN**. MyBatis sẽ tạo PreparedStatement và tham số hóa giá trị. Luôn dùng cho giá trị đầu vào của người dùng.
- `${}` = **NGUY HIỂM**. MyBatis sẽ thay thế trực tiếp chuỗi vào câu lệnh SQL. Chỉ dùng cho các trường hợp không thể tham số hóa (như tên cột trong `ORDER BY`) và **BẮT BUỘC** phải được kiểm tra qua một danh sách trắng (whitelist).

**Cách làm đúng:**
```xml
<!-- File: mappers/ProductMapper.xml -->
<mapper namespace="com.example.repository.ProductMyBatisMapper">
  <!-- ✅ AN TOÀN: Sử dụng #{} cho tất cả các giá trị đầu vào -->
  <select id="searchProducts" resultType="Product">
    SELECT * FROM products
    WHERE category = #{searchDto.category}
    AND price <= #{searchDto.maxPrice}
    
    <!-- ⚠️ NGUY HIỂM NHƯNG AN TOÀN NẾU ĐÃ VALIDATE:
         Chỉ dùng ${} cho ORDER BY sau khi đã kiểm tra qua whitelist. -->
    ORDER BY ${searchDto.orderByColumn} ${searchDto.sortDirection}
  </select>
</mapper>
```
**Logic kiểm tra whitelist trong Service:**
```java
// File: service/ProductService.java
private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of("name", "price", "category");

public List<Product> searchProductsWithMyBatis(ProductSearchDto searchDto) {
    // Whitelist validation
    if (!ALLOWED_SORT_COLUMNS.contains(searchDto.getOrderByColumn())) {
        throw new IllegalArgumentException("Cột sắp xếp không hợp lệ.");
    }
    // ... gọi mapper ...
}
```

## 4. Các lớp phòng thủ bổ sung

1.  **Xác thực dữ liệu đầu vào (Input Validation):**
    - Là tuyến phòng thủ đầu tiên.
    - Sử dụng Bean Validation (`@NotBlank`, `@Size`, `@Pattern`) trên các DTO để đảm bảo dữ liệu đầu vào có định dạng đúng và không chứa các ký tự nguy hiểm.
    - Luôn áp dụng nguyên tắc **whitelist** (chỉ cho phép những gì đã biết là tốt) thay vì **blacklist** (cố gắng chặn những gì đã biết là xấu).

2.  **Giới hạn quyền của tài khoản Database:**
    - Tài khoản mà ứng dụng dùng để kết nối DB chỉ nên có những quyền tối thiểu cần thiết (SELECT, INSERT, UPDATE, DELETE trên các bảng cụ thể).
    - **Không** cấp các quyền nguy hiểm như `DROP`, `ALTER`, `CREATE`.

3.  **Xử lý lỗi an toàn:**
    - Không bao giờ hiển thị thông báo lỗi chi tiết của cơ sở dữ liệu cho người dùng cuối.
    - Cấu hình một `ControllerAdvice` để bắt các ngoại lệ `DataAccessException` và trả về một thông báo lỗi chung chung.

## 5. Giám sát và Phát hiện

- **Audit Log:** Ghi lại các truy vấn bất thường hoặc các lỗi SQL để phân tích sau này.
- **Web Application Firewall (WAF):** Sử dụng WAF để phát hiện và chặn các mẫu tấn công SQL Injection phổ biến ở tầng mạng, trước khi chúng đến được ứng dụng của bạn.
- **Monitoring:** Dùng các công cụ như Prometheus/Grafana để giám sát số lượng lỗi DB. Nếu số lỗi tăng đột biến, đó có thể là dấu hiệu của một cuộc tấn công.
