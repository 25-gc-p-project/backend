from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
from openai import OpenAI
import os
import json

# OpenAI 클라이언트
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

app = FastAPI(
    title="HyoDream Recommendation AI",
    version="1.0.1"
)

# =========================
# 요청 / 응답 모델
# =========================

class RecommendRequest(BaseModel):
    diseases: List[str]
    allergies: List[str]
    goals: List[str]

class RecommendResponse(BaseModel):
    product_ids: List[int]

# =========================
# GPT 추천 로직
# =========================

def recommend_with_gpt(req: RecommendRequest) -> List[int]:
    prompt = f"""
너는 헬스케어 쇼핑몰의 상품 추천 AI다.

[사용자 건강 정보]
- 질병: {', '.join(req.diseases) if req.diseases else '없음'}
- 알레르기: {', '.join(req.allergies) if req.allergies else '없음'}
- 목표: {', '.join(req.goals) if req.goals else '없음'}

[규칙]
1. 알레르기 성분이 포함될 가능성이 있는 상품은 제외
2. 건강 목표에 가장 적합한 상품을 우선 추천
3. **반드시 정확히 5개의 product_id를 반환**
4. product_id는 정수형
5. 중복 없이 선택
6. 아래 JSON 형식으로만 응답

[응답 형식]
{{"product_ids": [101, 102, 103, 104, 105]}}
"""

    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": "JSON만 출력하는 추천 엔진이다."},
            {"role": "user", "content": prompt}
        ],
        temperature=0.2
    )

    content = response.choices[0].message.content
    result = json.loads(content)

    product_ids = result.get("product_ids", [])

    # 🔒 안전장치: 무조건 5개 보장
    if len(product_ids) != 5:
        raise ValueError(f"product_id는 반드시 5개여야 합니다: {product_ids}")

    return product_ids

# =========================
# API 엔드포인트
# =========================

@app.post("/recommend", response_model=RecommendResponse)
def recommend(req: RecommendRequest):
    product_ids = recommend_with_gpt(req)
    return {"product_ids": product_ids}

# =========================
# 헬스체크
# =========================

@app.get("/health")
def health():
    return {"status": "ok"}