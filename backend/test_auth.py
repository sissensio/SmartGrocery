import os
import sys
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

# Add the project root directory to sys.path to enable clean relative module imports
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from backend.main import app
from backend.database import Base, get_db
from backend.models import User, Household

# Configure a clean, isolated SQLite database file for testing
TEST_DB_FILE = "test_smartgrocery.db"
TEST_DATABASE_URL = f"sqlite:///./{TEST_DB_FILE}"

engine = create_engine(TEST_DATABASE_URL, connect_args={"check_same_thread": False})
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def override_get_db():
    """Dependency override yielding a session to the clean test database."""
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()

# Override the database session dependency inside the FastAPI app
app.dependency_overrides[get_db] = override_get_db

# Reset and initialize the schema in the test database
Base.metadata.drop_all(bind=engine)
Base.metadata.create_all(bind=engine)

client = TestClient(app)

def run_tests():
    print("==========================================================")
    print("        SmartGrocery Auth System Integration Test Suite    ")
    print("==========================================================")
    
    test_email = "rossi.famiglia@example.com"
    test_password = "SecretPassword123"
    test_name = "Mario Rossi"
    test_household = "Famiglia Rossi"
    
    # 1. Test User Enrollment
    print("[*] Testing user registration...")
    reg_payload = {
        "email": test_email,
        "password": test_password,
        "full_name": test_name,
        "household_name": test_household
    }
    
    res = client.post("/api/v1/auth/register", json=reg_payload)
    assert res.status_code == 201, f"Failed registration: {res.text}"
    user_data = res.json()
    assert user_data["email"] == test_email
    assert user_data["full_name"] == test_name
    assert "id" in user_data
    assert user_data["household_id"] is not None
    print(f"[+] Registration successful: User ID {user_data['id']}, Household ID {user_data['household_id']}")
    
    # 2. Test Email Deduplication Protection
    print("[*] Testing duplicate email protection...")
    res = client.post("/api/v1/auth/register", json=reg_payload)
    assert res.status_code == 400, f"Duplicate registration should have failed"
    print("[+] Duplicate registration blocked correctly.")
    
    # 3. Test Authentication and Access Token Retrieval
    print("[*] Testing user login...")
    login_payload = {
        "username": test_email,  # Standard OAuth2 form uses 'username' parameter
        "password": test_password
    }
    res = client.post("/api/v1/auth/token", data=login_payload)
    assert res.status_code == 200, f"Login failed: {res.text}"
    token_data = res.json()
    assert "access_token" in token_data
    assert token_data["token_type"] == "bearer"
    access_token = token_data["access_token"]
    print("[+] Login successful! JWT Access Token retrieved.")
    
    # 4. Test Protected API Routing with JWT
    print("[*] Testing protected route access (/me)...")
    headers = {"Authorization": f"Bearer {access_token}"}
    res = client.get("/api/v1/auth/me", headers=headers)
    assert res.status_code == 200, f"Protected route access failed: {res.text}"
    profile_data = res.json()
    assert profile_data["email"] == test_email
    assert profile_data["full_name"] == test_name
    print(f"[+] Protected route access successful for authenticated user: {profile_data['full_name']}")
    
    # 5. Test Access Denial for Bad Tokens
    print("[*] Testing access block with invalid token...")
    headers_invalid = {"Authorization": "Bearer invalid_token_xyz"}
    res = client.get("/api/v1/auth/me", headers=headers_invalid)
    assert res.status_code == 401, f"Access should have been blocked"
    print("[+] Access blocked correctly for invalid token.")
    
    print("\n==========================================================")
    print("   [SUCCESS] TUTTI I TEST DI AUTENTICAZIONE SONO PASSATI! ")
    print("==========================================================")

if __name__ == "__main__":
    try:
        run_tests()
    finally:
        # Guarantee removal of the temporary test database file
        if os.path.exists(TEST_DB_FILE):
            try:
                os.remove(TEST_DB_FILE)
            except Exception:
                pass
