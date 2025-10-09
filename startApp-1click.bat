@echo off
echo Starting PDFTeller...

:: Run both servers in parallel, kill them when this window closes
cd backend
start "" /b python main.py
cd ..

cd frontend
start "" /b npm run dev -- --host
cd ..

:loop
timeout /t 5 >nul
goto loop
