package com.grievance.management.service;

import com.grievance.management.dto.RiskAssessmentResponse;
import com.grievance.management.entity.AIAnalysis;
import com.grievance.management.entity.Grievance;
import com.grievance.management.entity.RiskAssessment;
import com.grievance.management.enums.RiskLevel;
import com.grievance.management.exception.ResourceNotFoundException;
import com.grievance.management.repository.RiskAssessmentRepository;
import com.grievance.management.repository.GrievanceRepository;
import com.grievance.management.enums.Priority;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final GrievanceRepository grievanceRepository;

    public void assessRisk(Grievance grievance, AIAnalysis analysis) {
        double riskScore = calculateRiskScore(analysis);
        RiskLevel riskLevel = determineRiskLevel(riskScore);

        // Combine the ML-predicted urgency with the independently calculated risk.
        // This prevents the final priority from being driven by only one signal.
        double combinedPriorityScore = calculateCombinedPriorityScore(analysis, riskScore);
        Priority finalPriority = determinePriority(combinedPriorityScore);
        grievance.setPriority(finalPriority);
        grievanceRepository.save(grievance);

        RiskAssessment assessment = RiskAssessment.builder()
                .grievance(grievance)
                .riskLevel(riskLevel)
                .riskScore(riskScore)
                .riskFactors(buildRiskFactors(analysis))
                .recommendations(buildRecommendations(riskLevel))
                .requiresImmediateAction(riskLevel == RiskLevel.CRITICAL || riskLevel == RiskLevel.HIGH)
                .build();

        riskAssessmentRepository.save(assessment);
    }

    public RiskAssessmentResponse getRiskByGrievanceId(Long grievanceId) {
        RiskAssessment assessment = riskAssessmentRepository.findByGrievanceId(grievanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Risk assessment not found for grievance: " + grievanceId));
        return mapToResponse(assessment);
    }

    private double calculateRiskScore(AIAnalysis analysis) {
        double score = 0.0;
        if (Boolean.TRUE.equals(analysis.getIsAnomaly())) score += 40;
        if (Boolean.TRUE.equals(analysis.getIsFakeComplaint())) score -= 20;
        if (analysis.getAnomalyScore() != null) score += analysis.getAnomalyScore() * 30;
        if (Boolean.TRUE.equals(analysis.getIsDuplicate())) score += 5;
        return Math.min(100, Math.max(0, score));
    }

    /**
     * Produces the final priority from two independent AI signals:
     * 65% = predicted urgency from the ML priority model
     * 35% = calculated grievance risk score
     *
     * Confidence slightly scales the ML signal so a low-confidence prediction
     * does not dominate the final decision.
     */
    private double calculateCombinedPriorityScore(AIAnalysis analysis, double riskScore) {
        double mlPriorityBase = switch (analysis.getPredictedPriority()) {
            case CRITICAL -> 90.0;
            case HIGH -> 70.0;
            case MEDIUM -> 45.0;
            case LOW -> 20.0;
        };

        double confidence = analysis.getPriorityConfidence() == null
                ? 0.75
                : Math.max(0.0, Math.min(1.0, analysis.getPriorityConfidence()));

        // Keep a baseline of 50% of the ML signal even when confidence is low.
        double confidenceAdjustedMl = mlPriorityBase * (0.50 + (0.50 * confidence));
        return Math.min(100.0, Math.max(0.0,
                (confidenceAdjustedMl * 0.65) + (riskScore * 0.35)));
    }

    private Priority determinePriority(double score) {
        if (score >= 75) return Priority.CRITICAL;
        if (score >= 50) return Priority.HIGH;
        if (score >= 25) return Priority.MEDIUM;
        return Priority.LOW;
    }

    private RiskLevel determineRiskLevel(double score) {
        if (score >= 75) return RiskLevel.CRITICAL;
        if (score >= 50) return RiskLevel.HIGH;
        if (score >= 25) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private String buildRiskFactors(AIAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(analysis.getIsAnomaly())) sb.append("Anomalous pattern detected. ");
        if (Boolean.TRUE.equals(analysis.getIsFakeComplaint())) sb.append("Potential fake complaint. ");
        if (Boolean.TRUE.equals(analysis.getIsDuplicate())) sb.append("Duplicate submission. ");
        return sb.toString().trim();
    }

    private String buildRecommendations(RiskLevel level) {
        return switch (level) {
            case CRITICAL -> "Immediate escalation required. Notify college administration and authorities.";
            case HIGH -> "Priority review needed. Assign to senior faculty resolver within 24 hours.";
            case MEDIUM -> "Standard review process. Assign within 48 hours.";
            case LOW -> "Routine handling. Assign within 5 working days.";
        };
    }

    private RiskAssessmentResponse mapToResponse(RiskAssessment r) {
        return RiskAssessmentResponse.builder()
                .riskLevel(r.getRiskLevel())
                .riskScore(r.getRiskScore())
                .riskFactors(r.getRiskFactors())
                .recommendations(r.getRecommendations())
                .requiresImmediateAction(r.getRequiresImmediateAction())
                .build();
    }
}
