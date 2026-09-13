import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { GrievanceService } from '../../core/services/grievance.service';
import { AuthService } from '../../core/services/auth.service';
import { Department } from '../../core/models/college.model';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar.component';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-submit-grievance',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, NavbarComponent, SidebarComponent],
  templateUrl: './submit-grievance.component.html',
  styleUrls: ['./submit-grievance.component.css']
})
export class SubmitGrievanceComponent implements OnInit {
  form: FormGroup;
  loading       = false;
  loadingDepts  = false;
  error         = '';
  success       = '';
  selectedFiles: File[] = [];
  departments:   Department[] = [];

  categories = ['ACADEMIC','INFRASTRUCTURE','HARASSMENT','FINANCIAL',
                'ADMINISTRATIVE','HOSTEL','TRANSPORTATION','LIBRARY','LABORATORY','OTHER'];
  types = ['INDIVIDUAL','GROUP','ANONYMOUS'];

  constructor(
    private fb: FormBuilder,
    private grievanceService: GrievanceService,
    private authService: AuthService,
    private http: HttpClient,
    private router: Router
  ) {
    this.form = this.fb.group({
      title:        ['', [Validators.required, Validators.minLength(10)]],
      description:  ['', [Validators.required, Validators.minLength(30)]],
      category:     ['', Validators.required],
      type:         ['INDIVIDUAL', Validators.required],
      departmentId: [null]
    });
  }

  ngOnInit(): void {
    // Load departments for the student's own college
    const user = this.authService.getCurrentUser();
    if (user?.userId) {
      this.loadDepartmentsForStudent();
    }
  }

  private loadDepartmentsForStudent(): void {
    this.loadingDepts = true;
    // Get the student's profile which includes their college
    this.http.get<any>(`${environment.apiUrl}/students/profile`).subscribe({
      next: student => {
        const collegeId = student?.college?.id;
        if (collegeId) {
          this.http.get<Department[]>(`${environment.apiUrl}/public/colleges/${collegeId}/departments`)
            .subscribe({
              next: depts => { this.departments = depts; this.loadingDepts = false; },
              error: () => this.loadingDepts = false
            });
        } else {
          this.loadingDepts = false;
        }
      },
      error: () => this.loadingDepts = false
    });
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) this.selectedFiles = Array.from(input.files);
  }

  removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    this.grievanceService.submitGrievance(this.form.value, this.selectedFiles).subscribe({
      next: res => {
        this.success = 'Grievance submitted successfully!';
        setTimeout(() => this.router.navigate(['/student/grievance', res.id]), 1500);
      },
      error: err => {
        this.error = err.error?.message ?? 'Failed to submit grievance.';
        this.loading = false;
      }
    });
  }

  get f() { return this.form.controls; }
  get charCount() { return (this.form.get('description')?.value ?? '').length; }
}
