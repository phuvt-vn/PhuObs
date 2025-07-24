# Bài 29: Ví dụ tạo JNDI và JDBC Connection Pooling

- **Thời lượng**: 13:22
- **Bài trước**: [[28 - Understanding Connection Pooling]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[30 - Customizing Error Pages - Part1]]

---

## Nội dung

Bài học này kết hợp các khái niệm từ hai bài trước để cung cấp một ví dụ hoàn chỉnh về cách định nghĩa một JNDI DataSource sử dụng connection pooling trong Tomcat.

### Bước 1: Chuẩn bị JDBC Driver

-   Connection pool của Tomcat cần có driver JDBC của cơ sở dữ liệu để có thể tạo kết nối.
-   Tải file `.jar` của JDBC driver cho cơ sở dữ liệu của bạn (ví dụ: `mysql-connector-java-8.0.26.jar` cho MySQL).
-   Đặt file `.jar` này vào thư mục `$CATALINA_HOME/lib`. Việc đặt driver ở đây sẽ giúp nó có thể được sử dụng bởi tất cả các ứng dụng web trên server.

### Bước 2: Cấu hình JNDI Resource trong Tomcat

Có hai nơi phổ biến để định nghĩa JNDI resource:

1.  **`$CATALINA_HOME/conf/context.xml` (Global - Toàn cục)**:
    -   Tài nguyên định nghĩa ở đây sẽ có sẵn cho **tất cả** các ứng dụng web được triển khai trên Tomcat.
    -   Đây là lựa chọn tốt cho các tài nguyên được chia sẻ.

2.  **`META-INF/context.xml` (Application-specific - Cụ thể cho ứng dụng)**:
    -   Tạo file `context.xml` trong thư mục `META-INF` của ứng dụng web của bạn.
    -   Tài nguyên định nghĩa ở đây sẽ chỉ có sẵn cho **duy nhất ứng dụng đó**.

**Ví dụ cấu hình trong `context.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Context>

    <!-- Định nghĩa JNDI DataSource Resource -->
    <Resource name="jdbc/myAppDB" 
              auth="Container"
              type="javax.sql.DataSource"
              factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
              
              -- -- Các thuộc tính của Connection Pool -- --
              maxTotal="100" 
              maxIdle="30" 
              maxWaitMillis="10000"
              
              -- -- Các thuộc tính của JDBC Driver -- --
              username="myuser"
              password="mypassword"
              driverClassName="com.mysql.cj.jdbc.Driver"
              url="jdbc:mysql://localhost:3306/mydatabase?useSSL=false"
    />

</Context>
```

**Giải thích các thuộc tính:**

-   `name="jdbc/myAppDB"`: Tên JNDI của tài nguyên. Ứng dụng sẽ sử dụng tên này để tra cứu.
-   `auth="Container"`: Chỉ định rằng việc xác thực (kết nối đến DB) sẽ do container (Tomcat) quản lý.
-   `type="javax.sql.DataSource"`: Loại đối tượng mà JNDI resource này đại diện.
-   `factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"`: Chỉ định factory class để tạo ra `DataSource`. Đây là factory của thư viện connection pool tích hợp của Tomcat.
-   `maxTotal="100"`: Số lượng kết nối tối đa (cả đang hoạt động và rảnh rỗi) mà pool có thể chứa.
-   `maxIdle="30"`: Số lượng kết nối rảnh rỗi tối đa được giữ lại trong pool.
-   `maxWaitMillis="10000"`: Thời gian tối đa (tính bằng mili giây) mà một request sẽ phải chờ để có kết nối trước khi một exception được ném ra (nếu pool đã đầy).
-   `username`, `password`, `driverClassName`, `url`: Các thông tin cấu hình chuẩn của JDBC driver, được chuyển đến driver để tạo kết nối.

### Bước 3: Cấu hình `web.xml` của ứng dụng

Để ứng dụng có thể "thấy" được JNDI resource do container quản lý, bạn cần khai báo một "resource reference" trong file `WEB-INF/web.xml` của ứng dụng.

```xml
<web-app ...>

    <resource-ref>
        <description>My Application Database</description>
        <res-ref-name>jdbc/myAppDB</res-ref-name>
        <res-type>javax.sql.DataSource</res-type>
        <res-auth>Container</res-auth>
    </resource-ref>

</web-app>
```
-   Thẻ `<resource-ref>` này tạo ra một liên kết giữa ứng dụng và JNDI resource được định nghĩa trong `context.xml`. Tên trong `<res-ref-name>` phải khớp với tên JNDI.

### Bước 4: Tra cứu và Sử dụng trong Code

Bây giờ, trong code Java (ví dụ: trong một Servlet), bạn có thể lấy và sử dụng `DataSource` như sau:

```java
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;

// ...

try {
    Context initContext = new InitialContext();
    // "java:/comp/env" là namespace chuẩn cho các JNDI resource của ứng dụng
    Context envContext  = (Context) initContext.lookup("java:/comp/env");
    DataSource ds = (DataSource) envContext.lookup("jdbc/myAppDB");

    // Lấy một kết nối từ pool
    Connection conn = ds.getConnection();
    
    // ... sử dụng kết nối ...
    
    // "Đóng" kết nối để trả nó về lại pool
    if (conn != null) {
        conn.close();
    }

} catch (Exception e) {
    e.printStackTrace();
}
```

Bằng cách này, bạn đã tách biệt hoàn toàn cấu hình cơ sở dữ liệu ra khỏi mã nguồn và tận dụng được hiệu năng của connection pooling do Tomcat quản lý.
