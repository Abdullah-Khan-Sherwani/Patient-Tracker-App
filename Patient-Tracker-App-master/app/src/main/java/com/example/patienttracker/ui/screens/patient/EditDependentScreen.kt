package com.example.patienttracker.ui.screens.patient

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.Dependent
import com.example.patienttracker.data.DependentRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.imePadding

// ============================================================
// Deep Teal & Mint Design System
// ============================================================
private val HeaderTopColor = Color(0xFF0E4944)      // Deep Teal
private val HeaderBottomColor = Color(0xFF16605A)   // Lighter Teal
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val CardWhite = Color(0xFFFFFFFF)           // Card surface
private val StatTextColor = Color(0xFF1F2937)       // Dark charcoal text
private val ButtonColor = Color(0xFF76DCB0)         // Mint accent
private val ErrorColor = Color(0xFFEF4444)          // Error red
private val BorderColor = Color(0xFF16605A)         // Lighter teal border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDependentScreen(
    navController: NavController,
    context: Context,
    dependentId: String
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var originalDependent by remember { mutableStateOf<Dependent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Form fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var otherRelationship by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    // Validation error states
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }
    var genderError by remember { mutableStateOf<String?>(null) }
    var relationshipError by remember { mutableStateOf<String?>(null) }
    var otherRelationshipError by remember { mutableStateOf<String?>(null) }
    
    // Dropdown state
    var relationshipExpanded by remember { mutableStateOf(false) }
    
    val standardRelationships = listOf("Child", "Infant", "Spouse", "Parent", "Sibling", "Grandparent", "Other")

    // Load dependent data
    LaunchedEffect(dependentId) {
        scope.launch {
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                val deps = DependentRepository.getDependentsForParent(currentUser.uid)
                originalDependent = deps.find { it.dependentId == dependentId }
                originalDependent?.let { dep ->
                    firstName = dep.firstName
                    lastName = dep.lastName
                    dob = dep.dob
                    gender = dep.gender
                    // Check if relationship is in standard list
                    if (standardRelationships.dropLast(1).contains(dep.relationship)) {
                        relationship = dep.relationship
                    } else {
                        relationship = "Other"
                        otherRelationship = dep.relationship
                    }
                }
            }
            isLoading = false
        }
    }

    // Date picker
    val calendar = Calendar.getInstance()
    val datePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dob = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }
    }
    
    // Update date picker when dob changes
    LaunchedEffect(dob) {
        if (dob.isNotBlank()) {
            try {
                val parts = dob.split("-")
                datePicker.updateDate(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            } catch (e: Exception) { }
        }
    }
    
    fun formatDisplayDate(isoDate: String): String {
        return try {
            if (isoDate.isBlank()) ""
            else {
                val parts = isoDate.split("-")
                "${parts[2]}/${parts[1]}/${parts[0]}"
            }
        } catch (e: Exception) { isoDate }
    }
    
    fun validateForm(): Boolean {
        var isValid = true
        
        if (firstName.isBlank()) {
            firstNameError = "First name is required"
            isValid = false
        } else {
            firstNameError = null
        }
        
        if (lastName.isBlank()) {
            lastNameError = "Last name is required"
            isValid = false
        } else {
            lastNameError = null
        }
        
        if (dob.isBlank()) {
            dobError = "Date of birth is required"
            isValid = false
        } else {
            dobError = null
        }
        
        if (gender.isBlank()) {
            genderError = "Please select a gender"
            isValid = false
        } else {
            genderError = null
        }
        
        if (relationship.isBlank()) {
            relationshipError = "Please select a relationship"
            isValid = false
        } else {
            relationshipError = null
        }
        
        if (relationship == "Other" && otherRelationship.isBlank()) {
            otherRelationshipError = "Please specify the relationship"
            isValid = false
        } else {
            otherRelationshipError = null
        }
        
        return isValid
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with gradient
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(HeaderTopColor, HeaderBottomColor)
                            )
                        )
                        .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Dependent",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ButtonColor)
                }
            } else if (originalDependent == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Dependent not found",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                // Scrollable form content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp)
                        .imePadding()
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Dependent Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatTextColor
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // First Name field
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { 
                            firstName = it
                            if (it.isNotBlank()) firstNameError = null
                        },
                        label = { Text("First Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = firstNameError != null,
                        supportingText = {
                            if (firstNameError != null) {
                                Text(firstNameError!!, color = ErrorColor)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ButtonColor,
                            unfocusedBorderColor = BorderColor,
                            errorBorderColor = ErrorColor
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Last Name field
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { 
                            lastName = it
                            if (it.isNotBlank()) lastNameError = null
                        },
                        label = { Text("Last Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = lastNameError != null,
                        supportingText = {
                            if (lastNameError != null) {
                                Text(lastNameError!!, color = ErrorColor)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ButtonColor,
                            unfocusedBorderColor = BorderColor,
                            errorBorderColor = ErrorColor
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Date of Birth field
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formatDisplayDate(dob),
                            onValueChange = { },
                            label = { Text("Date of Birth *") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            isError = dobError != null,
                            supportingText = {
                                if (dobError != null) {
                                    Text(dobError!!, color = ErrorColor)
                                }
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Select date",
                                    tint = ButtonColor
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ButtonColor,
                                unfocusedBorderColor = BorderColor,
                                errorBorderColor = ErrorColor
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { 
                                    datePicker.show()
                                    dobError = null
                                }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Gender section
                    Text(
                        text = "Gender *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (genderError != null) ErrorColor else StatTextColor
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { 
                                    gender = "Male"
                                    genderError = null
                                }
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = gender == "Male",
                                onClick = { 
                                    gender = "Male"
                                    genderError = null
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ButtonColor,
                                    unselectedColor = BorderColor
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Male",
                                fontSize = 16.sp,
                                color = StatTextColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(48.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { 
                                    gender = "Female"
                                    genderError = null
                                }
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = gender == "Female",
                                onClick = { 
                                    gender = "Female"
                                    genderError = null
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ButtonColor,
                                    unselectedColor = BorderColor
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Female",
                                fontSize = 16.sp,
                                color = StatTextColor
                            )
                        }
                    }
                    
                    if (genderError != null) {
                        Text(
                            text = genderError!!,
                            fontSize = 12.sp,
                            color = ErrorColor,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Relationship dropdown
                    Text(
                        text = "Relationship *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (relationshipError != null) ErrorColor else StatTextColor
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = relationshipExpanded,
                        onExpandedChange = { relationshipExpanded = !relationshipExpanded }
                    ) {
                        OutlinedTextField(
                            value = relationship,
                            onValueChange = { },
                            readOnly = true,
                            placeholder = { Text("Select Relationship") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            isError = relationshipError != null,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = relationshipExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ButtonColor,
                                unfocusedBorderColor = BorderColor,
                                errorBorderColor = ErrorColor
                            )
                        )
                        
                        ExposedDropdownMenu(
                            expanded = relationshipExpanded,
                            onDismissRequest = { relationshipExpanded = false }
                        ) {
                            standardRelationships.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        relationship = option
                                        relationshipExpanded = false
                                        relationshipError = null
                                        if (option != "Other") {
                                            otherRelationship = ""
                                            otherRelationshipError = null
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    if (relationshipError != null) {
                        Text(
                            text = relationshipError!!,
                            fontSize = 12.sp,
                            color = ErrorColor,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                    
                    // Other relationship text field
                    if (relationship == "Other") {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = otherRelationship,
                            onValueChange = { 
                                otherRelationship = it
                                if (it.isNotBlank()) otherRelationshipError = null
                            },
                            label = { Text("Specify Relationship *") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = otherRelationshipError != null,
                            supportingText = {
                                if (otherRelationshipError != null) {
                                    Text(otherRelationshipError!!, color = ErrorColor)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ButtonColor,
                                unfocusedBorderColor = BorderColor,
                                errorBorderColor = ErrorColor
                            ),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                // Save button at bottom
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = CardWhite
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
                    ) {
                        Button(
                            onClick = {
                                if (validateForm()) {
                                    scope.launch {
                                        val currentUser = Firebase.auth.currentUser
                                        if (currentUser == null) {
                                            Toast.makeText(context, "User not signed in", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }
                                        isSaving = true
                                        val finalRelationship = if (relationship == "Other") otherRelationship else relationship
                                        val updatedDep = Dependent(
                                            dependentId = dependentId,
                                            firstName = firstName.trim(),
                                            lastName = lastName.trim(),
                                            dob = dob,
                                            gender = gender,
                                            relationship = finalRelationship,
                                            createdAt = originalDependent!!.createdAt,
                                            updatedAt = Timestamp.now()
                                        )
                                        val res = DependentRepository.updateDependent(currentUser.uid, updatedDep)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Dependent updated successfully", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        } else {
                                            Toast.makeText(context, "Failed to update dependent: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                        isSaving = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonColor,
                                disabledContainerColor = ButtonColor.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Save Changes",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
