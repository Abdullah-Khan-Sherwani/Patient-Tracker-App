# Deploy Firestore Rules to Fix Permission Error

## Problem
Patients are getting `PERMISSION_DENIED` error when trying to read doctor availability for booking appointments.

## Solution
Deploy the updated `firestore.rules` file to Firebase.

## Option 1: Using Firebase Console (Recommended - Easiest)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **Patient-Tracker-App**
3. Click **Firestore Database** in the left menu
4. Click the **Rules** tab at the top
5. Copy and paste the following rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function getUserData() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data;
    }

    function isAdmin() {
      return request.auth != null &&
             exists(/databases/$(database)/documents/users/$(request.auth.uid)) &&
             getUserData().role == 'admin';
    }

    function isDoctor() {
      return request.auth != null &&
             exists(/databases/$(database)/documents/users/$(request.auth.uid)) &&
             getUserData().role == 'doctor';
    }

    function isOwn(uid) {
      return request.auth != null && request.auth.uid == uid;
    }

    // users collection
    match /users/{uid} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && (
        (isOwn(uid) && (request.resource.data.role == 'patient' || request.resource.data.role == 'doctor')) ||
        isAdmin()
      );
      allow update, delete: if request.auth != null &&
        (isOwn(uid) || isAdmin());
    }

    // appointments
    match /appointments/{id} {
      allow create: if request.auth != null &&
        (request.resource.data.patientUid == request.auth.uid || isDoctor() || isAdmin());
      // Allow all authenticated users to read appointments
      // This is needed for checking availability/capacity when booking
      allow read: if request.auth != null;
      allow update, delete: if request.auth != null &&
        (request.auth.uid == resource.data.patientUid || isAdmin());
    }

    // counters
    match /counters/{role} {
      allow read, write: if request.auth != null;
    }

    // notifications collection
    match /notifications/{notificationId} {
      allow create: if request.auth != null && (
        isAdmin() || 
        isDoctor() || 
        (request.resource.data.patientUid == request.auth.uid)
      );
      allow read: if request.auth != null && (
        resource.data.patientUid == request.auth.uid ||
        isAdmin()
      );
      allow update: if request.auth != null && (
        resource.data.patientUid == request.auth.uid ||
        isAdmin()
      );
      allow delete: if request.auth != null && isAdmin();
    }

    // doctor_availability collection - CRITICAL FOR BOOKING
    match /doctor_availability/{availabilityId} {
      // Allow all authenticated users to read availability
      allow read: if request.auth != null;
      // Allow write for authenticated users (app logic controls screen access)
      allow write: if request.auth != null;
    }

    // healthRecords collection
    match /healthRecords/{recordId} {
      allow create: if request.auth != null && (
        isDoctor() || isAdmin()
      );
      allow read: if request.auth != null && (
        resource.data.patientUid == request.auth.uid ||
        resource.data.doctorUid == request.auth.uid ||
        isAdmin()
      );
      allow update, delete: if request.auth != null && (
        resource.data.doctorUid == request.auth.uid ||
        isAdmin()
      );
    }

    // patientFavorites subcollection
    match /patients/{patientId}/favorites/{doctorId} {
      allow read, write: if request.auth != null && request.auth.uid == patientId;
    }

    // Default deny all other collections
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

6. Click **Publish** button
7. Wait for "Rules published successfully" message
8. Test the app again

## Option 2: Using Firebase CLI

If you have Firebase CLI installed:

```powershell
# Navigate to project directory
cd C:\Project\Patient-Tracker-App\Patient-Tracker-App-master

# Login to Firebase
firebase login

# Deploy only Firestore rules
firebase deploy --only firestore:rules

# Or deploy everything
firebase deploy
```

## Verification

After deploying, test by:
1. Login as a patient
2. Browse doctors in catalogue
3. Click "Book Appointment" on any doctor
4. You should now see available time blocks (Morning, Afternoon, etc.)
5. Check Logcat - no more PERMISSION_DENIED errors

## What This Fixes

The updated rules allow:
- ✅ **All authenticated users** (patients, doctors, admins) can **read** `doctor_availability`
- ✅ **All authenticated users** can **write** `doctor_availability` (app logic controls who accesses edit screen)
- ✅ Patients can now view doctor availability when booking appointments
- ✅ Doctors can edit their own availability
- ✅ Admins can manage all doctor availability

## Current Status

- ✅ Rules file exists locally at: `firestore.rules`
- ❌ Rules not deployed to Firebase (causing PERMISSION_DENIED)
- ⏳ **ACTION REQUIRED**: Deploy using Option 1 or Option 2 above
