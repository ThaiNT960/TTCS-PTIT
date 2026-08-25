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
nhom 10/
├── docker-compose.yml
├── social_chat_web/       # Ứng dụng chính (Frontend HTML/CSS/JS + Backend Spring Boot)
├── ai-service/            # Microservice AI kiểm duyệt nội dung (FastAPI)
└── ai_training_data/      # Dataset (.csv) và mã nguồn huấn luyện PhoBERT (.ipynb)
```

---

## 🚀 3. Cách Khởi Chạy Hệ Thống

1. Mở **Terminal / PowerShell** tại thư mục gốc chứa file `docker-compose.yml`.
2. Chạy lệnh sau để bật toàn bộ dịch vụ (Web, AI, Database):
   ```bash
   docker compose up -d --build
   ```
3. Kiểm tra trạng thái các dịch vụ đang chạy:
   ```bash
   docker ps
   ```

---

## 🌐 4. Truy Cập Hệ Thống

Sau khi chạy lệnh thành công, mở trình duyệt và truy cập:
👉 **[http://localhost:8080/login.html](http://localhost:8080/login.html)**

### 👥 Tài khoản thử nghiệm:
> 🔑 **Mật khẩu chung cho tất cả tài khoản:** `password123`

- **Tài khoản Quản trị (Admin):** `adminc@ptit.edu.vn`
- **Tài khoản Sinh viên (User):** 
  - `vana@student.ptit.edu.vn`
  - `thib@student.ptit.edu.vn`
  - `hoangd@student.ptit.edu.vn`
