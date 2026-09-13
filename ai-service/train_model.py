from pathlib import Path
import joblib
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline

BASE = Path(__file__).resolve().parent
DATA = BASE / "data" / "grievances.csv"
MODEL_DIR = BASE / "model"
MODEL_DIR.mkdir(exist_ok=True)

def text_column(df):
    return (df["title"].fillna("") + " " + df["description"].fillna("")).str.strip()

def make_pipeline():
    return Pipeline([
        ("tfidf", TfidfVectorizer(ngram_range=(1, 2), min_df=1, sublinear_tf=True)),
        ("clf", LogisticRegression(max_iter=2000, class_weight="balanced")),
    ])

def train():
    df = pd.read_csv(DATA)
    x = text_column(df)

    category_model = make_pipeline()
    category_model.fit(x, df["category"])

    priority_model = make_pipeline()
    priority_model.fit(x, df["priority"])

    fake_model = make_pipeline()
    fake_model.fit(x, df["is_fake"].astype(int))

    joblib.dump(category_model, MODEL_DIR / "category.joblib")
    joblib.dump(priority_model, MODEL_DIR / "priority.joblib")
    joblib.dump(fake_model, MODEL_DIR / "fake.joblib")
    df.to_pickle(MODEL_DIR / "training_data.pkl")

    print(f"Models trained from {len(df)} examples")

if __name__ == "__main__":
    train()
