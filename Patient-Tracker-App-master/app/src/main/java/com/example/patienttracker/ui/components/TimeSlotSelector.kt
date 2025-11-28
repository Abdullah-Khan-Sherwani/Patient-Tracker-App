package com.example.patienttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.patienttracker.data.SlotCategory
import com.example.patienttracker.data.TimeSlot

/**
 * Composable to display appointment time slots grouped by category.
 * Shows available slots for each time slot category with visual indicators.
 */
@Composable
fun TimeSlotCategorySelector(
    slots: List<TimeSlot>,
    selectedSlot: TimeSlot?,
    onSlotSelected: (TimeSlot) -> Unit,
    primaryColor: Color = Color(0xFF0E4944),       // Deep Teal
    backgroundColor: Color = Color(0xFFF0F5F4)     // Dim background
) {
    val slotsByCategory = slots.groupBy { it.category }
    
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        slotsByCategory.entries.sortedBy { it.key.ordinal }.forEach { (category, categorySlots) ->
            item {
                TimeslotCategorySection(
                    category = category,
                    slots = categorySlots,
                    selectedSlot = selectedSlot,
                    onSlotSelected = onSlotSelected,
                    primaryColor = primaryColor,
                    backgroundColor = backgroundColor
                )
            }
        }
    }
}

/**
 * Individual time slot category section with slots displayed as chips
 */
@Composable
fun TimeslotCategorySection(
    category: SlotCategory,
    slots: List<TimeSlot>,
    selectedSlot: TimeSlot?,
    onSlotSelected: (TimeSlot) -> Unit,
    primaryColor: Color = Color(0xFF0E4944),       // Deep Teal
    backgroundColor: Color = Color(0xFFF0F5F4)     // Dim background
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Category header with description
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = category.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor
            )
            Text(
                text = category.description,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Normal
            )
        }

        // Time slots in grid
        val slotsInRows = slots.chunked(3)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slotsInRows.forEach { rowSlots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowSlots.forEach { slot ->
                        TimeSlotChip(
                            slot = slot,
                            isSelected = selectedSlot?.id == slot.id,
                            onSelect = { onSlotSelected(slot) },
                            primaryColor = primaryColor,
                            backgroundColor = backgroundColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Fill remaining space in row
                    repeat(3 - rowSlots.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Individual time slot chip button
 */
@Composable
fun TimeSlotChip(
    slot: TimeSlot,
    isSelected: Boolean,
    onSelect: () -> Unit,
    primaryColor: Color = Color(0xFF0E4944),       // Deep Teal
    backgroundColor: Color = Color(0xFFF0F5F4),    // Dim background
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(enabled = slot.isAvailable) { onSelect() },
        shape = RoundedCornerShape(8.dp),
        color = when {
            isSelected -> primaryColor
            !slot.isAvailable -> Color.LightGray
            else -> backgroundColor
        },
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = slot.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else if (slot.isAvailable) Color.Black else Color.Gray,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        modifier = Modifier
                            .size(16.dp)
                            .padding(start = 4.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Compact view of time slots for display purposes (non-interactive)
 */
@Composable
fun TimeSlotDisplay(
    slot: TimeSlot,
    showCategory: Boolean = true,
    primaryColor: Color = Color(0xFF0E4944),       // Deep Teal
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFFF0F5F4), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = slot.displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryColor
        )
        
        if (showCategory) {
            Text(
                text = "•",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = slot.category.displayName,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
