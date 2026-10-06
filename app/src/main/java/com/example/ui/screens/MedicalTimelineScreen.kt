package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.HealthDataManager
import com.example.data.MedicalEvent
import com.example.ui.components.AccessibilityDialog
import com.example.ui.components.AccessibilityTopBarAction
import com.example.ui.theme.status_due_amber
import com.example.ui.theme.status_due_bg
import com.example.ui.theme.status_taken_bg
import com.example.ui.theme.status_taken_green

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalTimelineScreen(navController: NavController) {
    val events by HealthDataManager.medicalEvents.collectAsStateWithLifecycle()
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var selectedEventForDetail by remember { mutableStateOf<MedicalEvent?>(null) }
    var eventToEdit by remember { mutableStateOf<MedicalEvent?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Diagnosis", "Lab Result", "Prescription", "Procedure", "Consultation", "Hospitalization")

    val filteredEvents = events.filter { event ->
        val matchesCategory = selectedCategory == "All" || event.type.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
            event.title.contains(searchQuery, ignoreCase = true) ||
            event.finding.contains(searchQuery, ignoreCase = true) ||
            event.source.contains(searchQuery, ignoreCase = true) ||
            event.relatedCondition.contains(searchQuery, ignoreCase = true) ||
            event.doctorName.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    if (showAccessibilityDialog) {
        AccessibilityDialog(onDismissRequest = { showAccessibilityDialog = false })
    }

    if (showAddEventDialog || eventToEdit != null) {
        AddEditMedicalEventDialog(
            existingEvent = eventToEdit,
            onDismiss = {
                showAddEventDialog = false
                eventToEdit = null
            },
            onSave = { savedEvent ->
                if (eventToEdit != null) {
                    HealthDataManager.updateMedicalEvent(savedEvent)
                } else {
                    HealthDataManager.addMedicalEvent(savedEvent)
                }
                showAddEventDialog = false
                eventToEdit = null
            }
        )
    }

    if (selectedEventForDetail != null) {
        MedicalEventDetailDialog(
            event = selectedEventForDetail!!,
            onDismiss = { selectedEventForDetail = null },
            onEdit = {
                eventToEdit = selectedEventForDetail
                selectedEventForDetail = null
            },
            onDelete = {
                HealthDataManager.deleteMedicalEvent(selectedEventForDetail!!.id)
                selectedEventForDetail = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Medical History Tracker", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("${events.size} chronological records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    IconButton(onClick = { showAddEventDialog = true }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Medical Record", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddEventDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Log Medical Record", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // High-Level History Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Patient Longitudinal History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Verified health journey spanning 2019 to 2026", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HistoryStatTile(modifier = Modifier.weight(1f), label = "Total Events", value = "${events.size}")
                            HistoryStatTile(modifier = Modifier.weight(1f), label = "Active Conditions", value = "2 Tracked")
                            HistoryStatTile(modifier = Modifier.weight(1f), label = "Span", value = "7 Years")
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search records by condition, doctor, lab...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(categories) { category ->
                        val count = if (category == "All") events.size else events.count { it.type.equals(category, ignoreCase = true) }
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text("$category ($count)", fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            // Events List or Empty State
            if (filteredEvents.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No medical records found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (searchQuery.isNotBlank()) "No entries matched '$searchQuery'" else "No entries under '$selectedCategory'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(onClick = { showAddEventDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log New Record")
                            }
                        }
                    }
                }
            } else {
                items(filteredEvents, key = { it.id }) { event ->
                    TimelineCardItem(
                        event = event,
                        onClick = { selectedEventForDetail = event },
                        onEdit = { eventToEdit = event },
                        onDelete = { HealthDataManager.deleteMedicalEvent(event.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryStatTile(modifier: Modifier, label: String, value: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TimelineCardItem(
    event: MedicalEvent,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = when (event.type) {
        "Diagnosis" -> MaterialTheme.colorScheme.primary
        "Lab Result" -> Color(0xFF6B4FA0)
        "Prescription" -> Color(0xFF00796B)
        "Procedure" -> Color(0xFFC25400)
        "Hospitalization" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

    val categoryIcon = when (event.type) {
        "Diagnosis" -> Icons.Default.MedicalInformation
        "Lab Result" -> Icons.Default.Biotech
        "Prescription" -> Icons.Default.Medication
        "Procedure" -> Icons.Default.Visibility
        "Hospitalization" -> Icons.Default.LocalHospital
        else -> Icons.Default.Person
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.Top
    ) {
        // Vertical Timeline Node & Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(42.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = categoryColor,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(categoryIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(90.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Card Content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = categoryColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = event.type.uppercase(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor
                        )
                    }

                    Text(
                        text = event.date,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = event.finding,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = event.source,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    if (event.severity == "Attention" || event.severity == "Critical") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = status_due_bg
                        ) {
                            Text(
                                text = "MONITORED",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = status_due_amber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedicalEventDetailDialog(
    event: MedicalEvent,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(event.type.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Clinical Finding:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(event.finding, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Date Logged", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(event.date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Condition", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(event.relatedCondition, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (event.doctorName.isNotBlank()) {
                    Column {
                        Text("Physician / Evaluator", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(event.doctorName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }

                Column {
                    Text("Facility / Source", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(event.source, style = MaterialTheme.typography.bodyMedium)
                }

                if (event.notes.isNotBlank()) {
                    Column {
                        Text("Clinical Remarks & Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(event.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        }
    )
}

@Composable
fun AddEditMedicalEventDialog(
    existingEvent: MedicalEvent?,
    onDismiss: () -> Unit,
    onSave: (MedicalEvent) -> Unit
) {
    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var type by remember { mutableStateOf(existingEvent?.type ?: "Lab Result") }
    var date by remember { mutableStateOf(existingEvent?.date ?: "September 2026") }
    var finding by remember { mutableStateOf(existingEvent?.finding ?: "") }
    var source by remember { mutableStateOf(existingEvent?.source ?: "District Hospital B") }
    var relatedCondition by remember { mutableStateOf(existingEvent?.relatedCondition ?: "Type 2 Diabetes") }
    var doctorName by remember { mutableStateOf(existingEvent?.doctorName ?: "") }
    var notes by remember { mutableStateOf(existingEvent?.notes ?: "") }

    val typeOptions = listOf("Diagnosis", "Lab Result", "Prescription", "Procedure", "Consultation", "Hospitalization")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existingEvent != null) "Edit Medical Event" else "Log New Medical Event",
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
                    label = { Text("Event Title *") },
                    placeholder = { Text("e.g. Fasting Blood Sugar Test") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Category Selector
                Text("Category:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(typeOptions) { opt ->
                        FilterChip(
                            selected = type == opt,
                            onClick = { type = opt },
                            label = { Text(opt, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = relatedCondition,
                        onValueChange = { relatedCondition = it },
                        label = { Text("Condition") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = finding,
                    onValueChange = { finding = it },
                    label = { Text("Clinical Finding / Results *") },
                    placeholder = { Text("e.g. Normal values, dosage change, or lab reading") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Facility / Clinic") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Doctor Name (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Clinical Remarks & Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && finding.isNotBlank()) {
                        val event = MedicalEvent(
                            id = existingEvent?.id ?: "evt_${System.currentTimeMillis() % 10000}",
                            title = title.trim(),
                            type = type,
                            date = date.trim(),
                            finding = finding.trim(),
                            source = source.trim(),
                            relatedCondition = relatedCondition.trim(),
                            doctorName = doctorName.trim(),
                            notes = notes.trim(),
                            severity = if (finding.contains("elevated", ignoreCase = true) || finding.contains("abnormal", ignoreCase = true)) "Attention" else "Routine"
                        )
                        onSave(event)
                    }
                },
                enabled = title.isNotBlank() && finding.isNotBlank(),
                modifier = Modifier.height(48.dp)
            ) {
                Text(if (existingEvent != null) "Save Changes" else "Log Record", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel")
            }
        }
    )
}
