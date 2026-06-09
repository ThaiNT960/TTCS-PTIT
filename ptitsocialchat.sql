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
    email VARCHAR(255) UNIQUE DEFAULT NULL,
    full_name VARCHAR(255),
    role VARCHAR(255),
    avatar VARCHAR(255) DEFAULT NULL,
    cover_photo VARCHAR(255) DEFAULT NULL,
    bio TEXT,
    workplace VARCHAR(255), -- Mã sinh viên
    education VARCHAR(255), -- Chuyên ngành
    location VARCHAR(255),  -- Cơ sở học
    locked BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB;

-- Dữ liệu mẫu (Seed Data)
INSERT INTO users (id, username, password, email, full_name, role, avatar, workplace, education, location) VALUES
(1, 'vana', '$2a$10$ORNucSuHuZOl6Bdpn054gepjQCAv3bzhD5Yi/wMYJYKvwFiHi7312', 'vana@student.ptit.edu.vn', 'Nguyễn Văn A', 'ROLE_USER', NULL, 'B21DCCN001', 'Công nghệ thông tin', 'PTIT Hà Nội'),
(2, 'thib', '$2a$10$ORNucSuHuZOl6Bdpn054gepjQCAv3bzhD5Yi/wMYJYKvwFiHi7312', 'thib@student.ptit.edu.vn', 'Trần Thị B', 'ROLE_USER', NULL, 'B21DCCN002', 'Công nghệ thông tin', 'PTIT Hà Nội'),
(3, 'adminc', '$2a$10$ORNucSuHuZOl6Bdpn054gepjQCAv3bzhD5Yi/wMYJYKvwFiHi7312', 'adminc@ptit.edu.vn', 'Lê Văn C', 'ROLE_ADMIN', NULL, 'ADMIN', 'Quản trị', 'PTIT Hà Nội');

-- ==========================================
-- 2. BẢNG POSTS (Bài đăng)
-- ==========================================
CREATE TABLE posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT,
    image_url VARCHAR(255), -- Kept for compatibility
    privacy VARCHAR(50) DEFAULT 'PUBLIC',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'APPROVED',
    moderation_label VARCHAR(50) DEFAULT NULL,
    moderation_confidence DOUBLE DEFAULT NULL,
    CONSTRAINT FK_USER_POST FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_post_user_id (user_id),
    INDEX idx_post_created_at (created_at),
    INDEX idx_post_status (status)
) ENGINE=InnoDB;

-- ==========================================
-- 3. BẢNG COMMENTS (Bình luận)
-- ==========================================
CREATE TABLE comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT, 
    image_url VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    parent_comment_id BIGINT DEFAULT NULL,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT FK_COMMENT_POST FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT FK_COMMENT_USER FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_PARENT_COMMENT FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 4. BẢNG COMMENT_REACTIONS (Cảm xúc bình luận)
-- ==========================================
CREATE TABLE comment_reactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(50) NOT NULL DEFAULT 'LIKE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_COMMENT_REAC FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT FK_USER_REAC FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY UK_COMMENT_USER_REAC (comment_id, user_id)
) ENGINE=InnoDB;

-- ==========================================
-- 5. BẢNG POST_LIKES (Lượt thích bài viết)
-- ==========================================
CREATE TABLE post_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(50) DEFAULT 'LIKE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_user (post_id, user_id),
    CONSTRAINT FK_LIKE_POST FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT FK_LIKE_USER FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 6. BẢNG NOTIFICATIONS (Thông báo)
-- ==========================================
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    link VARCHAR(255),
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_NOTI_RECIPIENT FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_NOTI_SENDER FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 7. BẢNG FRIEND_REQUESTS & FRIENDS
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
-- 8. BẢNG CHAT (Messages & Conversations)
-- ==========================================
CREATE TABLE conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    description TEXT,
    category VARCHAR(100),
    privacy VARCHAR(50) DEFAULT 'PRIVATE',
    is_group_chat BOOLEAN DEFAULT FALSE,
    avatar VARCHAR(500)
) ENGINE=InnoDB;

CREATE TABLE conversation_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) DEFAULT 'MEMBER',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conv_member (conversation_id, user_id),
    CONSTRAINT FK_CM_CONV FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT FK_CM_USER FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE group_documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(255),
    category VARCHAR(50) DEFAULT 'Khác',
    size_bytes BIGINT DEFAULT 0,
    downloads INT DEFAULT 0,
    is_pinned BOOLEAN DEFAULT FALSE,
    uploader_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_GD_UPLOADER FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_GD_CONV FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE group_join_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_GJR_CONV FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT FK_GJR_USER FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_group_join (conversation_id, user_id)
) ENGINE=InnoDB;

CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT, 
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, 
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT DEFAULT NULL,
    conversation_id BIGINT DEFAULT NULL,
    file_url VARCHAR(500) DEFAULT NULL,
    file_name VARCHAR(255) DEFAULT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    deleted_by_sender BOOLEAN DEFAULT FALSE,
    deleted_by_receiver BOOLEAN DEFAULT FALSE,
    CONSTRAINT FK_MSG_SENDER FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_MSG_RECEIVER FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_MSG_CONV FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 9. BẢNG CÀI ĐẶT KIỂM DUYỆT & THÔNG BÁO ADMIN
-- ==========================================
CREATE TABLE moderation_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mode VARCHAR(50) NOT NULL DEFAULT 'NONE',
    ai_service_url VARCHAR(255) DEFAULT 'http://localhost:8000'
) ENGINE=InnoDB;

INSERT INTO moderation_settings (id, mode, ai_service_url) VALUES
(1, 'NONE', 'http://localhost:8000');

CREATE TABLE announcements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    admin_id BIGINT NOT NULL,
    CONSTRAINT FK_ANN_ADMIN FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
