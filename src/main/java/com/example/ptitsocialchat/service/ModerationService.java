package com.example.ptitsocialchat.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;
import java.util.HashMap;

/**
 * Service to call Python FastAPI Moderation service.
 */
@Service
public class ModerationService {
    
    private final RestTemplate restTemplate = new RestTemplate();

    public static class ModerationResult {
        private String label; // CLEAN, OFFENSIVE, HATE
        private int labelId;
        private double confidence;
        private boolean toxic;
        private boolean success;
        private String errorMessage;

        // Getters and Setters
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public int getLabelId() { return labelId; }
        public void setLabelId(int labelId) { this.labelId = labelId; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public boolean isToxic() { return toxic; }
        public void setToxic(boolean toxic) { this.toxic = toxic; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public ModerationResult moderate(String text, String aiServiceUrl) {
        ModerationResult result = new ModerationResult();

        if (text == null || text.trim().isEmpty()) {
            result.setSuccess(true);
            result.setLabel("CLEAN");
            result.setLabelId(0);
            result.setConfidence(1.0);
            result.setToxic(false);
            return result;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> body = new HashMap<>();
            body.put("text", text.trim());
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(aiServiceUrl + "/api/moderate", request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = response.getBody();
                result.setLabel((String) data.get("label"));
                result.setLabelId((Integer) data.get("label_id"));
                result.setConfidence((Double) data.get("confidence"));
                result.setToxic((Boolean) data.get("is_toxic"));
                result.setSuccess(true);
            } else {
                result.setSuccess(false);
                result.setErrorMessage("HTTP Error from AI service");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        return result;
    }

    public boolean isServiceAvailable(String aiServiceUrl) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(aiServiceUrl + "/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
