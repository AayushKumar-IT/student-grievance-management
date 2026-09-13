# Python ML Service — Student Grievance Management System

This service plugs into the existing Spring Boot backend at `POST /analyze`.

## Models

- **Category:** TF-IDF + Logistic Regression
- **Priority:** TF-IDF + Logistic Regression
- **Fake/spam:** TF-IDF + Logistic Regression
- **Duplicate detection:** TF-IDF cosine similarity against grievances supplied by Java
- **Anomaly score:** transparent rule layer using safety keywords, prediction confidence and priority

The included CSV is a small demonstration dataset. For a production-quality system, replace it with a larger, anonymized, institution-specific dataset and retrain/evaluate the models.

## Windows PowerShell setup

From the project root:

```powershell
cd ai-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
python train_model.py
python -m uvicorn app:app --host 0.0.0.0 --port 8000
```

Keep this terminal running.

## Test

Open another PowerShell:

```powershell
Invoke-RestMethod http://localhost:8000/health
```

Then:

```powershell
$body = @{
  title = "Hostel electricity failure"
  description = "There has been no electricity in my hostel room since yesterday and the warden is not responding."
  grievance_id = 999
  existing_grievances = @()
} | ConvertTo-Json -Depth 5

Invoke-RestMethod http://localhost:8000/analyze -Method Post -ContentType "application/json" -Body $body
```

## Spring Boot connection

The Java service uses:

`ai.server.url=http://localhost:8000`

The existing Java `/api/ai/analyze/{grievanceId}` endpoint can trigger the analysis.

## Combined priority decision

The Java backend combines two signals after Python analysis:
- 65%: ML-predicted urgency (LOW/MEDIUM/HIGH/CRITICAL), adjusted by prediction confidence
- 35%: calculated grievance risk score (anomaly, fake/duplicate signals)

The combined score determines the final grievance priority shown in the application. The raw Python `predicted_priority` remains available in `AIAnalysis` for transparency.
