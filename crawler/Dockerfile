FROM python:3.10-slim

WORKDIR /app

# Chromium과 드라이버 설치 (ARM64/AMD64 모두 지원, apt-key 불필요)
# --no-install-recommends 옵션을 넣어 용량과 메모리 사용량을 줄임
RUN apt-get update && apt-get install -y --no-install-recommends \
    chromium \
    chromium-driver \
    && rm -rf /var/lib/apt/lists/*

# 환경 변수 설정 (파이썬이 크롬 위치를 찾을 수 있게)
ENV CHROME_BIN=/usr/bin/chromium
ENV CHROMEDRIVER_PATH=/usr/bin/chromedriver

COPY requirements.txt .
RUN pip install --upgrade pip
RUN pip install --no-cache-dir -r requirements.txt

COPY ./app ./app

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
