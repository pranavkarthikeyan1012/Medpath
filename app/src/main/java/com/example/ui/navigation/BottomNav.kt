package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun MedPathBottomNavigation(navController: NavController, currentRoute: String?) {
    val items = listOf(
        BottomNavItem("dashboard", "Home", Icons.Filled.Home),
        BottomNavItem("ai_chat", "AI Assistant", Icons.Filled.AutoAwesome),
        BottomNavItem("medications", "Meds", Icons.Filled.Medication),
        BottomNavItem("appointments", "Visits", Icons.Filled.CalendarMonth),
        BottomNavItem("profile", "Profile", Icons.Filled.Person)
    )

    NavigationBar {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
