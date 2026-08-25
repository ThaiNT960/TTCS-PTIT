# PTIT SOCIAL CHAT

Hệ thống Mạng xã hội & Chat trực tuyến dành cho sinh viên PTIT, tích hợp AI kiểm duyệt nội dung tự động (PhoBERT).

---

## 📦 1. Tải Tài Nguyên Từ Google Drive

Do giới hạn dung lượng lưu trữ trên GitHub (mô hình AI và dataset), các tài nguyên liên quan được lưu trữ trên Google Drive:

- 🔗 **Link Google Drive:** [Tải tài nguyên tại đây](https://drive.google.com/drive/u/1/folders/1LC3nPYgw77pBqGqLMJgZJ1xJ_21gAU0D)
- **Tài nguyên trên Drive bao gồm:**
  - `ai-service/`: Microservice AI kiểm duyệt nội dung (FastAPI).
  - `ai_training_data/`: Dataset `.csv` và mã nguồn huấn luyện mô hình PhoBERT (`.ipynb`).
  - `docker-compose.yml`: File cấu hình chạy toàn bộ hệ thống (Web + AI + MySQL) chỉ bằng 1 câu lệnh.

---

## 📁 2. Cấu Trúc Thư Mục Sau Khi Tải Về

Đặt các thư mục và file từ Google Drive cùng cấp với `social_chat_web/`:

```text
ptit-social-chat/
├── docker-compose.yml
├── social_chat_web/       # Ứng dụng chính (Frontend HTML/CSS/JS + Backend Spring Boot)
├── ai-service/            # Microservice AI kiểm duyệt nội dung (FastAPI)
└── ai_training_data/      # Dataset (.csv) và mã nguồn huấn luyện PhoBERT (.ipynb)
```

---

## 🚀 3. Cách Khởi Chạy Hệ Thống

### Cách 1: Khởi chạy trọn bộ bằng Docker Compose (Khuyên dùng)
1. Mở **Terminal / PowerShell** tại thư mục gốc chứa file `docker-compose.yml`.
2. Chạy lệnh:
   ```bash
   docker compose up -d --build
   ```

### Cách 2: Khởi chạy Local (Không dùng Docker)
1. **Database:** Tạo database `ptitsocialchat` trên MySQL và import file `ptitsocialchat.sql`.
2. **Cấu hình:**
   - Kiểm tra/sửa lại tài khoản, mật khẩu MySQL trong file `.env` nếu khác `root`/`123456`.
   - Điền thông tin kết nối DB (URL, Username, Password) vào file `src/main/resources/application.properties`.
3. **AI Service:** Vào thư mục `ai-service` chạy `uvicorn main:app --port 8000 --reload`.
4. **Web Backend:** Vào thư mục `social_chat_web` chạy:
   - Windows: `.\mvnw.cmd spring-boot:run`
   - Linux/macOS: `./mvnw spring-boot:run`
   *(Hoặc chạy trực tiếp file `SocialchatApplication.java` trong IDE)*.

---

## 🌐 4. Truy Cập Hệ Thống

Sau khi chạy lệnh thành công, mở trình duyệt và truy cập:
 **[http://localhost:8080/login.html](http://localhost:8080/login.html)**

### 👥 Tài khoản thử nghiệm:
> 🔑 **Mật khẩu chung cho tất cả tài khoản:** `password123`

- **Tài khoản Quản trị (Admin):** `adminc@ptit.edu.vn`
- **Tài khoản Sinh viên (User):** 
  - `vana@student.ptit.edu.vn`
  - `thib@student.ptit.edu.vn`
  - `hoangd@student.ptit.edu.vn`

![Visitors](https://komarev.com/ghpvc/?username=ThaiNT960-2&repo=TTCS-PTIT&color=blue&style=flat-square)
