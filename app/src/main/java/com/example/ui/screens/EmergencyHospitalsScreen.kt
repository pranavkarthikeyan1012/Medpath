package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.example.data.HealthDataManager
import com.example.data.HealthcareFacility
import com.example.ui.components.AccessibilityDialog
import com.example.ui.components.AccessibilityTopBarAction
import com.example.ui.theme.status_due_amber
import com.example.ui.theme.status_due_bg
import com.example.ui.theme.status_taken_bg
import com.example.ui.theme.status_taken_green

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyHospitalsScreen(navController: NavController) {
    val facilities by HealthDataManager.facilities.collectAsStateWithLifecycle()
    val activeReservation by HealthDataManager.activeEmergencyReservation.collectAsStateWithLifecycle()
    val profile by HealthDataManager.userProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var facilityToReserve by remember { mutableStateOf<HealthcareFacility?>(null) }
    var selectedFilter by remember { mutableStateOf("All Nearby") }
    var searchQuery by remember { mutableStateOf("") }

    val filterOptions = listOf("All Nearby", "ICU Beds Available", "ER Beds Available", "Trauma Level 1", "Government", "Private")

    val emergencyFacilities = facilities.filter { fac ->
        val matchesSearch = searchQuery.isBlank() ||
            fac.name.contains(searchQuery, ignoreCase = true) ||
            fac.type.contains(searchQuery, ignoreCase = true) ||
            fac.address.contains(searchQuery, ignoreCase = true) ||
            fac.departments.any { it.contains(searchQuery, ignoreCase = true) }

        val matchesFilter = when (selectedFilter) {
            "ICU Beds Available" -> fac.icuBedsAvailable > 0
            "ER Beds Available" -> fac.emergencyBedsAvailable > 0
            "Trauma Level 1" -> fac.traumaLevel.contains("Level 1", ignoreCase = true)
            "Government" -> fac.type.contains("Government", ignoreCase = true)
            "Private" -> fac.type.contains("Private", ignoreCase = true)
            else -> true
        }

        matchesSearch && matchesFilter
    }.sortedWith(compareBy({ !it.isRecommendedEmergency }, { it.distanceKm }))

    if (showAccessibilityDialog) {
        AccessibilityDialog(onDismissRequest = { showAccessibilityDialog = false })
    }

    if (facilityToReserve != null) {
        ReserveEmergencyBedDialog(
            facility = facilityToReserve!!,
            patientName = profile.name,
            allergies = profile.allergies,
            onDismiss = { facilityToReserve = null },
            onConfirm = { bedType, complaint ->
                HealthDataManager.reserveEmergencyBed(facilityToReserve!!.id, bedType, complaint)
                facilityToReserve = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Emergency Hospitals & Beds", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Text("Live Bed & Slot Availability", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // SOS Emergency Hotline Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Emergency, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Immediate Life Threat?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("Dial National Ambulance / Trauma 108", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }

                            Button(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:108"))
                                    context.startActivity(dialIntent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call 108", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Fast Emergency Patient Medical Summary (Essential clinical data for EMTs/ER)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "PATIENT EMERGENCY TRIAGE CONTEXT:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${profile.name} (${profile.age}y/${profile.gender}) • Blood Group: ${profile.bloodGroup}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "⚠️ Known Allergies: ${if (profile.allergies.isNotEmpty()) profile.allergies.joinToString(", ") else "None recorded"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "• Chronic: Type 2 Diabetes • Emergency Contact: ${profile.emergencyContactName} (${profile.emergencyContactPhone})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Active Emergency Bed Reservation Banner (if reserved)
            if (activeReservation != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = status_taken_bg),
                        border = BorderStroke(2.dp, status_taken_green)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = status_taken_green, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("EMERGENCY BED RESERVED", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = status_taken_green)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = status_taken_green
                                ) {
                                    Text("HELD FOR 45 MIN", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(activeReservation!!.facilityName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Allocated: ${activeReservation!!.bedType} • Arrival ETA: ~${activeReservation!!.etaMinutes} mins", style = MaterialTheme.typography.bodyMedium)
                            Text("Chief Complaint: ${activeReservation!!.chiefComplaint}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(activeReservation!!.facilityName)}")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        context.startActivity(mapIntent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Directions")
                                }

                                OutlinedButton(
                                    onClick = { HealthDataManager.cancelEmergencyReservation() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Cancel Alert")
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar for Hospitals
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search hospitals by name, area, department...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Quick Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterOptions) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            // Recommended Hospitals Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nearby Emergency Facilities (${emergencyFacilities.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Live Updated", style = MaterialTheme.typography.labelSmall, color = status_taken_green, fontWeight = FontWeight.Bold)
                }
            }

            // List of Emergency Facilities
            items(emergencyFacilities, key = { it.id }) { facility ->
                EmergencyFacilityCard(
                    facility = facility,
                    onReserveClick = { facilityToReserve = facility },
                    onCallClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${facility.emergencyPhone.split("/").first().trim()}"))
                        context.startActivity(dialIntent)
                    }
                )
            }
        }
    }
}

@Composable
fun EmergencyFacilityCard(
    facility: HealthcareFacility,
    onReserveClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            if (facility.isRecommendedEmergency) 2.dp else 1.dp,
            if (facility.isRecommendedEmergency) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name, Distance, Recommended Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (facility.isRecommendedEmergency) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "★ TOP EMERGENCY RECOMMENDATION",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Text(
                        text = facility.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${facility.type} • ${facility.traumaLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${facility.distanceKm} km", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("~${facility.travelTimeMinutes} mins", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Bed Availability Dashboard Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LIVE BED AVAILABILITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(status_taken_green))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Real-time telemetry", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BedMetricPill(
                            modifier = Modifier.weight(1f),
                            label = "ICU Beds",
                            available = facility.icuBedsAvailable,
                            total = facility.icuBedsTotal,
                            isCritical = facility.icuBedsAvailable == 0
                        )
                        BedMetricPill(
                            modifier = Modifier.weight(1f),
                            label = "ER Acute Beds",
                            available = facility.emergencyBedsAvailable,
                            total = facility.emergencyBedsTotal,
                            isCritical = facility.emergencyBedsAvailable == 0
                        )
                        BedMetricPill(
                            modifier = Modifier.weight(1f),
                            label = "General Beds",
                            available = facility.generalBedsAvailable,
                            total = facility.generalBedsTotal,
                            isCritical = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ER Wait Time & Available Emergency Slots
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = status_due_amber)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ER Wait Time: ~${facility.erWaitTimeMinutes} mins (Immediate Triage)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Open Slots: ${facility.availableEmergencySlots.joinToString(" • ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Call ER & Reserve Bed / Alert ER
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onCallClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call ER", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onReserveClick,
                    modifier = Modifier.weight(1.3f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hold Bed & Alert ER", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BedMetricPill(
    modifier: Modifier,
    label: String,
    available: Int,
    total: Int,
    isCritical: Boolean
) {
    val statusColor = when {
        isCritical || available == 0 -> MaterialTheme.colorScheme.error
        available <= 2 -> status_due_amber
        else -> status_taken_green
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ReserveEmergencyBedDialog(
    facility: HealthcareFacility,
    patientName: String,
    allergies: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (bedType: String, complaint: String) -> Unit
) {
    var selectedBedType by remember { mutableStateOf("Emergency / ER Bed") }
    var chiefComplaint by remember { mutableStateOf("Acute shortness of breath & dizziness") }

    val bedOptions = listOf("Emergency / ER Bed", "ICU Bed", "Cardiac / Dialysis Bay", "General Ward")
    val complaintSuggestions = listOf(
        "Acute shortness of breath & dizziness",
        "Severe hypoglycemia / blood sugar drop",
        "Chest tightness / palpitations",
        "High fever with confusion",
        "Trauma / accidental injury"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Emergency, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reserve Emergency Bed & Alert ER", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Notify ${facility.name} that patient $patientName is en route. Hospital staff will prepare triage bay and allocate a bed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Bed Type Selector
                Text("Select Required Bed Type:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    bedOptions.forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedBedType == opt,
                                onClick = { selectedBedType = opt }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(opt, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Text("Chief Emergency Complaint:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = chiefComplaint,
                    onValueChange = { chiefComplaint = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Quick suggestions
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(complaintSuggestions) { suggestion ->
                        SuggestionChip(
                            onClick = { chiefComplaint = suggestion },
                            label = { Text(suggestion.take(24) + "...", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Alert summary note
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Transmitted with alert:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "• Blood Group: B+ • Drug Allergies: ${allergies.joinToString(", ")} • Condition: Type 2 Diabetes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedBedType, chiefComplaint) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Confirm & Transmit Alert", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel")
            }
        }
    )
}
