package dev.atick.feature.home.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class AppointmentForm(
    val specialty: String,
    val doctor: String,
    val date: LocalDate,
    val time: LocalTime,
    val note: String
)

@Composable
fun ScheduleAppointmentRoute(
    onBack: () -> Unit,
    onSubmit: (AppointmentForm) -> Unit
) {
    ScheduleAppointmentScreen(onBack, onSubmit)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleAppointmentScreen(
    onBack: () -> Unit,
    onSubmit: (AppointmentForm) -> Unit
) {
    // v1: static data; replace with VM-provided lists later
    val specialties = listOf("General Medicine", "Cardiology", "Dermatology")
    val doctors = listOf("Dr. Ayesha", "Dr. Hamid", "Dr. Sana")

    var specialty by remember { mutableStateOf(specialties.first()) }
    var doctor by remember { mutableStateOf(doctors.first()) }

    // Date/Time state (M3 pickers)
    var date by remember { mutableStateOf(LocalDate.now()) }
    var openDatePicker by remember { mutableStateOf(false) }

    var time by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var openTimePicker by remember { mutableStateOf(false) }

    var note by remember { mutableStateOf("") }

    val dateFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Appointment") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Specialty
            var spExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = spExpanded,
                onExpandedChange = { spExpanded = it }
            ) {
                OutlinedTextField(
                    value = specialty,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Specialty") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = spExpanded, onDismissRequest = { spExpanded = false }) {
                    specialties.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { specialty = option; spExpanded = false }
                        )
                    }
                }
            }

            // Doctor
            var drExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = drExpanded,
                onExpandedChange = { drExpanded = it }
            ) {
                OutlinedTextField(
                    value = doctor,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Doctor") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = drExpanded, onDismissRequest = { drExpanded = false }) {
                    doctors.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { doctor = option; drExpanded = false }
                        )
                    }
                }
            }

            // Date picker (dialog)
            OutlinedTextField(
                value = date.format(dateFmt),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { openDatePicker = true }) { Text("Pick") }
                }
            )

            if (openDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { openDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { openDatePicker = false }) { Text("OK") }
                    }
                ) {
                    val state = rememberDatePickerState(
                        initialSelectedDateMillis = date.atStartOfDay(
                            java.time.ZoneId.systemDefault()
                        ).toInstant().toEpochMilli()
                    )
                    DatePicker(
                        state = state,
                        showModeToggle = false
                    )
                    // Apply on confirm
                    LaunchedEffect(state.selectedDateMillis) {
                        // Only update when user changes
                        state.selectedDateMillis?.let {
                            date = LocalDate.ofEpochDay(it / 86_400_000L)
                        }
                    }
                }
            }

            // Time picker (dialog)
            OutlinedTextField(
                value = time.format(timeFmt),
                onValueChange = {},
                readOnly = true,
                label = { Text("Time") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { openTimePicker = true }) { Text("Pick") }
                }
            )

            if (openTimePicker) {
                val state = rememberTimePickerState(
                    initialHour = time.hour,
                    initialMinute = time.minute,
                    is24Hour = true
                )
                AlertDialog(
                    onDismissRequest = { openTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            time = LocalTime.of(state.hour, state.minute)
                            openTimePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { openTimePicker = false }) { Text("Cancel") }
                    },
                    text = { TimePicker(state = state) }
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    onSubmit(
                        AppointmentForm(
                            specialty = specialty,
                            doctor = doctor,
                            date = date,
                            time = time,
                            note = note.trim()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Book Appointment") }
        }
    }
}
