package com.example.patienttracker.data

/**
 * Represents an appointment time slot with category and availability information.
 */
data class TimeSlot(
    val id: String = "", // Unique identifier (e.g., "morning_1", "evening_2")
    val displayName: String = "", // Display name (e.g., "09:00 AM", "02:00 PM")
    val startTime: String = "", // Start time in HH:MM format
    val endTime: String = "", // End time in HH:MM format
    val category: SlotCategory = SlotCategory.MORNING,
    val isAvailable: Boolean = true,
    val doctorUid: String = ""
) {
    fun getDisplayWithCategory(): String {
        return "$displayName - ${category.displayName}"
    }
}

/**
 * Enum for appointment time slot categories throughout the day
 */
enum class SlotCategory(val displayName: String, val description: String) {
    MORNING("Morning", "Morning Session (8 AM - 12 PM)"),
    AFTERNOON("Afternoon", "Afternoon Session (12 PM - 4 PM)"),
    EVENING("Evening", "Evening Session (4 PM - 8 PM)"),
    NIGHT("Night", "Night Session (8 PM - 10 PM)")
}

/**
 * Default time slots for doctors
 * Can be customized per doctor or use these as defaults
 */
object DefaultTimeSlots {
    
    fun getMorningSlots(): List<TimeSlot> {
        return listOf(
            TimeSlot(
                id = "morning_1",
                displayName = "09:00 AM",
                startTime = "09:00",
                endTime = "09:30",
                category = SlotCategory.MORNING
            ),
            TimeSlot(
                id = "morning_2",
                displayName = "09:30 AM",
                startTime = "09:30",
                endTime = "10:00",
                category = SlotCategory.MORNING
            ),
            TimeSlot(
                id = "morning_3",
                displayName = "10:00 AM",
                startTime = "10:00",
                endTime = "10:30",
                category = SlotCategory.MORNING
            ),
            TimeSlot(
                id = "morning_4",
                displayName = "10:30 AM",
                startTime = "10:30",
                endTime = "11:00",
                category = SlotCategory.MORNING
            ),
            TimeSlot(
                id = "morning_5",
                displayName = "11:00 AM",
                startTime = "11:00",
                endTime = "11:30",
                category = SlotCategory.MORNING
            ),
            TimeSlot(
                id = "morning_6",
                displayName = "11:30 AM",
                startTime = "11:30",
                endTime = "12:00",
                category = SlotCategory.MORNING
            )
        )
    }
    
    fun getAfternoonSlots(): List<TimeSlot> {
        return listOf(
            TimeSlot(
                id = "afternoon_1",
                displayName = "02:00 PM",
                startTime = "14:00",
                endTime = "14:30",
                category = SlotCategory.AFTERNOON
            ),
            TimeSlot(
                id = "afternoon_2",
                displayName = "02:30 PM",
                startTime = "14:30",
                endTime = "15:00",
                category = SlotCategory.AFTERNOON
            ),
            TimeSlot(
                id = "afternoon_3",
                displayName = "03:00 PM",
                startTime = "15:00",
                endTime = "15:30",
                category = SlotCategory.AFTERNOON
            ),
            TimeSlot(
                id = "afternoon_4",
                displayName = "03:30 PM",
                startTime = "15:30",
                endTime = "16:00",
                category = SlotCategory.AFTERNOON
            ),
            TimeSlot(
                id = "afternoon_5",
                displayName = "04:00 PM",
                startTime = "16:00",
                endTime = "16:30",
                category = SlotCategory.AFTERNOON
            )
        )
    }
    
    fun getEveningSlots(): List<TimeSlot> {
        return listOf(
            TimeSlot(
                id = "evening_1",
                displayName = "05:00 PM",
                startTime = "17:00",
                endTime = "17:30",
                category = SlotCategory.EVENING
            ),
            TimeSlot(
                id = "evening_2",
                displayName = "05:30 PM",
                startTime = "17:30",
                endTime = "18:00",
                category = SlotCategory.EVENING
            ),
            TimeSlot(
                id = "evening_3",
                displayName = "06:00 PM",
                startTime = "18:00",
                endTime = "18:30",
                category = SlotCategory.EVENING
            ),
            TimeSlot(
                id = "evening_4",
                displayName = "06:30 PM",
                startTime = "18:30",
                endTime = "19:00",
                category = SlotCategory.EVENING
            ),
            TimeSlot(
                id = "evening_5",
                displayName = "07:00 PM",
                startTime = "19:00",
                endTime = "19:30",
                category = SlotCategory.EVENING
            ),
            TimeSlot(
                id = "evening_6",
                displayName = "07:30 PM",
                startTime = "19:30",
                endTime = "20:00",
                category = SlotCategory.EVENING
            )
        )
    }
    
    fun getNightSlots(): List<TimeSlot> {
        return listOf(
            TimeSlot(
                id = "night_1",
                displayName = "08:00 PM",
                startTime = "20:00",
                endTime = "20:30",
                category = SlotCategory.NIGHT
            ),
            TimeSlot(
                id = "night_2",
                displayName = "08:30 PM",
                startTime = "20:30",
                endTime = "21:00",
                category = SlotCategory.NIGHT
            ),
            TimeSlot(
                id = "night_3",
                displayName = "09:00 PM",
                startTime = "21:00",
                endTime = "21:30",
                category = SlotCategory.NIGHT
            ),
            TimeSlot(
                id = "night_4",
                displayName = "09:30 PM",
                startTime = "21:30",
                endTime = "22:00",
                category = SlotCategory.NIGHT
            )
        )
    }
    
    fun getAllSlots(): List<TimeSlot> {
        return getMorningSlots() + getAfternoonSlots() + getEveningSlots() + getNightSlots()
    }
    
    fun getSlotsByCategory(category: SlotCategory): List<TimeSlot> {
        return when (category) {
            SlotCategory.MORNING -> getMorningSlots()
            SlotCategory.AFTERNOON -> getAfternoonSlots()
            SlotCategory.EVENING -> getEveningSlots()
            SlotCategory.NIGHT -> getNightSlots()
        }
    }
}
