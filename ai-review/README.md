📌 AI Review – 감정 분석 API Server

네이버 스마트스토어 등에서 가져온 리뷰 텍스트를 입력하면
긍정 / 부정 비율을 계산해서 반환하는 AI 감정 분석 API 서버입니다.

백엔드 서버에서 리뷰 배열만 전달하면 즉시 분석 결과를 받을 수 있습니다.

🚀 기술 스택

FastAPI

TensorFlow (Keras)

python-mecab-ko (형태소 분석기)

NumPy

Uvicorn

📁 프로젝트 구조
ai-review/
 ├── app.py               # FastAPI 서버 / 감정 분석 API
 ├── best_model.h5        # 학습 완료된 감정 분석 모델
 ├── tokenizer.json       # 학습 시 사용한 토크나이저
 ├── stopwords.txt        # 불용어 목록
 ├── requirements.txt     # 필요한 패키지 목록
 └── README.md

⚙️ 설치 및 실행 방법
1) 가상환경 생성 및 활성화 (선택)
python3 -m venv .venv
source .venv/bin/activate

2) 필요한 패키지 설치
pip install -r requirements.txt

3) 서버 실행
uvicorn app:app --reload

✔ 서버 실행 후 접속 주소

API 엔드포인트 : http://127.0.0.1:8000/analyze

Swagger 문서 : http://127.0.0.1:8000/docs

🧠 감정 분석 API 사용 방법
📍 엔드포인트
POST /analyze

리뷰 문장을 배열 형태로 전달하면
각 리뷰를 모델이 긍정(1) / 부정(0)으로 판단하고
전체 긍정/부정 비율을 계산해 반환합니다.

📥 Request Body 예시
{
  "reviews": [
    "배송도 빠르고 너무 만족합니다!",
    "불량이 와서 최악이에요."
  ]
}

📤 Response 예시
{
  "total_reviews": 2,
  "positive_percent": 50.0,
  "negative_percent": 50.0,
  "positive_count": 1,
  "negative_count": 1
}

📌 API 동작 방식 간단 설명

입력된 리뷰 텍스트를 Mecab 형태소 분석으로 토큰화

불용어(stopwords.txt) 제거

tokenizer.json 기반 정수 인코딩

best_model.h5로 감정 점수 예측

점수가 0.5 이상이면 긍정, 미만이면 부정

전체 리뷰에서 긍정/부정 비율 계산하여 반환

🔐 백엔드 연동 방식

백엔드에서는 단순히 상품 리뷰 목록을 배열로 만들어서 아래처럼 요청하면 됩니다:

POST http://localhost:8000/analyze
Content-Type: application/json

{
  "reviews": 리뷰_텍스트_배열
}


응답으로 바로 긍정/부정 비율을 받으면 됩니다.
프론트 페이지에서 상품 리뷰 분석 결과 UI로 표시하면 됩니다.

⚠️ 주의사항

모델 파일(best_model.h5)이 커서 Git LFS 사용을 권장합니다.


① Docker 이미지 빌드
docker build -t ai-review .

② 컨테이너 실행
docker run -d -p 8000:8000 ai-review

③ API 호출 (백엔드에서)
POST http://localhost:8000/analyze
