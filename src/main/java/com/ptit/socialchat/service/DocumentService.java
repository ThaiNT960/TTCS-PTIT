package com.ptit.socialchat.service;

import com.ptit.socialchat.entity.*;
import com.ptit.socialchat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    @Autowired
    private GroupDocumentRepository documentRepository;
    
    @Autowired
    private ConversationMemberRepository memberRepository;

    private final String UPLOAD_DIR = "uploads/documents/";

    public DocumentService() {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Transactional
    public GroupDocument uploadDocument(MultipartFile file, String name, String description, String fileType, String category, Conversation conversation, User uploader) throws IOException {
        // Kiểm tra quyền (phải là thành viên mới được upload)
        boolean isMember = memberRepository.findByConversationAndUser(conversation, uploader).isPresent();
        if (!isMember) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không phải là thành viên của nhóm này.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String savedFileName = UUID.randomUUID().toString() + extension;
        Path path = Paths.get(UPLOAD_DIR + savedFileName);
        Files.write(path, file.getBytes());

        GroupDocument doc = new GroupDocument();
        doc.setName(name);
        doc.setDescription(description);
        doc.setFileType(fileType);
        doc.setCategory(category != null ? category : "Khác");
        doc.setFileUrl("/" + UPLOAD_DIR + savedFileName);
        doc.setSizeBytes(file.getSize());
        doc.setUploader(uploader);
        doc.setConversation(conversation);
        
        return documentRepository.save(doc);
    }

    public List<GroupDocument> getDocumentsByGroup(Long conversationId) {
        return documentRepository.findByConversationIdOrderByIsPinnedDescCreatedAtDesc(conversationId);
    }

    @Transactional
    public void incrementDownload(Long documentId, User user) {
        GroupDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu"));
        
        boolean isMember = memberRepository.findByConversationAndUser(doc.getConversation(), user).isPresent();
        if (!isMember) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không phải là thành viên của nhóm này.");
        }
        
        doc.setDownloads(doc.getDownloads() + 1);
        documentRepository.save(doc);
    }
    
    @Transactional
    public void deleteDocument(Long documentId, User user) {
        GroupDocument doc = documentRepository.findById(documentId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu"));
        
        boolean isUploader = doc.getUploader().getId().equals(user.getId());
        
        ConversationMember member = memberRepository.findByConversationAndUser(doc.getConversation(), user).orElse(null);
        boolean isAdminOrCoAdmin = member != null && ("ADMIN".equals(member.getRole()) || "CO_ADMIN".equals(member.getRole()));
        
        if (!isUploader && !isAdminOrCoAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xóa tài liệu này.");
        }
        
        // Xóa file vật lý
        try {
            String url = doc.getFileUrl();
            if (url.startsWith("/")) url = url.substring(1);
            Files.deleteIfExists(Paths.get(url));
        } catch (Exception ignored) {}
        
        documentRepository.delete(doc);
    }
    
    @Transactional
    public void togglePinDocument(Long documentId, User user) {
        GroupDocument doc = documentRepository.findById(documentId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu"));
        
        ConversationMember member = memberRepository.findByConversationAndUser(doc.getConversation(), user)
                .orElseThrow(() -> new IllegalArgumentException("Bạn không thuộc nhóm này"));
                
        if ("MEMBER".equals(member.getRole())) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ quản trị viên mới có quyền ghim tài liệu trong nhóm.");
        }
        
        doc.setPinned(!doc.getPinned());
        documentRepository.save(doc);
    }
}



