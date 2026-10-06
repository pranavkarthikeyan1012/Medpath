package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.*
import com.example.ui.components.AccessibilityDialog
import com.example.ui.components.AccessibilityTopBarAction
import com.example.ui.theme.status_due_amber
import com.example.ui.theme.status_due_bg
import com.example.ui.theme.status_taken_bg
import com.example.ui.theme.status_taken_border
import com.example.ui.theme.status_taken_green

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentRemindersScreen(navController: NavController) {
    val context = LocalContext.current
    val appointments by HealthDataManager.appointments.collectAsStateWithLifecycle()
    val previousDoctors by HealthDataManager.previousDoctors.collectAsStateWithLifecycle()
    val labTests by HealthDataManager.labTests.collectAsStateWithLifecycle()
    val pharmacies by HealthDataManager.pharmacies.collectAsStateWithLifecycle()
    val facilities by HealthDataManager.facilities.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var preselectedDoctorName by remember { mutableStateOf<String?>(null) }
    var bookedSuccessMessage by remember { mutableStateOf<String?>(null) }

    if (showAccessibilityDialog) {
        AccessibilityDialog(onDismissRequest = { showAccessibilityDialog = false })
    }

    if (bookedSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { bookedSuccessMessage = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = status_taken_green, modifier = Modifier.size(36.dp)) },
            title = { Text("Booking Confirmed", fontWeight = FontWeight.Bold) },
            text = { Text(bookedSuccessMessage!!) },
            confirmButton = {
                Button(onClick = {
                    bookedSuccessMessage = null
                    selectedTab = 0 // Return to visits
                }) {
                    Text("View in Visits")
                }
            }
        )
    }

    if (showAddDialog) {
        ScheduleAppointmentDialog(
            initialDoctor = preselectedDoctorName,
            onDismiss = {
                showAddDialog = false
                preselectedDoctorName = null
            },
            onAdd = { newApt ->
                HealthDataManager.addAppointment(newApt)
                showAddDialog = false
                preselectedDoctorName = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Appointments & Care",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    AccessibilityTopBarAction(
                        onOpenDialog = { showAccessibilityDialog = true }
                    )
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Schedule appointment reminder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category Tabs
            val tabs = listOf("Visits (${appointments.size})", "Treating Doctors", "Book Labs", "Hospitals & Meds")
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Section Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Upcoming Consultations & Diagnostics",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Automated reminders with pre-visit preparation checklists",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // List of Appointments
                        items(appointments, key = { it.id }) { appointment ->
                            AppointmentCard(
                                appointment = appointment,
                                onToggleReminder = { HealthDataManager.toggleAppointmentReminder(appointment.id) },
                                onToggleCompleted = { HealthDataManager.toggleAppointmentCompleted(appointment.id) },
                                onExploreFacility = { navController.navigate("facilities") },
                                onNavigateMaps = {
                                    val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode("${appointment.facilityName}, ${appointment.departmentLocation}")}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    context.startActivity(mapIntent)
                                }
                            )
                        }
                    }

                    1 -> {
                        // Previous Doctors who treated the patient
                        item {
                            Column {
                                Text(
                                    text = "Doctors Who Previously Treated You",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Physicians from your longitudinal records & hospital consultations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(previousDoctors, key = { it.id }) { doctor ->
                            DoctorCard(
                                doctor = doctor,
                                onBookFollowup = {
                                    preselectedDoctorName = doctor.name
                                    showAddDialog = true
                                },
                                onCallDoctor = {
                                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${doctor.phone}"))
                                    context.startActivity(callIntent)
                                }
                            )
                        }
                    }

                    2 -> {
                        // Laboratory Tests Directory
                        item {
                            Column {
                                Text(
                                    text = "Diagnostic Lab Tests & Home Collection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "NABL accredited diagnostics with certified home phlebotomy",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(labTests, key = { it.id }) { test ->
                            LabTestCard(
                                test = test,
                                onBookTest = { homeCollection ->
                                    val apt = HealthDataManager.bookLabTest(test, "Monday, Oct 12, 2026", homeCollection)
                                    bookedSuccessMessage = "Successfully scheduled ${test.name} (${if (homeCollection) "Home Phlebotomist Visit" else "Walk-in Lab"}). Details added to your Visits."
                                }
                            )
                        }
                    }

                    3 -> {
                        // Pharmacies & Nearby Hospitals Directory
                        item {
                            Column {
                                Text(
                                    text = "Nearby 24x7 Pharmacies & Hospitals",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Emergency medicine, Jan Aushadhi generic stores & trauma centers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        item {
                            Text("24x7 Pharmacies & Generic Stores:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }

                        items(pharmacies, key = { it.id }) { pharmacy ->
                            PharmacyCard(
                                pharmacy = pharmacy,
                                onCallPharmacy = {
                                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pharmacy.phone}"))
                                    context.startActivity(callIntent)
                                },
                                onNavigatePharmacy = {
                                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode("${pharmacy.name}, ${pharmacy.address}")}"))
                                    context.startActivity(mapIntent)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Nearby Hospital Emergency Centers:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                TextButton(onClick = { navController.navigate("emergency_hospitals") }) {
                                    Text("View Live Beds →")
                                }
                            }
                        }

                        items(facilities.take(3), key = { it.id }) { fac ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fac.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${fac.type} • ${fac.distanceKm} km away", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("ICU Beds: ${fac.icuBedsAvailable} • ER Beds: ${fac.emergencyBedsAvailable}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = {
                                        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(fac.name)}"))
                                        context.startActivity(mapIntent)
                                    }) {
                                        Icon(Icons.Default.Directions, contentDescription = "Navigate", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentCard(
    appointment: AppointmentReminder,
    onToggleReminder: () -> Unit,
    onToggleCompleted: () -> Unit,
    onExploreFacility: () -> Unit,
    onNavigateMaps: () -> Unit
) {
    // Local state for interactive checkboxes on the preparation checklist
    val checkedItems = remember { mutableStateMapOf<Int, Boolean>() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appointment.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (!appointment.isCompleted) 1.5.dp else 1.dp,
            color = if (!appointment.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Countdown badge & Reminder Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (appointment.isCompleted) MaterialTheme.colorScheme.surfaceVariant else status_due_bg,
                    border = BorderStroke(1.dp, if (appointment.isCompleted) MaterialTheme.colorScheme.outlineVariant else status_due_amber)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (appointment.isCompleted) Icons.Default.CheckCircle else Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (appointment.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else status_due_amber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (appointment.isCompleted) "COMPLETED" else appointment.relativeDays.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (appointment.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else status_due_amber
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Reminder Bell Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (appointment.reminderEnabled) "Reminder ON" else "Reminder OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (appointment.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onToggleReminder,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (appointment.reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = "Toggle reminder notification",
                            tint = if (appointment.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Details
            Text(
                text = appointment.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${appointment.doctorName} • ${appointment.specialty}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date, Time & Location Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${appointment.date} at ${appointment.time}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${appointment.facilityName} (${appointment.departmentLocation})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Preparation Checklist
            if (appointment.preparationChecklist.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Patient Preparation Checklist:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                appointment.preparationChecklist.forEachIndexed { index, item ->
                    val isChecked = checkedItems[index] == true
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checkedItems[index] = it },
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Clinical Notes
            if (appointment.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.MedicalInformation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Clinical Focus: ${appointment.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateMaps,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Navigate")
                }

                Button(
                    onClick = onToggleCompleted,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (appointment.isCompleted) status_taken_green else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (appointment.isCompleted) Icons.Default.Check else Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (appointment.isCompleted) "Completed" else "Mark Done")
                }
            }
        }
    }
}

@Composable
fun DoctorCard(
    doctor: Doctor,
    onBookFollowup: () -> Unit,
    onCallDoctor: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(doctor.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${doctor.specialty} • ${doctor.qualification}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(doctor.facilityName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Previously Treated For:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(doctor.pastTreatedCondition, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Last Consultation: ${doctor.lastTreatedDate} • Fee: ${doctor.consultationFee}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCallDoctor,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Clinic")
                }

                Button(
                    onClick = onBookFollowup,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Book Visit")
                }
            }
        }
    }
}

@Composable
fun LabTestCard(
    test: LabTest,
    onBookTest: (homeCollection: Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (test.isRecommendedForPatient) 1.5.dp else 1.dp,
            color = if (test.isRecommendedForPatient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(test.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(test.price, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (test.isRecommendedForPatient) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = status_due_bg,
                    border = BorderStroke(1.dp, status_due_amber)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = status_due_amber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(test.recommendedReason, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = status_due_amber)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(test.testDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text("${test.fastingRequired} • Results in ${test.reportTurnaround}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onBookTest(false) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Walk-in Lab")
                }

                Button(
                    onClick = { onBookTest(true) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Home Sample")
                }
            }
        }
    }
}

@Composable
fun PharmacyCard(
    pharmacy: Pharmacy,
    onCallPharmacy: () -> Unit,
    onNavigatePharmacy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(pharmacy.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("${pharmacy.type} • ${pharmacy.distanceKm} km away", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(pharmacy.openHours, style = MaterialTheme.typography.labelSmall, color = status_taken_green, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(pharmacy.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (pharmacy.discountOffer.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = status_taken_bg,
                    border = BorderStroke(1.dp, status_taken_border)
                ) {
                    Text(pharmacy.discountOffer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = status_taken_green)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCallPharmacy,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call Chemist")
                }

                Button(
                    onClick = onNavigatePharmacy,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Directions")
                }
            }
        }
    }
}

@Composable
fun ScheduleAppointmentDialog(
    initialDoctor: String? = null,
    onDismiss: () -> Unit,
    onAdd: (AppointmentReminder) -> Unit
) {
    var title by remember { mutableStateOf(if (!initialDoctor.isNullOrBlank()) "Follow-up with $initialDoctor" else "") }
    var doctorName by remember { mutableStateOf(initialDoctor ?: "") }
    var specialty by remember { mutableStateOf("") }
    var facilityName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("10:00 AM") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Schedule Appointment Reminder",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Appointment Title (e.g. Eye Exam)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Doctor / Specialist Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = facilityName,
                    onValueChange = { facilityName = it },
                    label = { Text("Hospital or Clinic Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (e.g. Oct 15, 2026)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (e.g. 10:30 AM)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Preparation Notes (e.g. Fasting)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val newApt = AppointmentReminder(
                            id = "apt_${System.currentTimeMillis()}",
                            title = title.trim(),
                            doctorName = if (doctorName.isBlank()) "Specialist Doctor" else doctorName.trim(),
                            specialty = if (specialty.isBlank()) "General Medicine" else specialty.trim(),
                            facilityName = if (facilityName.isBlank()) "Community Health Center" else facilityName.trim(),
                            departmentLocation = "OPD Department",
                            date = if (date.isBlank()) "Upcoming" else date.trim(),
                            time = time.trim(),
                            relativeDays = "Upcoming",
                            preparationChecklist = if (notes.isNotBlank()) listOf(notes.trim()) else listOf("Carry current medical records and prescription"),
                            notes = notes.trim(),
                            reminderEnabled = true,
                            isCompleted = false
                        )
                        onAdd(newApt)
                    }
                },
                modifier = Modifier.height(48.dp)
            ) {
                Text("Save Reminder", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
