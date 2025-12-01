package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.util.Log
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
import com.example.patienttracker.data.AppointmentRepository
import com.example.patienttracker.data.NotificationRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.Timestamp
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
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

// SESSION CONTEXT MEMORY - NEW
data class SessionContext(
    var patientName: String = "",
    var lastSelectedDoctor: DoctorFull? = null,
    var lastSelectedDate: String? = null,
    var lastSelectedTime: String? = null,
    var mentionedSymptoms: MutableList<String> = mutableListOf(),
    var preferredSpecialty: String? = null,
    var preferredTimeOfDay: String? = null, // "morning" or "evening"
    var lastAppointmentId: String? = null,
    var conversationHistory: MutableList<String> = mutableListOf() // For context awareness
)

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
    val messageType: MessageType = MessageType.TEXT,
    val isEmergency: Boolean = false // NEW: for emergency detection
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
    val action: (() -> Unit)? = null,
    val messageAction: ((addMessage: (ChatMessage) -> Unit) -> Unit)? = null
)

// ============================================================
// Deep Teal & Mint Design System - Chatbot colors
// ============================================================
private val LightPatientBubble = Color(0xFF0E4944)    // Deep Teal for patient
private val LightBotBubble = Color(0xFFE8F5F3)        // Light teal for bot
private val DarkPatientBubble = Color(0xFF76DCB0)     // Mint for patient in dark
private val DarkBotBubble = Color(0xFF1A3D38)         // Dark teal for bot

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
    
    // NEW: Session context memory for proactive assistance
    val sessionContext = remember { SessionContext() }

    // Welcome message on first load - NOW WITH PERSONALIZATION
    LaunchedEffect(Unit) {
        delay(300)
        
        // Get patient name from Firebase
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            try {
                val userDoc = Firebase.firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                sessionContext.patientName = userDoc.getString("firstName") ?: "there"
            } catch (e: Exception) {
                sessionContext.patientName = "there"
            }
        }
        
        val greeting = if (sessionContext.patientName.isNotEmpty() && sessionContext.patientName != "there") {
            "Hi ${sessionContext.patientName}, I'm Medify Assistant. I'm here to help you anytime."
        } else {
            "Hi, I'm Medify Assistant. I'm here to help you anytime."
        }
        
        messages = listOf(
            ChatMessage(
                text = greeting,
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
                            if (route.isNotBlank()) {
                                navController.navigate(route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onInlineAction = { action ->
                            // Execute inline action and refresh UI
                            action?.invoke()
                        },
                        onMessageAction = { messageAction ->
                            // Execute message action with addMessage callback
                            messageAction?.invoke { newMessage ->
                                messages = messages + newMessage
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
                        allDoctors = allDoctors,
                        sessionContext = sessionContext, // NEW
                        navController = navController // NEW
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

                    // Mic button (placeholder - not implemented)
                    IconButton(
                        onClick = {
                            // TODO: Implement voice input
                            android.widget.Toast.makeText(
                                context,
                                "Voice input coming soon!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

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
                                    allDoctors = allDoctors,
                                    sessionContext = sessionContext, // NEW
                                    navController = navController // NEW
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
    onActionClick: (String) -> Unit,
    onInlineAction: (((() -> Unit)?) -> Unit)? = null,
    onMessageAction: ((((addMessage: (ChatMessage) -> Unit) -> Unit)?) -> Unit)? = null
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
                                        onClick = { 
                                            // Execute messageAction if available (for adding messages)
                                            if (action.messageAction != null && onMessageAction != null) {
                                                onMessageAction(action.messageAction)
                                            } else if (action.action != null && onInlineAction != null) {
                                                onInlineAction(action.action)
                                            } else if (action.route.isNotBlank()) {
                                                onActionClick(action.route)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDarkMode)
                                                Color(0xFF16605A)    // Lighter Teal in dark mode
                                            else
                                                Color(0xFF76DCB0),   // Mint accent
                                            contentColor = if (isDarkMode)
                                                Color.White
                                            else
                                                Color(0xFF0E4944)    // Deep Teal text
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

// Message handler with intelligent responses and booking flow - UPGRADED
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
    allDoctors: List<DoctorFull>,
    sessionContext: SessionContext, // NEW
    navController: NavController // NEW
) {
    // Add user message
    val userMessage = ChatMessage(text = text, isFromUser = true)
    onMessagesUpdate(messages + userMessage)
    
    // Update conversation history for context awareness
    sessionContext.conversationHistory.add(text.lowercase())

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
            selectedDate = selectedDate,
            sessionContext = sessionContext, // NEW
            navController = navController, // NEW
            messages = messages, // NEW: For inline actions
            onMessagesUpdate = onMessagesUpdate, // NEW: For inline actions
            onTypingUpdate = onTypingUpdate // NEW: For inline actions
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
    selectedDate: String?,
    sessionContext: SessionContext, // NEW
    navController: NavController, // NEW
    messages: List<ChatMessage>, // NEW: For inline actions
    onMessagesUpdate: (List<ChatMessage>) -> Unit, // NEW: For inline actions
    onTypingUpdate: (Boolean) -> Unit // NEW: For inline actions
): ChatMessage {
    
    // ============================================
    // 1. EMERGENCY DETECTION (HIGHEST PRIORITY)
    // ============================================
    val emergencyKeywords = listOf(
        "severe bleeding", "not breathing", "can't breathe", "heart attack", 
        "chest pain severe", "suicidal", "suicide", "kill myself", 
        "stroke", "unconscious", "seizure", "choking", "severe burn"
    )
    
    if (emergencyKeywords.any { query.contains(it) }) {
        return ChatMessage(
            text = "⚠️ This may be an emergency.\n\nPlease call your local emergency number (911 or 112) or visit the nearest hospital immediately.\n\nDo not wait for an appointment.",
            isFromUser = false,
            isEmergency = true
        )
    }
    
    // ============================================
    // 1.5 URGENT AUTO-BOOKING (NEW FEATURE)
    // ============================================
    val urgencyKeywords = listOf(
        "urgent", "asap", "immediately", "emergency", "nearest appointment", 
        "book soon", "book now", "quick", "fast", "earliest", "today", "right now"
    )
    
    val hasUrgency = urgencyKeywords.any { query.contains(it) }
    
    // ============================================
    // 2. SYMPTOM-BASED HELP (NO DIAGNOSIS)
    // ============================================
    val symptomToSpecialty = mapOf(
    // ❤️ Heart & chest
    "chest pain" to "cardiologist",
    "heart pain" to "cardiologist",
    "palpitations" to "cardiologist",
    "high blood pressure" to "cardiologist",
    "bp" to "cardiologist",

    // 🫁 Lungs & breathing
    "breathing" to "pulmonologist",
    "shortness of breath" to "pulmonologist",
    "cough" to "pulmonologist",
    "asthma" to "pulmonologist",
    "lung" to "pulmonologist",

    // 🧠 Brain & nerves
    "headache" to "neurologist",
    "migraine" to "neurologist",
    "memory loss" to "neurologist",
    "dizziness" to "neurologist",
    "seizure" to "neurologist",

    // 🧠 Mental health
    "anxiety" to "psychiatrist",
    "depression" to "psychiatrist",
    "panic" to "psychiatrist",
    "stress" to "psychiatrist",
    "insomnia" to "psychiatrist",

    // 🍽 Stomach & digestion
    "stomach" to "gastroenterologist",
    "abdomen" to "gastroenterologist",
    "acidity" to "gastroenterologist",
    "bloating" to "gastroenterologist",
    "indigestion" to "gastroenterologist",
    "constipation" to "gastroenterologist",

    // 🧴 Skin & hair
    "skin" to "dermatologist",
    "rash" to "dermatologist",
    "hair loss" to "dermatologist",
    "acne" to "dermatologist",
    "eczema" to "dermatologist",
    "psoriasis" to "dermatologist",

    // 👁 Eyes
    "eye pain" to "ophthalmologist",
    "blurred vision" to "ophthalmologist",
    "itchy eyes" to "ophthalmologist",
    "red eyes" to "ophthalmologist",

    // 👂 ENT (ear, nose, throat)
    "ear pain" to "ent specialist",
    "hearing loss" to "ent specialist",
    "sinus" to "ent specialist",
    "runny nose" to "ent specialist",
    "tonsils" to "ent specialist",

    // 🦴 Bones & joints
    "joint pain" to "orthopedic",
    "back pain" to "orthopedic",
    "bone fracture" to "orthopedic",
    "knee pain" to "orthopedic",
    "shoulder pain" to "orthopedic",

    // 🤰 Women health
    "pregnancy" to "gynecologist",
    "period pain" to "gynecologist",
    "irregular periods" to "gynecologist",
    "pcos" to "gynecologist",
    "fertility" to "gynecologist",

    // 🧒 Children
    "child fever" to "pediatrician",
    "baby cough" to "pediatrician",
    "child vomiting" to "pediatrician",
    "baby rash" to "pediatrician",

    // 🦷 Teeth
    "tooth pain" to "dentist",
    "bleeding gums" to "dentist",
    "bad breath" to "dentist",
    "cavity" to "dentist",

    // 🚻 Urinary & kidneys
    "burning urination" to "urologist",
    "kidney pain" to "urologist",
    "urine infection" to "urologist",
    "uti" to "urologist",

    // 🎗 Cancer concerns
    "sudden lumps" to "oncologist",
    "unexplained weight loss" to "oncologist",
    "persistent fatigue" to "oncologist",

    // 🔥 General
    "fever" to "general physician",
    "cold" to "general physician",
    "flu" to "general physician",
    "weakness" to "general physician",
    "body pain" to "general physician",
    "fatigue" to "general physician"
)

    
    for ((symptom, specialty) in symptomToSpecialty) {
        if (query.contains(symptom)) {
            sessionContext.mentionedSymptoms.add(symptom)
            sessionContext.preferredSpecialty = specialty
            
            val specialistDoctors = allDoctors.filter { 
                it.speciality.lowercase().contains(specialty) 
            }
            
            // ========================================
            // URGENT AUTO-BOOKING TRIGGER
            // ========================================
            if (hasUrgency && specialistDoctors.isNotEmpty()) {
                // Synchronously find best available slot
                val bookingResult = kotlinx.coroutines.runBlocking {
                    findBestUrgentSlot(
                        specialistDoctors = specialistDoctors,
                        specialty = specialty,
                        symptom = symptom,
                        sessionContext = sessionContext
                    )
                }
                
                return if (bookingResult.isDataAccessError) {
                    // Technical issue - couldn't verify data
                    ChatMessage(
                        text = "I'm having trouble accessing doctor availability data. Please use the booking screen:",
                        isFromUser = false,
                        actionButtons = listOf(
                            ChatAction("Go to Booking", "doctor_catalogue")
                        )
                    )
                } else if (bookingResult.success && bookingResult.needsConfirmation) {
                    // Found best slot - request confirmation with inline actions
                    ChatMessage(
                        text = "I found the earliest available appointment:\n\n" +
                               "• Doctor: ${bookingResult.doctorName}\n" +
                               "• Speciality: ${specialty.replaceFirstChar { it.uppercase() }}\n" +
                               "• Date: ${bookingResult.date}\n" +
                               "• Time: ${bookingResult.timeBlock} (${bookingResult.timeRange})\n" +
                               "• Consultation Fee: PKR ${bookingResult.fee}\n\n" +
                               "⚠️ Note: By confirming this booking, you consent to allow the doctor to access your medical records for consultation purposes.",
                        isFromUser = false,
                        actionButtons = listOf(
                            ChatAction(
                                label = "Confirm Booking",
                                route = "",
                                messageAction = { addMessage ->
                                    // MESSAGE ACTION: Execute booking confirmation and add result to chat
                                    onTypingUpdate(true)
                                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        val currentUser = Firebase.auth.currentUser
                                        if (currentUser != null) {
                                            val confirmResult = confirmUrgentBooking(
                                                patientId = currentUser.uid,
                                                patientName = sessionContext.patientName,
                                                doctorId = bookingResult.doctorId!!,
                                                doctorName = bookingResult.doctorName!!,
                                                speciality = specialty.replaceFirstChar { it.uppercase() },
                                                date = bookingResult.date!!,
                                                timeBlock = bookingResult.timeBlock!!,
                                                symptom = symptom
                                            )
                                            
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                onTypingUpdate(false)
                                                val confirmationMessage = if (confirmResult.success) {
                                                    ChatMessage(
                                                        text = "✅ Your urgent appointment has been confirmed!\n\n" +
                                                               "• Doctor: ${confirmResult.doctorName}\n" +
                                                               "• Speciality: ${specialty.replaceFirstChar { it.uppercase() }}\n" +
                                                               "• Date: ${confirmResult.date}\n" +
                                                               "• Time Block: ${confirmResult.timeBlock}\n" +
                                                               "• Appointment #: ${confirmResult.appointmentNumber}\n\n" +
                                                               "You can view details in your appointments.",
                                                        isFromUser = false,
                                                        actionButtons = listOf(
                                                            ChatAction("View Appointment", "full_schedule"),
                                                            ChatAction("Back to Dashboard", "patient_home")
                                                        )
                                                    )
                                                } else {
                                                    ChatMessage(
                                                        text = "Sorry, there was an error confirming your booking: ${confirmResult.errorMessage}\n\nPlease try booking manually.",
                                                        isFromUser = false,
                                                        actionButtons = listOf(
                                                            ChatAction("Go to Booking", "doctor_catalogue")
                                                        )
                                                    )
                                                }
                                                addMessage(confirmationMessage)
                                            }
                                        }
                                    }
                                }
                            ),
                            ChatAction(
                                label = "Cancel",
                                route = "",
                                messageAction = { addMessage ->
                                    // MESSAGE ACTION: Show cancellation message
                                    val cancelMessage = ChatMessage(
                                        text = "Booking cancelled. How else can I help you?",
                                        isFromUser = false,
                                        actionButtons = listOf(
                                            ChatAction("Find Another Doctor", "doctor_catalogue"),
                                            ChatAction("View My Reports", "patient_reports")
                                        )
                                    )
                                    addMessage(cancelMessage)
                                }
                            )
                        )
                    )
                } else {
                    // No slots available in next 7 days
                    ChatMessage(
                        text = "I checked all ${specialty}s for the next 7 days, but all slots are currently booked.\n\nHere are your options:",
                        isFromUser = false,
                        actionButtons = listOf(
                            ChatAction("View All ${specialty}s", "doctor_catalogue"),
                            ChatAction("Try Different Specialty", "doctor_catalogue")
                        )
                    )
                }
            }
            
            // Normal symptom-based suggestion (no urgency)
            return ChatMessage(
                text = "I can't provide medical diagnosis, but ${symptom.replace("_", " ")} concerns are usually treated by ${specialty}s.\n\nWould you like to book an appointment with one?",
                isFromUser = false,
                actionButtons = if (specialistDoctors.isNotEmpty()) {
                    listOf(ChatAction("Yes, show ${specialty}s", "doctor_catalogue?specialty=$specialty"))
                } else {
                    listOf(ChatAction("Browse All Doctors", "doctor_catalogue"))
                }
            )
        }
    }
    
    // ============================================
    // 2.5 DEPENDENT/CHILD BOOKING (NEW FEATURE)
    // ============================================
    val dependentKeywords = listOf(
        "child", "kid", "baby", "son", "daughter", "children", "kids", 
        "dependent", "infant", "toddler", "nephew", "niece", "family member",
        "book for my child", "appointment for my child", "my son", "my daughter",
        "for my kid", "for child", "for baby"
    )
    
    val isDependentBooking = dependentKeywords.any { query.contains(it) }
    
    if (isDependentBooking) {
        return ChatMessage(
            text = "Great! I can help you book an appointment for your child or dependent.\n\n" +
                   "First, let's set up or select your dependent. Once added, you'll be able to:\n" +
                   "• Book appointments for them\n" +
                   "• View their medical records\n" +
                   "• Track their appointment history\n\n" +
                   "Let me take you to your dependents section.",
            isFromUser = false,
            actionButtons = listOf(
                ChatAction("Family Members", "patient_dependents"),
                ChatAction("Browse Doctors Instead", "doctor_catalogue")
            )
        )
    }
    
    // ============================================
    // 3. CONTEXT-AWARE BOOKING REUSE
    // ============================================
    val bookingKeywords = listOf("book", "appointment", "need appointment", "want to see", 
        "consult", "schedule", "visit", "see doctor", "need doctor", "book again", "same doctor")
    val isBookingIntent = bookingKeywords.any { query.contains(it) }
    
    if (isBookingIntent) {
        // Check if user wants to reuse previous doctor
        if ((query.contains("again") || query.contains("same")) && sessionContext.lastSelectedDoctor != null) {
            val doctor = sessionContext.lastSelectedDoctor!!
            return ChatMessage(
                text = "Got it! Booking with ${doctor.firstName} ${doctor.lastName} (${doctor.speciality}) again.\n\nWhen would you like to see them?",
                isFromUser = false,
                actionButtons = listOf(
                    ChatAction("Choose Date", "doctor_catalogue"),
                    ChatAction("Different Doctor", "doctor_catalogue")
                )
            )
        }
        
        // Proactive suggestion based on previous specialty
        if (sessionContext.preferredSpecialty != null && bookingStep == BookingStep.NONE) {
            val matchingDoctors = allDoctors.filter { 
                it.speciality.lowercase().contains(sessionContext.preferredSpecialty!!) 
            }
            
            if (matchingDoctors.isNotEmpty()) {
                onBookingStepChange(BookingStep.SELECT_DOCTOR)
                return ChatMessage(
                    text = "Since you were looking for ${sessionContext.preferredSpecialty}s, I found ${matchingDoctors.size} available.\n\nWho would you like to book?",
                    isFromUser = false,
                    actionButtons = listOf(
                        ChatAction("Show ${sessionContext.preferredSpecialty}s", "doctor_catalogue")
                    )
                )
            }
        }
        
        // Standard booking flow
        if (bookingStep == BookingStep.NONE) {
            onBookingStepChange(BookingStep.SELECT_DOCTOR)
            return ChatMessage(
                text = "Sure! Let me help you book an appointment.\n\nWho would you like to see?",
                isFromUser = false,
                actionButtons = listOf(
                    ChatAction("Browse Doctors", "doctor_catalogue")
                )
            )
        }
    }
    
    // ============================================
    // 4. SMART CLARIFICATION (NO ERROR MESSAGES)
    // ============================================
    val needsClarification = !query.contains("book") && !query.contains("doctor") && 
                             !query.contains("report") && !query.contains("timing") &&
                             !query.contains("help") && !query.contains("insurance") &&
                             !query.contains("cancel") && !query.contains("reschedule") &&
                             query.length > 3
    
    if (needsClarification) {
        val suggestions = mutableListOf<String>()
        
        if (query.contains("time") || query.contains("when")) {
            suggestions.add("What are your timings?")
        }
        if (query.contains("cost") || query.contains("pay") || query.contains("price")) {
            suggestions.add("Insurance or payment help")
        }
        if (query.contains("appointment")) {
            suggestions.add("Book an appointment")
            suggestions.add("Reschedule appointment")
        }
        
        if (suggestions.isEmpty()) {
            suggestions.addAll(listOf(
                "Book an appointment",
                "Find a doctor",
                "View my reports"
            ))
        }
        
        return ChatMessage(
            text = "I think you're asking about one of these:",
            isFromUser = false,
            actionButtons = suggestions.map { ChatAction(it, "") }
        )
    }
    
    // ============================================
    // 5. EXISTING INTENTS (ENHANCED WITH TONE)
    // ============================================
    return when {
        query.contains("doctor") || query.contains("find") || query.contains("search") -> {
            val personalizedText = if (sessionContext.patientName.isNotEmpty()) {
                "Let me help you find the right doctor for your needs, ${sessionContext.patientName}."
            } else {
                "Let me help you find the right doctor for your needs."
            }
            ChatMessage(
                text = personalizedText,
                isFromUser = false,
                actionButtons = listOf(
                    ChatAction("Browse Doctors", "doctor_catalogue")
                )
            )
        }
        query.contains("report") || query.contains("history") || query.contains("medical") || query.contains("record") -> {
            // PROACTIVE: Ask if they want to discuss with doctor
            val hasRecentReports = sessionContext.conversationHistory.any { 
                it.contains("upload") || it.contains("record") 
            }
            
            val proactiveText = if (hasRecentReports) {
                "You can view your medical reports here.\n\nWould you like to discuss them with a doctor?"
            } else {
                "You can view your medical reports and history here."
            }
            
            ChatMessage(
                text = proactiveText,
                isFromUser = false,
                actionButtons = listOf(
                    ChatAction("View Medical History", "patient_reports"),
                    ChatAction("Book Consultation", "doctor_catalogue")
                )
            )
        }
        query.contains("reschedule") || query.contains("cancel") -> {
            // PROACTIVE: Suggest rescheduling instead of just canceling
            ChatMessage(
                text = "I can help you manage your appointments.\n\nWould you like to reschedule or cancel?",
                isFromUser = false,
                actionButtons = listOf(
                    ChatAction("View Appointments", "full_schedule"),
                    ChatAction("Book New Appointment", "doctor_catalogue")
                )
            )
        }
        query.contains("timing") || query.contains("hour") || query.contains("open") || query.contains("available") -> {
            ChatMessage(
                text = "Our doctors are available 24/7 for emergency consultations.\n\nRegular appointments: 9 AM to 9 PM daily.",
                isFromUser = false,
                actionButtons = listOf(
                    ChatAction("Book Appointment", "doctor_catalogue")
                )
            )
        }
        query.contains("insurance") || query.contains("payment") || query.contains("cost") || query.contains("fee") -> {
            ChatMessage(
                text = "We accept most major insurance plans and offer flexible payment options.\n\nYou can view detailed payment information in your profile.",
                isFromUser = false,
                actionButtons = listOf(
                    ChatAction("Go to Profile", "patient_profile"),
                    ChatAction("Book Appointment", "doctor_catalogue")
                )
            )
        }
        query.contains("help") || query.contains("support") || query.contains("assist") -> {
            val helpText = if (sessionContext.patientName.isNotEmpty()) {
                "${sessionContext.patientName}, I'm here to assist you with:"
            } else {
                "I'm here to assist you with:"
            }
            ChatMessage(
                text = "$helpText\n• Booking appointments\n• Finding doctors\n• Viewing reports\n• Managing your profile\n\nWhat would you like help with?",
                isFromUser = false
            )
        }
        query.contains("thank") || query.contains("thanks") -> {
            ChatMessage(
                text = "You're welcome! Is there anything else I can help you with?",
                isFromUser = false
            )
        }
        query.contains("yes") || query.contains("okay") || query.contains("sure") -> {
            ChatMessage(
                text = "Great! What would you like to do next?",
                isFromUser = false,
                actionButtons = listOf(
                    ChatAction("Book Appointment", "doctor_catalogue"),
                    ChatAction("Find Doctor", "doctor_catalogue"),
                    ChatAction("View Reports", "patient_reports")
                )
            )
        }
        else -> {
            // Friendly fallback with suggestions
            ChatMessage(
                text = "I'm here to learn and help you with medical appointments and records.\n\nTry asking about:",
                isFromUser = false,
                actionButtons = listOf(
                    ChatAction("Book Appointment", "doctor_catalogue"),
                    ChatAction("Find Doctor", "doctor_catalogue"),
                    ChatAction("View Reports", "patient_reports"),
                    ChatAction("Timings & Payments", "patient_profile")
                )
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

// ============================================
// URGENT AUTO-BOOKING ENGINE
// ============================================
data class UrgentBookingResult(
    val success: Boolean,
    val needsConfirmation: Boolean = false,
    val appointmentNumber: String? = null,
    val doctorId: String? = null,
    val doctorName: String? = null,
    val date: String? = null,
    val timeBlock: String? = null,
    val timeRange: String? = null,
    val fee: String? = null,
    val errorMessage: String? = null,
    val isDataAccessError: Boolean = false
)

data class AvailableSlot(
    val doctor: DoctorFull,
    val date: String,
    val dateObj: LocalDate,
    val timeBlock: TimeBlockDef,
    val timeRange: String,
    val hours: Int,
    val bookedCount: Int,
    val capacity: Int
)

/**
 * NEW ALGORITHM: Find best available urgent slot with confirmation flow
 * 1. Validate timing data exists for all doctors
 * 2. Collect ALL available slots across all doctors for next 7 days
 * 3. Sort by: earliest date → earliest time block → lowest booking load
 * 4. Return best match for user confirmation (does NOT auto-book)
 */
suspend fun findBestUrgentSlot(
    specialistDoctors: List<DoctorFull>,
    specialty: String,
    symptom: String,
    sessionContext: SessionContext
): UrgentBookingResult {
    try {
        Log.d("UrgentBooking", "========================================")
        Log.d("UrgentBooking", "Starting urgent booking search for $specialty")
        Log.d("UrgentBooking", "Total specialist doctors: ${specialistDoctors.size}")
        
        val db = Firebase.firestore
        val currentUser = Firebase.auth.currentUser 
            ?: return UrgentBookingResult(false, errorMessage = "User not authenticated")
        
        // STEP 1: Validate timing data exists and is parseable
        specialistDoctors.forEach { doctor ->
            Log.d("UrgentBooking", "Doctor: ${doctor.firstName} ${doctor.lastName}")
            Log.d("UrgentBooking", "  - Speciality: '${doctor.speciality}'")
            Log.d("UrgentBooking", "  - Timings: '${doctor.timings}'")
            Log.d("UrgentBooking", "  - Days: '${doctor.days}'")
            Log.d("UrgentBooking", "  - Valid: ${doctor.timings.isNotBlank() && doctor.timings.contains("-") && doctor.days.isNotBlank()}")
        }
        
        val doctorsWithValidTimings = specialistDoctors.filter { doctor ->
            doctor.timings.isNotBlank() && 
            doctor.timings.contains("-") && 
            doctor.days.isNotBlank()
        }
        
        Log.d("UrgentBooking", "Doctors with valid timings: ${doctorsWithValidTimings.size}")
        
        // If no doctors have timing data, use all doctors with default timings
        val doctorsToCheck = if (doctorsWithValidTimings.isEmpty()) {
            Log.w("UrgentBooking", "⚠️ No doctors with timing data, using default timings for all")
            specialistDoctors.map { doctor ->
                DoctorFull(
                    id = doctor.id,
                    firstName = doctor.firstName,
                    lastName = doctor.lastName,
                    email = doctor.email,
                    phone = doctor.phone,
                    speciality = doctor.speciality,
                    days = "monday,tuesday,wednesday,thursday,friday", // Default working days
                    timings = "09:00-17:00" // Default 9 AM to 5 PM
                )
            }
        } else {
            doctorsWithValidTimings
        }
        
        // Time blocks definition
        val timeBlocks = listOf(
            TimeBlockDef("Morning", 6, 12),
            TimeBlockDef("Afternoon", 12, 17),
            TimeBlockDef("Evening", 17, 21),
            TimeBlockDef("Night", 21, 24)
        )
        
        // STEP 2: Collect ALL available slots across all doctors
        val allAvailableSlots = mutableListOf<AvailableSlot>()
        val today = LocalDate.now()
        
        for (dayOffset in 0..6) {
            val checkDate = today.plusDays(dayOffset.toLong())
            val dateStr = checkDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val dayOfWeekNumber = checkDate.dayOfWeek.value // 1=Monday, 7=Sunday
            
            for (doctor in doctorsToCheck) {
                // REAL AVAILABILITY: Fetch from doctor_availability collection for this day
                val availabilitySnapshot = db.collection("doctor_availability")
                    .whereEqualTo("doctorUid", doctor.id)
                    .whereEqualTo("dayOfWeek", dayOfWeekNumber)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                
                // If no real availability data, skip this doctor for this day
                if (availabilitySnapshot.isEmpty) continue
                
                // Parse real availability times
                val availDoc = availabilitySnapshot.documents.first()
                val doctorStart = availDoc.getString("startTime") ?: continue
                val doctorEnd = availDoc.getString("endTime") ?: continue
                
                // Check each time block
                for (block in timeBlocks) {
                    // Calculate if doctor is available in this block
                    val (overlapStart, overlapEnd, hours) = calculateUrgentBlockAvailability(
                        doctorStart = doctorStart,
                        doctorEnd = doctorEnd,
                        blockStartHour = block.startHour,
                        blockEndHour = block.endHour
                    )
                    
                    if (overlapStart == null || hours == 0) continue
                    
                    // Query existing appointments for this doctor, date, and block
                    val blockQuery = db.collection("appointments")
                        .whereEqualTo("doctorUid", doctor.id)
                        .whereEqualTo("appointmentDate", dateStr)
                        .whereEqualTo("timeSlot", block.name)
                        .whereIn("status", listOf("scheduled", "confirmed", "pending"))
                        .get()
                        .await()
                    
                    val bookedCount = blockQuery.size()
                    val capacity = hours.toInt() * 4 // 4 patients per hour
                    
                    if (bookedCount >= capacity) continue // Block is full
                    
                    // Add to available slots list
                    allAvailableSlots.add(AvailableSlot(
                        doctor = doctor,
                        date = dateStr,
                        dateObj = checkDate,
                        timeBlock = block,
                        timeRange = "$overlapStart - $overlapEnd",
                        hours = hours,
                        bookedCount = bookedCount,
                        capacity = capacity
                    ))
                }
            }
        }
        
        // STEP 3: Sort and select best match
        Log.d("UrgentBooking", "Total available slots found: ${allAvailableSlots.size}")
        
        if (allAvailableSlots.isEmpty()) {
            Log.e("UrgentBooking", "❌ NO AVAILABLE SLOTS in next 7 days")
            return UrgentBookingResult(
                success = false,
                errorMessage = "No available appointments in next 7 days"
            )
        }
        
        // Log all available slots before sorting
        allAvailableSlots.forEach { slot ->
            Log.d("UrgentBooking", "Available: ${slot.doctor.firstName} ${slot.doctor.lastName} | ${slot.date} | ${slot.timeBlock.name} (${slot.timeRange}) | Load: ${slot.bookedCount}/${slot.capacity}")
        }
        
        val bestSlot = allAvailableSlots.sortedWith(
            compareBy<AvailableSlot> { it.dateObj }           // Earliest date first
                .thenBy { it.timeBlock.startHour }             // Earliest time block
                .thenBy { it.bookedCount }                     // Lowest booking load
        ).first()
        
        Log.d("UrgentBooking", "✅ BEST MATCH: ${bestSlot.doctor.firstName} ${bestSlot.doctor.lastName} | ${bestSlot.date} | ${bestSlot.timeBlock.name}")
        Log.d("UrgentBooking", "========================================")
        
        // STEP 4: Return slot for confirmation (NOT auto-booked yet)
        return UrgentBookingResult(
            success = true,
            needsConfirmation = true,
            doctorId = bestSlot.doctor.id,
            doctorName = "Dr. ${bestSlot.doctor.firstName} ${bestSlot.doctor.lastName}",
            date = bestSlot.date,
            timeBlock = bestSlot.timeBlock.name,
            timeRange = bestSlot.timeRange,
            fee = "1500" // Default consultation fee
        )
        
    } catch (e: Exception) {
        e.printStackTrace()
        return UrgentBookingResult(
            success = false,
            isDataAccessError = true,
            errorMessage = "Error accessing booking data: ${e.message}"
        )
    }
}

/**
 * Confirm and execute the urgent booking after user clicks "Confirm Booking"
 */
suspend fun confirmUrgentBooking(
    patientId: String,
    patientName: String,
    doctorId: String,
    doctorName: String,
    speciality: String,
    date: String,
    timeBlock: String,
    symptom: String
): UrgentBookingResult {
    try {
        val timestamp = convertDateToTimestamp(date)
        val appointmentResult = AppointmentRepository.createAppointment(
            doctorUid = doctorId,
            doctorName = doctorName,
            speciality = speciality,
            appointmentDate = timestamp,
            timeSlot = timeBlock,
            notes = "Urgent booking for: $symptom"
        )
        
        if (appointmentResult.isSuccess) {
            val appointment = appointmentResult.getOrNull()
            
            // Send notifications to patient and doctor
            try {
                val currentUser = Firebase.auth.currentUser
                if (currentUser != null && appointment != null) {
                    NotificationRepository().createNotificationForBoth(
                        patientUid = currentUser.uid,
                        doctorUid = doctorId,
                        patientTitle = "Urgent Appointment Booked",
                        patientMessage = "Your urgent appointment with Dr. $doctorName on $date ($timeBlock) has been confirmed. Appointment #${appointment.appointmentNumber}",
                        doctorTitle = "New Urgent Appointment",
                        doctorMessage = "Urgent appointment booked by patient on $date ($timeBlock). Reason: $symptom. Appointment #${appointment.appointmentNumber}",
                        type = "appointment_created",
                        appointmentId = appointment.appointmentId
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatbotScreen", "Failed to send notifications: ${e.message}")
            }
            
            return UrgentBookingResult(
                success = true,
                appointmentNumber = appointment?.appointmentNumber ?: "N/A",
                doctorName = doctorName,
                date = date,
                timeBlock = timeBlock
            )
        } else {
            return UrgentBookingResult(
                success = false,
                errorMessage = "Failed to create appointment"
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return UrgentBookingResult(
            success = false,
            errorMessage = "Booking error: ${e.message}"
        )
    }
}

data class TimeBlockDef(
    val name: String,
    val startHour: Int,
    val endHour: Int
)

// Helper function to calculate time block availability for urgent booking
private fun calculateUrgentBlockAvailability(
    doctorStart: String?,
    doctorEnd: String?,
    blockStartHour: Int,
    blockEndHour: Int
): Triple<String?, String?, Int> {
    if (doctorStart.isNullOrBlank() || doctorEnd.isNullOrBlank()) {
        return Triple(null, null, 0)
    }
    
    try {
        val docStartTime = LocalTime.parse(doctorStart, DateTimeFormatter.ofPattern("HH:mm"))
        val docEndTime = LocalTime.parse(doctorEnd, DateTimeFormatter.ofPattern("HH:mm"))
        val blockStart = LocalTime.of(blockStartHour, 0)
        val blockEnd = LocalTime.of(blockEndHour, 0)
        
        val noOverlap = docEndTime.isBefore(blockStart) || docEndTime == blockStart || 
                        docStartTime.isAfter(blockEnd) || docStartTime == blockEnd
        
        if (noOverlap) return Triple(null, null, 0)
        
        val overlapStart = if (docStartTime.isAfter(blockStart)) docStartTime else blockStart
        val overlapEnd = if (docEndTime.isBefore(blockEnd)) docEndTime else blockEnd
        
        if (overlapStart.isBefore(overlapEnd)) {
            val duration = java.time.Duration.between(overlapStart, overlapEnd)
            val hours = kotlin.math.ceil(duration.toMinutes() / 60.0).toInt()
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            val formattedStart = overlapStart.format(formatter)
            val formattedEnd = overlapEnd.format(formatter)
            return Triple(formattedStart, formattedEnd, hours)
        }
        
        return Triple(null, null, 0)
    } catch (e: Exception) {
        e.printStackTrace()
        return Triple(null, null, 0)
    }
}

fun convertDateToTimestamp(dateStr: String): Timestamp {
    return try {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = formatter.parse(dateStr)
        Timestamp(date ?: Date())
    } catch (e: Exception) {
        Timestamp(Date())
    }
}
