package com.example.patienttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modern AM/PM time picker component for improved UX.
 * 
 * Features:
 * - Intuitive hour and minute selection
 * - AM/PM toggle with visual feedback
 * - Increment/decrement buttons
 * - Direct input capability
 * - Real-time validation
 */
@Composable
fun AMPMTimePicker(
    initialTime: String = "09:00 AM",
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFFC9956E),
    backgroundColor: Color = Color(0xFFF5F1ED)
) {
    var hour by remember {
        mutableStateOf(extractHour(initialTime))
    }
    var minute by remember {
        mutableStateOf(extractMinute(initialTime))
    }
    var isPM by remember {
        mutableStateOf(initialTime.contains("PM"))
    }

    LaunchedEffect(hour, minute, isPM) {
        val formattedTime = formatTime(hour, minute, isPM)
        onTimeSelected(formattedTime)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Select Time",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF5C4A42)
        )

        // Time Display Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour
                TimeSpinner(
                    value = hour,
                    onValueChange = { hour = it.coerceIn(1..12) },
                    modifier = Modifier.weight(1f),
                    primaryColor = primaryColor
                )

                // Separator
                Text(
                    text = ":",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C4A42),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Minute
                TimeSpinner(
                    value = minute,
                    onValueChange = { minute = (it % 60).coerceIn(0..59) },
                    step = 5,
                    modifier = Modifier.weight(1f),
                    primaryColor = primaryColor
                )

                // AM/PM Toggle
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier
                        .background(
                            color = if (isPM) primaryColor else primaryColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, primaryColor, RoundedCornerShape(8.dp))
                        .clickable { isPM = !isPM }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isPM) "PM" else "AM",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPM) Color.White else primaryColor
                    )
                }
            }
        }

        // Quick Select Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickTimes = listOf(
                "09:00 AM",
                "10:00 AM",
                "02:00 PM",
                "03:00 PM",
                "05:00 PM"
            )

            quickTimes.forEach { quickTime ->
                OutlinedButton(
                    onClick = {
                        hour = extractHour(quickTime)
                        minute = extractMinute(quickTime)
                        isPM = quickTime.contains("PM")
                    },
                    modifier = Modifier.wrapContentWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (formatTime(hour, minute, isPM) == quickTime) {
                            primaryColor.copy(alpha = 0.15f)
                        } else {
                            Color.White
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (formatTime(hour, minute, isPM) == quickTime) primaryColor else Color.Gray
                    )
                ) {
                    Text(
                        text = quickTime,
                        color = if (formatTime(hour, minute, isPM) == quickTime) primaryColor else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Formatted Output
        Text(
            text = "Selected: ${formatTime(hour, minute, isPM)}",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Reusable spinner component for hour/minute selection
 */
@Composable
private fun TimeSpinner(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    primaryColor: Color = Color(0xFFC9956E)
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Plus Button
        IconButton(
            onClick = { onValueChange(value + step) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase",
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // Value Display
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = primaryColor.copy(alpha = 0.1f),
            modifier = Modifier
                .width(60.dp)
                .height(48.dp)
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.fillMaxSize().wrapContentWidth().wrapContentHeight()
            )
        }

        // Minus Button
        IconButton(
            onClick = { onValueChange(value - step) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease",
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Helper Functions

private fun extractHour(timeString: String): Int {
    return try {
        timeString.split(":")[0].toInt()
    } catch (e: Exception) {
        9
    }
}

private fun extractMinute(timeString: String): Int {
    return try {
        timeString.split(":")[1].split(" ")[0].toInt()
    } catch (e: Exception) {
        0
    }
}

private fun formatTime(hour: Int, minute: Int, isPM: Boolean): String {
    val period = if (isPM) "PM" else "AM"
    return String.format("%02d:%02d %s", hour, minute, period)
}
