package com.grievance.management.controller;

import com.grievance.management.entity.Evidence;
import com.grievance.management.service.EvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/evidence")
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceService evidenceService;

    /** Students upload evidence for their own grievance; admins/faculty can also add. */
    @PostMapping("/grievance/{grievanceId}")
    @PreAuthorize("hasAnyRole('STUDENT','FACULTY','COLLEGE_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<Evidence>> uploadEvidence(
            @PathVariable Long grievanceId,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(evidenceService.uploadEvidence(grievanceId, files));
    }

    /** Any authenticated user can view evidence for a grievance. */
    @GetMapping("/grievance/{grievanceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Evidence>> getEvidenceByGrievance(@PathVariable Long grievanceId) {
        return ResponseEntity.ok(evidenceService.getEvidenceByGrievance(grievanceId));
    }

    /** Any authenticated user can download evidence files. */
    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadEvidence(@PathVariable Long id) {
        return evidenceService.downloadEvidence(id);
    }

    /** Only admins and faculty can delete evidence; students delete their own via ownership check in service. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','FACULTY','COLLEGE_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> deleteEvidence(@PathVariable Long id) {
        evidenceService.deleteEvidence(id);
        return ResponseEntity.noContent().build();
    }
}
