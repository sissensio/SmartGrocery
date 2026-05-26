from pydantic import BaseModel, Field
from typing import List, Optional

class ScanRequest(BaseModel):
    ocrText: str = Field(..., description="The raw OCR text extracted from the receipt.")

class ParsedItem(BaseModel):
    name: str = Field(..., description="Clean name of the product.")
    brand: str = Field("", description="Brand of the product.")
    price: float = Field(0.0, description="Total price paid for this item.")
    unitPrice: float = Field(0.0, description="Unit price per item or per kg.")
    category: str = Field("Dispensa", description="Category: Latticini, Dispensa, Frutta e Verdura, Macelleria, Bevande, Igiene e Casa, Colazione, Surgelati, Spuntini.")
    isShared: bool = Field(True, description="Whether this item belongs to the shared Household Pool.")
    barcode: str = Field("", description="Barcode (EAN/UPC) if detected.")
    weight: Optional[float] = Field(None, description="Weight in kg if weighed item.")
    pricePerKg: Optional[float] = Field(None, description="Price per kg if weighed item.")
    confidence: float = Field(0.95, description="OCR confidence metric (0..1).")

class ParsingReceiptResult(BaseModel):
    storeName: str = Field(..., description="Normalized store name (e.g. Lidl, Esselunga).")
    items: List[ParsedItem] = Field(default_factory=list, description="List of parsed grocery items.")
    totalAmount: float = Field(..., description="Total amount printed on the receipt.")
    vatNumber: Optional[str] = Field(None, description="Store's VAT number (11 digits).")
    address: Optional[str] = Field(None, description="Store address.")
    phone: Optional[str] = Field(None, description="Store phone number.")
    receiptDate: Optional[str] = Field(None, description="Date of the receipt in format YYYY-MM-DD.")
    receiptTime: Optional[str] = Field(None, description="Time of the receipt in format HH:mm.")
