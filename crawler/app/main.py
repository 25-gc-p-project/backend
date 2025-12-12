from fastapi import FastAPI
from pydantic import BaseModel
from app.crawler import crawl_product

app = FastAPI()

class CrawlRequest(BaseModel):
    url: str
    max_pages: int = 5

@app.post("/crawl")
def crawl(req: CrawlRequest):
    result = crawl_product(req.url, req.max_pages)
    return result
