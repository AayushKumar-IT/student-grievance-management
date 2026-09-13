package com.grievance.management.service;

import com.grievance.management.dto.AIAnalysisResponse;
import com.grievance.management.entity.AIAnalysis;
import com.grievance.management.entity.Grievance;
import com.grievance.management.enums.GrievanceCategory;
import com.grievance.management.enums.Priority;
import com.grievance.management.exception.ResourceNotFoundException;
import com.grievance.management.repository.AIAnalysisRepository;
import com.grievance.management.repository.GrievanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AIService {

    private final AIAnalysisRepository aiAnalysisRepository;
    private final GrievanceRepository grievanceRepository;
    private final RiskAssessmentService riskAssessmentService;
    private final RestTemplate restTemplate;

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;
    private Object grievanceService;

    public AIService(AIAnalysisRepository aiAnalysisRepository,
                     GrievanceRepository grievanceRepository,
                     RiskAssessmentService riskAssessmentService,
                     RestTemplate restTemplate) {
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.grievanceRepository = grievanceRepository;
        this.riskAssessmentService = riskAssessmentService;
        this.restTemplate = restTemplate;
        this.grievanceService = grievanceService;
    }

    public AIAnalysisResponse getAnalysisByGrievanceId(Long grievanceId) {
        AIAnalysis analysis = aiAnalysisRepository.findByGrievanceId(grievanceId)
                .orElseThrow(() -> new ResourceNotFoundException("AI Analysis not found for grievance: " + grievanceId));
        return mapToResponse(analysis);
    }

    /** Run Python ML analysis synchronously so the newly submitted grievance can return with risk data. */
    public AIAnalysisResponse analyzeGrievance(Long grievanceId) {
        Grievance grievance = grievanceRepository.findById(grievanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));
        return performAnalysis(grievance);
    }

    /** Background-compatible method used by the existing startup backfill runner. */
    public void analyzeGrievanceAsync(Long grievanceId) {
        try {
            grievanceRepository.findById(grievanceId).ifPresent(this::performAnalysis);
        } catch (Exception e) {
            log.error("Error during AI analysis for grievance {}: {}", grievanceId, e.getMessage());
        }
    }

    private AIAnalysisResponse performAnalysis(Grievance grievance) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", grievance.getTitle());
            payload.put("description", grievance.getDescription());
            payload.put("grievance_id", grievance.getId());
            payload.put("existing_grievances", buildExistingGrievances(grievance));

            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(
                    aiServerUrl + "/analyze", payload, Map.class);

            if (result == null) {
                throw new IllegalStateException("Python ML service returned an empty response");
            }

            AIAnalysis analysis = buildAnalysisFromResult(grievance, result);
            saveAnalysisAndRisk(grievance, analysis);
            log.info("Python ML analysis completed for grievance {}", grievance.getId());
            return mapToResponse(analysis);
        } catch (Exception e) {
            log.warn("Python ML service unavailable for grievance {}. Using Java fallback: {}",
                    grievance.getId(), e.getMessage());
            return runLocalFallback(grievance);
        }
    }

    private List<Map<String, Object>> buildExistingGrievances(Grievance current) {
        return grievanceRepository.findAll().stream()
                .filter(g -> !g.getId().equals(current.getId()))
                .map(g -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", g.getId());
                    item.put("title", g.getTitle());
                    item.put("description", g.getDescription());
                    return item;
                })
                .toList();
    }

    private void saveAnalysisAndRisk(Grievance grievance, AIAnalysis analysis) {
        aiAnalysisRepository.save(analysis);
        // RiskAssessmentService now calculates the final priority using a combined
        // decision from ML urgency + grievance risk. Do not overwrite it with the
        // raw ML priority prediction.
        riskAssessmentService.assessRisk(grievance, analysis);
    }

    /** Fallback keeps the application usable when Python is stopped. */
    private AIAnalysisResponse runLocalFallback(Grievance grievance) {
        AIAnalysis existing = aiAnalysisRepository.findByGrievanceId(grievance.getId()).orElse(null);
        if (existing != null) {
            return mapToResponse(existing);
        }

        String text = ((grievance.getTitle() == null ? "" : grievance.getTitle()) + " "
                + (grievance.getDescription() == null ? "" : grievance.getDescription())).toLowerCase();

        boolean isAnomaly = text.contains("harassment") || text.contains("ragging")
                || text.contains("assault") || text.contains("threat") || text.contains("violence")
                || text.contains("abuse") || text.contains("discriminat") || text.contains("urgent")
                || text.contains("emergency") || text.contains("danger") || text.contains("unsafe");

        boolean isFake = text.length() < 20 || text.equals("test")
                || text.contains("lorem ipsum")
                || ((grievance.getTitle() == null ? 0 : grievance.getTitle().length()) < 5
                && (grievance.getDescription() == null ? 0 : grievance.getDescription().length()) < 10);

        double anomalyScore = isAnomaly ? 0.75 : 0.10;
        double fakeConfidence = isFake ? 0.85 : 0.05;

        AIAnalysis analysis = AIAnalysis.builder()
                .grievance(grievance)
                .predictedCategory(grievance.getCategory())
                .categoryConfidence(0.80)
                .predictedPriority(grievance.getPriority())
                .priorityConfidence(0.75)
                .isFakeComplaint(isFake)
                .fakeConfidence(fakeConfidence)
                .isDuplicate(false)
                .duplicateSimilarityScore(0.0)
                .isAnomaly(isAnomaly)
                .anomalyScore(anomalyScore)
                .analysisNotes("Java fallback analysis (Python ML service unavailable)")
                .build();

        saveAnalysisAndRisk(grievance, analysis);
        return mapToResponse(analysis);
    }

    private AIAnalysis buildAnalysisFromResult(Grievance grievance, Map<String, Object> result) {
        GrievanceCategory predictedCategory = null;
        String categoryStr = (String) result.get("predicted_category");
        if (categoryStr != null) {
            try {
                predictedCategory = GrievanceCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown category from Python ML: {}", categoryStr);
            }
        }

        Priority predictedPriority = null;
        String priorityStr = (String) result.get("predicted_priority");
        if (priorityStr != null) {
            try {
                predictedPriority = Priority.valueOf(priorityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown priority from Python ML: {}", priorityStr);
            }
        }

        return AIAnalysis.builder()
                .grievance(grievance)
                .predictedCategory(predictedCategory != null ? predictedCategory : grievance.getCategory())
                .categoryConfidence(getDouble(result, "category_confidence", 0.0))
                .predictedPriority(predictedPriority != null ? predictedPriority : grievance.getPriority())
                .priorityConfidence(getDouble(result, "priority_confidence", 0.0))
                .isFakeComplaint(getBoolean(result, "is_fake", false))
                .fakeConfidence(getDouble(result, "fake_confidence", 0.0))
                .isDuplicate(getBoolean(result, "is_duplicate", false))
                .duplicateOfId(getLong(result, "duplicate_of_id"))
                .duplicateSimilarityScore(getDouble(result, "similarity_score", 0.0))
                .isAnomaly(getBoolean(result, "is_anomaly", false))
                .anomalyScore(getDouble(result, "anomaly_score", 0.0))
                .analysisNotes((String) result.getOrDefault("notes", "Python ML analysis"))
                .build();
    }

    private Double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : defaultValue;
    }

    private Boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private AIAnalysisResponse mapToResponse(AIAnalysis a) {
        return AIAnalysisResponse.builder()
                .predictedCategory(a.getPredictedCategory())
                .categoryConfidence(a.getCategoryConfidence())
                .predictedPriority(a.getPredictedPriority())
                .priorityConfidence(a.getPriorityConfidence())
                .isFakeComplaint(a.getIsFakeComplaint())
                .fakeConfidence(a.getFakeConfidence())
                .isDuplicate(a.getIsDuplicate())
                .duplicateOfId(a.getDuplicateOfId())
                .duplicateSimilarityScore(a.getDuplicateSimilarityScore())
                .isAnomaly(a.getIsAnomaly())
                .anomalyScore(a.getAnomalyScore())
                .analysisNotes(a.getAnalysisNotes())
                .build();
    }
}
