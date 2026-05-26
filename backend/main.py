import os
import json
import uvicorn
from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from .schemas import ScanRequest, ParsingReceiptResult
from .services import process_ocr_scan

app = FastAPI(
    title="SmartGrocery Manager On-Premise API Backend",
    version="1.0.0",
    description="In-house high-privacy OCR receipt processing server powered by local LLM models."
)

# Configure CORS so Android emulator and physical devices can connect seamlessly
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def get_configured_ip() -> str:
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "network_config.json")
    if os.path.exists(path):
        try:
            with open(path, "r", encoding="utf-8") as f:
                return json.load(f).get("LOCAL_BACKEND_IP", "0.0.0.0")
        except:
            pass
    return "0.0.0.0"

@app.get("/")
def read_root():
    return {
        "status": "active",
        "service": "SmartGrocery Manager Backend API Engine",
        "version": "1.0.0",
        "docs_url": "/docs",
        "lan_ip": get_configured_ip()
    }

@app.post("/api/v1/scan", response_model=ParsingReceiptResult, status_code=status.HTTP_200_OK)
async def scan_receipt_endpoint(request: ScanRequest):
    """
    Receives raw OCR text from the mobile client, aligns pricing line elements geometrically,
    processes it through the local Llama 3 LLM (or fallbacks), and returns structured items.
    """
    if not request.ocrText.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="The OCR text payload cannot be empty."
        )
    
    try:
        result = await process_ocr_scan(request.ocrText)
        return result
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"An unhandled error occurred during AI receipt parsing: {str(e)}"
        )

if __name__ == "__main__":
    uvicorn.run("backend.main:app", host="0.0.0.0", port=8000, reload=True)
