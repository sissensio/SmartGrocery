from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from ..database import get_db
from ..models import User, Household
from ..schemas import UserCreate, UserResponse, Token
from ..auth import get_password_hash, verify_password, create_access_token, get_current_user

router = APIRouter(
    prefix="/api/v1/auth",
    tags=["Authentication & Users"]
)

@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def register_user(user_in: UserCreate, db: Session = Depends(get_db)):
    """
    Registers a new user, hashes their password with bcrypt, 
    and automatically associates them with a Household group (Household Pool).
    """
    # 1. Check if email is already taken
    existing_user = db.query(User).filter(User.email == user_in.email).first()
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Questa email è già registrata."
        )
    
    # 2. Setup the Household group
    if user_in.household_name:
        household = Household(name=user_in.household_name)
    else:
        household = Household(name=f"Lista Personale di {user_in.full_name}")
    
    db.add(household)
    db.flush()  # Generates the UUID code for the household
    
    # 3. Encrypt password and store the new user record
    hashed_pwd = get_password_hash(user_in.password)
    new_user = User(
        email=user_in.email,
        hashed_password=hashed_pwd,
        full_name=user_in.full_name,
        household_id=household.id
    )
    
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    
    return new_user

@router.post("/token", response_model=Token)
def login_for_access_token(
    form_data: OAuth2PasswordRequestForm = Depends(), 
    db: Session = Depends(get_db)
):
    """
    Authenticates a user's email and password, returning a signed JWT access token.
    Compatible with FastAPI automatic Swagger docs (/docs) Authorize function.
    """
    # Form data uses 'username' field for email login in standard OAuth2 OAuthPasswordRequestForm
    user = db.query(User).filter(User.email == form_data.username).first()
    if not user or not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Email o password errati.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    # Generate token containing subject sub
    access_token = create_access_token(data={"sub": user.email})
    return {"access_token": access_token, "token_type": "bearer"}

@router.get("/me", response_model=UserResponse)
def read_users_me(current_user: User = Depends(get_current_user)):
    """
    Protected Endpoint: Decodes the Bearer token and returns the current user profile.
    """
    return current_user
