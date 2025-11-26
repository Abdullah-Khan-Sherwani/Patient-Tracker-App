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
import com.example.patienttracker.ui.screens.auth.ForgotPasswordScreen
import com.example.patienttracker.ui.screens.patient.PatientHomeScreen
import com.example.patienttracker.ui.screens.patient.PatientDashboard
import com.example.patienttracker.ui.screens.auth.PatientAccountCreatedScreen
import com.example.patienttracker.ui.screens.admin.AdminHomeScreen
import com.example.patienttracker.ui.screens.admin.AddDoctorScreen
import com.example.patienttracker.ui.screens.admin.AddPatientScreen
import com.example.patienttracker.ui.screens.admin.ManageUsersScreen
import com.example.patienttracker.ui.screens.admin.AdminCreateAppointmentScreen
import com.example.patienttracker.ui.screens.admin.AdminAllAppointmentsScreen
import com.example.patienttracker.ui.screens.admin.AdminAppointmentDetailsScreen
import com.example.patienttracker.ui.screens.admin.AdminSystemReportsScreen
import com.example.patienttracker.ui.screens.admin.AdminProfileScreen
import com.example.patienttracker.ui.screens.admin.AdminSettingsScreen
import com.example.patienttracker.ui.screens.admin.AdminAboutScreen
import com.example.patienttracker.ui.screens.doctor.DoctorHomeScreen
import com.example.patienttracker.ui.screens.auth.UnifiedLoginScreen
import com.example.patienttracker.ui.screens.auth.PatientWelcomeScreen
import com.example.patienttracker.ui.screens.auth.DoctorWelcomeScreen
import com.example.patienttracker.ui.screens.auth.PrivacyPolicyScreen as AuthPrivacyPolicyScreen
import com.example.patienttracker.ui.screens.auth.TermsAndConditionsScreen as AuthTermsAndConditionsScreen
import com.example.patienttracker.ui.screens.patient.DoctorListScreen
import com.example.patienttracker.ui.screens.patient.DoctorFull
import com.example.patienttracker.ui.screens.patient.BookAppointmentScreen
import com.example.patienttracker.ui.screens.patient.FullScheduleScreen
import com.example.patienttracker.ui.screens.patient.PatientProfileScreen
import com.example.patienttracker.ui.screens.patient.TermsAndConditionsScreen
import com.example.patienttracker.ui.screens.patient.PrivacyPolicyScreen
import com.example.patienttracker.ui.screens.patient.ChangePasswordScreen
import com.example.patienttracker.ui.screens.patient.drawer.*
import com.example.patienttracker.ui.screens.patient.PatientHealthRecordsScreen
import com.example.patienttracker.ui.screens.patient.UploadHealthRecordScreen
import com.example.patienttracker.ui.screens.patient.EnhancedUploadHealthRecordScreen
import com.example.patienttracker.ui.screens.patient.MyRecordsScreen
import com.example.patienttracker.ui.screens.patient.SimplifiedBookAppointmentScreen
import com.example.patienttracker.ui.screens.patient.PatientNotificationsScreen
import com.example.patienttracker.ui.screens.doctor.DoctorViewPatientRecordsScreen
import com.example.patienttracker.ui.screens.doctor.EnhancedDoctorViewPatientRecordsScreen
import com.example.patienttracker.ui.screens.doctor.DoctorPatientListScreen
import com.example.patienttracker.ui.screens.doctor.DoctorProfileScreen
import com.example.patienttracker.ui.screens.doctor.DoctorMessagesScreen
import com.example.patienttracker.ui.screens.doctor.DoctorAppointmentsFullScreen
import com.example.patienttracker.ui.screens.doctor.DoctorNotificationsScreen
import com.example.patienttracker.ui.screens.doctor.DoctorManageScheduleScreen
import com.example.patienttracker.ui.screens.doctor.DoctorViewRecordsScreen
import com.example.patienttracker.ui.screens.doctor.DoctorSettingsScreen
import com.example.patienttracker.ui.screens.doctor.DoctorChangePasswordScreen
import com.example.patienttracker.ui.screens.doctor.DoctorTermsScreen
import com.example.patienttracker.ui.screens.doctor.DoctorPrivacyScreen
import com.example.patienttracker.ui.screens.common.EditAvailabilityScreen
import com.example.patienttracker.ui.viewmodel.ThemeViewModel
import com.example.patienttracker.ui.screens.patient.SelectSpecialtyScreen
import com.example.patienttracker.ui.screens.patient.SelectDoctorScreen
import com.example.patienttracker.ui.screens.patient.SelectDateTimeScreen
import com.example.patienttracker.ui.screens.patient.ConfirmAppointmentScreen
import com.example.patienttracker.ui.screens.patient.AppointmentSuccessScreen
import com.example.patienttracker.ui.screens.patient.ChatbotScreen
import com.example.patienttracker.ui.screens.patient.PatientDependentsScreen
import com.example.patienttracker.ui.screens.patient.AddDependentScreen
import com.example.patienttracker.ui.screens.patient.ViewDependentScreen
import com.example.patienttracker.ui.screens.patient.EditDependentScreen
import com.example.patienttracker.ui.screens.patient.ChoosePatientForAppointmentScreen
import com.example.patienttracker.ui.screens.patient.DependentUploadRecordsScreen
import com.example.patienttracker.ui.screens.patient.DependentViewRecordsScreen
import com.example.patienttracker.ui.screens.patient.DependentAppointmentHistoryScreen
import com.example.patienttracker.ui.screens.guest.GuestHomeScreen
import com.example.patienttracker.ui.screens.guest.GuestDoctorsScreen
import com.example.patienttracker.ui.screens.guest.GuestDoctorDetailsScreen
import com.example.patienttracker.ui.screens.guest.GuestAboutScreen
import com.example.patienttracker.ui.screens.guest.GuestContactScreen
import com.example.patienttracker.ui.screens.guest.GuestSettingsScreen

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
                },
                onForgotPassword = {
                    navController.navigate("forgot_password")
                }
            )
        }
        
        composable("forgot_password") {
            ForgotPasswordScreen(navController, context)
        }

        composable("privacy_policy") {
            AuthPrivacyPolicyScreen(navController)
        }

        composable("terms_and_conditions") {
            AuthTermsAndConditionsScreen(navController)
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

        composable(
            route = "full_schedule?selectedTab={selectedTab}",
            arguments = listOf(
                navArgument("selectedTab") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = "upcoming"
                }
            )
        ) { backStackEntry ->
            val selectedTab = backStackEntry.arguments?.getString("selectedTab") ?: "upcoming"
            FullScheduleScreen(navController, context, selectedTab)
        }

        composable("patient_profile/{firstName}/{lastName}") { backStackEntry ->
            val first = backStackEntry.arguments?.getString("firstName") ?: ""
            val last = backStackEntry.arguments?.getString("lastName") ?: ""
            PatientProfileScreen(navController, first, last, themeViewModel)
        }

        composable("patient_notifications") {
            PatientNotificationsScreen(navController, context)
        }

        composable("patient_dependents") {
            PatientDependentsScreen(navController, context)
        }

        composable("add_dependent") {
            AddDependentScreen(navController, context)
        }

        composable("view_dependent/{dependentId}") { backStackEntry ->
            val dependentId = backStackEntry.arguments?.getString("dependentId") ?: ""
            ViewDependentScreen(navController, context, dependentId)
        }

        composable("edit_dependent/{dependentId}") { backStackEntry ->
            val dependentId = backStackEntry.arguments?.getString("dependentId") ?: ""
            EditDependentScreen(navController, context, dependentId)
        }

        composable("dependent_upload_records/{dependentId}/{dependentName}") { backStackEntry ->
            val dependentId = backStackEntry.arguments?.getString("dependentId") ?: ""
            val dependentName = try {
                java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("dependentName") ?: "",
                    java.nio.charset.StandardCharsets.UTF_8.toString()
                )
            } catch (e: Exception) { backStackEntry.arguments?.getString("dependentName") ?: "" }
            DependentUploadRecordsScreen(navController, context, dependentId, dependentName)
        }

        composable("dependent_view_records/{dependentId}/{dependentName}") { backStackEntry ->
            val dependentId = backStackEntry.arguments?.getString("dependentId") ?: ""
            val dependentName = try {
                java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("dependentName") ?: "",
                    java.nio.charset.StandardCharsets.UTF_8.toString()
                )
            } catch (e: Exception) { backStackEntry.arguments?.getString("dependentName") ?: "" }
            DependentViewRecordsScreen(navController, context, dependentId, dependentName)
        }

        composable("dependent_appointments/{dependentId}/{dependentName}") { backStackEntry ->
            val dependentId = backStackEntry.arguments?.getString("dependentId") ?: ""
            val dependentName = try {
                java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("dependentName") ?: "",
                    java.nio.charset.StandardCharsets.UTF_8.toString()
                )
            } catch (e: Exception) { backStackEntry.arguments?.getString("dependentName") ?: "" }
            DependentAppointmentHistoryScreen(navController, context, dependentId, dependentName)
        }

        composable("terms_and_conditions") {
            TermsAndConditionsScreen(navController)
        }

        composable("privacy_policy") {
            PrivacyPolicyScreen(navController)
        }

        composable("change_password") {
            ChangePasswordScreen(navController)
        }

        // Drawer Navigation Routes
        composable("help_center") {
            HelpCenterScreen(navController)
        }

        composable("help_book_appointment") {
            HelpBookAppointmentScreen(navController)
        }

        composable("help_upload_reports") {
            HelpUploadReportsScreen(navController)
        }

        composable("help_view_history") {
            HelpViewHistoryScreen(navController)
        }

        composable("faqs") {
            FAQsScreen(navController)
        }

        composable("about_medify") {
            AboutMedifyScreen(navController)
        }

        composable("contact_support") {
            ContactSupportScreen(navController)
        }

        // Health Records Routes
        composable("patient_health_records") {
            PatientHealthRecordsScreen(navController, context)
        }

        composable("upload_health_record") {
            UploadHealthRecordScreen(navController, context)
        }

        // Favorite Doctors Screen
        composable("favorite_doctors") {
            com.example.patienttracker.ui.screens.patient.FavoriteDoctorsScreen(navController, context)
        }

        // Doctor Catalogue Screen
        composable(
            route = "doctor_catalogue?specialty={specialty}",
            arguments = listOf(navArgument("specialty") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val specialty = backStackEntry.arguments?.getString("specialty")
            com.example.patienttracker.ui.screens.patient.DoctorCatalogueScreen(navController, context, preselectedSpecialty = specialty)
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

        composable("admin_create_appointment") {
            AdminCreateAppointmentScreen(navController, context)
        }

        composable("admin_all_appointments") {
            AdminAllAppointmentsScreen(navController, context)
        }

        composable("admin_appointment_details/{appointmentId}") { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""
            AdminAppointmentDetailsScreen(navController, context, appointmentId)
        }

        composable("admin_system_reports") {
            AdminSystemReportsScreen(navController, context)
        }

        composable("admin_profile") {
            AdminProfileScreen(navController, context)
        }

        composable("admin_settings") {
            AdminSettingsScreen(navController, context)
        }

        composable("admin_about") {
            AdminAboutScreen(navController, context)
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
        composable("choose_patient_for_appointment/{doctorId}/{doctorName}/{specialty}/{date}/{blockName}/{timeRange}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val doctorName = backStackEntry.arguments?.getString("doctorName") ?: ""
            val specialty = backStackEntry.arguments?.getString("specialty") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val blockName = backStackEntry.arguments?.getString("blockName") ?: ""
            val timeRange = backStackEntry.arguments?.getString("timeRange") ?: ""
            com.example.patienttracker.ui.screens.patient.ChoosePatientForAppointmentScreen(navController, context, doctorId, doctorName, specialty, date, blockName, timeRange)
        }

        composable("confirm_appointment/{doctorId}/{doctorName}/{specialty}/{date}/{blockName}/{timeRange}/{dependentId}/{dependentName}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val doctorName = try {
                java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("doctorName") ?: "",
                    java.nio.charset.StandardCharsets.UTF_8.toString()
                )
            } catch (e: Exception) { backStackEntry.arguments?.getString("doctorName") ?: "" }
            val specialty = backStackEntry.arguments?.getString("specialty") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val blockName = backStackEntry.arguments?.getString("blockName") ?: ""
            val timeRange = try {
                java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("timeRange") ?: "",
                    java.nio.charset.StandardCharsets.UTF_8.toString()
                )
            } catch (e: Exception) { backStackEntry.arguments?.getString("timeRange") ?: "" }
            val dependentId = backStackEntry.arguments?.getString("dependentId") ?: ""
            val dependentName = try {
                java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("dependentName") ?: "",
                    java.nio.charset.StandardCharsets.UTF_8.toString()
                )
            } catch (e: Exception) { backStackEntry.arguments?.getString("dependentName") ?: "" }
            com.example.patienttracker.ui.screens.patient.ConfirmAppointmentScreen(navController, context, doctorId, doctorName, specialty, date, blockName, timeRange, dependentId, dependentName)
        }

        composable("appointment_success/{appointmentNumber}/{doctorName}/{date}/{blockName}/{timeRange}/{recipientType}") { backStackEntry ->
            val appointmentNumber = backStackEntry.arguments?.getString("appointmentNumber") ?: "000"
            val doctorName = try {
                java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("doctorName") ?: "",
                    java.nio.charset.StandardCharsets.UTF_8.toString()
                )
            } catch (e: Exception) { backStackEntry.arguments?.getString("doctorName") ?: "" }
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val blockName = backStackEntry.arguments?.getString("blockName") ?: ""
            val timeRange = try {
                java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("timeRange") ?: "",
                    java.nio.charset.StandardCharsets.UTF_8.toString()
                )
            } catch (e: Exception) { backStackEntry.arguments?.getString("timeRange") ?: "" }
            val recipientType = backStackEntry.arguments?.getString("recipientType") ?: "self"
            AppointmentSuccessScreen(navController, context, appointmentNumber, doctorName, date, blockName, timeRange, recipientType)
        }

        // Chatbot Route
        composable("chatbot") {
            ChatbotScreen(navController)
        }

        // New Doctor Feature Routes
        composable("doctor_profile") {
            DoctorProfileScreen(navController, context)
        }

        composable("doctor_change_password") {
            DoctorChangePasswordScreen(navController, context)
        }

        composable("doctor_terms") {
            DoctorTermsScreen(navController, context)
        }

        composable("doctor_privacy") {
            DoctorPrivacyScreen(navController, context)
        }

        composable("doctor_messages") {
            DoctorMessagesScreen(navController, context)
        }

        composable("doctor_appointments_full") {
            DoctorAppointmentsFullScreen(navController, context)
        }

        composable("doctor_notifications") {
            DoctorNotificationsScreen(navController, context)
        }

        composable("doctor_manage_schedule") {
            DoctorManageScheduleScreen(navController, context)
        }

        composable("doctor_view_records") {
            DoctorViewRecordsScreen(navController, context)
        }

        composable("doctor_settings") {
            DoctorSettingsScreen(navController, context)
        }

        // Edit Availability Screen (used by both doctor and admin)
        composable(
            route = "edit_availability/{doctorUid}",
            arguments = listOf(navArgument("doctorUid") { type = NavType.StringType })
        ) { backStackEntry ->
            val doctorUid = backStackEntry.arguments?.getString("doctorUid") ?: ""
            EditAvailabilityScreen(navController, context, doctorUid)
        }

        // Guest Mode Routes
        composable("guest_home") {
            GuestHomeScreen(navController, context)
        }

        composable("guest_doctors") {
            GuestDoctorsScreen(navController, context)
        }

        composable(
            route = "guest_doctor_details/{doctorUid}",
            arguments = listOf(navArgument("doctorUid") { type = NavType.StringType })
        ) { backStackEntry ->
            val doctorUid = backStackEntry.arguments?.getString("doctorUid") ?: ""
            GuestDoctorDetailsScreen(navController, context, doctorUid)
        }

        composable("guest_about") {
            GuestAboutScreen(navController, context)
        }

        composable("guest_contact") {
            GuestContactScreen(navController, context)
        }

        composable("guest_settings") {
            GuestSettingsScreen(navController, context)
        }
    }
}