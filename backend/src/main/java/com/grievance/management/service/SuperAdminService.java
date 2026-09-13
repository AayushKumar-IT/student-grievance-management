package com.grievance.management.service;

import com.grievance.management.entity.*;
import com.grievance.management.enums.GrievanceStatus;
import com.grievance.management.enums.RiskLevel;
import com.grievance.management.exception.ResourceNotFoundException;
import com.grievance.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final CollegeRepository          collegeRepository;
    private final DepartmentRepository       departmentRepository;
    private final FacultyRepository          facultyRepository;
    private final StudentRepository          studentRepository;
    private final GrievanceRepository        grievanceRepository;
    private final UserRepository             userRepository;
    private final CollegeAdminRepository     collegeAdminRepository;
    private final RiskAssessmentRepository   riskAssessmentRepository;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public Map<String, Object> getDashboardStats() {
        long pending = grievanceRepository.countByStatus(GrievanceStatus.SUBMITTED)
                     + grievanceRepository.countByStatus(GrievanceStatus.UNDER_REVIEW)
                     + grievanceRepository.countByStatus(GrievanceStatus.ASSIGNED)
                     + grievanceRepository.countByStatus(GrievanceStatus.IN_PROGRESS);

        long resolved = grievanceRepository.countByStatus(GrievanceStatus.RESOLVED)
                      + grievanceRepository.countByStatus(GrievanceStatus.CLOSED);

        long critical = riskAssessmentRepository.countByRiskLevel(RiskLevel.CRITICAL)
                      + riskAssessmentRepository.countByRiskLevel(RiskLevel.HIGH);

        return Map.of(
                "totalColleges",      collegeRepository.count(),
                "totalStudents",      studentRepository.count(),
                "totalFaculty",       facultyRepository.count(),
                "totalCollegeAdmins", collegeAdminRepository.count(),
                "totalGrievances",    grievanceRepository.count(),
                "pendingGrievances",  pending,
                "resolvedGrievances", resolved,
                "criticalGrievances", critical
        );
    }

    // ── Colleges ──────────────────────────────────────────────────────────────

    public List<College> getAllColleges() {
        return collegeRepository.findAll();
    }

    public College createCollege(College college) {
        return collegeRepository.save(college);
    }

    public College updateCollege(Long id, College updated) {
        College college = collegeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("College not found: " + id));
        college.setName(updated.getName());
        college.setAddress(updated.getAddress());
        college.setCity(updated.getCity());
        college.setState(updated.getState());
        college.setEmail(updated.getEmail());
        college.setPhoneNumber(updated.getPhoneNumber());
        return collegeRepository.save(college);
    }

    public void deleteCollege(Long id) {
        if (!collegeRepository.existsById(id))
            throw new ResourceNotFoundException("College not found: " + id);
        collegeRepository.deleteById(id);
    }

    // ── College Admins ────────────────────────────────────────────────────────

    public List<CollegeAdmin> getAllCollegeAdmins() {
        return collegeAdminRepository.findAll();
    }

    public List<CollegeAdmin> getCollegeAdminsFiltered(Long collegeId) {
        if (collegeId != null)
            return collegeAdminRepository.findByCollegeId(collegeId);
        return collegeAdminRepository.findAll();
    }

    // ── Faculty ───────────────────────────────────────────────────────────────

    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAll();
    }

    public List<Faculty> getFacultyFiltered(Long collegeId, Long departmentId) {
        if (collegeId != null && departmentId != null)
            return facultyRepository.findByDepartmentId(departmentId);
        if (collegeId != null)
            return facultyRepository.findByCollegeId(collegeId);
        return facultyRepository.findAll();
    }

    // ── Students ──────────────────────────────────────────────────────────────

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public List<Student> getStudentsFiltered(Long collegeId, Long departmentId) {
        if (collegeId != null && departmentId != null)
            return studentRepository.findByDepartmentId(departmentId);
        if (collegeId != null)
            return studentRepository.findByCollegeId(collegeId);
        return studentRepository.findAll();
    }

    // ── Departments ───────────────────────────────────────────────────────────

    public List<Department> getDepartmentsByCollege(Long collegeId) {
        return departmentRepository.findByCollegeId(collegeId);
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    public Map<String, Object> getSystemReports() {
        long pending = grievanceRepository.countByStatus(GrievanceStatus.SUBMITTED)
                     + grievanceRepository.countByStatus(GrievanceStatus.UNDER_REVIEW)
                     + grievanceRepository.countByStatus(GrievanceStatus.ASSIGNED)
                     + grievanceRepository.countByStatus(GrievanceStatus.IN_PROGRESS);

        long critical = riskAssessmentRepository.countByRiskLevel(RiskLevel.CRITICAL)
                      + riskAssessmentRepository.countByRiskLevel(RiskLevel.HIGH);

        return Map.of(
                "totalUsers",         userRepository.count(),
                "totalColleges",      collegeRepository.count(),
                "totalGrievances",    grievanceRepository.count(),
                "resolvedGrievances", grievanceRepository.countByStatus(GrievanceStatus.RESOLVED),
                "pendingGrievances",  pending,
                "criticalGrievances", critical
        );
    }
}
