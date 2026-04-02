DROP DATABASE IF EXISTS ptitsocialchat;

CREATE DATABASE IF NOT EXISTS ptitsocialchat
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE ptitsocialchat;

-- ==========================================
-- 1. BẢNG USERS (Người dùng)
-- ==========================================
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(255),
    avatar VARCHAR(255) DEFAULT NULL
) ENGINE=InnoDB;

-- Dữ liệu mẫu (Seed Data)
INSERT INTO users (id, username, password, full_name, role, avatar) VALUES
(1, 'vana', 'password123', 'Nguyễn Văn A', 'ROLE_USER', NULL),
(2, 'thib', 'password123', 'Trần Thị B', 'ROLE_USER', NULL),
(3, 'adminc', 'password123', 'Lê Văn C', 'ROLE_ADMIN', NULL);

-- ==========================================
-- 2. BẢNG POSTS (Bài đăng)
-- ==========================================
CREATE TABLE posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT, 
    image_url VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    CONSTRAINT FK_USER_POST FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 3. BẢNG COMMENTS (Bình luận)
-- ==========================================
CREATE TABLE comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT, 
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT FK_COMMENT_POST FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT FK_COMMENT_USER FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 4. BẢNG POST_LIKES (Lượt thích bài viết)
-- ==========================================
CREATE TABLE post_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_user (post_id, user_id),
    CONSTRAINT FK_LIKE_POST FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT FK_LIKE_USER FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 5. BẢNG FRIEND_REQUESTS (Yêu cầu kết bạn)
-- ==========================================
CREATE TABLE friend_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    status VARCHAR(50) NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, 
    UNIQUE KEY uk_friend_request (sender_id, receiver_id),
    CONSTRAINT FK_FR_SENDER FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_FR_RECEIVER FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 6. BẢNG FRIENDS (Danh sách bạn bè)
-- ==========================================
CREATE TABLE friends (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,   
    friend_id BIGINT NOT NULL, 
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, 
    UNIQUE KEY uk_friend_pair (user_id, friend_id),
    CONSTRAINT FK_FRIEND_1 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_FRIEND_2 FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 7. BẢNG MESSAGES (Tin nhắn trò chuyện)
-- ==========================================
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT, 
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, 
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    CONSTRAINT FK_MSG_SENDER FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_MSG_RECEIVER FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
