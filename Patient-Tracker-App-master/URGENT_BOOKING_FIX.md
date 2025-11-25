# Urgent Auto-Booking Logic Fix

## Problem Identified
The urgent booking feature was showing "no appointment found" even when valid slots existed because:
1. **First-match algorithm**: Stopped at first available doctor without comparing across all doctors
2. **No timing data validation**: Silent failures when doctor timing data was missing/malformed
3. **Auto-booking without confirmation**: Booked immediately without user consent
4. **Missing fee information**: Did not display consultation fee to user
5. **Premature fallback**: Showed alternatives before exhaustively searching all possibilities

## Solution Implemented

### 1. **New Best-Match Algorithm**
Replaced `attemptUrgentBooking()` with `findBestUrgentSlot()` that:

#### Step 1: Timing Data Validation
```kotlin
val doctorsWithValidTimings = specialistDoctors.filter { doctor ->
    doctor.timings.isNotBlank() && 
    doctor.timings.contains("-") && 
    doctor.days.isNotBlank()
}

if (doctorsWithValidTimings.isEmpty()) {
    return UrgentBookingResult(
        success = false,
        isDataAccessError = true,
        errorMessage = "No doctors with valid timing data available"
    )
}
```
- Validates timing data exists and is parseable BEFORE searching
- Returns specific error flag (`isDataAccessError`) if validation fails
- User sees technical error message instead of "no appointments" message

#### Step 2: Collect ALL Available Slots
```kotlin
val allAvailableSlots = mutableListOf<AvailableSlot>()

for (dayOffset in 0..6) {
    for (doctor in doctorsWithValidTimings) {
        for (block in timeBlocks) {
            // Calculate availability, check capacity
            if (available && bookedCount < capacity) {
                allAvailableSlots.add(AvailableSlot(...))
            }
        }
    }
}
```
- No early returns - collects ALL possibilities
- Stores each available slot with complete metadata
- Checks all 4 time blocks (Morning, Afternoon, Evening, Night) for each doctor

#### Step 3: Sort by Best Match
```kotlin
val bestSlot = allAvailableSlots.sortedWith(
    compareBy<AvailableSlot> { it.dateObj }           // Earliest date first
        .thenBy { it.timeBlock.startHour }            // Earliest time block
        .thenBy { it.bookedCount }                    // Lowest booking load
).first()
```
- **Primary**: Earliest date
- **Secondary**: Earliest time block within that date
- **Tertiary**: Lowest patient load (if multiple doctors match)

### 2. **Confirmation Flow (Not Auto-Booking)**

#### Updated Return Type
```kotlin
data class UrgentBookingResult(
    val success: Boolean,
    val needsConfirmation: Boolean = false,      // NEW
    val appointmentNumber: String? = null,
    val doctorId: String? = null,                // NEW
    val doctorName: String? = null,
    val date: String? = null,
    val timeBlock: String? = null,
    val timeRange: String? = null,               // NEW
    val fee: String? = null,                     // NEW
    val errorMessage: String? = null,
    val isDataAccessError: Boolean = false       // NEW
)
```

#### Confirmation Message
```kotlin
"I found the earliest available appointment for ${specialty}s:

• Doctor: ${bookingResult.doctorName}
• Date: ${bookingResult.date}
• Time: ${bookingResult.timeBlock} (${bookingResult.timeRange})
• Consultation Fee: PKR ${bookingResult.fee}

Would you like to confirm this booking?"

[Confirm Booking] [Cancel]
```

### 3. **Separate Confirmation Function**
```kotlin
suspend fun confirmUrgentBooking(
    patientId: String,
    patientName: String,
    doctorId: String,
    doctorName: String,
    date: String,
    timeBlock: String,
    symptom: String
): UrgentBookingResult
```
- Called ONLY when user clicks "Confirm Booking"
- Creates actual appointment via `AppointmentRepository.createAppointment()`
- Returns final confirmation with appointment number

### 4. **Error Handling Improvements**

#### Data Access Errors
```kotlin
if (bookingResult.isDataAccessError) {
    ChatMessage(
        text = "I'm having trouble accessing doctor availability data. Please use the booking screen:",
        actionButtons = [
            ChatAction("Go to Booking", "doctor_catalogue")
        ]
    )
}
```

#### No Appointments Found (After Full Search)
```kotlin
if (!bookingResult.success) {
    ChatMessage(
        text = "I checked all ${specialty}s for the next 7 days, but all slots are currently booked.

Here are your options:",
        actionButtons = [
            ChatAction("View All ${specialty}s", "doctor_catalogue"),
            ChatAction("Try Different Specialty", "doctor_catalogue")
        ]
    )
}
```
- Only shown if NO slots exist after checking ALL doctors
- Clear message that 7-day search was exhaustive

## User Experience Flow

### Before Fix
1. User: "I have chest pain, urgent"
2. Bot: "No appointment found" ❌ (even when slots exist)
3. Bot: Shows fallback buttons immediately

### After Fix
1. User: "I have chest pain, urgent"
2. Bot: "I found the earliest available appointment for cardiologists:
   - Doctor: Dr. Ahmed Khan
   - Date: 15/01/2025
   - Time: Morning (09:00 - 12:00)
   - Consultation Fee: PKR 2000
   
   Would you like to confirm this booking?"
   
   [Confirm Booking] [Cancel]

3. User: *Clicks Confirm Booking*
4. Bot: "✅ Your urgent appointment has been confirmed!
   - Appointment #: APT-2025-001
   - Doctor: Dr. Ahmed Khan
   - Date: 15/01/2025
   - Time: Morning"

## Key Improvements

✅ **Best-match selection** across all doctors (not first-match)
✅ **Timing data validation** before search begins
✅ **Confirmation step** - user sees details before booking
✅ **Fee display** in confirmation message
✅ **Exhaustive search** - checks all possibilities before showing "no appointments"
✅ **Clear error messages** distinguishing technical errors from availability issues
✅ **2-message flow** as requested (find → confirm)

## Technical Details

### Data Structures
- `AvailableSlot`: Stores slot metadata for sorting
- `findBestUrgentSlot()`: Returns slot info for confirmation
- `confirmUrgentBooking()`: Executes actual booking

### Sorting Algorithm
```kotlin
compareBy<AvailableSlot> { it.dateObj }      // LocalDate comparison
    .thenBy { it.timeBlock.startHour }        // 6, 12, 17, 21 (hour integers)
    .thenBy { it.bookedCount }                // 0, 1, 2, 3... (load)
```

### Time Blocks
- Morning: 6-12 (6 hours → 24 patients capacity)
- Afternoon: 12-17 (5 hours → 20 patients capacity)
- Evening: 17-21 (4 hours → 16 patients capacity)
- Night: 21-24 (3 hours → 12 patients capacity)

### Capacity Calculation
```kotlin
val capacity = hours.toInt() * 4  // 4 patients per hour
```

## Testing Checklist

- [ ] Test with doctors having missing timing data → shows data access error
- [ ] Test with multiple available doctors → selects earliest date + earliest time
- [ ] Test confirmation flow → both Confirm and Cancel buttons work
- [ ] Test fee display → shows consultation fee correctly
- [ ] Test "no appointments" case → only shows when truly no slots exist
- [ ] Test booking creation → appointment number generated correctly
- [ ] Test across all 4 time blocks → correctly identifies availability
- [ ] Test across 7 days → prioritizes earlier dates

## Files Modified

### ChatbotScreen.kt
- **Lines 1190-1215**: Added `AvailableSlot` data class
- **Lines 1218-1330**: Replaced `attemptUrgentBooking()` with `findBestUrgentSlot()`
- **Lines 1333-1375**: Added `confirmUrgentBooking()` function
- **Lines 815-890**: Updated urgent booking trigger to use confirmation flow
- **Lines 830-880**: Added confirmation message with action buttons

## Deployment Notes

1. **Build & Run**: User must rebuild the app to test changes
2. **Firestore**: No schema changes required
3. **Testing**: Use real doctor data with valid timing strings ("09:00-17:00")
4. **Monitoring**: Check logs for "URGENT BOOKING ATTEMPT" and error messages
