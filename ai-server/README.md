🚀 실행 방법
1) Docker Desktop 실행
2) 프로젝트 루트에서 컨테이너 실행
docker compose up -d --build


성공 시 아래 컨테이너가 올라옵니다:

📡 API 문서

FastAPI 자동 문서 접속:

👉 http://localhost:8000/docs

📌 API 사용법
1) 추천 카테고리

POST /recommend-categories

2) 추천 상품 ID

POST /recommend-products

요청 예시:

{
  "diseases": ["당뇨"],
  "allergies": ["견과류"],
  "goals": ["면역력"]
}


응답 예시:

{
  "product_ids": [102, 55]
}


🐳 참고

로컬 MySQL/Redis가 켜져 있으면 포트 충돌날 수 있음
→ 기존 프로세스 종료 후 docker compose up 다시 실행

Spring Backend에서 AI 서버 호출 시 주소:

http://hyodream-ai:8000/recommend-products

🔑 OpenAI API Key : <YOUR_API_KEY>