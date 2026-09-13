package com.grievance.management.controller;

import com.grievance.management.dto.GrievanceResponse;
import com.grievance.management.entity.*;
import com.grievance.management.service.CollegeAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/college-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COLLEGE_ADMIN')")
public class CollegeAdminController {

    private final CollegeAdminService collegeAdminService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(collegeAdminService.getDashboardStats());
    }

    @GetMapping("/departments")
    public ResponseEntity<List<Department>> getDepartments() {
        return ResponseEntity.ok(collegeAdminService.getDepartments());
    }

    @PostMapping("/departments")
    public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
        return ResponseEntity.ok(collegeAdminService.createDepartment(department));
    }

    @GetMapping("/faculty")
    public ResponseEntity<List<Faculty>> getCollegeFaculty() {
        return ResponseEntity.ok(collegeAdminService.getCollegeFaculty());
    }

    @GetMapping("/students")
    public ResponseEntity<List<Student>> getCollegeStudents() {
        return ResponseEntity.ok(collegeAdminService.getCollegeStudents());
    }

    // Returns GrievanceResponse DTOs — includes aiAnalysis + riskAssessment
    @GetMapping("/grievances")
    public ResponseEntity<List<GrievanceResponse>> getCollegeGrievances() {
        return ResponseEntity.ok(collegeAdminService.getCollegeGrievances());
    }

    // Returns GrievanceResponse DTO after assignment
    @PostMapping("/grievances/{id}/assign")
    public ResponseEntity<GrievanceResponse> assignGrievance(
            @PathVariable Long id,
            @RequestParam Long facultyId) {
        return ResponseEntity.ok(collegeAdminService.assignGrievance(id, facultyId));
    }
}
