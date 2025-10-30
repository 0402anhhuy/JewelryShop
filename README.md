# 🛒 JewelryShop-Springboot 

Dự án **JewelryShop** là một ứng dụng web thương mại điện tử mini được xây dựng bằng  
**Spring boot + Thymeleaf+ Bootstrap + JPA + SQLServer/MySQL/ postgreSQL  +Decorator Sitemesh+ JWT**,  
triển khai theo mô hình **MVC + DAO + Service Layer**.

🎯 **Mục tiêu:** Xây dựng nền tảng mua hàng và quản lý thương mại điện tử mini chuyên về các sản phẩm công nghệ.

---

## 1️⃣ Cấu trúc dự án

```text
Uteshop-servlet/
├── .git/
├── pom.xml               # Quản lý dependencies 
├── src/
│   └── main/
│       ├── java/ute/shop/
│       │   ├── config/                      # Cấu hình kết nối DB, Security, tải dữ liệu ban đầu, và tích hợp thanh toán (Momo, VNPay)
│       │   ├── controller/                  # Xử lý request
│       │   ├── repository/                  # Interface DAO
│       │   ├── entity/                      # Chứa các lớp mô hình dữ liệu
│       │   ├── service/                     # Interface Service
│       │   ├── service/impl/                # Business logic
│       │   ├── service/specification/       # Xử lý tiêu chí lọc & chuyển đổi dữ liệu đầu vào để query database (JPA Specification)
│       │   └──  dto/                        # Các lớp trung gian (Data Transfer Object) dùng cho xác thực, response, hoặc truyền dữ liệu giữa tầng
│       ├── resources/
│       │   ├── fonts/                              # Lưu trữ các font chữ được sử dụng trong giao diện (ví dụ: Roboto, Montserrat)
│       │   ├── static/                             # Các tài nguyên tĩnh: CSS, JS, hình ảnh, icon,...
│       │   ├── templates/                          # Giao diện hiển thị (Thymeleaf Templates)
│       │   │   ├── admin/                          # Giao diện quản trị hệ thống
│       │   │   │   ├── audits/                     # Nhật ký hoạt động của Admin
│       │   │   │   ├── collections/                # Quản lý bộ sưu tập sản phẩm
│       │   │   │   ├── layout/                     # Layout tổng thể cho trang admin
│       │   │   │   ├── orders/                     # Quản lý đơn hàng
│       │   │   │   ├── products/                   # Quản lý sản phẩm
│       │   │   │   ├── shippers/                   # Quản lý đơn vị vận chuyển
│       │   │   │   ├── stocks/                     # Quản lý kho hàng
│       │   │   │   ├── suppliers/                  # Quản lý nhà cung cấp
│       │   │   │   ├── users/                      # Quản lý người dùng
│       │   │   │   └── dashboard.html              # Trang tổng quan cho admin
│       │   │   ├── manager/                        # Giao diện dành cho quản lý (Manager)
│       │   │   │   ├── collections/                # Quản lý bộ sưu tập sản phẩm
│       │   │   │   ├── layout/                     # Layout cho giao diện manager
│       │   │   │   ├── orders/                     # Theo dõi đơn hàng
│       │   │   │   ├── products/                   # Quản lý sản phẩm cửa hàng
│       │   │   │   ├── shippers/                   # Theo dõi tình trạng vận chuyển
│       │   │   │   ├── stocks/                     # Theo dõi tồn kho
│       │   │   │   ├── suppliers/                  # Làm việc với nhà cung cấp
│       │   │   │   └── dashboard.html              # Trang tổng quan dành cho manager
│       │   │   ├── client_1/                       # Giao diện người dùng (client)
│       │   │   │   ├── homepage/                   # Trang chủ và các phần giới thiệu
│       │   │   │   └── layout/                     # Layout tổng thể cho người dùng
│       │   │   └── product/                        # Trang chi tiết sản phẩm
│       │   ├── application.properties              # Cấu hình mặc định (chung cho toàn dự án)
│       │   ├── application-dev.properties          # Cấu hình môi trường phát triển (DEV)
│       │   └── application-prod.properties         # Cấu hình môi trường triển khai (PRODUCTION)
```
## 2️⃣ Các bước cài đặt

1. Cài **MYSQL Server** (tham khảo hướng dẫn trên YouTube)
2. Tạo database **jewelry**
3. Tải mã nguồn **JewelryShop** từ GitHub nhóm
4. Khởi động **Springboot** → truy cập [http://localhost:8080/]
5. Đăng nhập tài khoản **Admin** mặc định:
    - Username: **admin@gmail.com**
    - Password: **admin123**
6. Website deloy của nhóm: [http://jewelryshop.ap-southeast-2.elasticbeanstalk.com/]
---

## 3️⃣ Phân chia công việc

### 👩‍💻 Nguyễn Quốc Đạt — Guest/User + Admin
- **Tuần 1:** Vẽ & đặc tả use case; phân tích luồng nghiệp vụ
- **Tuần 2:** Lược đồ tuần tự, giao diện quản lý cửa hàng của admin
- **Tuần 3:** Chức năng Guest/User: xây dựng đăng nhập phân luồng, đăng ký tài khoản cho guest, quên mật khẩu, lọc sản phẩm, đánh giá sản phẩm
- **Tuần 4:** Chức năng Admin: xây dựng quản lý hệ thống tổng quan, quản lý đơn hàng, quản lý sản phẩm, quản lý bộ sưu tập, quản lý kho và nhập xuất, quản lý người dùng, quản lý shipper, quản lý nhật ký hệ thống người dùng

---

### 👩‍💻 Trần Đào Quốc Huy — User + Manager
- **Tuần 1:** Use case, hành vi đặt hàng
- **Tuần 2:** Lược đồ tuần tự, giao diện quản lý của manager
- **Tuần 3:** Chức năng User: Thêm giỏ hàng, thanh toán đơn hàng, theo dõi đơn hàng, mua lại đơn hàng
- **Tuần 4:** Chức năng Manager: xây dựng quản lý hệ thống tổng quan, quản lý đơn hàng, quản lý sản phẩm, quản lý bộ sưu tập

---

### 👨‍💻 Trần Anh Huy — User + Manager
- **Tuần 1:** Tạo dataseeder nạp dữ liệu ban đầu cho website, thiết kế CSDL tổng thể
- **Tuần 2:** Lược đồ tuần tự, xây dựng giao diện chính của Guest/User
- **Tuần 3:** Chức năng User: xem chi tiết sản phẩm, quản lý thông tin khách hàng, lưu trữ địa chỉ giao hàng, đổi mật khẩu khi cần
- **Tuần 4:** Chức năng Manager: xây dựng quản lý bộ sưu tập, quản lý shipper, quản lý kho và nhập xuất

---

## 4️⃣ Tài liệu & Báo cáo
 
📄 **docs/Nhom10_FinalProject.docx** → Báo cáo cuối kỳ

---

## 5️⃣ Công nghệ sử dụng

### 💻 Ngôn ngữ lập trình
- **Java (JDK 17)** → Xử lý logic nghiệp vụ, quản lý người dùng, sản phẩm, đơn hàng

### 🖥️ Giao diện người dùng
- ** HTML, CSS, Bootstrap 5**, Javascript → Thiết kế trực quan, thân thiện, responsive, xử lý sự kiện

### 🗄️ Cơ sở dữ liệu
- **MySQL Server** → Lưu trữ thông tin sản phẩm, người dùng, và đánh giá sản phẩm,...

### 🔗 Truy cập dữ liệu
- **JPA** → Kết nối, truy vấn và thao tác dữ liệu từ Java

### 🧠 Phân tích & Thiết kế hệ thống
- **Enterprise Architect** → Vẽ Use Case, Class, Sequence, Activity Diagram

### ⚙️ Công cụ phát triển
- **IntelliJ IDEA 2025.2.3** → giúp lập trình & chạy thử ứng dụng

### 🌐 Quản lý mã nguồn
- **Git + GitHub** → Làm việc nhóm, kiểm soát phiên bản, triển khai thử nghiệm

---

## 6️⃣ Target (Mục tiêu chức năng)

### 🔷 Chức năng chung
- Tìm kiếm và lọc sản phẩm
- Đăng ký tài khoản (gửi mã OTP kích hoạt qua Email)
- Đăng nhập, đăng xuất, quên mật khẩu (OTP qua Email)
- Mật khẩu mã hóa bảo mật

---

### 👤 Guest (Khách truy cập)
- Xem trang chủ, hiển thị sản phẩm bán chạy (Top 10), sắp xếp doanh số giảm dần
- Xem chi tiết sản phẩm nhưng không thể mua hoặc đánh giá

---

### 👥 User (Người dùng)
- Truy cập trang chủ & danh mục sản phẩm
- Hiển thị danh sách 20 sản phẩm mới, bán chạy, đánh giá cao, yêu thích (lazy loading)
- Trang hồ sơ cá nhân (Profile)
- Quản lý thông tin người dùng, nhiều địa chỉ nhận hàng
- Xem, thích, bình luận, đánh giá sản phẩm
- Giỏ hàng lưu trên database
- Thanh toán: hỗ trợ **COD, VNPAY, MOMO**
- Quản lý lịch sử mua hàng theo trạng thái:
    - Đơn hàng mới
    - Đã xác nhận
    - Đang giao
    - Đã giao
    - Đã hủy
    - Trả hàng / Hoàn tiền

---

### 🏪 Manager (Seller)
- Quản lý hệ thống tổng quản (thể hiện thống kê, số đơn hàng, doanh thu, so sánh giữa các tháng)
- Quản lý sản phẩm: thêm/sửa/xóa, tìm kiếm
- Quản lý đơn hàng theo trạng thái: xóa, sửa, tìm kiếm
- Quản lý bộ sưu tập: thêm/sửa/xóa, tìm kiếm
- Quản lý shipper : thêm/xóa, coi đơn hàng đang quản lý
- Quản lý kho và nhập xuất: thêm/sửa giao dịch
- Quản lý phản hồi, đánh giá sản phẩm

---

### 🛠️ Admin (Quản trị viên)
- Quản lý hệ thống tổng quản (thể hiện thống kê, số đơn hàng, doanh thu, so sánh giữa các tháng)
- Quản lý sản phẩm: thêm/sửa/xóa, tìm kiếm
- Quản lý đơn hàng theo trạng thái: xóa, sửa, tìm kiếm
- Quản lý bộ sưu tập: thêm/sửa/xóa, tìm kiếm
- Quản lý shipper : thêm/xóa, coi đơn hàng đang quản lý
- Quản lý kho và nhập xuất: thêm/sửa giao dịch
- Quản lý phản hồi, đánh giá sản phẩm
- Quản lý người dùng: thêm người dùng theo role, chỉnh sửa, xóa, tìm kiếm
- Quản lý nhật ký hệ thống: xem audit tác động lên người dùng

---

## 🔐 Bảo mật
- Mã hóa mật khẩu bằng **SHA-256 hoặc BCrypt**
- Phân quyền theo vai trò (**Role-based Authorization**)
- Lọc request qua **AuthFilter** và **EncodingFilter (UTF-8)**
- Bảo vệ trang quản trị bằng **JWT** hoặc **Session Validation**

---

## 💾 Cấu hình & Triển khai
- **CSDL:** MySql Server
- **ORM:** JPA (Hibernate)
- **IDE:** IntelliJ IDEA
- **Dependency Manager:** Maven
- **Version Control:** Git + GitHub

---

## ✅ Kiểm thử & Báo cáo
- **Báo cáo cuối kỳ** gồm:
    - Use Case, ERD, Sequence Diagram
    - Kết quả kiểm thử
    - Tổng kết vai trò từng thành viên nhóm

---

## 👨‍💻 Thành viên nhóm

| Họ và tên             | Vai trò | Phụ trách            |
|-----------------------|----------|----------------------|
| **Nguyễn Quốc Đạt**   | Nhóm trưởng | Admin, User, Guest   |
| **Trần Đào Quốc Huy** | Thành viên | User, Guest, Manager |
| **Trần Anh Huy**      | Thành viên | User, Guest, Manager |

---

📌 **Tổng kết:**
> Dự án được phát triển theo mô hình phân lớp chuẩn (**MVC – DAO – Service**), đảm bảo dễ bảo trì, mở rộng, và áp dụng các công nghệ hiện đại trong Java Web như **JPA** cùng giao diện thân thiện và hệ thống bảo mật an toàn.

