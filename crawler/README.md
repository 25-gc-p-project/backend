# 🕷️ Naver SmartStore Crawler Service

`naver-crawler`는 **네이버 스마트스토어 상품 정보 + 리뷰 데이터를 자동 수집**하는  
Python 기반 크롤링 서비스입니다.

Java 모놀리식 환경에서는 Python 실행이 어려운 구조이기 때문에,  
이 프로젝트는 **Docker 기반의 독립 실행형 FastAPI 서버**로 설계되었습니다.  
백엔드는 단순히 HTTP API 호출만으로 크롤링 결과를 받아 사용할 수 있습니다.

---

## 🚀 Features

- 🔍 **상품 상세 정보 자동 수집**
  - payReferenceKey, productNo, 가격, 판매자 정보 등 파싱
- 📝 **네이버 모바일 API 기반 리뷰 전체 수집**
  - 리뷰 점수, 작성자, 내용, 옵션, 이미지 등 상세 데이터
- 🛡️ **undetected-chromedriver 기반 봇 탐지 우회**
- 🐳 **Docker 컨테이너로 배포 가능**
  - 백엔드에서 파이썬 환경 구축 불필요
- ⚡ **FastAPI 기반 REST API 제공**
  - Swagger UI 자동 제공 (`/docs`)

---

## 📁 Project Structure

crawler/
├── app/
│ ├── crawler.py # 크롤링 로직 (Selenium + internal API)
│ ├── main.py # FastAPI 엔드포인트
│ └── init.py
├── requirements.txt
├── Dockerfile
└── README.md

yaml
코드 복사

---

## 🐳 Docker 실행 방법

기본적으로 `crawler/` 폴더 루트에서 아래 명령어 실행:

### 1) Build Docker Image
```bash
docker build -t naver-crawler .
2) Run Container
bash
코드 복사
docker run -d -p 8001:8000 naver-crawler
3) Swagger 문서 확인
브라우저에서:

bash
코드 복사
http://localhost:8001/docs
자동 문서화된 API 확인 가능.

📡 API Usage
➤ POST /crawl
네이버 스마트스토어 상품 URL을 전달하면
상품 정보 + 리뷰 전체 리스트를 자동으로 크롤링하여 반환합니다.

Request Body
json
코드 복사
{
  "url": "https://smartstore.naver.com/.../products/123456",
  "max_pages": 5
}
Response Example
json
코드 복사
{
  "product": {
    "name": "상품명",
    "price": 12900,
    "payReferenceKey": "2812345",
    "productNo": 7454505963,
    "review_count": 120,
    "images": [...]
  },
  "review_count": 120,
  "reviews": [
    {
      "id": "123456",
      "reviewScore": 5,
      "reviewContent": "만족합니다!",
      "productOptionContent": "색상: 블랙",
      "reviewAttaches": [...]
    }
  ]
}
⚙️ Technologies Used
Python 3.10

FastAPI

Selenium

undetected-chromedriver

Requests

Docker

Uvicorn

📌 Notes
네이버 측 보안 정책 변경 시 크롤링 방식이 일부 달라질 수 있습니다.

undetected-chromedriver가 대부분의 봇 탐지를 우회하지만,
간혹 캡차가 표시되는 경우가 있음 → 수동으로 풀어야 할 수 있습니다.

크롤링 속도를 너무 빠르게 하면 IP가 일시적으로 제한될 수 있습니다.
