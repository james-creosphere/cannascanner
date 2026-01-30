# Zebra RFID SDK Setup

This folder is a placeholder for the Zebra RFID API3 SDK.

## Setup Instructions

1. **Download the SDK**
   - Go to: https://www.zebra.com/us/en/support-downloads/software/rfid-software/rfid-sdk-for-android.html
   - Download the latest "RFID SDK for Android"
   - You may need to create a Zebra developer account

2. **Extract and Copy the AAR**
   - Extract the downloaded ZIP file
   - Find the AAR file (usually named something like `RFIDAPI3Lib-X.X.X.X.aar`)
   - Copy it to this folder
   - Rename it to `API3_LIB-release.aar`

3. **Rebuild the Project**
   - In Android Studio, select Build > Clean Project
   - Then Build > Rebuild Project

## Supported Devices

The SDK supports:
- RFD40 (Bluetooth and USB)
- RFD8500
- RFD90
- FX Series (Fixed readers)
- And other Zebra RFID readers

## Documentation

- API Documentation: https://techdocs.zebra.com/dcs/rfid/android/
- Sample Apps: https://github.com/ZebraDevs/RFID-Android-Inventory-Sample
