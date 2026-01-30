# CannaScanner

A barcode and RFID scanning application for Zebra Android devices using DataWedge and the Zebra RFID SDK.

## Features

### Audit-Weight Mode
Perform inventory audits with barcode scanning and optional weight entry.

- Enter your full name and room at the start of an audit session
- Scan barcodes using the device's hardware scanner
- Enter weight (in grams) after each scan (optional)
- Each scan is timestamped automatically
- View all scanned items in a list
- Export to CSV saved in the **Downloads** folder: `AUDIT-WEIGHT-YYYY-MM-DD-HH-MM-Username.csv`

**CSV Output Columns:**
| Column | Description |
|--------|-------------|
| Timestamp | Date and time of scan (YYYY-MM-DD HH:MM:SS) |
| Barcode | The scanned barcode data |
| Weight (g) | Weight in grams (if entered) |
| Room | The room entered at session start |
| Auditor | The full name entered at session start |

### Audit-Speed Mode
Rapid barcode scanning without weight entry prompts.

- Enter your full name and room at the start of an audit session
- Scan barcodes rapidly - items are added immediately with no dialogs
- Each scan is timestamped automatically
- View all scanned items in a list
- Export to CSV saved in the **Downloads** folder: `AUDIT-SPEED-YYYY-MM-DD-HH-MM-Username.csv`

**CSV Output Columns:**
| Column | Description |
|--------|-------------|
| Timestamp | Date and time of scan (YYYY-MM-DD HH:MM:SS) |
| Barcode | The scanned barcode data |
| Room | The room entered at session start |
| Auditor | The full name entered at session start |

### RFID Tag Audit Mode
Audit RFID tags using the Zebra RFD40 or compatible RFID reader.

- Enter your full name and room at the start of an audit session
- Connect to RFID reader (RFD40 via Bluetooth or USB)
- Scan RFID tags - unique EPCs are captured automatically
- Each scan is timestamped automatically
- Export to CSV saved in the **Downloads** folder: `RFID-AUDIT-YYYY-MM-DD-HH-MM-Username.csv`

**CSV Output Columns:**
| Column | Description |
|--------|-------------|
| Timestamp | Date and time of scan (YYYY-MM-DD HH:MM:SS) |
| EPC | The RFID tag EPC (Electronic Product Code) |
| Room | The room entered at session start |
| Auditor | The full name entered at session start |

**Note:** This feature requires the Zebra RFID SDK. See [RFIDAPI3Library/README.md](RFIDAPI3Library/README.md) for setup instructions.

## CSV File Location

All CSV exports are saved to the device's **Downloads** folder for easy access:
- Open the **Files** app on the device
- Navigate to **Downloads**
- CSV files will be named with the audit type, date, time, and username

## Requirements

- Zebra Android device with DataWedge
- Android 8.0 (API 26) or higher
- For RFID: Zebra RFD40 or compatible RFID reader

## Installation

1. Build the APK using Android Studio or Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

2. Install via ADB:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## DataWedge Configuration

The app automatically configures a DataWedge profile named "CannaScanner" when you open the Audit screen. This profile:

- Enables barcode scanning
- Sends scan data via broadcast intent
- Disables keystroke output

If you experience issues, you can manually verify the profile in the DataWedge app on your device.

## Usage

### Barcode Audits (Weight or Speed)
1. Launch **CannaScanner**
2. Tap **Audit-Weight** or **Audit-Speed**
3. Enter your full name and room, then tap **Start Audit**
4. Scan barcodes:
   - Weight mode: A dialog appears after each scan to optionally enter weight
   - Speed mode: Items are added immediately
5. Tap **Export CSV** to save and share the audit data
6. Tap **Finish** when done

### RFID Audits
1. Launch **CannaScanner**
2. Tap **RFID Tag Audit**
3. Enter your full name and room, then tap **Start RFID Audit**
4. Tap **Connect** to connect to the RFID reader
5. Tap **Start Scan** to begin reading tags
6. Tap **Stop Scan** when done scanning
7. Tap **Export CSV** to save and share the audit data

## Permissions

The app requires the following permissions:
- **Storage**: To save CSV files to Downloads
- **Bluetooth**: For RFID reader connectivity
- **Location**: Required by Android for Bluetooth scanning

## License

This application is provided as-is without guarantee or warranty and may be modified to suit individual needs.

## Credits

Based on [Zebra DataWedge Android Samples](https://github.com/ZebraDevs/DataWedge-Android-Samples).
