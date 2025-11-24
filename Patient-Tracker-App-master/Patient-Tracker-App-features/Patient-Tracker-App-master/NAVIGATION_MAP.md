# Complete Navigation Map

## Authentication Flow
```
splash
  └─> login (UnifiedLoginScreen)
       ├─> register_patient (RegisterPatientScreen)
       │    └─> account_created/{patientId}
       └─> [After Login]
            ├─> patient_home/{firstName}/{lastName}
            ├─> doctor_home/{firstName}/{lastName}/{doctorId}
            └─> admin_home
```

## Patient Navigation
```
patient_home
  ├─> doctor_list/{speciality}
  │    └─> book_appointment_simple/{doctorUid}/{doctorName}/{speciality}
  │         └─> [Success] → back to patient_home
  │
  ├─> my_records (MyRecordsScreen)
  │    ├─> upload_health_record_enhanced
  │    └─> [View Access Log Dialog]
  │
  ├─> patient_profile/{firstName}/{lastName}
  │
  ├─> full_schedule
  │
  └─> patient_health_records (old screen, kept for compatibility)
       └─> upload_health_record (old upload screen)
```

## Doctor Navigation
```
doctor_home
  ├─> doctor_patient_list (DoctorPatientListScreen)
  │    └─> doctor_view_patient_records_enhanced/{patientUid}/{patientName}
  │         ├─> [Sort by: Date, Name, Type]
  │         ├─> [Filter: All, Images, PDFs, Private]
  │         └─> [Glass Break Dialog for Private Records]
  │
  └─> doctor_view_patient_records/{patientUid}/{patientName} (old screen, kept for compatibility)
```

## Admin Navigation
```
admin_home
  ├─> admin_add_doctor
  ├─> admin_add_patient
  └─> admin_manage_users
```

## Route Reference

### New Enhanced Routes
- `upload_health_record_enhanced` - Multi-file upload with privacy, notes, medication
- `my_records` - Patient's record management with access logs
- `book_appointment_simple/{doctorUid}/{doctorName}/{speciality}` - Simplified calendar booking
- `doctor_view_patient_records_enhanced/{patientUid}/{patientName}` - Enhanced viewer with sorting/filtering
- `doctor_patient_list` - Doctor's patient list with search

### Legacy Routes (Kept for Compatibility)
- `upload_health_record` - Original single-file upload
- `patient_health_records` - Original patient records view
- `book_appointment` - Original booking with saved state
- `doctor_view_patient_records/{patientUid}/{patientName}` - Original doctor viewer

## Screen Access From UI

### Patient Home Screen
- "Record" category button → `my_records`
- "Doctors" category → `doctor_list/All`
- "Specialties" grid items → `doctor_list/{speciality}`
- Profile icon → `patient_profile/{firstName}/{lastName}`
- Schedule view → `full_schedule`

### Doctor Home Screen
- Search icon → `doctor_patient_list`
- Bottom bar "Patients" → `doctor_patient_list` (to be implemented)

### Doctor List Screen
- "Book Appointment" button → `book_appointment_simple/{doctorUid}/{doctorName}/{speciality}`

### My Records Screen
- FAB/Upload button → `upload_health_record_enhanced`
- View count badge → Access Log Dialog

### Doctor Patient List Screen
- Patient card → `doctor_view_patient_records_enhanced/{patientUid}/{patientName}`

## Navigation Best Practices

1. **Always use proper route parameters**: Avoid using savedStateHandle for critical data
2. **URL-encode special characters** in navigation arguments (spaces, slashes)
3. **Provide fallback values** for all navigation arguments
4. **Use popBackStack()** for back navigation instead of navigate() when going back
5. **Clear backstack** when appropriate using popUpTo with inclusive = true

## Deep Link Support (Future)
```
patienttracker://
  ├─> login
  ├─> records/{recordId}
  ├─> appointments/{appointmentId}
  └─> doctors/{doctorId}
```
