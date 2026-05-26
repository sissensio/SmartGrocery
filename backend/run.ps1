# SmartGrocery Manager Backend Starter Script for Windows PowerShell
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir\..

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "    SmartGrocery Manager Backend On-Premise API Engine   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Check Python installation
if (-not (Get-Command "python" -ErrorAction SilentlyContinue)) {
    Write-Error "Python was not found on your system PATH! Please install Python 3.10+."
    Exit 1
}

# Setup Python Virtual Environment
if (-not (Test-Path "backend\.venv")) {
    Write-Host "Initializing new Python virtual environment (.venv)..." -ForegroundColor Yellow
    python -m venv backend\.venv
}

Write-Host "Activating virtual environment..." -ForegroundColor Yellow
& "backend\.venv\Scripts\Activate.ps1"

Write-Host "Installing/updating dependencies from requirements.txt..." -ForegroundColor Yellow
python -m pip install --upgrade pip
pip install -r backend\requirements.txt

Write-Host "----------------------------------------------------------" -ForegroundColor Gray
Write-Host "Starting FastAPI Uvicorn Server on http://localhost:8000" -ForegroundColor Green
Write-Host "Access API Documentation at http://localhost:8000/docs" -ForegroundColor Green
Write-Host "Press Ctrl+C to terminate the server." -ForegroundColor Gray
Write-Host "----------------------------------------------------------" -ForegroundColor Gray

# Run Uvicorn as a module from the root to ensure proper package relative imports
python -m uvicorn backend.main:app --host 0.0.0.0 --port 8000 --reload
