from fastapi import FastAPI
from pydantic import BaseModel
import tensorflow as tf
import numpy as np
import json
import re

from tensorflow.keras.preprocessing.sequence import pad_sequences
from mecab import MeCab

# ==============================
# FastAPI 초기화
# ==============================
app = FastAPI()

# ==============================
# 모델 & 토크나이저 로드
# ==============================
model = tf.keras.models.load_model("best_model.h5")

with open("tokenizer.json", "r") as f:
    data = json.load(f)
    tokenizer = tf.keras.preprocessing.text.tokenizer_from_json(data)

# 형태소 분석기(MeCab)
mecab = MeCab()

# 불용어 로드
with open("stopwords.txt", "r", encoding="utf-8") as f:
    stopwords = [w.strip() for w in f.readlines()]

MAX_LEN = 80

# ==============================
# 요청 형식
# ==============================
class ReviewRequest(BaseModel):
    reviews: list[str]

# ==============================
# 전처리 함수
# ==============================
def preprocess(text):
    text = re.sub(r"[^ㄱ-ㅎㅏ-ㅣ가-힣 ]", "", text)
    tokens = mecab.morphs(text)
    tokens = [t for t in tokens if t not in stopwords]

    seq = tokenizer.texts_to_sequences([tokens])
    pad = pad_sequences(seq, maxlen=MAX_LEN)
    return pad

# ==============================
# 감정 분석 API
# ==============================
@app.post("/analyze")
def analyze(request: ReviewRequest):
    reviews = request.reviews

    pos = 0
    neg = 0

    for r in reviews:
        x = preprocess(r)
        score = float(model.predict(x)[0])

        if score > 0.5:
            pos += 1
        else:
            neg += 1

    total = pos + neg

    return {
        "total_reviews": total,
        "positive_percent": round(pos / total * 100, 2),
        "negative_percent": round(neg / total * 100, 2),
        "positive_count": pos,
        "negative_count": neg
    }

# ==============================
# 서버 상태 체크
# ==============================
@app.get("/health")
def health():
    return {"status": "ok"}
