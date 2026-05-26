import os
from datetime import datetime, timedelta
from typing import Optional
import jwt
import bcrypt
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session
from .database import get_db
from .models import User

# Configuration for JWT Tokens
# For on-premise local dev, we fall back to a secure hardcoded string, but prioritize loading from environment variables
SECRET_KEY = os.getenv("JWT_SECRET_KEY", "super_secret_local_key_smartgrocery_dev_only_change_in_prod")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_DAYS = 30

# OAuth2 schema setting the URL where the token is requested
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/token")

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verifies that a plain text password matches a hashed bcrypt password."""
    try:
        return bcrypt.checkpw(plain_password.encode('utf-8'), hashed_password.encode('utf-8'))
    except Exception:
        return False

def get_password_hash(password: str) -> str:
    """Generates a secure salt and returns a hashed bcrypt password as a string."""
    salt = bcrypt.gensalt()
    return bcrypt.hashpw(password.encode('utf-8'), salt).decode('utf-8')

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    """Generates a signed JWT access token containing the user details with an expiration."""
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(days=ACCESS_TOKEN_EXPIRE_DAYS)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

def get_current_user(token: str = Depends(oauth2_scheme), db: Session = Depends(get_db)) -> User:
    """FastAPI Dependency: decodes and validates a JWT token and returns the authenticated User."""
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Credenziali non valide o scadute.",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        email: str = payload.get("sub")
        if email is None:
            raise credentials_exception
    except jwt.PyJWTError:
        raise credentials_exception
        
    user = db.query(User).filter(User.email == email).first()
    if user is None:
        raise credentials_exception
    return user
