import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { SuperAdminService } from '../../core/services/super-admin.service';
import { College } from '../../core/models/college.model';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-college-admins',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, SidebarComponent, LoadingSpinnerComponent],
  templateUrl: './college-admins.component.html',
  styleUrls: ['./college-admins.component.css']
})
export class CollegeAdminsComponent implements OnInit {
  colleges:      College[] = [];
  admins:        any[]     = [];
  filtered:      any[]     = [];

  selectedCollegeId: number | null = null;
  searchTerm = '';
  loading    = false;
  error      = '';

  constructor(
    private superAdminService: SuperAdminService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.http.get<College[]>(`${environment.apiUrl}/public/colleges`).subscribe({
      next:  data => this.colleges = data,
      error: ()   => this.error = 'Failed to load colleges.'
    });
    this.fetchAdmins();
  }

  onCollegeChange(): void { this.fetchAdmins(); }

  fetchAdmins(): void {
    this.loading = true; this.error = '';
    this.superAdminService.getCollegeAdmins(this.selectedCollegeId ?? undefined).subscribe({
      next:  data => { this.admins = data; this.applySearch(); this.loading = false; },
      error: ()   => { this.error = 'Failed to load college admins.'; this.loading = false; }
    });
  }

  applySearch(): void {
    const q = this.searchTerm.toLowerCase().trim();
    this.filtered = q
      ? this.admins.filter(a =>
          `${a.user?.firstName} ${a.user?.lastName} ${a.user?.email} ${a.college?.name}`
            .toLowerCase().includes(q))
      : [...this.admins];
  }

  clearFilters(): void {
    this.selectedCollegeId = null;
    this.searchTerm = '';
    this.fetchAdmins();
  }

  get hasFilters(): boolean {
    return !!(this.selectedCollegeId || this.searchTerm);
  }
}
