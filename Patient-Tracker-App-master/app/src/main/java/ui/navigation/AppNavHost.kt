package com.example.patienttracker.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.patienttracker.auth.AuthManager
import com.example.patienttracker.ui.screens.common.SplashScreen
import com.example.patienttracker.ui.screens.auth.RegisterPatientScreen
import com.example.patienttracker.ui.screens.patient.PatientHomeScreen
import com.example.patienttracker.ui.screens.patient.PatientDashboard
import com.example.patienttracker.ui.screens.auth.PatientAccountCreatedScreen
import com.example.patienttracker.ui.screens.admin.AdminHomeScreen
import com.example.patienttracker.ui.screens.admin.AddDoctorScreen
import com.example.patienttracker.ui.screens.admin.AddPatientScreen
import com.example.patienttracker.ui.screens.admin.ManageUsersScreen
import com.example.patienttracker.ui.screens.doctor.DoctorHomeScreen
import com.example.patienttracker.ui.screens.auth.UnifiedLoginScreen
import com.example.patienttracker.ui.screens.auth.PatientWelcomeScreen
import com.example.patienttracker.ui.screens.auth.DoctorWelcomeScreen
import com.example.patienttracker.ui.screens.patient.DoctorListScreen
import com.example.patienttracker.ui.screens.patient.DoctorFull
import com.example.patienttracker.ui.screens.patient.BookAppointmentScreen
import com.example.patienttracker.ui.screens.patient.FullScheduleScreen
import com.example.patienttracker.ui.screens.patient.PatientProfileScreen
import com.example.patienttracker.ui.screens.patient.PatientHealthRecordsScreen
import com.example.patienttracker.ui.screens.patient.UploadHealthRecordScreen
import com.example.patienttracker.ui.screens.patient.EnhancedUploadHealthRecordScreen
import com.example.patienttracker.ui.screens.patient.MyRecordsScreen
import com.example.patienttracker.ui.screens.patient.SimplifiedBookAppointmentScreen
import com.example.patienttracker.ui.screens.doctor.DoctorViewPatientRecordsScreen
import com.example.patienttracker.ui.screens.doctor.EnhancedDoctorViewPatientRecordsScreen
import com.example.patienttracker.ui.screens.doctor.DoctorPatientListScreen
import com.example.patienttracker.ui.viewmodel.ThemeViewModel
import com.example.patienttracker.ui.screens.patient.SelectSpecialtyScreen
import com.example.patienttracker.ui.screens.patient.SelectDoctorScreen
import com.example.patienttracker.ui.screens.patient.SelectDateTimeScreen
import com.example.patienttracker.ui.screens.patient.ConfirmAppointmentScreen
import com.example.patienttracker.ui.screens.patient.AppointmentSuccessScreen

private object Route {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER_PATIENT = "register_patient"
    const val ACCOUNT_CREATED = "account_created"
    const val ACCOUNT_CREATED_ARG = "patientId"
    const val PATIENT_WELCOME = "patient_welcome"
    const val DOCTOR_WELCOME = "doctor_welcome"

    // Home routes
    const val PATIENT_HOME = "patient_home"
    const val PATIENT_HOME_ARGS = "patient_home/{firstName}/{lastName}"
    const val DOCTOR_HOME = "doctor_home"
    const val DOCTOR_HOME_ARGS = "doctor_home/{firstName}/{lastName}/{doctorId}"
    const val ADMIN_HOME = "admin_home"
}

@Composable
fun AppNavHost(context: Context, themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf(Route.SPLASH) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash screen - handles auth check
        composable(Route.SPLASH) {
            SplashScreen()
            
            LaunchedEffect(Unit) {
                // Check authentication state
                if (AuthManager.isUserLoggedIn()) {
                    // User is logged in, get their role and navigate accordingly
                    try {
                        val role = AuthManager.getCurrentUserRole()
                        val profile = AuthManager.getCurrentUserProfile()
                        
                        when (role) {
                            "patient" -> {
                                if (profile != null) {
                                    // Navigate directly to patient home
                                    navController.navigate("${Route.PATIENT_HOME}/${profile.firstName}/${profile.lastName}") {
                                        popUpTo(Route.SPLASH) { inclusive = true }
                                    }
                                } else {
                                    // Fallback: go to login
                                    navController.navigate(Route.LOGIN) {
                                        popUpTo(Route.SPLASH) { inclusive = true }
                                    }
                                }
                            }
                            "doctor" -> {
                                if (profile != null) {
                                    // Navigate directly to doctor home
                                    navController.navigate("${Route.DOCTOR_HOME}/${profile.firstName}/${profile.lastName}/${profile.humanId}") {
                                        popUpTo(Route.SPLASH) { inclusive = true }
                                    }
                                } else {
                                    // Fallback: go to login
                                    navController.navigate(Route.LOGIN) {
                                        popUpTo(Route.SPLASH) { inclusive = true }
                                    }
                                }
                            }
                            "admin" -> {
                                navController.navigate(Route.ADMIN_HOME) {
                                    popUpTo(Route.SPLASH) { inclusive = true }
                                }
                            }
                            else -> {
                                // Unknown role, go to login
                                navController.navigate(Route.LOGIN) {
                                    popUpTo(Route.SPLASH) { inclusive = true }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // If there's any error, go to login
                        navController.navigate(Route.LOGIN) {
                            popUpTo(Route.SPLASH) { inclusive = true }
                        }
                    }
                } else {
                    // User is not logged in, go to unified login
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.SPLASH) { inclusive = true }
                    }
                }
            }
        }

        // Unified login screen for all roles
        composable(Route.LOGIN) {
            UnifiedLoginScreen(
                navController = navController,
                context = context,
                onSignUp = {
                    navController.navigate(Route.REGISTER_PATIENT) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Route.REGISTER_PATIENT) {
            RegisterPatientScreen(navController, context)
        }

        composable(
            route = "${Route.ACCOUNT_CREATED}/{${Route.ACCOUNT_CREATED_ARG}}",
            arguments = listOf(navArgument(Route.ACCOUNT_CREATED_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString(Route.ACCOUNT_CREATED_ARG) ?: ""
            PatientAccountCreatedScreen(navController, patientId)
        }

        composable(
            route = Route.PATIENT_HOME_ARGS,
            arguments = listOf(
                navArgument("firstName") { type = NavType.StringType; defaultValue = "" },
                navArgument("lastName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val firstName = backStackEntry.arguments?.getString("firstName") ?: ""
            val lastName = backStackEntry.arguments?.getString("lastName") ?: ""
            PatientDashboard(navController, context, themeViewModel.isDarkMode.value)
        }

        composable(Route.PATIENT_HOME) {
            PatientDashboard(navController, context, themeViewModel.isDarkMode.value)
        }

        composable("doctor_list/{speciality}") { backStackEntry ->
            val speciality = backStackEntry.arguments?.getString("speciality")
            DoctorListScreen(navController, context, speciality)
        }

        composable("book_appointment") {
            val doctor = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<DoctorFull>("selectedDoctor")

            if (doctor != null) {
                BookAppointmentScreen(navController, context, doctor)
            } else {
                DoctorListScreen(navController, context, specialityFilter = "All")
            }
        }

        composable("full_schedule") {
            FullScheduleScreen(navController, context)
        }

        composable("patient_profile/{firstName}/{lastName}") { backStackEntry ->
            val first = backStackEntry.arguments?.getString("firstName") ?: ""
            val last = backStackEntry.arguments?.getString("lastName") ?: ""
            PatientProfileScreen(navController, first, last, themeViewModel)
        }

        // Health Records Routes
        composable("patient_health_records") {
            PatientHealthRecordsScreen(navController, context)
        }

        composable("upload_health_record") {
            UploadHealthRecordScreen(navController, context)
        }

        // Enhanced Upload Health Record Screen with privacy, notes, medication
        composable("upload_health_record_enhanced") {
            EnhancedUploadHealthRecordScreen(navController, context)
        }

        // My Records Screen - Patient view of their own records with access logs
        composable("my_records") {
            MyRecordsScreen(navController, context)
        }

        // Simplified Booking Screen with calendar view
        composable("book_appointment_simple/{doctorUid}/{doctorName}/{speciality}") { backStackEntry ->
            val doctorUid = backStackEntry.arguments?.getString("doctorUid") ?: ""
            val doctorName = backStackEntry.arguments?.getString("doctorName") ?: ""
            val speciality = backStackEntry.arguments?.getString("speciality") ?: ""
            SimplifiedBookAppointmentScreen(navController, context, doctorUid, doctorName, speciality)
        }

        // Doctor View Patient Records - Original screen
        composable("doctor_view_patient_records/{patientUid}/{patientName}") { backStackEntry ->
            val patientUid = backStackEntry.arguments?.getString("patientUid") ?: ""
            val patientName = backStackEntry.arguments?.getString("patientName") ?: "Patient"
            DoctorViewPatientRecordsScreen(navController, context, patientUid, patientName)
        }

        // Enhanced Doctor View Patient Records with sorting, filtering, glass break
        composable("doctor_view_patient_records_enhanced/{patientUid}/{patientName}") { backStackEntry ->
            val patientUid = backStackEntry.arguments?.getString("patientUid") ?: ""
            val patientName = backStackEntry.arguments?.getString("patientName") ?: "Patient"
            EnhancedDoctorViewPatientRecordsScreen(navController, context, patientUid, patientName)
        }

        // Doctor Patient List Screen - View all patients with appointments
        composable("doctor_patient_list") {
            DoctorPatientListScreen(navController, context)
        }

        composable(
            route = Route.DOCTOR_HOME_ARGS,
            arguments = listOf(
                navArgument("firstName") { type = NavType.StringType },
                navArgument("lastName") { type = NavType.StringType },
                navArgument("doctorId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val firstName = backStackEntry.arguments?.getString("firstName") ?: ""
            val lastName = backStackEntry.arguments?.getString("lastName") ?: ""
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            DoctorHomeScreen(navController, context, firstName, lastName, doctorId)
        }

        composable(Route.DOCTOR_HOME) {
            DoctorHomeScreen(navController, context)
        }

        // Admin Routes
        composable(Route.ADMIN_HOME) {
            AdminHomeScreen(navController, context)
        }

        composable("admin_add_doctor") {
            AddDoctorScreen(navController, context)
        }

        composable("admin_add_patient") {
            AddPatientScreen(navController, context)
        }

        composable("admin_manage_users") {
            ManageUsersScreen(navController, context)
        }

        // New Booking Flow Routes
        composable("select_specialty") {
            SelectSpecialtyScreen(navController, context)
        }

        composable("select_doctor/{specialty}") { backStackEntry ->
            val specialty = backStackEntry.arguments?.getString("specialty") ?: "General"
            SelectDoctorScreen(navController, context, specialty)
        }

        composable("select_datetime/{doctorId}/{doctorFirstName}/{doctorLastName}/{specialty}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val doctorFirstName = backStackEntry.arguments?.getString("doctorFirstName") ?: ""
            val doctorLastName = backStackEntry.arguments?.getString("doctorLastName") ?: ""
            val specialty = backStackEntry.arguments?.getString("specialty") ?: ""
            SelectDateTimeScreen(navController, context, doctorId, doctorFirstName, doctorLastName, specialty)
        }

        composable("confirm_appointment/{doctorId}/{doctorName}/{specialty}/{date}/{timeSlot}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val doctorName = backStackEntry.arguments?.getString("doctorName") ?: ""
            val specialty = backStackEntry.arguments?.getString("specialty") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val timeSlot = backStackEntry.arguments?.getString("timeSlot") ?: ""
            ConfirmAppointmentScreen(navController, context, doctorId, doctorName, specialty, date, timeSlot)
        }

        composable("appointment_success/{appointmentId}/{doctorName}/{date}/{timeSlot}") { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""
            val doctorName = backStackEntry.arguments?.getString("doctorName") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val timeSlot = backStackEntry.arguments?.getString("timeSlot") ?: ""
            AppointmentSuccessScreen(navController, context, appointmentId, doctorName, date, timeSlot)
        }
    }
}