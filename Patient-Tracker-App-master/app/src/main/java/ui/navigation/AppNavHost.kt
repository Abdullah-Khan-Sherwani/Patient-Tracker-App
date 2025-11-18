package com.example.patienttracker.ui.navigation

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.delay
import com.example.patienttracker.ui.screens.common.SplashScreen
import com.example.patienttracker.ui.screens.auth.RegisterPatientScreen
import com.example.patienttracker.ui.screens.patient.PatientHomeScreen
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
import com.example.patienttracker.ui.screens.doctor.DoctorViewPatientRecordsScreen

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
fun AppNavHost(context: Context) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf(Route.SPLASH) }
    var isAuthCheckComplete by remember { mutableStateOf(false) }
    var nextDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            delay(500) // Allow UI to render first
            
            // Perform async authentication check
            if (AuthManager.isUserLoggedIn()) {
                try {
                    val role = AuthManager.getCurrentUserRole()
                    val profile = AuthManager.getCurrentUserProfile()

                    nextDestination = when (role) {
                        "patient" -> {
                            if (profile != null) {
                                "${Route.PATIENT_HOME}/${profile.firstName}/${profile.lastName}"
                            } else {
                                Route.LOGIN
                            }
                        }
                        "doctor" -> {
                            if (profile != null) {
                                "${Route.DOCTOR_HOME}/${profile.firstName}/${profile.lastName}/${profile.humanId}"
                            } else {
                                Route.LOGIN
                            }
                        }
                        "admin" -> Route.ADMIN_HOME
                        else -> Route.LOGIN
                    }
                } catch (e: Exception) {
                    Log.e("AppNavHost", "Error fetching user role/profile", e)
                    nextDestination = Route.LOGIN
                }
            } else {
                nextDestination = Route.LOGIN
            }
            
            isAuthCheckComplete = true
        } catch (e: Exception) {
            Log.e("AppNavHost", "Critical error in auth check", e)
            nextDestination = Route.LOGIN
            isAuthCheckComplete = true
        }
    }

    if (isAuthCheckComplete && nextDestination != null) {
        startDestination = nextDestination ?: Route.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash screen - only rendered briefly
        composable(Route.SPLASH) {
            SplashScreen()
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
            PatientHomeScreen(navController, context)
            // Pass firstName and lastName through savedStateHandle for the screen to use
            LaunchedEffect(Unit) {
                navController.currentBackStackEntry?.savedStateHandle?.set("firstName", firstName)
                navController.currentBackStackEntry?.savedStateHandle?.set("lastName", lastName)
            }
        }

        composable(Route.PATIENT_HOME) {
            PatientHomeScreen(navController, context)
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
            PatientProfileScreen(navController, first, last)
        }

        // Health Records Routes
        composable("patient_health_records") {
            PatientHealthRecordsScreen(navController, context)
        }

        composable("upload_health_record") {
            UploadHealthRecordScreen(navController, context)
        }

        composable("doctor_view_patient_records/{patientUid}/{patientName}") { backStackEntry ->
            val patientUid = backStackEntry.arguments?.getString("patientUid") ?: ""
            val patientName = backStackEntry.arguments?.getString("patientName") ?: "Patient"
            DoctorViewPatientRecordsScreen(navController, context, patientUid, patientName)
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
    }
}