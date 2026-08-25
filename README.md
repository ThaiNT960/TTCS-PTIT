# 🌐 PTIT SOCIAL CHAT - Mạng Xã Hội Sinh Viên PTIT

Hệ thống Mạng xã hội & Chat trực tuyến dành cho sinh viên PTIT, tích hợp công nghệ AI kiểm duyệt nội dung tự động. 
Dự án được xây dựng với kiến trúc Microservices / Multi-services bao gồm **Web Application (Spring Boot 3 + Vanilla Web)** và **AI Service (Python FastAPI + PhoBERT)**.

---

## 👥 Tài Khoản Dùng Thử (Test Accounts)

> 🔑 **Mật khẩu chung cho tất cả tài khoản mẫu:** `password123`

| Vai trò | Email đăng nhập | Quyền hạn |
| :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `adminc@ptit.edu.vn` | Quản lý người dùng, bài viết, cộng đồng, tài liệu, xem báo cáo thống kê |
| **Sinh viên 1 (User)** | `vana@student.ptit.edu.vn` | Đăng bài, bình luận, nhắn tin realtime, tham gia cộng đồng |
| **Sinh viên 2 (User)** | `thib@student.ptit.edu.vn` | Tương tác mạng xã hội |
| **Sinh viên 3 (User)** | `hoangd@student.ptit.edu.vn` | Tương tác mạng xã hội |

---

## 🛠️ Công Nghệ Sử Dụng

- **Backend Web:** Java 17, Spring Boot 3.2.2, Spring Security (JWT), Spring Data JPA, Spring WebSocket (STOMP / SockJS).
- **Database:** MySQL 8.0+.
- **Frontend:** HTML5, CSS3, Vanilla JavaScript.
- **AI Microservice:** Python (FastAPI, PyTorch, PhoBERT Transformer) - *Kiểm duyệt & phát hiện nội dung độc hại*.
- **Containerization:** Docker & Docker Compose.

---

## 📦 Tải Tài Nguyên Mở Rộng (AI Service & Docker Compose)

Do giới hạn dung lượng lưu trữ trên GitHub (dataset huấn luyện, mô hình AI PhoBERT và file báo cáo PDF lớn), các module liên quan được lưu trữ trên Google Drive:

- 🔗 **Link tải trọn bộ Google Drive:** `[DÁN_LINK_GOOGLE_DRIVE_CỦA_BẠN_VÀO_ĐÂY]`
- **Tài nguyên trên Drive bao gồm:**
  - `ai-service/`: Microservice AI kiểm duyệt nội dung (FastAPI).
  - `ai_training_data/`: Dataset `.csv` và mã nguồn huấn luyện mô hình PhoBERT (`.ipynb`).
  - `docker-compose.yml`: File cấu hình chạy toàn bộ hệ thống (Web + AI + MySQL) chỉ bằng 1 câu lệnh.
  - `Bao_cao_TTCS_Nhom10.pdf`: Báo cáo chi tiết đề tài Thực tập cơ sở (Nhóm 10).

---

## 🚀 Hướng Dẫn Triển Khai Hệ Thống

Bạn có thể lựa chọn 1 trong 2 cách triển khai dưới đây:

### 🌟 Cách 1: Khởi Chạy Toàn Bộ Hệ Thống Bằng Docker Compose (Khuyên Dùng)

> 💡 Yêu cầu: Đã cài đặt **Docker** và **Docker Desktop / Docker Compose**.

1. Tải file `docker-compose.yml` và thư mục `ai-service/` từ link Google Drive ở trên về, đặt chung thư mục với `social_chat_web/`:
   ```text
   Nhóm 10 TTCS/
   ├── docker-compose.yml
   ├── social_chat_web/
   └── ai-service/
   ```
2. Mở Terminal / PowerShell tại thư mục gốc chứa file `docker-compose.yml`.
3. Khởi chạy toàn bộ hệ thống bằng lệnh:
   ```bash
   docker compose up -d --build
   ```
4. Kiểm tra trạng thái các container:
   ```bash
   docker ps
   ```

---

### 💻 Cách 2: Khởi Chạy Cục Bộ (Local Development - Không dùng Docker Compose)

#### Bước 1: Chuẩn bị Cơ sở dữ liệu MySQL
1. Tạo database trên MySQL:
   ```sql
   CREATE DATABASE ptitsocialchat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Import file script `ptitsocialchat.sql` (có sẵn trong thư mục `social_chat_web/`) vào database `ptitsocialchat`.

#### Bước 2: Kiểm tra cấu hình kết nối (.env)
File `.env` đã có sẵn tại thư mục gốc của project:
```env
DB_URL=jdbc:mysql://localhost:3306/ptitsocialchat?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=123456
JWT_SECRET=9a4f2c8d3b7a1e5f9g2h6i0j4k8l2m6n0o4p8q2r6s0t4u8v2w6x0y4z8a2b6c0d
AI_SERVICE_URL=http://localhost:8000
```
*(Nếu mật khẩu MySQL của bạn khác `123456`, vui lòng chỉnh lại cho khớp)*.

#### Bước 3: Khởi chạy AI Service (FastAPI)
1. Tải thư mục `ai-service` từ Google Drive về máy.
2. Mở terminal tại thư mục `ai-service` và chạy:
   ```bash
   pip install -r requirements.txt
   uvicorn main:app --host 0.0.0.0 --port 8000 --reload
   ```

#### Bước 4: Khởi chạy Web Backend (Spring Boot)
Mở terminal tại thư mục `social_chat_web/` và chạy:
- **Windows:**
  ```cmd
  .\mvnw.cmd spring-boot:run
  ```
- **Linux / macOS:**
  ```bash
  ./mvnw spring-boot:run
  ```
*(Hoặc mở dự án bằng IntelliJ IDEA / Eclipse và Run file `SocialchatApplication.java`)*.

---

## 🌐 Đường Dẫn Truy Cập Hệ Thống

Sau khi hệ thống khởi động thành công:

| Thành phần | Đường dẫn (URL) | Mô tả |
| :--- | :--- | :--- |
| 📱 **Trang Đăng Nhập** | [http://localhost:8080/login.html](http://localhost:8080/login.html) | Đăng nhập tài khoản sinh viên / admin |
| 📝 **Trang Đăng Ký** | [http://localhost:8080/register.html](http://localhost:8080/register.html) | Tạo tài khoản sinh viên mới |
| 🏠 **Trang Chủ / Bảng Tin** | [http://localhost:8080/home.html](http://localhost:8080/home.html) | Xem bài viết, đăng status, chat realtime |
| 🛡️ **Trang Quản Trị Admin** | [http://localhost:8080/admin.html](http://localhost:8080/admin.html) | Quản lý hệ thống (dành cho Admin) |
| 🔌 **Backend REST API** | [http://localhost:8080/api](http://localhost:8080/api) | Endpoint REST API |
| 🤖 **AI Microservice API** | [http://localhost:8000](http://localhost:8000) | Dịch vụ AI kiểm duyệt độc lập (FastAPI) |

---

## 📁 Cấu Trúc Thư Mục Dự Án (social_chat_web)

```text
social_chat_web/
├── .env                       # File cấu hình biến môi trường kết nối DB, JWT, AI
├── .gitignore                  # Cấu hình bỏ qua file build target, IDE
├── Dockerfile                 # File build image Docker Multi-stage
├── ptitsocialchat.sql          # File script CSDL mẫu MySQL
├── pom.xml                    # File quản lý thư viện Maven
└── src/
    ├── main/
    │   ├── java/com/ptit/socialchat/
    │   │   ├── config/        # Cấu hình Security, WebSocket, MVC Resource
    │   │   ├── controller/    # REST API & WebSocket Controller
    │   │   ├── dto/           # Data Transfer Objects
    │   │   ├── entity/        # JPA Entity (User, Post, Message, Community,...)
    │   │   ├── repository/    # Spring Data JPA Repositories
    │   │   └── service/       # Business Logic & Upload Services
    │   └── resources/
    │       ├── application.properties
    │       └── static/        # Giao diện HTML, CSS, JS, Images
```
