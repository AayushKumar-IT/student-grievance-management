# Datasets — AI-Based Student Grievance Management System

This folder contains the training and reference datasets used by the Python AI microservice to analyse student grievances.

---

## Files

| File | Rows | Description |
|------|------|-------------|
| `student_grievances_synthetic.csv` | 1000 | Synthetic labelled student grievance dataset |

---

## Dataset: `student_grievances_synthetic.csv`

### Purpose

This dataset trains and evaluates all five AI models used by the microservice:

| Model | Trained On | Output |
|-------|-----------|--------|
| Category Classifier | `title`, `description` → `category` | `predicted_category` + `category_confidence` |
| Priority Classifier | `title`, `description` → `priority` | `predicted_priority` + `priority_confidence` |
| Fake Complaint Detector | `title`, `description` → `is_fake` | `is_fake` + `fake_confidence` |
| Duplicate Detector | Cosine similarity between grievance texts | `is_duplicate` + `similarity_score` |
| Anomaly Detector | Isolation Forest on TF-IDF vectors | `is_anomaly` + `anomaly_score` |

---

### Column Schema

| Column | Type | Values | Description |
|--------|------|--------|-------------|
| `id` | integer | 1–1000 | Unique row identifier |
| `title` | string | free text | Short grievance headline |
| `description` | string | free text | Full grievance narrative |
| `category` | enum | see below | Grievance category label |
| `priority` | enum | `LOW` `MEDIUM` `HIGH` `CRITICAL` | Urgency level |
| `is_fake` | boolean | `true` / `false` | Whether the grievance is spam, test, or fabricated |
| `is_duplicate` | boolean | `true` / `false` | Whether the grievance is a repeat of an earlier submission |
| `is_anomaly` | boolean | `true` / `false` | Whether the grievance is unusual, threatening, or an outlier |

### Category Values

| Value | Description |
|-------|-------------|
| `ACADEMIC` | Grading, faculty conduct, exams, syllabus, attendance |
| `INFRASTRUCTURE` | Buildings, equipment, electrical, water, safety |
| `HARASSMENT` | Ragging, discrimination, bullying, threats, privacy violations |
| `FINANCIAL` | Fees, scholarships, refunds, billing errors, financial fraud |
| `ADMINISTRATIVE` | Certificates, records, portals, office processes |
| `HOSTEL` | Accommodation, mess, warden, hostel facilities |
| `TRANSPORTATION` | College buses, routes, drivers, passes |
| `LIBRARY` | Books, access, fines, e-resources, library staff |
| `LABORATORY` | Lab safety, equipment, marks, practical sessions |
| `OTHER` | Anything not fitting the above categories |

---

### Label Distribution

#### Category

| Category | Count | % |
|----------|-------|---|
| ACADEMIC | 148 | 14.8 |
| INFRASTRUCTURE | 108 | 10.8 |
| HARASSMENT | 110 | 11.0 |
| FINANCIAL | 100 | 10.0 |
| ADMINISTRATIVE | 100 | 10.0 |
| HOSTEL | 110 | 11.0 |
| TRANSPORTATION | 110 | 11.0 |
| LIBRARY | 100 | 10.0 |
| LABORATORY | 100 | 10.0 |
| OTHER | 114 | 11.4 |

#### Priority

| Priority | Count | % |
|----------|-------|---|
| LOW | 98 | 9.8 |
| MEDIUM | 212 | 21.2 |
| HIGH | 436 | 43.6 |
| CRITICAL | 254 | 25.4 |

#### Flags

| Flag | True | False | True % |
|------|------|-------|--------|
| `is_fake` | 42 | 958 | 4.2 |
| `is_duplicate` | 20 | 980 | 2.0 |
| `is_anomaly` | 118 | 882 | 11.8 |

---

### Fake Complaint Patterns (for classifier training)

Rows where `is_fake = true` include:

- One-word or near-empty submissions (rows 398, 871, 874, 875)
- Explicit test/system check entries (rows 206, 600, 861)
- Spam text or external links (rows 872, 878)
- Gibberish or repeated characters (rows 873, 874, 879, 880)
- Deliberately exaggerated/vague allegations (rows 876, 877)
- Entries with internal anomalous keywords (row 496 — injection-style text)

### Duplicate Patterns (for similarity training)

Rows where `is_duplicate = true` are near-copies of earlier rows. Key pairs:

| Duplicate Row | Original Row | Topic |
|---------------|-------------|-------|
| 305 | 1 | Unfair grading |
| 397 | 305 | Unfair grading (also fake) |
| 501 | (implied) | Class cancellations |
| 862 | 52 | Hostel food quality |
| 863 | 61 | Bus late |
| 864 | 72 | Library fine |
| 865 | 83 | Lab safety |
| 866 | 21 | Harassment by senior |
| 867 | 33 | Exam fee double charge |
| 868 | 41 | Certificate not issued |
| 869 | 21 | Ragging |
| 870 | 11 | Broken projector |

### Anomaly Patterns (for Isolation Forest training)

Rows where `is_anomaly = true` include:

- Systemic institutional fraud (rows 537, 852, 853, 963, 969)
- Physical safety emergencies (rows 319, 412, 416, 581, 763, 896)
- Criminal-level conduct (rows 234, 438, 854, 959, 964, 967)
- Severe data/privacy breaches (rows 438, 542, 545, 625, 698, 970)
- Injection/manipulation-style text (row 496)
- Events with external legal consequences (rows 856, 992, 993)

---

### How the AI Microservice Uses This Dataset

```
student_grievances_synthetic.csv
        +
CFPB Consumer Complaint Dataset (external — see README.md in project root)
        ↓
Preprocessing:
  - Strip punctuation, lowercase, remove stopwords
  - TF-IDF vectorisation (unigrams + bigrams, max 10000 features)
        ↓
┌─────────────────────────────────────────────────────────────────┐
│  Model 1: Category Classifier                                   │
│  Algorithm: Logistic Regression (multi-class, one-vs-rest)      │
│  Input:  TF-IDF(title + description)                            │
│  Output: predicted_category, category_confidence                │
├─────────────────────────────────────────────────────────────────┤
│  Model 2: Priority Classifier                                   │
│  Algorithm: Random Forest (4-class)                             │
│  Input:  TF-IDF(title + description)                            │
│  Output: predicted_priority, priority_confidence                │
├─────────────────────────────────────────────────────────────────┤
│  Model 3: Fake Complaint Detector                               │
│  Algorithm: Logistic Regression (binary)                        │
│  Input:  TF-IDF(title + description) + length features          │
│  Output: is_fake, fake_confidence                               │
├─────────────────────────────────────────────────────────────────┤
│  Model 4: Anomaly Detector                                      │
│  Algorithm: Isolation Forest                                    │
│  Input:  TF-IDF(description) reduced via TruncatedSVD           │
│  Output: is_anomaly, anomaly_score (normalised 0–1)             │
├─────────────────────────────────────────────────────────────────┤
│  Model 5: Duplicate Detector                                    │
│  Algorithm: Cosine Similarity on TF-IDF vectors                 │
│  Input:  TF-IDF of incoming grievance vs stored grievance texts │
│  Threshold: similarity > 0.80 = duplicate                       │
│  Output: is_duplicate, similarity_score, duplicate_of_id        │
└─────────────────────────────────────────────────────────────────┘
        ↓
All models saved as .pkl files using joblib
        ↓
Loaded at FastAPI/Flask startup
        ↓
POST /analyze → run all 5 models → return JSON → Spring Boot backend
```

---

### Using This Dataset

**Load in Python:**
```python
import pandas as pd

df = pd.read_csv("datasets/student_grievances_synthetic.csv")

# Separate real grievances from fakes for training classifiers
real = df[df["is_fake"] == False]

X = real["title"] + " " + real["description"]
y_category = real["category"]
y_priority = real["priority"]
y_anomaly  = real["is_anomaly"]
```

**Train/test split:**
```python
from sklearn.model_selection import train_test_split

X_train, X_test, y_train, y_test = train_test_split(
    X, y_category, test_size=0.2, random_state=42, stratify=y_category
)
```

**Combine with CFPB data:**
```python
cfpb = pd.read_csv("datasets/cfpb_complaints.csv")
# Remap CFPB product → GrievanceCategory
category_map = {
    "Student loan": "FINANCIAL",
    "Debt collection": "FINANCIAL",
    "Education": "ACADEMIC",
    ...
}
cfpb["category"] = cfpb["product"].map(category_map)
combined = pd.concat([real, cfpb[["title","description","category"]]], ignore_index=True)
```

---

### Extending the Dataset

To improve model accuracy:

1. Add more rows to this CSV following the same column format
2. Ensure `is_fake = false` for real training examples
3. Mark obvious duplicates with `is_duplicate = true` and reference the original in the description
4. Mark edge cases, threats, and institutional fraud with `is_anomaly = true`
5. Maintain category balance — aim for at least 100 rows per category

A minimum of 500 real rows per category is recommended for production-quality classifiers.
