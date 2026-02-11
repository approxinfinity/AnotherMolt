#!/bin/bash
# iOS Build and TestFlight Upload Script for AnotherThread
#
# Prerequisites:
# 1. Xcode installed with command line tools
# 2. Valid Apple Developer account with App Store Connect access
# 3. App created in App Store Connect with bundle ID: com.ez2bg.anotherthread
# 4. API Key created at https://appstoreconnect.apple.com/access/api
#
# Usage:
#   ./scripts/build-ios.sh              # Build only
#   ./scripts/build-ios.sh --upload     # Build and upload to TestFlight

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IOS_APP_DIR="$PROJECT_ROOT/iosApp"
ARCHIVE_PATH="$PROJECT_ROOT/build/ios/AnotherThread.xcarchive"
IPA_PATH="$PROJECT_ROOT/build/ios/AnotherThread.ipa"
EXPORT_OPTIONS="$PROJECT_ROOT/scripts/ExportOptions.plist"

echo "=== AnotherThread iOS Build Script ==="
echo "Project root: $PROJECT_ROOT"
echo ""

# Check for Xcode
if ! command -v xcodebuild &> /dev/null; then
    echo "Error: xcodebuild not found. Install Xcode and its command line tools."
    exit 1
fi

# Create build directory
mkdir -p "$PROJECT_ROOT/build/ios"

# Step 1: Build Kotlin framework for iOS device
echo "=== Step 1: Building Kotlin framework ==="
cd "$PROJECT_ROOT"
# The framework is built automatically by Xcode via embedAndSignAppleFrameworkForXcode
# But we can pre-compile to catch errors early
./gradlew :composeApp:compileKotlinIosArm64

# Step 2: Archive the iOS app
echo ""
echo "=== Step 2: Archiving iOS app ==="
cd "$IOS_APP_DIR"

xcodebuild archive \
    -project iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Release \
    -archivePath "$ARCHIVE_PATH" \
    -destination "generic/platform=iOS" \
    -allowProvisioningUpdates \
    CODE_SIGN_STYLE=Automatic \
    DEVELOPMENT_TEAM=TLTLQZXFAJ

echo "Archive created at: $ARCHIVE_PATH"

# Step 3: Export IPA
echo ""
echo "=== Step 3: Exporting IPA ==="

# Create ExportOptions.plist if it doesn't exist
if [ ! -f "$EXPORT_OPTIONS" ]; then
    echo "Creating ExportOptions.plist..."
    cat > "$EXPORT_OPTIONS" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>app-store-connect</string>
    <key>teamID</key>
    <string>TLTLQZXFAJ</string>
    <key>uploadSymbols</key>
    <true/>
    <key>destination</key>
    <string>upload</string>
</dict>
</plist>
EOF
fi

xcodebuild -exportArchive \
    -archivePath "$ARCHIVE_PATH" \
    -exportOptionsPlist "$EXPORT_OPTIONS" \
    -exportPath "$PROJECT_ROOT/build/ios" \
    -allowProvisioningUpdates

echo ""
echo "=== Build Complete ==="
echo "IPA location: $PROJECT_ROOT/build/ios/"

# Step 4: Upload to TestFlight (if requested)
if [ "$1" == "--upload" ]; then
    echo ""
    echo "=== Step 4: Uploading to TestFlight ==="
    echo ""
    echo "To upload, you need an App Store Connect API Key."
    echo "Create one at: https://appstoreconnect.apple.com/access/api"
    echo ""
    echo "Then run:"
    echo "  xcrun altool --upload-app -f \"$IPA_PATH\" -t ios \\"
    echo "    --apiKey YOUR_API_KEY_ID \\"
    echo "    --apiIssuer YOUR_ISSUER_ID"
    echo ""
    echo "Or use Transporter app from the Mac App Store."
    echo ""

    # If API key environment variables are set, attempt upload
    if [ -n "$APP_STORE_API_KEY" ] && [ -n "$APP_STORE_ISSUER_ID" ]; then
        echo "Found API credentials in environment, uploading..."
        xcrun altool --upload-app \
            -f "$PROJECT_ROOT/build/ios/AnotherThread.ipa" \
            -t ios \
            --apiKey "$APP_STORE_API_KEY" \
            --apiIssuer "$APP_STORE_ISSUER_ID"
        echo "Upload complete! Check App Store Connect for processing status."
    fi
fi

echo ""
echo "Done!"
