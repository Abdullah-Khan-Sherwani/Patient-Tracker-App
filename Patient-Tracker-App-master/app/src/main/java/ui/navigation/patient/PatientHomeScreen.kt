package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import com.example.patienttracker.R
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.snapshotFlow
import com.example.patienttracker.data.Appointment
import com.example.patienttracker.data.AppointmentRepository
import com.example.patienttracker.data.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.tasks.await
import java.time.Instant
import kotlinx.coroutines.launch

// Add patient color tokens to match design specification
private val HeaderGradientStart = Color(0xFF04645A)    // #04645A rgb(4,100,90)
private val HeaderGradientEnd = Color(0xFF0FB992)      // #0FB992 rgb(15,185,146)
private val PageBg = Color(0xFFF4F6F7)                 // #F4F6F7 rgb(244,246,247)
private val CardSurface = Color(0xFFFFFFFF)            // #FFFFFF white
private val IconBgMint = Color(0xFFDFF7F0)             // #DFF7F0 rgb(223,247,240)
private val IconGlyph = Color(0xFF04786A)              // #04786A rgb(4,120,106)
private val CTAGreen = Color(0xFF18BC86)               // #18BC86 rgb(24,188,134)
private val MutedDivider = Color(0xFFE9EDF0)           // #E9EDF0 rgb(233,237,240)

// Aliases for existing code compatibility
private val PatientAccent = IconGlyph                  // #04786A
private val PatientBg = PageBg                         // #F4F6F7
private val PatientText = Color(0xFF082026)            // Keep existing text color
private val PatientNeutral = MutedDivider              // #E9EDF0

// Data class for date selection chips - must be defined before use
data class DayChip(val date: LocalDate, val day: String, val dow: String)

@Composable
fun PatientHomeScreen(navController: NavController, context: Context) {
    val gradient = Brush.verticalGradient(
        // use the mint/teal gradient from design spec
        listOf(HeaderGradientStart, HeaderGradientEnd)
    )

    // Pull name from navigation arguments or saved state
    val firstNameArg = navController.currentBackStackEntry?.arguments?.getString("firstName")
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("firstName")
        ?: "Patient"
    val lastNameArg = navController.currentBackStackEntry?.arguments?.getString("lastName")
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("lastName")
        ?: ""

    Scaffold(
        bottomBar = { 
            BottomBar(
                navController = navController,
                firstName = firstNameArg,
                lastName = lastNameArg
            ) 
        },
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        containerColor = Color.Transparent,
        contentColor = Color.Unspecified
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PageBg)
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                    // Bottom padding is handled by Scaffold's bottomBar
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
        ) {
            item {
                HeaderCard(
                    gradient = gradient, 
                    firstName = firstNameArg, 
                    lastName = lastNameArg,
                    navController = navController
                )
            }

            item {
                CategoriesRow(
                    items = listOf(
                        Category("Favorite", R.drawable.ic_favourites),
                        Category("Doctors", R.drawable.ic_doctors),
                        Category("Specialties", R.drawable.ic_specialties),
                        Category("Record", R.drawable.ic_records),
                    ),
                    onCategoryClick = { category ->
                        when (category.label) {
                            "Doctors" -> navController.navigate("doctor_list/All")
                            "Specialties" -> navController.navigate("doctor_list/All") // optional
                            "Record" -> navController.navigate("my_records")
                        }
                    }
                )
            }

            item {
                ManageDependentsCard(navController = navController)
            }

            item {
                UpcomingSchedule(gradient = gradient, navController = navController)
            }

            item {
                SpecialtiesGrid(
                    titleGradient = gradient,
                    specialties = listOf(
                        Spec("Cardiology", R.drawable.ic_cardiology),
                        Spec("Dermatology", R.drawable.ic_dermatology),
                        Spec("General Medicine", R.drawable.ic_general_medicine),
                        Spec("Gynecology", R.drawable.ic_gynecology),
                        Spec("Odontology", R.drawable.ic_odontology),
                        Spec("Oncology", R.drawable.ic_oncology),
                    ),
                    onSpecialtyClick = { spec ->
                        navController.navigate("doctor_list/${spec.title}")
                    }
                )
            }
        }
    }
}

/* ----------------------------- Header ------------------------------ */

@Composable
private fun HeaderCard(gradient: Brush, firstName: String, lastName: String, navController: NavController) {
    var unreadCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val notificationRepo = remember { NotificationRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    
    // Load unread notification count
    LaunchedEffect(Unit) {
        currentUser?.uid?.let { uid ->
            scope.launch {
                try {
                    unreadCount = notificationRepo.getUnreadCount(uid)
                } catch (e: Exception) {
                    // Handle error silently
                }
            }
        }
    }
    
    Surface(
        color = IconGlyph,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Quick actions
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NotificationIconWithBadge(
                        unreadCount = unreadCount,
                        onClick = { navController.navigate("patient_notifications") }
                    )
                    IconBubble(R.drawable.ic_settings) { /* TODO: handle settings click */ }
                    IconBubble(R.drawable.ic_search) { /* TODO: handle search click */ }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Hi,",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = PatientAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        firstName, // Changed from "$firstName $lastName" to just firstName
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PatientText
                    )
                }
                Spacer(Modifier.width(12.dp))
                // Avatar placeholder - make it clickable
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(IconBgMint)
                        .clickable {
                            // Safe navigation with fallback
                            val safeFirstName = firstName.ifBlank { "Patient" }
                            val safeLastName = lastName.ifBlank { "" }
                            navController.navigate("patient_profile/$safeFirstName/$safeLastName") 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val initials = buildString {
                        if (firstName.isNotBlank()) append(firstName.first().uppercaseChar())
                        if (lastName.isNotBlank()) append(lastName.first().uppercaseChar())
                    }.ifBlank { "P" }
                    Text(initials)
                }
            }
        }
    }
}

@Composable
private fun IconBubble(
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(IconBgMint)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(IconGlyph)
        )
    }
}

@Composable
private fun NotificationIconWithBadge(
    unreadCount: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(36.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(IconBgMint)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_notifications),
                contentDescription = "Notifications",
                modifier = Modifier.size(20.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(IconGlyph)
            )
        }
        
        // Badge
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5252)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/* --------------------------- Categories ---------------------------- */

data class Category(val label: String, @DrawableRes val iconRes: Int)

@Composable
private fun CategoriesRow(items: List<Category>, onCategoryClick: (Category) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            "Categories",
            style = MaterialTheme.typography.titleMedium.copy(
                color = PatientAccent,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { category ->
                CategoryChip(category) { onCategoryClick(it) }
            }
        }
        Divider(Modifier.padding(top = 12.dp), color = PatientNeutral)
    }
}


@Composable
private fun CategoryChip(
    cat: Category,
    onClick: (Category) -> Unit = {}    // callback for handling click
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = PatientAccent)
            ) {
                onClick(cat)
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = cat.iconRes),
            contentDescription = cat.label,
            modifier = Modifier
                .size(52.dp) // consistent icon size
                .padding(top = 4.dp),
            contentScale = ContentScale.Fit
        )
    }
}

/* ----------------------- Manage Dependents Card ----------------------- */

@Composable
private fun ManageDependentsCard(navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { navController.navigate("patient_dependents") },
        color = PatientNeutral,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Family Members",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PatientAccent
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add, edit, or view family members",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = PatientText
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE91E63)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = "Dependents",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/* ---------------------- Upcoming Schedule ------------------------- */

private fun generateDateChipsAroundToday(
    pastDays: Int = 15,
    futureDays: Int = 15,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): Pair<List<DayChip>, Int> {
    val today = LocalDate.now(zoneId)
    val start = today.minusDays(pastDays.toLong())
    val total = pastDays + futureDays + 1

    val list = (0 until total).map { offset ->
        val date = start.plusDays(offset.toLong())
        val dow = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)
        DayChip(date = date, day = date.dayOfMonth.toString(), dow = dow)
    }
    // 'today' will be at index == pastDays
    return list to pastDays
}

private fun monthLabel(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.month.getDisplayName(TextStyle.FULL, locale)

@Composable
private fun UpcomingSchedule(gradient: Brush, navController: NavController) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    var allAppointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { _ ->
            try {
                val result = AppointmentRepository.getPatientAppointments()
                allAppointments = result.getOrElse { emptyList() }
            } catch (e: Exception) {
                // Handle error silently for now
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        val (dates, todayIndex) = remember { generateDateChipsAroundToday(pastDays = 7, futureDays = 7) }
        val locale = Locale.getDefault()
        var displayedMonth by remember { mutableStateOf(monthLabel(dates[todayIndex].date, locale)) }
        var selected by rememberSaveable { mutableIntStateOf(todayIndex) }

        val listState = rememberLazyListState()

        LaunchedEffect(dates) {
            listState.scrollToItem((todayIndex - 2).coerceAtLeast(0))
        }

        // update month label as scroll changes
        LaunchedEffect(listState, dates) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { idx ->
                    val probeIndex = (idx + 2).coerceIn(0, dates.lastIndex)
                    displayedMonth = monthLabel(dates[probeIndex].date, locale)
                }
        }

        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = gradient)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Upcoming Schedule",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.weight(1f))
            Text(
                displayedMonth,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelLarge
            )
        }

        // --- Date selector ---
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(dates.size) { i ->
                DayPill(
                    dates[i],
                    selected = (i == selected),
                    onClick = {
                        selected = i
                        displayedMonth = monthLabel(dates[i].date, locale)
                    }
                )
            }
        }

        // --- Filter appointments by selected date ---
        val selectedDate = dates[selected].date
        val filtered = remember(selectedDate, allAppointments) {
            allAppointments.filter { appointment ->
                try {
                    val appointmentDate = Instant.ofEpochSecond(appointment.appointmentDate.seconds)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    appointmentDate == selectedDate
                } catch (e: Exception) {
                    false
                }
            }
        }

        // --- Show schedule card ---
        if (filtered.isEmpty()) {
            NoAppointmentsCard(gradient, selectedDate)
        } else {
            ScheduleCard(
                gradient = gradient,
                selectedDate = selectedDate,
                appointments = filtered,
                navController = navController
            )
        }
    }
}

@Composable 
private fun DayPill(item: DayChip, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) PatientAccent else PatientBg
    val fg = if (selected) Color.White else PatientText
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .clickable { onClick() } // Add this line to make it clickable
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = item.day,
            color = fg,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = item.dow,
            color = fg.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

data class ScheduleEntry(val subtitle: String, val time: String, val doctor: String)

@Composable
private fun ScheduleCard(
    gradient: Brush,
    selectedDate: LocalDate,
    appointments: List<Appointment>,
    navController: NavController
) {
    val locale = Locale.getDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd", locale)
    val displayDate = selectedDate.format(dateFormatter)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Appointments for $displayDate",
                    color = PatientText,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "See all",
                    color = PatientAccent,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable { navController.navigate("full_schedule") }
                )
            }
            
            Spacer(Modifier.height(12.dp))

            appointments.forEachIndexed { index, appointment ->
                Column {
                    // Show time block and range together
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayTime = if (appointment.timeSlot.contains("-") || appointment.timeSlot.contains(":")) {
                            formatTimeRange(appointment.timeSlot)
                        } else {
                            "${appointment.timeSlot} • ${getTimeRangeForBlock(appointment.timeSlot)}"
                        }
                        Text(
                            displayTime,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PatientText
                        )
                        Spacer(Modifier.width(8.dp))

                    }
                    
                    // Show doctor info
                    Text(
                        "${appointment.doctorName} (${appointment.speciality})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PatientText.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    if (index != appointments.lastIndex) {
                        Divider(Modifier.padding(vertical = 12.dp), color = PatientNeutral)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoAppointmentsCard(gradient: Brush, selectedDate: LocalDate) {
    val locale = Locale.getDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd", locale)
    val displayDate = selectedDate.format(dateFormatter)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No appointments for $displayDate",
                color = PatientText,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Click on other dates to check appointments",
                color = PatientAccent,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


/* --------------------------- Specialties --------------------------- */

data class Spec(val title: String, @DrawableRes val iconRes: Int)

@Composable
private fun SpecialtiesGrid(
    titleGradient: Brush,
    specialties: List<Spec>,
    onSpecialtyClick: (Spec) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Specialties",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = PatientAccent,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(specialties) { spec ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSpecialtyClick(spec) }
                        .background(CardSurface)
                        .padding(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = spec.iconRes),
                        contentDescription = spec.title,
                        modifier = Modifier.size(52.dp)
                    )
                    Text(
                        spec.title,
                        textAlign = TextAlign.Center,
                        color = PatientText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun SpecCard(spec: Spec, onClick: (Spec) -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)                      // square, fills the cell
            .clip(RoundedCornerShape(16.dp))
            .semantics { role = Role.Button }     // accessibility
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = PatientAccent)
            ) { onClick(spec) },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = spec.iconRes),
            contentDescription = spec.title,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),                  // breathing room inside the tile
            contentScale = ContentScale.Fit
        )
    }
}

/* --------------------------- Bottom Bar ---------------------------- */

@Composable
private fun BottomBar(
    navController: NavController,
    firstName: String,
    lastName: String
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = PatientBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp + bottomInset)
                .padding(bottom = bottomInset.coerceAtMost(3.dp))
        ) {
            Divider(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                color = PatientNeutral
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomItem(
                    iconRes = R.drawable.ic_home,
                    label = "Home",
                    selected = true
                ) { 
                    navController.navigate("patient_home/$firstName/$lastName") {
                        popUpTo("patient_home/$firstName/$lastName") { inclusive = true }
                    }
                }

                BottomItem(
                    iconRes = R.drawable.ic_messages,
                    label = "Chat"
                ) { /* navController.navigate("chat_screen") */ }

                BottomItem(
                    iconRes = R.drawable.ic_user_profile,
                    label = "Profile"
                ) { 
                    val safeFirstName = firstName.ifBlank { "Patient" }
                    val safeLastName = lastName.ifBlank { "" }
                    navController.navigate("patient_profile/$safeFirstName/$safeLastName") 
                }

                BottomItem(
                    iconRes = R.drawable.ic_booking,
                    label = "Schedule"
                ) { navController.navigate("full_schedule") }
            }
        }
    }
}

@Composable
private fun BottomItem(
    @DrawableRes iconRes: Int,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            colorFilter = if (selected) androidx.compose.ui.graphics.ColorFilter.tint(IconGlyph) else null
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) PatientAccent else PatientText
        )
    }
}

/**
 * Convert time block name to time range string
 */
private fun getTimeRangeForBlock(blockName: String): String {
    return when (blockName.lowercase()) {
        "morning" -> "6:00 AM - 12:00 PM"
        "afternoon" -> "12:00 PM - 6:00 PM"
        "evening" -> "6:00 PM - 12:00 AM"
        else -> blockName // Return original if not a recognized block
    }
}

private fun formatTimeRange(timeRange: String): String {
    return try {
        // Clean the input - remove + signs and extra spaces, handle AM/PM already present
        val cleaned = timeRange.replace("+", " ").replace("\\s+".toRegex(), " ").trim()
        
        // Check if it's already formatted with AM/PM
        if (cleaned.contains("AM", ignoreCase = true) || cleaned.contains("PM", ignoreCase = true)) {
            // Just ensure proper spacing
            return cleaned.replace("AM", " AM").replace("PM", " PM")
                .replace("am", " AM").replace("pm", " PM")
                .replace("\\s+".toRegex(), " ").trim()
        }
        
        // Parse as 24-hour format
        val parts = cleaned.split("-").map { it.trim() }
        if (parts.size == 2) {
            val startTime = java.time.LocalTime.parse(parts[0], java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val endTime = java.time.LocalTime.parse(parts[1], java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH)
            "${startTime.format(formatter)} - ${endTime.format(formatter)}"
        } else {
            cleaned
        }
    } catch (e: Exception) {
        timeRange
    }
}
