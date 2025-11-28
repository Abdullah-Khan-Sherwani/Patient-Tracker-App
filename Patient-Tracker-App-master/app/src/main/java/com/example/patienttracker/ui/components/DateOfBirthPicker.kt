package com.example.patienttracker.ui.components

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Date picker for Date of Birth with both visual picker and manual text input.
 * Supports date validation and formatted display.
 */
@Composable
fun DateOfBirthPicker(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    maxDate: Long = System.currentTimeMillis(),
    minDate: Long = System.currentTimeMillis() - (100 * 365 * 24 * 60 * 60 * 1000L), // 100 years ago
    label: String = "Date of Birth",
    primaryColor: Color = Color(0xFF0E4944),       // Deep Teal
    backgroundColor: Color = Color(0xFFF0F5F4),    // Dim background
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    var showManualInput by remember { mutableStateOf(false) }
    var manualDateInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val selectedDateStr = if (selectedDate > 0) dateFormat.format(Date(selectedDate)) else "Select date"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )

        if (showManualInput) {
            // Manual Input Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter Date Manually",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day input
                    OutlinedTextField(
                        value = if (selectedDate > 0) SimpleDateFormat("dd", Locale.getDefault()).format(
                            Date(selectedDate)
                        ) else "",
                        onValueChange = { },
                        label = { Text("Day") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = false,
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp)
                    )

                    // Month input
                    OutlinedTextField(
                        value = if (selectedDate > 0) SimpleDateFormat("MM", Locale.getDefault()).format(
                            Date(selectedDate)
                        ) else "",
                        onValueChange = { },
                        label = { Text("Month") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = false,
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp)
                    )

                    // Year input
                    OutlinedTextField(
                        value = if (selectedDate > 0) SimpleDateFormat("yyyy", Locale.getDefault()).format(
                            Date(selectedDate)
                        ) else "",
                        onValueChange = { },
                        label = { Text("Year") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = false,
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp)
                    )
                }

                Text(
                    text = "Or use the date picker below",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )

                Button(
                    onClick = { showManualInput = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Use Date Picker", color = primaryColor, fontSize = 12.sp)
                }
            }
        } else {
            // Date Picker Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled) {
                        showDatePicker(context, selectedDate, minDate, maxDate) { newDate ->
                            onDateSelected(newDate)
                            inputError = ""
                        }
                    }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = primaryColor
                    )
                    Text(
                        text = selectedDateStr,
                        fontSize = 14.sp,
                        color = if (selectedDate > 0) Color.Black else Color.Gray,
                        fontWeight = if (selectedDate > 0) FontWeight.Normal else FontWeight.Light
                    )
                }

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Manual input",
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { showManualInput = true },
                    tint = primaryColor.copy(alpha = 0.6f)
                )
            }
        }

        // Error Message
        if (inputError.isNotEmpty()) {
            Text(
                text = inputError,
                fontSize = 12.sp,
                color = Color(0xFFEF4444)
            )
        }

        // Age Info
        if (selectedDate > 0) {
            val age = calculateAge(selectedDate)
            Text(
                text = "Age: $age years",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Light
            )
        }
    }
}

/**
 * Show system date picker dialog
 */
private fun showDatePicker(
    context: Context,
    currentDate: Long,
    minDate: Long,
    maxDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    if (currentDate > 0) {
        calendar.timeInMillis = currentDate
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)
            onDateSelected(selectedCalendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Set date range constraints
    datePickerDialog.datePicker.minDate = minDate
    datePickerDialog.datePicker.maxDate = maxDate

    datePickerDialog.show()
}

/**
 * Calculate age from birth date
 */
fun calculateAge(birthDateMillis: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = birthDateMillis

    val birthYear = calendar.get(Calendar.YEAR)
    val birthMonth = calendar.get(Calendar.MONTH)
    val birthDay = calendar.get(Calendar.DAY_OF_MONTH)

    val today = Calendar.getInstance()
    var age = today.get(Calendar.YEAR) - birthYear

    // Check if birthday hasn't occurred yet this year
    if (today.get(Calendar.MONTH) < birthMonth ||
        (today.get(Calendar.MONTH) == birthMonth && today.get(Calendar.DAY_OF_MONTH) < birthDay)
    ) {
        age--
    }

    return age
}

/**
 * Date of birth validation
 */
fun isValidDateOfBirth(birthDateMillis: Long): Pair<Boolean, String> {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = birthDateMillis

    val today = Calendar.getInstance()
    val age = calculateAge(birthDateMillis)

    return when {
        birthDateMillis > System.currentTimeMillis() -> Pair(false, "Birth date cannot be in the future")
        age < 13 -> Pair(false, "You must be at least 13 years old to use this app")
        age > 120 -> Pair(false, "Please enter a valid date of birth")
        else -> Pair(true, "Valid date of birth")
    }
}
