# CannaScanner

A barcode scanning application for Zebra Android devices using DataWedge.

## Features

### Audit-Weight Mode
Perform inventory audits with barcode scanning and weight entry.

- Enter your name at the start of an audit session
- Scan barcodes using the device's hardware scanner
- Enter weight (in grams) after each scan (optional)
- View all scanned items in a list
- Export to CSV with the format: `AUDIT-WEIGHT-YYYY-MM-DD-HH-MM-Username.csv`

**CSV Output Columns:**
| Column | Description |
|--------|-------------|
| Barcode | The scanned barcode data |
| Weight (g) | Weight in grams (if entered) |
| Auditor | The user name entered at session start |

### Audit-Speed Mode
Rapid barcode scanning without weight entry prompts.

- Enter your name at the start of an audit session
- Scan barcodes rapidly - items are added immediately with no dialogs
- View all scanned items in a list
- Export to CSV with the format: `AUDIT-SPEED-YYYY-MM-DD-HH-MM-Username.csv`

**CSV Output Columns:**
| Column | Description |
|--------|-------------|
| Barcode | The scanned barcode data |
| Auditor | The user name entered at session start |

### RFID Tag Audit Mode
Audit RFID tags using the Zebra RFD40 or compatible RFID reader.

- Enter your name at the start of an audit session
- Connect to RFID reader (RFD40 via Bluetooth or USB)
- Scan RFID tags - unique EPCs are captured automatically
- Export to CSV with the format: `RFID-AUDIT-YYYY-MM-DD-HH-MM-Username.csv`

**CSV Output Columns:**
| Column | Description |
|--------|-------------|
| EPC | The RFID tag EPC (Electronic Product Code) |
| Auditor | The user name entered at session start |

**Note:** This feature requires the Zebra RFID SDK. See [RFIDAPI3Library/README.md](RFIDAPI3Library/README.md) for setup instructions.

## Requirements

- Zebra Android device with DataWedge
- Android 5.0 (API 21) or higher

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

1. Launch **CannaScanner**
2. Tap **Audit**
3. Enter your name and tap **Start Audit**
4. Scan barcodes - a dialog will appear after each scan to optionally enter weight
5. Tap **Export CSV** to share the audit data
6. Tap **Finish** when done

## License

This application is provided as-is without guarantee or warranty and may be modified to suit individual needs.

## Credits

Based on [Zebra DataWedge Android Samples](https://github.com/ZebraDevs/DataWedge-Android-Samples).
