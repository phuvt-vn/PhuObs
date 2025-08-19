# Sử dụng image Tomcat chính thức
FROM tomcat:10.1-jdk17-temurin

# Tải Redisson cho Tomcat 10 và các dependency của nó
RUN wget https://repo1.maven.org/maven2/org/redisson/redisson-tomcat-10/3.23.0/redisson-tomcat-10-3.23.0.jar -P /usr/local/tomcat/lib/
RUN wget https://repo1.maven.org/maven2/org/redisson/redisson-all/3.23.0/redisson-all-3.23.0.jar -P /usr/local/tomcat/lib/

# Tạo cấu hình Redisson
COPY redisson.yaml /usr/local/tomcat/conf/redisson.yaml

# Copy context.xml đã chỉnh sửa vào thư mục conf của Tomcat
COPY context.xml /usr/local/tomcat/conf/context.xml

# Mở cổng mặc định của Tomcat
EXPOSE 8080