# SystemE-commerce - Spring Boot E-commerce Platform

## 📋 Tổng quan

SystemE-commerce là một nền tảng thương mại điện tử được xây dựng bằng Spring Boot, cung cấp đầy đủ các tính năng:

- 🛍️ **Product Catalog** - Quản lý sản phẩm và danh mục
- 🛒 **Shopping Cart** - Giỏ hàng với session management
- 📦 **Inventory Management** - Hệ thống giữ hàng với TTL 15 phút
- 💰 **Payment Integration** - Tích hợp SePay và mock payment
- 📋 **Order Management** - Quản lý đơn hàng với status tracking
- 👨‍💼 **Admin Panel** - Quản lý đơn hàng và inventory
- 🔧 **Recovery System** - Tự động recovery khi server sập

---

## 🛠️ Yêu cầu hệ thống

### ☕ Java & Build Tools
- **Java**: 17 hoặc cao hơn
- **Maven**: 3.6+ (hoặc sử dụng Maven Wrapper có sẵn)
- **IDE**: IntelliJ IDEA, Eclipse, hoặc VS Code

### 🗄️ Database
- **MySQL**: 8.0 hoặc cao hơn
- **Port**: 3306 (default)

### 🌐 Optional (cho development)
- **Postman** - Test APIs
- **MySQL Workbench** - Quản lý database

---

## 🚀 Hướng dẫn cài đặt

### 1️⃣ **Clone Repository**

```bash
git clone <repository-url>
cd SystemE-commerce
```

### 2️⃣ **Cài đặt Java 17**

#### Windows:
```bash
# Sử dụng Chocolatey
choco install openjdk17

# Hoặc download từ Oracle/OpenJDK
# https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
```

#### macOS:
```bash
# Sử dụng Homebrew
brew install openjdk@17

# Hoặc sử dụng SDKMAN
curl -s "https://get.sdkman.io" | bash
sdk install java 17.0.2-open
```

#### Linux (Ubuntu/Debian):
```bash
sudo apt update
sudo apt install openjdk-17-jdk

# Kiểm tra version
java -version
javac -version
```

### 3️⃣ **Cài đặt MySQL**

#### Windows:
```bash
# Download MySQL Installer từ https://dev.mysql.com/downloads/installer/
# Hoặc sử dụng Chocolatey
choco install mysql
```

#### macOS:
```bash
# Sử dụng Homebrew
brew install mysql
brew services start mysql

# Secure installation
mysql_secure_installation
```

#### Linux (Ubuntu/Debian):
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql

# Secure installation
sudo mysql_secure_installation
```

### 4️⃣ **Cấu hình Database**

#### Tạo Database và User:
```sql
-- Đăng nhập MySQL với quyền root
mysql -u root -p

-- Tạo database
CREATE DATABASE e_commerce CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo user cho application
CREATE USER 'springstudent'@'localhost' IDENTIFIED BY 'springstudent';
GRANT ALL PRIVILEGES ON e_commerce.* TO 'springstudent'@'localhost';
FLUSH PRIVILEGES;

-- Kiểm tra
SHOW DATABASES;
SELECT User, Host FROM mysql.user WHERE User = 'springstudent';

-- Thoát
EXIT;
```

#### Kiểm tra kết nối:
```bash
mysql -u springstudent -p -h localhost e_commerce
# Nhập password: springstudent
```

### 5️⃣ **Cấu hình Application**

File `src/main/resources/application.properties` đã được cấu hình sẵn:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/e_commerce?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8&createDatabaseIfNotExist=true
spring.datasource.username=springstudent
spring.datasource.password=springstudent

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Server
server.port=8080

# Email (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=trangnphe171779@fpt.edu.vn
spring.mail.password=zyuz rrlv cpee jmzl
```

**⚠️ Lưu ý:** Thay đổi email configuration nếu cần test email features.

---

## 🏃‍♂️ Chạy ứng dụng

### 🔧 **Cách 1: Sử dụng Maven Wrapper (Khuyến nghị)**

#### Windows:
```cmd
# Build project
.\mvnw clean compile

# Chạy tests (optional)
.\mvnw test

# Chạy application
.\mvnw spring-boot:run
```

#### macOS/Linux:
```bash
# Cấp quyền thực thi (lần đầu)
chmod +x mvnw

# Build project
./mvnw clean compile

# Chạy tests (optional)
./mvnw test

# Chạy application
./mvnw spring-boot:run
```

### 🔧 **Cách 2: Sử dụng Maven (nếu đã cài đặt)**

```bash
# Build project
mvn clean compile

# Chạy application
mvn spring-boot:run
```

### 🔧 **Cách 3: Chạy JAR file**

```bash
# Build JAR
./mvnw clean package -DskipTests

# Chạy JAR
java -jar target/SystemE-commerce-0.0.1-SNAPSHOT.jar
```

### 🔧 **Cách 4: Từ IDE**

1. Import project vào IDE (IntelliJ IDEA/Eclipse)
2. Chạy class `SystemECommerceApplication.java`
3. Hoặc sử dụng Maven plugin trong IDE

---

## 📊 Database Migration & Seed Data

### 🗄️ **Auto Migration**

Application sử dụng **Hibernate Auto DDL** để tự động tạo bảng:

```properties
spring.jpa.hibernate.ddl-auto=update
```

**Khi chạy lần đầu:**
- Hibernate sẽ tự động tạo tất cả 11 bảng
- Không cần chạy migration scripts thủ công
- Database schema được tạo từ JPA entities

### 🌱 **Seed Data (Dữ liệu mẫu)**

#### Automatic Seed (Chạy tự động):
```java
// DataInitializer sẽ tự động tạo admin users khi start
Admin User: username=admin, password=admin123
Staff User: username=staff, password=staff123
```

#### Manual Seed (Tùy chọn):
Nếu muốn thêm dữ liệu mẫu cho products, categories:

```sql
-- Kết nối database
mysql -u springstudent -p e_commerce

-- Thêm categories
INSERT INTO categories (name, description, created_at, updated_at) VALUES
('Electronics', 'Electronic devices and gadgets', NOW(), NOW()),
('Clothing', 'Fashion and apparel', NOW(), NOW()),
('Books', 'Books and educational materials', NOW(), NOW());

-- Thêm products
INSERT INTO products (name, description, base_price, category_id, is_active, created_at, updated_at) VALUES
('iPhone 15', 'Latest Apple smartphone', 999.99, 1, true, NOW(), NOW()),
('Samsung Galaxy S24', 'Android flagship phone', 899.99, 1, true, NOW(), NOW()),
('MacBook Pro', 'Professional laptop', 1999.99, 1, true, NOW(), NOW());

-- Thêm product variants
INSERT INTO product_variants (sku, size, color, price, stock_quantity, reserved_quantity, product_id, is_active, created_at, updated_at) VALUES
('IPHONE15-128-BLACK', '128GB', 'Black', 999.99, 50, 0, 1, true, NOW(), NOW()),
('IPHONE15-256-WHITE', '256GB', 'White', 1099.99, 30, 0, 1, true, NOW(), NOW()),
('GALAXY-S24-128-BLUE', '128GB', 'Blue', 899.99, 40, 0, 2, true, NOW(), NOW());
```

---

## ✅ Kiểm tra cài đặt

### 🌐 **1. Truy cập Application**

```bash
# Main application
http://localhost:8080

# Swagger API Documentation
http://localhost:8080/swagger-ui.html

# API Docs JSON
http://localhost:8080/v3/api-docs
```

### 🧪 **2. Test APIs**

#### Health Check:
```bash
curl http://localhost:8080/api/inventory/health
```

#### Get Products:
```bash
curl http://localhost:8080/api/products
```

#### Admin API (cần API key):
```bash
# Lấy API key từ database hoặc logs khi start
curl -H "X-API-Key: <admin-api-key>" http://localhost:8080/api/admin/orders
```

### 📊 **3. Kiểm tra Database**

```sql
-- Kết nối database
mysql -u springstudent -p e_commerce

-- Kiểm tra tables
SHOW TABLES;

-- Kiểm tra admin users
SELECT * FROM admin_users;

-- Kiểm tra products (nếu đã seed)
SELECT * FROM products;
SELECT * FROM product_variants;
```

### 📝 **4. Kiểm tra Logs**

Application logs sẽ hiển thị:
```
INFO  - Starting SystemECommerceApplication
INFO  - Started SystemECommerceApplication in X.XXX seconds
INFO  - Admin users initialized successfully
INFO  - Tomcat started on port(s): 8080 (http)
```

---

## 🔧 Troubleshooting

### ❌ **Lỗi thường gặp**

#### 1. **Database Connection Error**
```
Error: Communications link failure
```
**Giải pháp:**
- Kiểm tra MySQL service đang chạy
- Verify username/password trong `application.properties`
- Kiểm tra port 3306 không bị block

#### 2. **Java Version Error**
```
Error: Java 17 or higher required
```
**Giải pháp:**
```bash
# Kiểm tra Java version
java -version

# Set JAVA_HOME nếu cần
export JAVA_HOME=/path/to/java17
```

#### 3. **Port 8080 Already in Use**
```
Error: Port 8080 was already in use
```
**Giải pháp:**
```bash
# Tìm process sử dụng port 8080
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # macOS/Linux

# Kill process hoặc đổi port trong application.properties
server.port=8081
```

#### 4. **Maven Build Error**
```
Error: Could not resolve dependencies
```
**Giải pháp:**
```bash
# Clear Maven cache
./mvnw dependency:purge-local-repository

# Rebuild
./mvnw clean compile
```

### 🔍 **Debug Mode**

Chạy với debug logs:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--logging.level.fs.fresher.SystemE_commerce=DEBUG"
```

---

## 📚 Tài liệu tham khảo

### 🔗 **API Documentation**
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Endpoints**: [API_ENDPOINTS_LIST.md](API_ENDPOINTS_LIST.md)
- **Database Schema**: [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md)

### 📖 **Feature Guides**
- **Server Crash Recovery**: [SERVER_CRASH_RECOVERY_GUIDE.md](SERVER_CRASH_RECOVERY_GUIDE.md)
- **Exception Handling**: [EXCEPTION_HANDLING_GUIDE.md](EXCEPTION_HANDLING_GUIDE.md)
- **Email Setup**: [EMAIL_SETUP_GUIDE.md](EMAIL_SETUP_GUIDE.md)
- **SePay Testing**: [SEPAY_TESTING_GUIDE.md](SEPAY_TESTING_GUIDE.md)

### 🧪 **Testing**
- **Unit Tests**: [SERVICE_UNIT_TESTS_COMPLETE_GUIDE.md](SERVICE_UNIT_TESTS_COMPLETE_GUIDE.md)
- **SePay Unit Tests**: [SEPAY_UNIT_TESTS_README.md](SEPAY_UNIT_TESTS_README.md)

---

## 🚀 Production Deployment

### 🔒 **Security Checklist**
- [ ] Đổi default admin passwords
- [ ] Cấu hình SSL/HTTPS
- [ ] Setup firewall rules
- [ ] Cấu hình email credentials
- [ ] Review logging levels

### 📊 **Performance Tuning**
```properties
# JVM Options
-Xms512m -Xmx2g
-XX:+UseG1GC

# Database Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

### 🔄 **Monitoring**
- Health Check: `/api/inventory/health`
- Metrics: Spring Boot Actuator (nếu enable)
- Logs: Application logs + MySQL slow query log

---

## 👥 Support

Nếu gặp vấn đề trong quá trình setup:

1. **Kiểm tra logs** application và database
2. **Verify requirements** Java 17, MySQL 8.0+
3. **Test connectivity** database connection
4. **Check documentation** trong các file guide
5. **Review configuration** trong `application.properties`

---

## 📄 License

This project is for educational purposes.

---

**🎉 Chúc bạn setup thành công! Happy coding! 🚀**