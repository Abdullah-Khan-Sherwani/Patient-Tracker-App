package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

// Booking flow states
enum class BookingStep {
    NONE,
    SELECT_DOCTOR,
    SELECT_DATE,
    SELECT_TIME,
    CONFIRM
}

// Data models
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val actionButtons: List<ChatAction> = emptyList(),
    val doctorList: List<DoctorFull> = emptyList(),
    val dateOptions: List<String> = emptyList(),
    val timeSlots: List<String> = emptyList(),
    val bookingSummary: BookingSummary? = null,
    val messageType: MessageType = MessageType.TEXT
)

enum class MessageType {
    TEXT,
    DOCTOR_LIST,
    DATE_PICKER,
    TIME_SLOTS,
    BOOKING_SUMMARY
}

data class BookingSummary(
    val doctorName: String,
    val specialty: String,
    val date: String,
    val timeSlot: String,
    val fee: String,
    val doctorId: String
)

data class ChatAction(
    val label: String,
    val route: String = "",
    val action: (() -> Unit)? = null
)

// Color scheme
private val LightPatientBubble = Color(0xFFD4AF8C)
private val LightBotBubble = Color(0xFFF5F1ED)
private val DarkPatientBubble = Color(0xFF607D8B)
private val DarkBotBubble = Color(0xFF37474F)

@Composable
fun ChatbotScreen(navController: NavController) {
    val context = LocalContext.current
    val isDarkMode = MaterialTheme.colorScheme.background == Color(0xFF1C1B1F)
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isTyping by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Booking flow state
    var bookingStep by remember { mutableStateOf(BookingStep.NONE) }
    var selectedDoctor by remember { mutableStateOf<DoctorFull?>(null) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var allDoctors by remember { mutableStateOf<List<DoctorFull>>(emptyList()) }

    // Welcome message on first load
    LaunchedEffect(Unit) {
        delay(300)
        messages = listOf(
            ChatMessage(
                text = "Hi, I'm Medify Assistant. Ask me anything or tap a shortcut below.",
                isFromUser = false
            )
        )
        // Preload doctors
        scope.launch {
            allDoctors = fetchDoctorsFromFirestore()
        }
    }

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                        .padding(
                            top = WindowInsets.systemBars
                                .asPaddingValues()
                                .calculateTopPadding() + 8.dp,
                            start = 8.dp,
                            end = 8.dp,
                            bottom = 12.dp
                        )
                ) {
                    // Back button
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Title and subtitle
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Medify Assistant",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Here to help you anytime",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }

                    // Bot avatar
                    Surface(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "Bot",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Chat messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    ChatMessageBubble(
                        message = message,
                        isDarkMode = isDarkMode,
                        onActionClick = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // Typing indicator
                if (isTyping) {
                    item {
                        TypingIndicator(isDarkMode = isDarkMode)
                    }
                }
            }

            // Suggestion chips
            SuggestionChips(
                onChipClick = { suggestion ->
                    messageText = suggestion
                    handleSendMessage(
                        text = suggestion,
                        messages = messages,
                        onMessagesUpdate = { messages = it },
                        onTypingUpdate = { isTyping = it },
                        bookingStep = bookingStep,
                        onBookingStepChange = { bookingStep = it },
                        selectedDoctor = selectedDoctor,
                        onDoctorSelect = { selectedDoctor = it },
                        selectedDate = selectedDate,
                        onDateSelect = { selectedDate = it },
                        allDoctors = allDoctors
                    )
                    messageText = ""
                }
            )

            // Input area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "Ask something…",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 3
                    )

                    // Send button
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                handleSendMessage(
                                    text = messageText,
                                    messages = messages,
                                    onMessagesUpdate = { messages = it },
                                    onTypingUpdate = { isTyping = it },
                                    bookingStep = bookingStep,
                                    onBookingStepChange = { bookingStep = it },
                                    selectedDoctor = selectedDoctor,
                                    onDoctorSelect = { selectedDoctor = it },
                                    selectedDate = selectedDate,
                                    onDateSelect = { selectedDate = it },
                                    allDoctors = allDoctors
                                )
                                messageText = ""
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = if (messageText.isBlank())
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.primary
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (messageText.isBlank())
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    else
                                        MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isDarkMode: Boolean,
    onActionClick: (String) -> Unit
) {
    val bubbleColor = if (message.isFromUser) {
        if (isDarkMode) DarkPatientBubble else LightPatientBubble
    } else {
        if (isDarkMode) DarkBotBubble else LightBotBubble
    }

    val textColor = if (message.isFromUser) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Bot avatar for bot messages
            if (!message.isFromUser) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Message bubble
            Column(
                horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 280.dp),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                    ),
                    color = bubbleColor,
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )

                        // Action buttons
                        if (message.actionButtons.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                message.actionButtons.forEach { action ->
                                    Button(
                                        onClick = { onActionClick(action.route) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDarkMode)
                                                Color(0xFF8D6E63)
                                            else
                                                Color(0xFFD4AF8C),
                                            contentColor = if (isDarkMode)
                                                Color.White
                                            else
                                                Color(0xFF3E2723)
                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        )
                                    ) {
                                        Text(
                                            text = action.label,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Timestamp
                Text(
                    text = timeString,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TypingIndicator(isDarkMode: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bot avatar
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkMode) DarkBotBubble else LightBotBubble,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    var scale by remember { mutableStateOf(1f) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(index * 150L)
                            scale = 1.3f
                            delay(300)
                            scale = 1f
                            delay(300)
                        }
                    }
                    Surface(
                        modifier = Modifier.size(8.dp * scale),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    ) {}
                }
            }
        }
    }
}

@Composable
fun SuggestionChips(onChipClick: (String) -> Unit) {
    val suggestions = listOf(
        "Book an appointment",
        "Find a doctor",
        "View my reports",
        "Reschedule appointment",
        "What are your timings?",
        "Insurance or payment help"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { suggestion ->
            SuggestionChipItem(
                text = suggestion,
                onClick = { onChipClick(suggestion) }
            )
        }
    }
}

@Composable
fun SuggestionChipItem(text: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(text = text, fontSize = 13.sp) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

// Message handler with intelligent responses and booking flow
private fun handleSendMessage(
    text: String,
    messages: List<ChatMessage>,
    onMessagesUpdate: (List<ChatMessage>) -> Unit,
    onTypingUpdate: (Boolean) -> Unit,
    bookingStep: BookingStep,
    onBookingStepChange: (BookingStep) -> Unit,
    selectedDoctor: DoctorFull?,
    onDoctorSelect: (DoctorFull?) -> Unit,
    selectedDate: String?,
    onDateSelect: (String?) -> Unit,
    allDoctors: List<DoctorFull>
) {
    // Add user message
    val userMessage = ChatMessage(text = text, isFromUser = true)
    onMessagesUpdate(messages + userMessage)

    // Show typing indicator
    onTypingUpdate(true)

    // Simulate bot response
    kotlinx.coroutines.GlobalScope.launch {
        delay(1500)
        onTypingUpdate(false)

        val botResponse = generateBotResponse(
            query = text.lowercase(),
            bookingStep = bookingStep,
            onBookingStepChange = onBookingStepChange,
            allDoctors = allDoctors,
            selectedDoctor = selectedDoctor,
            selectedDate = selectedDate
        )
        onMessagesUpdate(messages + userMessage + botResponse)
    }
}

private fun generateBotResponse(
    query: String,
    bookingStep: BookingStep,
    onBookingStepChange: (BookingStep) -> Unit,
    allDoctors: List<DoctorFull>,
    selectedDoctor: DoctorFull?,
    selectedDate: String?
): ChatMessage {
    // Check for booking intent
    val bookingKeywords = listOf("book", "appointment", "need appointment", "want to see", 
        "consult", "schedule", "visit", "see doctor", "need doctor")
    val isBookingIntent = bookingKeywords.any { query.contains(it) }
    
    return when {
        isBookingIntent && bookingStep == BookingStep.NONE -> {
            onBookingStepChange(BookingStep.SELECT_DOCTOR)
            ChatMessage(
                text = "Sure! Who would you like to book an appointment with?",
                isFromUser = false,
                messageType = MessageType.DOCTOR_LIST,
                doctorList = allDoctors
            )
        }
        query.contains("doctor") || query.contains("find") || query.contains("search") -> {
            ChatMessage(
                text = "Let me help you find the right doctor for your needs.",
                isFromUser = false,
                actionButtons = listOf(
                    ChatAction("Browse Doctors", "doctor_catalogue")
                )
            )
        }
        query.contains("report") || query.contains("history") || query.contains("medical") -> {
            ChatMessage(
                text = "You can view your medical reports and history here.",
                isFromUser = false,
                actionButtons = listOf(
                    ChatAction("View Medical History", "patient_reports")
                )
            )
        }
        query.contains("reschedule") || query.contains("cancel") -> {
            ChatMessage(
                text = "You can manage your appointments and reschedule as needed.",
                isFromUser = false,
                actionButtons = listOf(
                    ChatAction("View Appointments", "patient_appointments")
                )
            )
        }
        query.contains("timing") || query.contains("hour") || query.contains("open") -> {
            ChatMessage(
                text = "Our doctors are available 24/7 for emergency consultations. Regular appointments are from 9 AM to 9 PM daily.",
                isFromUser = false
            )
        }
        query.contains("insurance") || query.contains("payment") || query.contains("cost") -> {
            ChatMessage(
                text = "We accept most major insurance plans and offer flexible payment options. You can view payment details in your profile.",
                isFromUser = false,
                actionButtons = listOf(
                    ChatAction("Go to Profile", "patient_profile")
                )
            )
        }
        query.contains("help") || query.contains("support") -> {
            ChatMessage(
                text = "I'm here to assist you with:\n• Booking appointments\n• Finding doctors\n• Viewing reports\n• Managing your profile\n\nWhat would you like help with?",
                isFromUser = false
            )
        }
        else -> {
            ChatMessage(
                text = "I'm here to learn and help. Try asking about:\n• Booking appointments\n• Finding doctors\n• Viewing reports\n• Timings and payments",
                isFromUser = false
            )
        }
    }
}

// Helper functions for booking flow
fun generateAvailableDates(doctor: DoctorFull): List<String> {
    if (doctor.days.isEmpty()) return emptyList()
    
    val availableDays = doctor.days.split(",").map { it.trim().lowercase() }
    val dates = mutableListOf<String>()
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var currentDate = LocalDate.now()
    
    // Generate next 14 days that match doctor's available days
    var count = 0
    while (count < 14 && dates.size < 7) {
        val dayName = currentDate.dayOfWeek.toString().lowercase()
        if (availableDays.any { dayName.startsWith(it) }) {
            dates.add(currentDate.format(dateFormatter))
        }
        currentDate = currentDate.plusDays(1)
        count++
    }
    
    return dates
}

fun generateTimeSlots(doctor: DoctorFull): List<String> {
    if (doctor.timings.isEmpty()) return emptyList()
    
    // Parse timings like "09:00-17:00" or "9 AM - 5 PM"
    val slots = mutableListOf<String>()
    try {
        val timeParts = doctor.timings.split("-").map { it.trim() }
        if (timeParts.size == 2) {
            // Generate hourly slots
            slots.addAll(listOf(
                "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
                "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"
            ))
        }
    } catch (e: Exception) {
        // Default slots
        slots.addAll(listOf(
            "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
            "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"
        ))
    }
    
    return slots
}

suspend fun bookAppointment(
    patientId: String,
    doctorId: String,
    date: String,
    timeSlot: String
): Boolean {
    return try {
        val db = Firebase.firestore
        val appointmentData = hashMapOf(
            "patientId" to patientId,
            "doctorId" to doctorId,
            "date" to date,
            "timeSlot" to timeSlot,
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )
        
        db.collection("appointments")
            .add(appointmentData)
            .await()
        
        true
    } catch (e: Exception) {
        false
    }
}
