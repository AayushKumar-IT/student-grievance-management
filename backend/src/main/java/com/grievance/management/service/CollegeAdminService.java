package com.grievance.management.service;

import com.grievance.management.dto.GrievanceResponse;
import com.grievance.management.entity.*;
import com.grievance.management.enums.GrievanceStatus;
import com.grievance.management.exception.ResourceNotFoundException;
import com.grievance.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CollegeAdminService {

    private final CollegeAdminRepository collegeAdminRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final GrievanceRepository grievanceRepository;
    private final AssignmentService assignmentService;
    private final GrievanceService grievanceService;

    public CollegeAdminService(
            CollegeAdminRepository collegeAdminRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            FacultyRepository facultyRepository,
            StudentRepository studentRepository,
            GrievanceRepository grievanceRepository,
            AssignmentService assignmentService,
            @Lazy GrievanceService grievanceService) {
        this.collegeAdminRepository = collegeAdminRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.grievanceRepository = grievanceRepository;
        this.assignmentService = assignmentService;
        this.grievanceService = grievanceService;
    }

    private CollegeAdmin getCurrentAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return collegeAdminRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Admin profile not found"));
    }

    public Map<String, Object> getDashboardStats() {
        CollegeAdmin admin = getCurrentAdmin();
        Long collegeId = admin.getCollege().getId();

        long total    = grievanceRepository.countByCollegeId(collegeId);
        long pending  = grievanceRepository.countByCollegeIdAndStatus(collegeId, GrievanceStatus.SUBMITTED)
                      + grievanceRepository.countByCollegeIdAndStatus(collegeId, GrievanceStatus.UNDER_REVIEW)
                      + grievanceRepository.countByCollegeIdAndStatus(collegeId, GrievanceStatus.ASSIGNED)
                      + grievanceRepository.countByCollegeIdAndStatus(collegeId, GrievanceStatus.IN_PROGRESS);
        long resolved = grievanceRepository.countByCollegeIdAndStatus(collegeId, GrievanceStatus.RESOLVED)
                      + grievanceRepository.countByCollegeIdAndStatus(collegeId, GrievanceStatus.CLOSED);
        long rejected = grievanceRepository.countByCollegeIdAndStatus(collegeId, GrievanceStatus.REJECTED);

        return Map.of(
                "totalGrievances",    total,
                "pendingGrievances",  pending,
                "resolvedGrievances", resolved,
                "rejectedGrievances", rejected,
                "totalStudents",      studentRepository.findByCollegeId(collegeId).size(),
                "totalFaculty",       facultyRepository.findByCollegeId(collegeId).size()
        );
    }

    public List<Department> getDepartments() {
        CollegeAdmin admin = getCurrentAdmin();
        return departmentRepository.findByCollegeId(admin.getCollege().getId());
    }

    public Department createDepartment(Department department) {
        CollegeAdmin admin = getCurrentAdmin();
        department.setCollege(admin.getCollege());
        return departmentRepository.save(department);
    }

    public List<Faculty> getCollegeFaculty() {
        CollegeAdmin admin = getCurrentAdmin();
        return facultyRepository.findByCollegeId(admin.getCollege().getId());
    }

    public List<Student> getCollegeStudents() {
        CollegeAdmin admin = getCurrentAdmin();
        return studentRepository.findByCollegeId(admin.getCollege().getId());
    }

    // Returns proper GrievanceResponse DTOs (includes aiAnalysis + riskAssessment)
    public List<GrievanceResponse> getCollegeGrievances() {
        CollegeAdmin admin = getCurrentAdmin();
        return grievanceRepository.findByCollegeId(admin.getCollege().getId())
                .stream()
                .map(grievanceService::mapToResponsePublic)
                .toList();
    }

    // Returns proper GrievanceResponse DTO after assignment
    public GrievanceResponse assignGrievance(Long grievanceId, Long facultyId) {
        Grievance grievance = assignmentService.assignGrievance(grievanceId, facultyId);
        return grievanceService.mapToResponsePublic(grievance);
    }
}
