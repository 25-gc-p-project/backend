FROM python:3.10-slim

WORKDIR /app

# 2. 필수 시스템 패키지 설치 (h5py, numpy 등을 위해 필요)
RUN apt-get update && apt-get install -y \
    gcc \
    libhdf5-dev \
    && rm -rf /var/lib/apt/lists/*

# 3. 라이브러리 설치
COPY requirements.txt .
RUN pip install --upgrade pip
RUN pip install --no-cache-dir -r requirements.txt

# 4. 소스 코드 복사
COPY . .

# 5. (참고) 실행 명령어는 기존에 쓰시던 것 그대로 두시면 됩니다.
# 예: CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

# FastAPI 포트
EXPOSE 8000

# 서버 실행
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]