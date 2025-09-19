@echo off
echo [INFO] Starting web prototype...

REM Go into the WEB_PROTOTYPE folder
echo [INFO] Changing directory to WEB_PROTOTYPE...
cd WEB_PROTOTYPE

REM Start the Flask server in the background
echo [INFO] Launching Flask server (process_pdf_flask.py)...
start /B python process_pdf_flask.py

REM Wait 2 seconds for server to start
echo [INFO] Waiting 2 seconds for server to start...
timeout /t 2 >nul

REM Open the HTML file in the default browser
echo [INFO] Opening your_pdf-index.html in browser...
start "" "your_pdf-index.html"

REM Go back to parent directory (optional)
echo [INFO] Returning to parent directory...
cd ..

echo [INFO] All done!
pause
