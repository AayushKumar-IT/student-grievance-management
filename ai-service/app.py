from pathlib import Path
from typing import List, Optional
import math
import re
import joblib
import pandas as pd
from fastapi import FastAPI
from pydantic import BaseModel, Field
from sklearn.metrics.pairwise import cosine_similarity

BASE = Path(__file__).resolve().parent
MODEL_DIR = BASE / "model"
DATA_DIR = BASE / "data"

app = FastAPI(title="Student Grievance ML Service", version="1.0.0")

CATEGORY_MODEL = MODEL_DIR / "category.joblib"
PRIORITY_MODEL = MODEL_DIR / "priority.joblib"
FAKE_MODEL = MODEL_DIR / "fake.joblib"
TRAINING_DATA = MODEL_DIR / "training_data.pkl"

class ExistingGrievance(BaseModel):
    id: Optional[int] = None
    title: str = ""
    description: str = ""

class AnalyzeRequest(BaseModel):
    title: str = ""
    description: str = ""
    grievance_id: Optional[int] = None
    existing_grievances: List[ExistingGrievance] = Field(default_factory=list)

class AnalyzeResponse(BaseModel):
    predicted_category: str
    category_confidence: float
    predicted_priority: str
    priority_confidence: float
    is_fake: bool
    fake_confidence: float
    is_duplicate: bool
    duplicate_of_id: Optional[int] = None
    similarity_score: float
    is_anomaly: bool
    anomaly_score: float
    notes: str

category_model = None
priority_model = None
fake_model = None
training_df = None


def load_models():
    global category_model, priority_model, fake_model, training_df
    if not (CATEGORY_MODEL.exists() and PRIORITY_MODEL.exists() and FAKE_MODEL.exists() and TRAINING_DATA.exists()):
        from train_model import train
        train()
    category_model = joblib.load(CATEGORY_MODEL)
    priority_model = joblib.load(PRIORITY_MODEL)
    fake_model = joblib.load(FAKE_MODEL)
    training_df = pd.read_pickle(TRAINING_DATA)


def clean_text(text: str) -> str:
    text = (text or "").lower()
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def predict_with_confidence(model, text: str):
    probs = model.predict_proba([text])[0]
    classes = model.classes_
    index = int(probs.argmax())
    return str(classes[index]), float(probs[index])


def fake_prediction(text: str):
    label, confidence = predict_with_confidence(fake_model, text)
    is_fake = label == "1"
    return is_fake, confidence if is_fake else 1.0 - confidence


def duplicate_check(text: str, existing: List[ExistingGrievance]):
    if not existing:
        return False, None, 0.0

    corpus = [clean_text(text)] + [clean_text(f"{g.title} {g.description}") for g in existing]
    # Use the category model's TF-IDF vocabulary for consistent vectorization.
    vectorizer = category_model.named_steps["tfidf"]
    vectors = vectorizer.transform(corpus)
    sims = cosine_similarity(vectors[0:1], vectors[1:]).ravel()
    if len(sims) == 0:
        return False, None, 0.0
    idx = int(sims.argmax())
    score = float(sims[idx])
    # 0.82 is intentionally conservative to avoid marking merely related complaints as duplicates.
    if score >= 0.82 and existing[idx].id is not None:
        return True, existing[idx].id, score
    return False, None, score


def anomaly_score(text: str, category_conf: float, priority: str, is_fake: bool) -> float:
    """Heuristic anomaly score built on top of ML predictions.

    This is deliberately transparent: safety-related keywords and unusually weak
    predictions raise the score. It is not a medical/legal risk classifier.
    """
    lower = clean_text(text)
    danger_words = [
        "ragging", "assault", "threat", "violence", "abuse", "harassment",
        "unsafe", "danger", "emergency", "physical attack", "blackmail"
    ]
    hits = sum(1 for word in danger_words if word in lower)
    score = 0.08
    score += min(0.55, hits * 0.20)
    score += max(0.0, 0.25 - category_conf * 0.25)
    if priority == "CRITICAL":
        score += 0.18
    elif priority == "HIGH":
        score += 0.08
    if is_fake:
        score -= 0.10
    return max(0.0, min(1.0, score))


@app.on_event("startup")
def startup():
    load_models()

@app.get("/health")
def health():
    return {"status": "ok", "service": "grievance-ml", "models_loaded": category_model is not None}

@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(request: AnalyzeRequest):
    text = clean_text(f"{request.title} {request.description}")

    category, category_conf = predict_with_confidence(category_model, text)
    priority, priority_conf = predict_with_confidence(priority_model, text)
    is_fake, fake_conf = fake_prediction(text)
    is_duplicate, duplicate_id, similarity = duplicate_check(text, request.existing_grievances)
    anomaly = anomaly_score(text, category_conf, priority, is_fake)

    # Duplicate evidence contributes to anomaly score because repeated submissions
    # deserve review, but it does not automatically mean the complaint is fake.
    if is_duplicate:
        anomaly = min(1.0, anomaly + 0.10)

    notes = (
        "TF-IDF + Logistic Regression models used for category, priority and fake/spam prediction; "
        "cosine similarity used for duplicate detection; transparent rule layer used for anomaly score."
    )

    return AnalyzeResponse(
        predicted_category=category,
        category_confidence=round(category_conf, 4),
        predicted_priority=priority,
        priority_confidence=round(priority_conf, 4),
        is_fake=is_fake,
        fake_confidence=round(fake_conf, 4),
        is_duplicate=is_duplicate,
        duplicate_of_id=duplicate_id,
        similarity_score=round(similarity, 4),
        is_anomaly=anomaly >= 0.50,
        anomaly_score=round(anomaly, 4),
        notes=notes,
    )
