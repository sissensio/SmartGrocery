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

# --- Authentication & User Management Schemas ---
from datetime import datetime

class HouseholdResponse(BaseModel):
    id: str = Field(..., description="Unique code/UUID of the household.")
    name: str = Field(..., description="Name of the household.")
    created_at: datetime = Field(..., description="Creation date of the household group.")

    class Config:
        from_attributes = True

class UserCreate(BaseModel):
    email: str = Field(..., description="Unique email address for registration.")
    password: str = Field(..., description="Password in plain text (will be encrypted on database).")
    full_name: str = Field(..., description="Full name of the user.")
    household_name: Optional[str] = Field(None, description="Optional name to create a new household group.")

class UserResponse(BaseModel):
    id: int = Field(..., description="Database ID of the user.")
    email: str = Field(..., description="Email address.")
    full_name: str = Field(..., description="Full name of the user.")
    household_id: Optional[str] = Field(None, description="Assigned household pool ID.")
    created_at: datetime = Field(..., description="Registration timestamp.")

    class Config:
        from_attributes = True

class UserLogin(BaseModel):
    email: str = Field(..., description="Email address.")
    password: str = Field(..., description="Plain-text password.")

class Token(BaseModel):
    access_token: str = Field(..., description="Signed JWT Bearer access token.")
    token_type: str = Field("bearer", description="Token type, defaults to bearer.")

class TokenData(BaseModel):
    email: Optional[str] = Field(None, description="Email subject extracted from the token.")
