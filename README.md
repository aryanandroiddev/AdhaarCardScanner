Aadhaar Card Scanner App

A simple Android application to scan Aadhaar cards using CameraX and ML Kit, extract the 12-digit Aadhaar number from QR code or text, and display it in a user-friendly format. Users can copy the scanned Aadhaar number to the clipboard.

Table of Contents

Features

Screenshots

Tech Stack

Setup & Installation

Usage

Code Structure

Dependencies

License

Features

Scan the Aadhaar QR code or printed number using the device camera.

Detect Aadhaar number even from encrypted or partially visible QR codes.

Copy scanned Aadhaar number to clipboard.

Scan line animation to indicate the scanning process.

Error handling for permissions, barcode, and text recognition.

Clean and readable MVVM architecture.

Screenshots

(Add app screenshots here if available)

Tech Stack

Language: Kotlin

Architecture: MVVM

Camera: CameraX

OCR & Barcode: Google ML Kit (Barcode Scanning + Text Recognition)

UI: XML layouts with data binding

Concurrency: Executors for camera analysis

Setup & Installation

Clone the repository:

git clone https://github.com/aryanandroiddev/AdhaarCardScanner.git
cd AdhaarScanner


Open in Android Studio:

Select Open an existing project and navigate to the cloned folder.

Sync Gradle:

Ensure all dependencies are downloaded.

Run on Device:

Connect a physical Android device or emulator.

Grant Camera permission when prompted.

Usage

Open the app on your device.

Allow Camera Permission.

Point the camera at the Aadhaar card (QR code or printed number).

Wait for the scan to complete.

The scanned Aadhaar number will appear on the screen.

Press the Copy button to copy it to the clipboard.

Press Retry to scan again if needed.

Note: If the Aadhaar QR is encrypted, the app may show a message:
"Aadhaar number (12 digits) not found in QR. The QR may be a secure/encrypted UIDAI QR."

Code Structure
dev.arya.adhaarcardscanner
│
├─ ui
│   ├─ MainActivity.kt         # Main camera & UI handling
│   └─ ScanStatus.kt           # Scan status sealed class
│
├─ viewmodel
│   └─ ScanViewModel.kt        # MVVM ViewModel for scanning state
│
├─ repository
│   └─ ScanRepository.kt       # ML Kit barcode and text scanner
│
├─ utils
│   └─ AadhaarUtils.kt         # Aadhaar regex & formatting utility
│
├─ res
│   ├─ layout                  # XML layouts
│   └─ drawable                # Assets & icons
└─ build.gradle                # Gradle dependencies

Dependencies
// CameraX
implementation "androidx.camera:camera-core:1.3.0"
implementation "androidx.camera:camera-camera2:1.3.0"
implementation "androidx.camera:camera-lifecycle:1.3.0"
implementation "androidx.camera:camera-view:1.3.0"

// ML Kit - Barcode & Text Recognition
implementation "com.google.mlkit:barcode-scanning:17.1.0"
implementation "com.google.mlkit:text-recognition:16.0.0"

// Coroutines & LiveData
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.2"
implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.7.2"
