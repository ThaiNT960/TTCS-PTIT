
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
    avatar VARCHAR(255) DEFAULT NULL,
    cover_photo VARCHAR(255) DEFAULT NULL,
    bio TEXT,
    workplace VARCHAR(255),
    education VARCHAR(255),
    location VARCHAR(255),
    role VARCHAR(255),
    privacy_setting VARCHAR(50) DEFAULT 'PUBLIC'
) ENGINE=InnoDB;

-- Dữ liệu mẫu (Seed Data)
INSERT INTO users (id, username, password, full_name, role) VALUES
(1, 'vana', 'password123', 'Nguyễn Văn A', 'ROLE_USER'),
(2, 'thib', 'password123', 'Trần Thị B', 'ROLE_USER'),
(3, 'adminc', 'password123', 'Lê Văn C', 'ROLE_ADMIN');

-- ==========================================
-- 2. BẢNG CONVERSATIONS (Cuộc hội thoại)
-- ==========================================
CREATE TABLE conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    is_group_chat BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB;

-- ==========================================
-- 3. BẢNG CONVERSATION_MEMBERS (Thành viên cuộc hội thoại)
-- ==========================================
CREATE TABLE conversation_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_CM_CONV FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT FK_CM_USER FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 4. BẢNG POSTS (Bài đăng)
-- ==========================================
CREATE TABLE posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT, 
    video_url VARCHAR(255),
    check_in_location VARCHAR(255),
    privacy VARCHAR(50) DEFAULT 'PUBLIC',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    CONSTRAINT FK_USER_POST FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 5. BẢNG POST_MEDIA (Đa phương tiện bài đăng)
-- ==========================================
CREATE TABLE post_media (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    media_url VARCHAR(255) NOT NULL,
    media_type VARCHAR(50) NOT NULL,
    CONSTRAINT FK_PM_POST FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 6. BẢNG COMMENTS (Bình luận)
-- ==========================================
CREATE TABLE comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT, 
    image_url VARCHAR(255),
    parent_comment_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT FK_COMMENT_POST FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT FK_COMMENT_USER FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 7. BẢNG POST_LIKES (Lượt thích bài viết)
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
-- 8. BẢNG COMMENT_REACTIONS (Tương tác bình luận)
-- ==========================================
CREATE TABLE comment_reactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(50) NOT NULL,
    CONSTRAINT FK_CR_COMMENT FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT FK_CR_USER FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 9. BẢNG FRIEND_REQUESTS (Yêu cầu kết bạn)
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
-- 10. BẢNG FRIENDS (Danh sách bạn bè)
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
-- 11. BẢNG MESSAGES (Tin nhắn trò chuyện)
-- ==========================================
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT, 
    image_url VARCHAR(255),
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, 
    sender_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    CONSTRAINT FK_MSG_SENDER FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_MSG_CONV FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 12. BẢNG STORIES (Tin ngắn 24h)
-- ==========================================
CREATE TABLE stories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    media_url VARCHAR(255) NOT NULL,
    media_type VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    CONSTRAINT FK_STORY_USER FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 13. BẢNG STORY_VIEWS (Lượt xem story)
-- ==========================================
CREATE TABLE story_views (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    story_id BIGINT NOT NULL,
    viewer_id BIGINT NOT NULL,
    viewed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_SV_STORY FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    CONSTRAINT FK_SV_USER FOREIGN KEY (viewer_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- 14. BẢNG NOTIFICATIONS (Thông báo)
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
-- 15. BẢNG GROUPS_TABLE (Hội nhóm)
-- ==========================================
CREATE TABLE groups_table (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    cover_photo VARCHAR(255),
    privacy VARCHAR(50) DEFAULT 'PUBLIC'
) ENGINE=InnoDB;

-- ==========================================
-- 16. BẢNG GROUP_MEMBERS (Thành viên nhóm)
-- ==========================================
CREATE TABLE group_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_GM_GROUP FOREIGN KEY (group_id) REFERENCES groups_table(id) ON DELETE CASCADE,
    CONSTRAINT FK_GM_USER FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
