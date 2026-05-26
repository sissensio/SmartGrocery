import os
from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

# SQLite DB File Name
DB_FILE = "smartgrocery.db"
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# Put the database in the root folder so it is easily manageable and visible
DATABASE_URL = f"sqlite:///{os.path.normpath(os.path.join(BASE_DIR, '..', DB_FILE))}"

# Create the SQLAlchemy engine. 
# check_same_thread=False is necessary for SQLite when accessed by multiple threads in FastAPI
engine = create_engine(
    DATABASE_URL, connect_args={"check_same_thread": False}
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

def get_db():
    """Dependency injection helper that opens a database session for a request and closes it after."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
