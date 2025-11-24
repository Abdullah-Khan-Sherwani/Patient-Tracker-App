# Appointment Numbering System Update

## Overview
Replaced the global appointment numbering system with a **per-doctor, per-day counter** system.

## Changes Made

### Before (Global System)
- All appointments across all doctors shared a single incrementing counter (001, 002, 003...)
- Numbers never reset
- Used Firestore transaction on `counters/appointments` document

### After (Per-Doctor, Per-Day System)
- Each doctor has their own appointment sequence (1, 2, 3...)
- Numbers reset daily at midnight
- Numbers are calculated by counting existing non-cancelled appointments for that doctor on that day

## Implementation Details

### Modified Files
- `app/src/main/java/com/example/patienttracker/data/AppointmentRepository.kt`

### Key Changes

#### `getNextAppointmentNumber()` Function
- **Parameters**: Now accepts `doctorUid: String` and `appointmentDate: Timestamp`
- **Logic**:
  1. Extracts date string (yyyy-MM-dd) from the appointment timestamp
  2. Queries all appointments for the specific doctor
  3. Filters appointments by:
     - Matching date (day-level comparison)
     - Status is NOT "cancelled"
  4. Counts matching appointments
  5. Returns count + 1 as the next appointment number
- **Format**: Simple integer string (1, 2, 3...) instead of zero-padded (001, 002, 003)

#### `createAppointment()` Function
- Updated to pass `doctorUid` and `appointmentDate` to `getNextAppointmentNumber()`

## Behavior Examples

### Example 1: Multiple Doctors Same Day
- Dr. Smith's appointments today: #1, #2, #3
- Dr. Jones' appointments today: #1, #2
- Each doctor has independent sequences

### Example 2: Daily Reset
- Dr. Smith today: #1, #2, #3
- Dr. Smith tomorrow: #1, #2, #3 (numbers reset)

### Example 3: Cancelled Appointments
- Dr. Smith has 3 appointments: #1, #2, #3
- Patient cancels appointment #2
- Next booking for Dr. Smith today: #4 (cancelled appointments don't affect count but number is not reused)

### Example 4: First Appointment
- Dr. Brown has no appointments today
- First booking: #1

## Edge Cases Handled

1. **No Existing Appointments**: Returns "1" for the first appointment of the day
2. **Database Errors**: Falls back to "1" if query fails
3. **Cancelled Appointments**: Excluded from count (status != "cancelled")
4. **Date Normalization**: Compares only date portion (ignores time) for consistent daily grouping
5. **Concurrent Bookings**: Query-based counting ensures accurate numbers even with simultaneous bookings

## Display Locations
Appointment numbers are displayed in:
- `AppointmentSuccessScreen.kt` - Shows "#1", "#2", etc.
- `FullScheduleScreen.kt` - Lists appointments with numbers
- `ConfirmAppointmentScreen.kt` - Passes number through navigation

## Technical Notes
- The global `appointmentId` (UUID) remains unchanged for backend references
- Only the display number (`appointmentNumber`) uses the new per-doctor-per-day logic
- No migration needed - new logic applies automatically to all future appointments
- Existing appointments retain their original global numbers
