package com.grievance.management.service;

import com.grievance.management.dto.AIAnalysisResponse;
import com.grievance.management.dto.GrievanceRequest;
import com.grievance.management.dto.GrievanceResponse;
import com.grievance.management.dto.RiskAssessmentResponse;
import com.grievance.management.entity.*;
import com.grievance.management.enums.GrievanceStatus;
import com.grievance.management.enums.Priority;
import com.grievance.management.exception.ResourceNotFoundException;
import com.grievance.management.exception.UnauthorizedException;
import com.grievance.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GrievanceService {

    private final GrievanceRepository  grievanceRepository;
    private final StudentRepository    studentRepository;
    private final FacultyRepository    facultyRepository;
    private final UserRepository       userRepository;
    private final DepartmentRepository departmentRepository;
    private final EvidenceService      evidenceService;
    private final AssignmentService    assignmentService;
    private final AIService            aiService;

    public GrievanceService(GrievanceRepository grievanceRepository,
                            StudentRepository studentRepository,
                            FacultyRepository facultyRepository,
                            UserRepository userRepository,
                            DepartmentRepository departmentRepository,
                            EvidenceService evidenceService,
                            AssignmentService assignmentService,
                            @Lazy AIService aiService) {
        this.grievanceRepository  = grievanceRepository;
        this.studentRepository    = studentRepository;
        this.facultyRepository    = facultyRepository;
        this.userRepository       = userRepository;
        this.departmentRepository = departmentRepository;
        this.evidenceService      = evidenceService;
        this.assignmentService    = assignmentService;
        this.aiService            = aiService;
    }

    // ── Submission ─────────────────────────────────────────────────────────────

    @Transactional
    public GrievanceResponse submitGrievance(GrievanceRequest request, List<MultipartFile> files) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Grievance grievance = Grievance.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .type(request.getType())
                .priority(Priority.MEDIUM)
                .status(GrievanceStatus.SUBMITTED)
                .student(student)
                .college(student.getCollege())
                .build();

        if (request.getDepartmentId() != null) {
            departmentRepository.findById(request.getDepartmentId())
                    .ifPresent(grievance::setDepartment);
        }

        Grievance saved = grievanceRepository.save(grievance);

        if (files != null && !files.isEmpty()) {
            evidenceService.uploadEvidence(saved.getId(), files);
        }

        // Run Python ML analysis before returning so AIAnalysis and RiskAssessment are available immediately.
        aiService.analyzeGrievance(saved.getId());
        saved = grievanceRepository.findById(saved.getId()).orElse(saved);

        // Auto-assign to a resolver in the college if one is available
        try {
            assignmentService.autoAssign(saved.getId());
            saved = grievanceRepository.findById(saved.getId()).orElse(saved);
        } catch (Exception ignored) {
            // No resolver available — grievance stays SUBMITTED
        }

        return mapToResponse(saved);
    }

    // ── Queries ────────────────────────────────────────────────────────────────

    public List<GrievanceResponse> getAllGrievances() {
        return grievanceRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    public GrievanceResponse getGrievanceById(Long id) {
        return mapToResponse(grievanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found with id: " + id)));
    }

    public List<GrievanceResponse> getMyGrievances() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        return grievanceRepository.findByStudentId(student.getId()).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    /** FIX: use Faculty.id (not User.id) to query assigned grievances. */
    public List<GrievanceResponse> getAssignedGrievances() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Faculty faculty = facultyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty profile not found"));
        return grievanceRepository.findByAssignedFacultyId(faculty.getId()).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<GrievanceResponse> getCommonGrievances() {
        return grievanceRepository.findByIsDuplicateTrue().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── Updates ────────────────────────────────────────────────────────────────

    /** FIX: Verify the authenticated student owns this grievance before allowing edit. */
    public GrievanceResponse updateGrievance(Long id, GrievanceRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Grievance grievance = grievanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        if (!grievance.getStudent().getId().equals(student.getId())) {
            throw new UnauthorizedException("You are not authorised to edit this grievance.");
        }
        if (grievance.getStatus() != GrievanceStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED grievances can be edited.");
        }

        grievance.setTitle(request.getTitle());
        grievance.setDescription(request.getDescription());
        grievance.setCategory(request.getCategory());
        return mapToResponse(grievanceRepository.save(grievance));
    }

    public GrievanceResponse updateStatus(Long id, String status, String note) {
        Grievance grievance = grievanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));
        grievance.setStatus(GrievanceStatus.valueOf(status.toUpperCase()));
        if (note != null) grievance.setResolutionNote(note);
        if (GrievanceStatus.RESOLVED.name().equals(status.toUpperCase())) {
            grievance.setResolvedAt(java.time.LocalDateTime.now());
        }
        return mapToResponse(grievanceRepository.save(grievance));
    }

    /**
     * Called by AIService after analysis completes.
     * Updates the Grievance.priority to match the AI-predicted priority.
     * FIX: priority was hardcoded to MEDIUM and never updated from AI result.
     */
    @Transactional
    public void updatePriorityFromAI(Long grievanceId, Priority predictedPriority) {
        if (predictedPriority == null) return;
        grievanceRepository.findById(grievanceId).ifPresent(g -> {
            g.setPriority(predictedPriority);
            grievanceRepository.save(g);
        });
    }

    // ── Public mapping alias ───────────────────────────────────────────────────

    public GrievanceResponse mapToResponsePublic(Grievance g) {
        return mapToResponse(g);
    }

    // ── Private mapping ────────────────────────────────────────────────────────

    private GrievanceResponse mapToResponse(Grievance g) {
        AIAnalysisResponse aiAnalysisResponse = null;
        if (g.getAiAnalysis() != null) {
            AIAnalysis a = g.getAiAnalysis();
            aiAnalysisResponse = AIAnalysisResponse.builder()
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

        RiskAssessmentResponse riskAssessmentResponse = null;
        if (g.getRiskAssessment() != null) {
            RiskAssessment r = g.getRiskAssessment();
            riskAssessmentResponse = RiskAssessmentResponse.builder()
                    .riskLevel(r.getRiskLevel())
                    .riskScore(r.getRiskScore())
                    .riskFactors(r.getRiskFactors())
                    .recommendations(r.getRecommendations())
                    .requiresImmediateAction(r.getRequiresImmediateAction())
                    .build();
        }

        return GrievanceResponse.builder()
                .id(g.getId())
                .title(g.getTitle())
                .description(g.getDescription())
                .category(g.getCategory())
                .type(g.getType())
                .priority(g.getPriority())
                .status(g.getStatus())
                .studentName(g.getStudent().getUser().getFirstName() + " " + g.getStudent().getUser().getLastName())
                .studentEnrollment(g.getStudent().getEnrollmentNumber())
                .collegeName(g.getCollege().getName())
                .departmentName(g.getDepartment() != null ? g.getDepartment().getName() : null)
                .assignedFacultyName(g.getAssignedFaculty() != null
                        ? g.getAssignedFaculty().getUser().getFirstName() + " " + g.getAssignedFaculty().getUser().getLastName()
                        : null)
                .isDuplicate(g.isDuplicate())
                .duplicateOfId(g.getDuplicateOfId())
                .resolutionNote(g.getResolutionNote())
                .submittedAt(g.getSubmittedAt())
                .resolvedAt(g.getResolvedAt())
                .aiAnalysis(aiAnalysisResponse)
                .riskAssessment(riskAssessmentResponse)
                .build();
    }
}
