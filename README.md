# CannaScanner

A barcode and RFID scanning application for Zebra Android devices using DataWedge and the Zebra RFID SDK, with automatic Airtable integration.

## Features

### Audit-Weight Mode
Perform inventory audits with barcode scanning and optional weight entry.

- Enter your full name and select a room at the start of an audit session
- Scan barcodes using the device's hardware scanner
- Enter weight (in grams) after each scan (optional)
- Each scan is timestamped automatically
- View all scanned items in a list
- **Submit** to save CSV and upload to Airtable automatically
- **Reset** to clear all scans and start over

**CSV Output Columns:**
| Column | Description |
|--------|-------------|
| Timestamp | Date and time of scan (YYYY-MM-DD HH:MM:SS) |
| Barcode | The scanned barcode data |
| Weight (g) | Weight in grams (if entered) |
| Room | The room selected at session start |
| Auditor | The full name entered at session start |

### Audit-Speed Mode
Rapid barcode scanning without weight entry prompts.

- Enter your full name and select a room at the start of an audit session
- Scan barcodes rapidly - items are added immediately with no dialogs
- Each scan is timestamped automatically
- View all scanned items in a list
- **Submit** to save CSV and upload to Airtable automatically
- **Reset** to clear all scans and start over

**CSV Output Columns:**
| Column | Description |
|--------|-------------|
| Timestamp | Date and time of scan (YYYY-MM-DD HH:MM:SS) |
| Barcode | The scanned barcode data |
| Room | The room selected at session start |
| Auditor | The full name entered at session start |

### RFID Tag Audit Mode
Audit RFID tags using the Zebra RFD40 or compatible RFID reader.

- Enter your full name and select a room at the start of an audit session
- Connect to RFID reader (RFD40 via Bluetooth or USB)
- Scan RFID tags - unique EPCs are captured automatically
- Each scan is timestamped automatically
- **Submit** to save CSV and upload to Airtable automatically
- **Reset** to clear all scans and start over

**CSV Output Columns:**
| Column | Description |
|--------|-------------|
| Timestamp | Date and time of scan (YYYY-MM-DD HH:MM:SS) |
| EPC | The RFID tag EPC (Electronic Product Code) |
| Room | The room selected at session start |
| Auditor | The full name entered at session start |

**Note:** This feature requires the Zebra RFID SDK. See [RFIDAPI3Library/README.md](RFIDAPI3Library/README.md) for setup instructions.

### Room Selection
Room is selected from a dropdown menu with predefined options:
- Vault
- Nursery
- Field
- Office
- Production Room
- Barn
- Prop Room

### Airtable Integration
Automatically upload scan data to Airtable for centralized tracking and reporting.

- Configure once in **Settings** - credentials are saved on device
- All audit types automatically upload to Airtable on Submit
- If upload fails, a dialog shows the CSV filename for manual upload
- CSV is always saved to Downloads as a backup

**Required Airtable Table Columns:**
| Column | Field Type |
|--------|------------|
| Timestamp | Single line text |
| Barcode | Single line text |
| Room | Single line text |
| Auditor | Single line text |
| AuditType | Single line text |
| Weight | Number (optional, for Weight audits) |

**Setup:**
1. Create an Airtable base with a table containing the columns above
2. Generate a Personal Access Token at https://airtable.com/create/tokens
3. In CannaScanner, tap **Settings** and enter:
   - API Key (Personal Access Token)
   - Base ID (from your Airtable URL: `airtable.com/appXXXXXX/...`)
   - Table Name
4. Toggle **Enable Airtable Upload** on
5. Tap **Test Connection** to verify, then **Save Settings**

### Audit Log & Retry Uploads
View history of all audits and retry failed uploads when internet is restored.

- Access via **Audit Log** button on the main screen
- Shows all completed audits with upload status:
  - **Uploaded** (green) - Successfully uploaded to Airtable
  - **Failed** (red) - Upload failed, with **Retry** button
  - **Pending** (orange) - Upload in progress
- Failed uploads store scan data locally for retry
- Tap **Retry** to attempt upload again when internet is available
- Keeps history of the last 50 audits

**Offline Workflow:**
1. Complete audits even without internet - CSV is always saved locally
2. When internet is restored, go to **Audit Log**
3. Tap **Retry** next to any failed uploads
4. Status updates to "Uploaded" on success

## CSV File Location

All CSV exports are saved to the device's **Downloads** folder for easy access:
- Open the **Files** app on the device
- Navigate to **Downloads**
- CSV files will be named with the audit type, date, time, and username

## Requirements

- Zebra Android device with DataWedge
- Android 8.0 (API 26) or higher
- For RFID: Zebra RFD40 or compatible RFID reader
- For Airtable: Internet connectivity and Airtable account

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
3. Enter your full name, select a room, then tap **Start Audit**
4. Scan barcodes:
   - Weight mode: A dialog appears after each scan to optionally enter weight
   - Speed mode: Items are added immediately
5. Tap **Submit** to save CSV and upload to Airtable
6. Tap **Reset** to clear all scans if needed

### RFID Audits
1. Launch **CannaScanner**
2. Tap **RFID Tag Audit**
3. Enter your full name, select a room, then tap **Start RFID Audit**
4. Tap **Connect** to connect to the RFID reader
5. Tap **Start Scan** to begin reading tags
6. Tap **Stop Scan** when done scanning
7. Tap **Submit** to save CSV and upload to Airtable

### Retrying Failed Uploads
1. Launch **CannaScanner**
2. Tap **Audit Log**
3. Find audits marked as "Failed" (red)
4. Tap **Retry** to attempt upload again

## Permissions

The app requires the following permissions:
- **Internet**: For Airtable API uploads
- **Storage**: To save CSV files to Downloads
- **Bluetooth**: For RFID reader connectivity
- **Location**: Required by Android for Bluetooth scanning

## License

This application is provided as-is without guarantee or warranty and may be modified to suit individual needs.

## Credits

Based on [Zebra DataWedge Android Samples](https://github.com/ZebraDevs/DataWedge-Android-Samples).
