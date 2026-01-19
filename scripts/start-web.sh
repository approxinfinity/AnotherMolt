#!/bin/bash
# Start web client and wait for it to be ready

WEB_URL="${WEB_CLIENT_URL:-http://192.168.1.239:8080}"

# Kill any existing web client
pkill -f "wasmJsBrowserDevelopmentRun" 2>/dev/null
sleep 1

# Start the web client in background
./gradlew composeApp:wasmJsBrowserDevelopmentRun --quiet 2>&1 &
GRADLE_PID=$!

echo "Starting web client..."

# Wait for server to be ready (max 60 seconds)
for i in {1..60}; do
    if curl -s --max-time 2 "$WEB_URL" > /dev/null 2>&1; then
        echo "Web client is ready at $WEB_URL"
        exit 0
    fi
    sleep 1
done

echo "ERROR: Web client failed to start within 60 seconds"
exit 1
