package com.example.ptitsocialchat.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service xử lý và lưu trữ file upload tĩnh vào thư mục cấu hình.
 */
@Service
public class FileUploadService {

    // Thư mục gốc lưu file upload (tương đối từ working directory)
    private static final String UPLOAD_ROOT = "uploads";

    /**
     * Lưu file upload vào thư mục con chỉ định.
     *
     * @param file         File upload từ client
     * @param subDirectory Thư mục con (ví dụ: "posts", "chats", "avatars")
     * @return URL tương đối để truy cập file (ví dụ: "/uploads/posts/uuid-filename.jpg")
     */
    public String saveFile(MultipartFile file, String subDirectory) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống.");
        }

        // Validate extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) originalFilename = "unknown.jpg";

        String ext = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            ext = originalFilename.substring(dotIndex + 1).toLowerCase();
        }
        if (!ext.matches("^(jpg|jpeg|png|gif|webp)$")) {
            throw new IllegalArgumentException("Định dạng file không hợp lệ. Chỉ chấp nhận: jpg, jpeg, png, gif, webp.");
        }

        // Tạo tên file duy nhất
        String fileName = UUID.randomUUID().toString() + "-" + originalFilename;

        // Tạo thư mục nếu chưa có
        Path uploadDir = Paths.get(UPLOAD_ROOT, subDirectory);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Ghi file
        Path filePath = uploadDir.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // Trả về URL tương đối
        return "/" + UPLOAD_ROOT + "/" + subDirectory + "/" + fileName;
    }
}
