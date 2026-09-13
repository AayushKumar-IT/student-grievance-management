package com.grievance.management.config;

import com.grievance.management.repository.AIAnalysisRepository;
import com.grievance.management.repository.GrievanceRepository;
import com.grievance.management.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * On startup, finds any grievances that have no AI analysis record
 * (e.g. submitted while the AI server was offline) and re-triggers
 * the Python ML analysis (with Java fallback) so a risk score is available.
 */
@Slf4j
@Component
@Order(2)   // runs after DataInitializer (order 1 implied)
@RequiredArgsConstructor
public class AIBackfillRunner implements CommandLineRunner {

    private final GrievanceRepository   grievanceRepository;
    private final AIAnalysisRepository  aiAnalysisRepository;
    private final AIService             aiService;

    @Override
    public void run(String... args) {
        var missing = grievanceRepository.findAll().stream()
                .filter(g -> aiAnalysisRepository.findByGrievanceId(g.getId()).isEmpty())
                .toList();

        if (missing.isEmpty()) {
            log.info("AI backfill: all grievances already have analysis.");
            return;
        }

        log.info("AI backfill: {} grievance(s) missing analysis — triggering now.", missing.size());
        missing.forEach(g -> aiService.analyzeGrievanceAsync(g.getId()));
    }
}
