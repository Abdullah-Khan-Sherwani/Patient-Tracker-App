# Patient Tracker App - Further Improvements Implementation Plan

## Overview
This document outlines the remaining improvements needed for the Patient Tracker App, organized by priority and complexity.

---

## 1. Firebase Push Notifications (HIGH PRIORITY)
**Status**: Not Implemented  
**Complexity**: HIGH

### Requirements:
- Send notifications to patients when appointments are booked/cancelled
- Send notifications to doctors when new appointments are assigned
- Real-time notification delivery using Firebase Cloud Messaging (FCM)

### Implementation Steps:
1. **Setup Firebase Cloud Messaging**
   - Add `com.google.firebase:firebase-messaging` dependency
   - Create `PatientTrackerMessagingService` extending `FirebaseMessagingService`
   - Request POST_NOTIFICATIONS permission in AndroidManifest.xml

2. **Update NotificationRepository**
   - Add FCM token storage in user profile
   - Implement `sendNotification()` to save to Firestore + FCM
   - Create Cloud Functions to trigger push notifications

3. **Files to Modify**:
   - `build.gradle.kts` (add Firebase Messaging)
   - `NotificationRepository.kt` (enhance with FCM)
   - `AndroidManifest.xml` (add permission and service)

---

## 2. Upload Records Option on Appointment Screen (HIGH PRIORITY)
**Status**: Partially Implemented  
**Complexity**: MEDIUM

### Requirements:
- Add "Upload Records/Reports" button on appointment confirmation screen
- Support PDF, JPG, JPEG, PNG (max 10MB)
- Store file references in appointment document

### Implementation Steps:
1. **Update `AppointmentSuccessScreen.kt`**
   - Add file picker launcher
   - Add upload button and progress indicator
   - Store fileUrl in appointment document

2. **Update `AppointmentRepository.kt`**
   - Add method to upload and link records to appointment
   - Update appointment document with `attachedRecords` array

3. **Files to Modify**:
   - `AppointmentSuccessScreen.kt`
   - `AppointmentRepository.kt`
   - `StorageRepository.kt`

---

## 3. AM/PM User Experience Improvement (MEDIUM PRIORITY)
**Status**: Needs Improvement  
**Complexity**: LOW

### Requirements:
- Replace 24-hour format with intuitive AM/PM picker
- Show visual indicators for morning/afternoon/evening slots
- Better time slot selection UI

### Implementation Steps:
1. **Create `TimePickerComposable.kt`**
   - Build custom AM/PM time picker
   - Show time slots grouped by period
   - Add visual indicators (morning icon, afternoon icon, etc.)

2. **Update Time Selection Screens**
   - Replace existing time picker with new component
   - Store as 24-hour format internally but display as AM/PM

3. **Files to Create**:
   - `ui/components/TimePickerComposable.kt`

4. **Files to Modify**:
   - `SelectDateTimeScreen.kt`
   - `FullScheduleScreen.kt`

---

## 4. Admin: Edit Doctor Details (MEDIUM PRIORITY)
**Status**: Not Implemented  
**Complexity**: MEDIUM

### Requirements:
- Admin can edit doctor information (name, speciality, credentials, etc.)
- Preserve doctor's UID and auth account
- Update Firestore and display changes immediately

### Implementation Steps:
1. **Create `AdminEditDoctorScreen.kt`**
   - Form to edit doctor details
   - Call `AdminRepository.updateDoctorDetails()`
   - Show success/error messages

2. **Update `AdminManagementRepository.kt`**
   - Add `updateDoctorDetails()` method
   - Validate data before update
   - Log changes in admin audit trail

3. **Update Firestore Rules**
   - Allow admin to update doctor user documents

4. **Files to Create**:
   - `ui/screens/admin/AdminEditDoctorScreen.kt`

5. **Files to Modify**:
   - `ManageUsersScreen.kt` (add Edit button)
   - `AdminManagementRepository.kt`
   - `firestore.rules`

---

## 5. Editable Consultation Fees by Admin (MEDIUM PRIORITY)
**Status**: Currently Fixed at 1500  
**Complexity**: MEDIUM

### Requirements:
- Admin can set consultation fee globally or per-doctor
- Fees stored in Firestore configuration
- Display updated fees when booking appointments

### Implementation Steps:
1. **Create `ConsultationFeeRepository.kt`**
   - Add `getConsultationFee()` - fetch from Firestore
   - Add `updateConsultationFee()` - admin only
   - Cache fees locally with TTL

2. **Update `AdminSettingsScreen.kt`**
   - Add fee management section
   - Input field for global fee or per-doctor fees
   - Display current fees

3. **Update Appointment Creation**
   - Fetch fee from repository instead of hardcoding 1500
   - Update `AppointmentRepository.kt`

4. **Firestore Structure**:
   - `configuration/consultationFees` document

5. **Files to Create**:
   - `data/ConsultationFeeRepository.kt`

6. **Files to Modify**:
   - `AdminSettingsScreen.kt`
   - `AppointmentRepository.kt`
   - `firestore.rules`

---

## 6. Patient Search Feature (MEDIUM PRIORITY)
**Status**: Not Implemented  
**Complexity**: MEDIUM

### Requirements:
- Search doctors by name, speciality, location
- Search in multiple screens (Doctor Catalogue, Favorites, etc.)
- Real-time search with debouncing

### Implementation Steps:
1. **Create `SearchViewModel.kt`**
   - Implement search logic with debouncing
   - Handle filtered results
   - Cache search history

2. **Create `SearchComposable.kt`**
   - Reusable search bar component
   - Search history suggestions
   - Clear search button

3. **Update Multiple Screens**
   - Add search composable to Doctor Catalogue
   - Add search to Favorites
   - Add search to Appointments

4. **Update Firestore Queries**
   - Implement efficient search using `where` clauses
   - Consider Firestore search capabilities (case-insensitive filtering)

5. **Files to Create**:
   - `data/SearchRepository.kt`
   - `ui/viewmodels/SearchViewModel.kt`
   - `ui/components/SearchComposable.kt`

6. **Files to Modify**:
   - `DoctorCatalogueScreen.kt`
   - `FavoriteDoctorsScreen.kt`
   - `PatientDashboard.kt`

---

## 7. Fix Notifications (MEDIUM PRIORITY)
**Status**: Partially Implemented  
**Complexity**: MEDIUM

### Requirements:
- Display real-time notifications in app
- Mark notifications as read
- Show unread count badge

### Implementation Steps:
1. **Create `NotificationViewModel.kt`**
   - Listen to Firestore `notifications` collection
   - Update UI with real-time changes
   - Handle read/unread state

2. **Update `PatientNotificationsScreen.kt`**
   - Display notifications from Firestore
   - Add "Mark as Read" functionality
   - Show unread count in navigation badge

3. **Update Navigation**
   - Add notification count badge to bottom navigation

4. **Files to Modify**:
   - `NotificationRepository.kt` (add observeNotifications)
   - `PatientNotificationsScreen.kt`
   - `PatientHomeScreen.kt` (navigation)

---

## 8. Guest Mode: Add Doctor Catalogue View (MEDIUM PRIORITY)
**Status**: Screens Exist, Navigation Missing  
**Complexity**: LOW-MEDIUM

### Requirements:
- Guests can browse doctors without login
- Display doctor cards with details
- "Sign In to Book" call-to-action

### Implementation Steps:
1. **Update `GuestHomeScreen.kt`**
   - Add "View Doctors" button/navigation
   - Link to `GuestDoctorsScreen.kt` or `GuestDoctorDetailsScreen.kt`

2. **Update `AppNavHost.kt`**
   - Ensure guest navigation routes are accessible
   - Add guest doctor browsing routes

3. **Test Navigation Flow**
   - Guest → Home → Browse Doctors → Doctor Details → Sign In

4. **Files to Modify**:
   - `GuestHomeScreen.kt`
   - `AppNavHost.kt`

---

## 9. Privacy Policy on Sign-In Page (LOW PRIORITY)
**Status**: Not Implemented  
**Complexity**: LOW

### Requirements:
- Add "Privacy Policy" link on login page
- Display privacy policy in bottom sheet or separate screen
- Add "Terms & Conditions" link as well

### Implementation Steps:
1. **Create `PrivacyPolicyScreen.kt`**
   - Display policy text (can be fetched from Firestore)
   - Scrollable content
   - Accept/Close buttons

2. **Update `UnifiedLoginScreen.kt`**
   - Add clickable "Privacy Policy" text at bottom
   - Add "Terms & Conditions" link
   - Navigate to policy screens

3. **Files to Create**:
   - `ui/screens/auth/PrivacyPolicyScreen.kt`
   - `ui/screens/auth/TermsAndConditionsScreen.kt`

4. **Files to Modify**:
   - `UnifiedLoginScreen.kt`
   - `AppNavHost.kt`

---

## 10. Date of Birth: Manual Text Input (LOW PRIORITY)
**Status**: Calendar-Only  
**Complexity**: LOW

### Requirements:
- Allow typing DOB directly (DD/MM/YYYY format)
- Calendar picker as secondary option
- Input validation

### Implementation Steps:
1. **Create Hybrid DOB Input**
   - `OutlinedTextField` for manual input
   - `DatePickerDialog` for calendar selection
   - Format validation

2. **Update Registration Screens**
   - `RegisterPatientScreen.kt`
   - `RegisterDoctorScreen.kt` (if applicable)

3. **Add Input Validation**
   - Validate DD/MM/YYYY format
   - Check age requirements (if any)
   - Show error messages

4. **Files to Create**:
   - `ui/components/DateOfBirthPickerComposable.kt`

5. **Files to Modify**:
   - `RegisterPatientScreen.kt`
   - Input validation utilities

---

## 11. Dark Mode for Admin & Doctor Profiles (HIGH PRIORITY)
**Status**: Not Implemented  
**Complexity**: MEDIUM-HIGH

### Requirements:
- Implement Material3 dynamic theming
- Support system dark mode
- Allow manual toggle in settings
- Persist user preference

### Implementation Steps:
1. **Create Theme System**
   - `Theme.kt` with Material3 colors
   - Light and dark color palettes
   - Support dynamic colors (Android 12+)

2. **Create `ThemeManager.kt`**
   - Read/write theme preference to DataStore
   - Provide `isDarkMode` flow
   - Allow theme toggle

3. **Update `MainActivity.kt`**
   - Apply theme from DataStore
   - Listen to theme changes

4. **Update All Screens**
   - Apply theme colors consistently
   - Test light and dark modes

5. **Add Theme Toggle to Settings**
   - `AdminSettingsScreen.kt`
   - `DoctorProfileScreen.kt`
   - Patient settings (already may exist)

6. **Files to Create**:
   - `ui/theme/Theme.kt`
   - `ui/theme/Color.kt`
   - `data/ThemeRepository.kt` or `ThemeManager.kt`
   - `ui/components/ThemeToggleComposable.kt`

7. **Files to Modify**:
   - `MainActivity.kt`
   - `AdminSettingsScreen.kt`
   - `DoctorProfileScreen.kt`
   - All UI screens (apply theme colors)

---

## Priority Implementation Order

### Phase 1 (Immediate - This Week)
1. Dark Mode for Admin & Doctor (HIGH impact, used everywhere)
2. AM/PM Time Picker (HIGH impact, better UX)
3. Upload Records on Appointment Screen (HIGH impact, feature completion)

### Phase 2 (Next Week)
4. Firebase Push Notifications (HIGH impact, critical feature)
5. Fix In-App Notifications (User expectation)
6. Editable Consultation Fees (Admin requirement)

### Phase 3 (Following Week)
7. Patient Search Feature (Nice to have, enhances UX)
8. Admin Edit Doctor Details (Admin management)
9. Guest Doctor View (Feature completeness)

### Phase 4 (Polish)
10. Privacy Policy on Login
11. Date of Birth Manual Input
12. Testing and refinements

---

## Technical Notes

### Dependencies to Add (if not already present)
```kotlin
// build.gradle.kts
dependencies {
    // Firebase Messaging
    implementation("com.google.firebase:firebase-messaging-ktx")
    
    // DataStore for preferences (if not present)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Material3 (already likely present)
    implementation("androidx.compose.material3:material3:x.x.x")
}
```

### Firestore Collections to Create
- `configuration/consultationFees`
- `notifications` (already exists)
- `settings` (for global app settings)

### Firestore Security Rules Updates Needed
- Admin update doctor permissions
- Notifications read/write permissions (partially done)
- Configuration read permissions

---

## Testing Checklist
- [ ] Dark mode works on all screens
- [ ] Time picker shows AM/PM correctly
- [ ] File uploads work with 10MB limit
- [ ] Push notifications are received
- [ ] Search is responsive and accurate
- [ ] Guest mode navigation is smooth
- [ ] Admin can edit doctor details
- [ ] Consultation fees update correctly
- [ ] All screens are accessible in light/dark modes
- [ ] DOB input accepts manual text

---

## Notes for Development
- Use `@OptIn(ExperimentalMaterial3Api::class)` where needed for Material3
- Implement proper error handling and user feedback
- Add loading states for async operations
- Use Hilt for dependency injection
- Follow existing code patterns and naming conventions
- Test on multiple device sizes

