# Medical Records Access Control Implementation

## Overview

This document describes the end-to-end access control system for medical records in the Patient Tracker App. The system ensures that doctors can only access patient records when they have a valid appointment relationship.

## Access Control Flow

### Patient Upload Flow

1. **Patient navigates to Upload Health Records screen** (`EnhancedUploadHealthRecordScreen.kt`)
2. **Patient selects upload target**:
   - Self (own records)
   - Dependent (child, spouse, etc.)
3. **Patient selects files** (images or PDFs, max 10MB each)
4. **Patient fills metadata**:
   - Description (required)
   - Notes (optional)
   - Past Medication (optional)
   - Privacy toggle (private = doctors need explicit permission)
5. **Upload process**:
   - File → Supabase Storage (`SupabaseStorageRepository`)
   - Metadata → Firestore (`HealthRecordRepository`)

### Storage Structure

```
Supabase Storage Bucket: medical_records
├── {patientId}/
│   ├── self/
│   │   ├── report_1234567890.pdf
│   │   └── xray_1234567891.jpg
│   └── {dependentId}/
│       └── prescription_1234567892.pdf
```

### Doctor Access Flow

1. **Doctor attempts to view patient records** (`EnhancedDoctorViewPatientRecordsScreen.kt`)
2. **System checks access** (`HealthRecordRepository.getDoctorAccessibleRecordsForPatient()`):
   
   ```kotlin
   // Step 1: Check appointment relationship
   hasAppointmentRelationship() → checks for:
   - Active scheduled appointments (today or future)
   - Previously completed appointments
   
   // Step 2: If no relationship exists
   → Return AccessDeniedException
   → UI shows "Access Denied" screen
   
   // Step 3: If relationship exists, filter records
   hasActiveAppointment() → checks for:
   - status = "scheduled" AND
   - appointmentDate >= today
   
   // Step 4: Apply privacy filter
   - Non-private records: Always accessible
   - Private records: Only if hasActiveAppointment OR in doctorAccessList
   ```

3. **UI States** (`DoctorRecordsUiState`):
   - `Loading` - Checking access
   - `Success(records)` - Records displayed
   - `AccessDenied(message)` - No valid appointment relationship
   - `Error(message)` - Technical error

### Access Matrix

| Scenario | Access Level |
|----------|--------------|
| No appointment with patient | ❌ Completely blocked (AccessDenied) |
| Only past completed appointments | ✅ Non-private records only |
| Active appointment (scheduled, today+) | ✅ All records (including private) |
| Doctor in `doctorAccessList` | ✅ That specific record |
| Glass break emergency | ✅ Access granted with logging |

## Key Files

### Repository Layer

| File | Purpose |
|------|---------|
| `AppointmentRepository.kt` | Appointment CRUD + access checking |
| `HealthRecordRepository.kt` | Record CRUD + access filtering |
| `SupabaseStorageRepository.kt` | File upload/delete to Supabase |

### Key Functions

```kotlin
// AppointmentRepository.kt

/**
 * Check for active appointment (today or future)
 * Used for: Private record access
 */
suspend fun hasActiveAppointment(doctorUid: String, patientUid: String): Result<Boolean>

/**
 * Check for any appointment relationship (active OR completed)
 * Used for: Baseline record access
 */
suspend fun hasAppointmentRelationship(doctorUid: String, patientUid: String): Result<Boolean>
```

```kotlin
// HealthRecordRepository.kt

/**
 * Get records with access control
 * Throws AccessDeniedException if no relationship
 */
suspend fun getDoctorAccessibleRecordsForPatient(patientUid: String): Result<List<HealthRecord>>

/**
 * Quick access check without fetching records
 */
suspend fun checkDoctorAccessToPatient(patientUid: String): Result<Boolean>
```

### UI Layer

| File | Purpose |
|------|---------|
| `EnhancedUploadHealthRecordScreen.kt` | Patient upload with dependent selector |
| `EnhancedDoctorViewPatientRecordsScreen.kt` | Doctor view with access control UI |

## Security Considerations

1. **Date Validation**: `hasActiveAppointment()` compares appointment date against today at midnight
2. **Status Validation**: Only `scheduled` status counts as active
3. **Cascading Access**: Completed appointments grant limited access (non-private only)
4. **Audit Trail**: `viewedBy` and `glassBreakAccess` logs track all access
5. **Glass Break**: Emergency access allowed but logged and notified

## Error Handling

| Exception | UI Response |
|-----------|-------------|
| `AccessDeniedException` | Shows access-denied screen with explanation |
| `Exception("User not authenticated")` | Redirect to login |
| Other exceptions | Generic error state |

## Future Enhancements

- [ ] Push notifications for glass break access
- [ ] Admin audit dashboard for access logs
- [ ] Time-limited access tokens for shared records
- [ ] Record-level permission grants by patients
