import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { GrievanceService } from '../../core/services/grievance.service';
import { GrievanceResponse, GrievanceStatus } from '../../core/models/grievance.model';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-incident-validation',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, NavbarComponent, SidebarComponent,
            StatusBadgeComponent, LoadingSpinnerComponent],
  templateUrl: './incident-validation.component.html',
  styleUrls: ['./incident-validation.component.css']
})
export class IncidentValidationComponent implements OnInit {
  grievances: GrievanceResponse[] = [];
  loading  = true;
  error    = '';
  success  = '';

  // Track which grievance is being validated and what status is selected
  validatingId: number | null = null;
  selectedStatus: { [key: number]: string } = {};
  resolutionNote: { [key: number]: string } = {};
  saving = false;

  validationStatuses: GrievanceStatus[] = ['UNDER_REVIEW', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'];

  constructor(private grievanceService: GrievanceService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.grievanceService.getAssignedGrievances().subscribe({
      next: data => {
        // Show active grievances that require faculty action
        this.grievances = data.filter(g =>
          g.status === 'SUBMITTED' ||
          g.status === 'IN_PROGRESS' ||
          g.status === 'UNDER_REVIEW' ||
          g.status === 'ASSIGNED'
        );
        this.loading = false;
      },
      error: () => { this.error = 'Failed to load grievances.'; this.loading = false; }
    });
  }

  startValidation(g: GrievanceResponse): void {
    this.validatingId = g.id;
    // Pre-select the next logical status
    this.selectedStatus[g.id] = g.status === 'SUBMITTED' || g.status === 'ASSIGNED'
      ? 'UNDER_REVIEW' : g.status === 'UNDER_REVIEW' ? 'IN_PROGRESS' : 'RESOLVED';
    this.resolutionNote[g.id] = '';
    this.success = '';
    this.error = '';
  }

  cancelValidation(): void {
    this.validatingId = null;
  }

  submitValidation(g: GrievanceResponse): void {
    const status = this.selectedStatus[g.id];
    const note   = this.resolutionNote[g.id] || undefined;
    if (!status) return;

    this.saving = true;
    this.error = '';
    this.grievanceService.updateStatus(g.id, status, note).subscribe({
      next: updated => {
        const idx = this.grievances.findIndex(x => x.id === g.id);
        if (idx > -1) this.grievances[idx] = updated;
        this.success = `Grievance #${g.id} updated to ${status}.`;
        this.validatingId = null;
        this.saving = false;
        // Refresh list to remove resolved/rejected from active view
        this.load();
      },
      error: err => {
        this.error = err.error?.message ?? 'Update failed.';
        this.saving = false;
      }
    });
  }
}
