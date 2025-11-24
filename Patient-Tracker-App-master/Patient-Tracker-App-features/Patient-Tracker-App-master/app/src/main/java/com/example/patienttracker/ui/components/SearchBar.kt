package com.example.patienttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable search bar component for doctor/patient search.
 * Provides real-time search input with clear button.
 */
@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search doctors...",
    onSearch: (String) -> Unit = {},
    backgroundColor: Color = Color(0xFFF5F1ED),
    searchIconColor: Color = Color(0xFFC9956E),
    textColor: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = searchIconColor,
                modifier = Modifier.size(22.dp)
            )

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = textColor,
                    fontSize = 15.sp
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = Color.Gray.copy(alpha = 0.6f),
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                },
                cursorBrush = SolidColor(searchIconColor)
            )

            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = searchIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Search bar with dropdown suggestions.
 * Shows autocomplete suggestions as user types.
 */
@Composable
fun SearchBarWithSuggestions(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String> = emptyList(),
    onSuggestionSelected: (String) -> Unit = {},
    placeholder: String = "Search doctors...",
    backgroundColor: Color = Color(0xFFF5F1ED),
    searchIconColor: Color = Color(0xFFC9956E),
    textColor: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    var expandedSuggestions by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = backgroundColor,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = searchIconColor,
                    modifier = Modifier.size(20.dp)
                )

                TextField(
                    value = value,
                    onValueChange = {
                        onValueChange(it)
                        expandedSuggestions = it.isNotEmpty() && suggestions.isNotEmpty()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    placeholder = {
                        Text(
                            text = placeholder,
                            color = Color.Gray.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    textStyle = LocalTextStyle.current.copy(color = textColor)
                )

                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = searchIconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Suggestions dropdown
        if (expandedSuggestions && suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = backgroundColor,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    suggestions.take(5).forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                onValueChange(suggestion)
                                onSuggestionSelected(suggestion)
                                expandedSuggestions = false
                            }
                        )
                    }
                }
            }
        }
    }
}
