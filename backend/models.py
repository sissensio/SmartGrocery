import uuid
from datetime import datetime
from sqlalchemy import Column, Integer, String, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from .database import Base

def generate_uuid() -> str:
    """Helper to generate standard UUIDs as string keys for households."""
    return str(uuid.uuid4())

class Household(Base):
    __tablename__ = "households"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    name = Column(String(100), nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    
    # Bidirectional relationship linking users in this household
    users = relationship("User", back_populates="household", cascade="all, delete-orphan")

class User(Base):
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String(150), unique=True, index=True, nullable=False)
    hashed_password = Column(String(255), nullable=False)
    full_name = Column(String(100), nullable=False)
    household_id = Column(String(36), ForeignKey("households.id", ondelete="SET NULL"), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    
    # Relationship linking back to Household pool
    household = relationship("Household", back_populates="users")
