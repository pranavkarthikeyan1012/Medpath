package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*

@Composable
fun MedPathApp() {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = currentRoute != null && currentRoute != "landing" && currentRoute != "login",
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(250)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                MedPathBottomNavigation(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "landing",
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(
                        animationSpec = tween(320, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> fullWidth / 6 }
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> -fullWidth / 6 }
                    )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(
                        animationSpec = tween(320, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> -fullWidth / 6 }
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> fullWidth / 6 }
                    )
            }
        ) {
            composable("landing") { LandingScreen(navController) }
            composable("login") { LoginScreen(navController) }
            composable("dashboard") { DashboardScreen(navController) }
            composable("profile") { UserProfileScreen(navController) }
            composable("profile_analysis") { ProfileAnalysisScreen(navController) }
            composable("medications") { MedicationTrackerScreen(navController) }
            composable("appointments") { AppointmentRemindersScreen(navController) }
            composable("timeline") { MedicalTimelineScreen(navController) }
            composable("upload") { UploadRecordsScreen(navController) }
            composable("context") { CurrentContextScreen(navController) }
            composable("navigator") { CareNavigatorScreen(navController) }
            composable("facilities") { FacilityExplorerScreen(navController) }
            composable("emergency_hospitals") { EmergencyHospitalsScreen(navController) }
            composable("prescription") { PrescriptionExplanationScreen(navController) }
            composable("symptom_checker") { SymptomCheckerScreen(navController) }
            composable("ocr_scanner") { OcrScannerScreen(navController) }
            composable("ai_chat") { AiChatbotScreen(navController) }
        }
    }
}

