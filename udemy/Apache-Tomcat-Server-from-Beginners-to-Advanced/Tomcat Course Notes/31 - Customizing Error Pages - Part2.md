# Bài 31: Tùy chỉnh trang lỗi - Phần 2

- **Thời lượng**: 06:04
- **Bài trước**: [[30 - Customizing Error Pages - Part1]]
- **[[_Index|Về lại mục lục]]**
- **Bài tiếp theo**: [[32 - Request Interception Using Valves]]

---

## Nội dung

Bài học này xây dựng dựa trên phần 1, tập trung vào cách làm cho các trang lỗi tùy chỉnh trở nên "động" (dynamic) hơn bằng cách sử dụng JSP. Khi một lỗi xảy ra, container sẽ cung cấp một số thông tin hữu ích về lỗi đó dưới dạng các thuộc tính (attributes) trong request, và chúng ta có thể truy cập chúng từ trang JSP.

### Truy cập thông tin lỗi trong trang JSP

Để trang JSP của bạn có thể truy cập được các thông tin lỗi này, bạn cần thêm chỉ thị sau vào đầu trang:

```jsp
<%@ page isErrorPage="true" %>
```

Chỉ thị `isErrorPage="true"` làm hai việc:
1.  Nó báo cho container JSP biết rằng đây là một trang được thiết kế để xử lý lỗi.
2.  Nó tạo ra một biến ẩn có tên là `exception`, là một đối tượng thuộc kiểu `java.lang.Throwable`. Bạn có thể sử dụng biến này để truy cập thông tin về exception đã gây ra lỗi.

### Các thuộc tính Request có sẵn

Khi container chuyển hướng đến một trang lỗi, nó sẽ đặt các thuộc tính sau vào đối tượng `request`, bạn có thể truy cập chúng bằng Expression Language (EL) hoặc scriptlet.

-   `javax.servlet.error.status_code`: (Kiểu `Integer`) Mã trạng thái HTTP của lỗi (ví dụ: 404, 500).
-   `javax.servlet.error.exception_type`: (Kiểu `Class`) Lớp của exception đã xảy ra.
-   `javax.servlet.error.message`: (Kiểu `String`) Thông điệp lỗi, lấy từ `exception.getMessage()`.
-   `javax.servlet.error.exception`: (Kiểu `Throwable`) Chính là đối tượng exception đã gây ra lỗi.
-   `javax.servlet.error.request_uri`: (Kiểu `String`) URL mà người dùng đã yêu cầu, gây ra lỗi.
-   `javax.servlet.error.servlet_name`: (Kiểu `String`) Tên của servlet đã xử lý request và gây ra lỗi.

### Ví dụ về một trang lỗi 500 động (`500-error-page.jsp`)

```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html>
<head>
    <title>Lỗi Server</title>
    <style>
        body { font-family: Arial, sans-serif; }
        .error-container { border: 1px solid #ccc; padding: 20px; margin: 20px; }
        .stack-trace { background-color: #f0f0f0; padding: 10px; font-family: monospace; white-space: pre-wrap; }
    </style>
</head>
<body>
    <div class="error-container">
        <h1>Rất tiếc, đã có lỗi xảy ra! (Lỗi 500)</h1>
        <p>Hệ thống đã gặp phải một sự cố không mong muốn. Chúng tôi đã ghi nhận lại lỗi này và sẽ sớm khắc phục.</p>
        <p>Vui lòng thử lại sau hoặc <a href="${pageContext.request.contextPath}/">quay về trang chủ</a>.</p>

        <%-- 
            Phần thông tin gỡ lỗi này chỉ nên hiển thị cho lập trình viên,
            không nên hiển thị cho người dùng cuối trong môi trường production.
            Bạn có thể dùng một biến cờ để kiểm soát việc hiển thị này.
        --%>
        <hr>
        <h2>Thông tin gỡ lỗi (Dành cho Developer):</h2>
        <p><b>URI yêu cầu:</b> ${requestScope['javax.servlet.error.request_uri']}</p>
        <p><b>Mã lỗi:</b> ${requestScope['javax.servlet.error.status_code']}</p>
        <p><b>Loại Exception:</b> ${requestScope['javax.servlet.error.exception_type']}</p>
        <p><b>Thông điệp:</b> ${requestScope['javax.servlet.error.message']}</p>
        
        <h3>Stack Trace:</h3>
        <div class="stack-trace">
            <% 
                if (exception != null) {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                    exception.printStackTrace(pw);
                    out.print(sw.toString());
                }
            %>
        </div>
    </div>
</body>
</html>
```

Bằng cách sử dụng JSP, bạn có thể tạo ra các trang lỗi vừa thân thiện với người dùng cuối, vừa cung cấp đủ thông tin chi tiết cho đội ngũ phát triển để chẩn đoán và khắc phục sự cố.
