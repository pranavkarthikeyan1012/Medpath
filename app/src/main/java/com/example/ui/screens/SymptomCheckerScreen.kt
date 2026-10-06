package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import com.example.ai.GeminiSymptomCheckerService
import com.example.ai.SymptomAssessmentResult
import com.example.data.HealthDataManager
import com.example.data.MedicalEvent
import com.example.ui.components.AccessibilityDialog
import com.example.ui.components.AccessibilityTopBarAction
import com.example.ui.theme.status_due_amber
import com.example.ui.theme.status_due_bg
import com.example.ui.theme.status_taken_bg
import com.example.ui.theme.status_taken_green
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomCheckerScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profile by HealthDataManager.userProfile.collectAsStateWithLifecycle()
    val medications by HealthDataManager.medications.collectAsStateWithLifecycle()

    var showAccessibilityDialog by remember { mutableStateOf(false) }

    var symptomDescription by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf("1-6 hours") }
    var selectedSeverity by remember { mutableStateOf("Moderate") }
    var includeProfileContext by remember { mutableStateOf(true) }

    var isAnalyzing by remember { mutableStateOf(false) }
    var assessmentResult by remember { mutableStateOf<SymptomAssessmentResult?>(null) }
    var isSavedToHistory by remember { mutableStateOf(false) }

    var selectedLanguage by remember { mutableStateOf("English") }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val indianLanguages = listOf(
        "English",
        "தமிழ் (Tamil)",
        "हिन्दी (Hindi)",
        "తెలుగు (Telugu)",
        "ಕನ್ನಡ (Kannada)",
        "മലയാളം (Malayalam)",
        "বাংলা (Bengali)",
        "मराठी (Marathi)",
        "ગુજરાતી (Gujarati)",
        "ਪੰਜਾਬੀ (Punjabi)",
        "ଓଡ଼ିଆ (Odia)"
    )

    val quickSymptoms = listOf(
        "Dizziness & shakiness",
        "Unusual fatigue / weakness",
        "Frequent thirst & urination",
        "Shortness of breath",
        "Swollen feet or ankles",
        "Nausea / loss of appetite",
        "Headache with blurred vision",
        "Cold sweats & palpitations"
    )

    val durations = listOf("Under 1 hour", "1-6 hours", "1-2 days", "Over 1 week")
    val severities = listOf("Mild", "Moderate", "Severe")

    if (showAccessibilityDialog) {
        AccessibilityDialog(onDismissRequest = { showAccessibilityDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Symptom Checker", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        }
                        Text("Powered by Gemini 3.5 Flash", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    // Language Selector Chip
                    Box {
                        AssistChip(
                            onClick = { showLanguageMenu = true },
                            label = { Text(selectedLanguage.split(" ").first(), fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Translate, contentDescription = "Language", modifier = Modifier.size(16.dp)) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                        )

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            indianLanguages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        selectedLanguage = lang
                                        showLanguageMenu = false
                                        Toast.makeText(context, "Symptom language set to $lang", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

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
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Patient Context Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (includeProfileContext) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Personalized Clinical Context", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Switch(
                                checked = includeProfileContext,
                                onCheckedChange = { includeProfileContext = it }
                            )
                        }

                        if (includeProfileContext) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Evaluating with Arun Kumar (52/M) • Type 2 Diabetes • Creatinine 1.6 mg/dL • Known Allergies: ${profile.allergies.joinToString(", ")} • Active Meds: Metformin, Glimepiride, Atorvastatin",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Quick Symptom Suggestions
            item {
                Column {
                    Text(
                        text = "Common Symptoms (Tap to add):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickSymptoms) { symptom ->
                            SuggestionChip(
                                onClick = {
                                    symptomDescription = if (symptomDescription.isBlank()) {
                                        symptom
                                    } else {
                                        "$symptomDescription, $symptom"
                                    }
                                },
                                label = { Text(symptom, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }
            }

            // Symptom Description Input
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Describe What You Are Experiencing *",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Include details like location, triggers (e.g. after meals or exertion), and any feelings of fever or pain.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = symptomDescription,
                            onValueChange = { symptomDescription = it },
                            placeholder = { Text("e.g. Started feeling very shaky and sweaty about an hour after lunch, with mild dizziness...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Duration Selector
                        Text("Onset / Duration:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(durations) { dur ->
                                FilterChip(
                                    selected = selectedDuration == dur,
                                    onClick = { selectedDuration = dur },
                                    label = { Text(dur, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Severity Selector
                        Text("Perceived Severity:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            severities.forEach { sev ->
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = selectedSeverity == sev,
                                    onClick = { selectedSeverity = sev },
                                    label = {
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Text(sev, fontWeight = if (selectedSeverity == sev) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Analyze Action Button
                        Button(
                            onClick = {
                                if (symptomDescription.isNotBlank()) {
                                    isAnalyzing = true
                                    assessmentResult = null
                                    isSavedToHistory = false
                                    coroutineScope.launch {
                                        val result = GeminiSymptomCheckerService.analyzeSymptoms(
                                            symptoms = symptomDescription,
                                            duration = selectedDuration,
                                            severity = selectedSeverity,
                                            profile = if (includeProfileContext) profile else null,
                                            activeMedications = if (includeProfileContext) medications else null,
                                            language = selectedLanguage
                                        )
                                        assessmentResult = result
                                        isAnalyzing = false
                                    }
                                }
                            },
                            enabled = symptomDescription.isNotBlank() && !isAnalyzing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Analyzing with Gemini AI...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyze Symptoms with Gemini AI", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Results Section
            if (assessmentResult != null) {
                val result = assessmentResult!!
                item {
                    val urgencyColor = when (result.urgencyLevel) {
                        "EMERGENCY" -> MaterialTheme.colorScheme.error
                        "URGENT" -> status_due_amber
                        else -> status_taken_green
                    }

                    val urgencyBg = when (result.urgencyLevel) {
                        "EMERGENCY" -> MaterialTheme.colorScheme.errorContainer
                        "URGENT" -> status_due_bg
                        else -> status_taken_bg
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(2.dp, urgencyColor)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Triage Level Header
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = urgencyBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (result.urgencyLevel) {
                                            "EMERGENCY" -> Icons.Default.Emergency
                                            "URGENT" -> Icons.Default.Warning
                                            else -> Icons.Default.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = urgencyColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "TRIAGE LEVEL: ${result.urgencyLevel}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = urgencyColor
                                        )
                                        Text(
                                            text = result.summaryTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Primary Assessment Narrative
                            Text(
                                text = "Clinical Assessment & Explanation:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.primaryAssessment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Immediate Action Steps
                            Text(
                                text = "Recommended Immediate Steps:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            result.immediateSteps.forEachIndexed { index, step ->
                                Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(step, style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Medication & Safety Contraindications
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Medication Safety & Contraindications", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    result.medicationSafetyNotes.forEach { note ->
                                        Text("• $note", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 2.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Red Flag Warnings
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Red Flag Emergency Signs", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    result.redFlagWarnings.forEach { warning ->
                                        Text("• $warning", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(bottom = 2.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Action Links based on Urgency
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (result.urgencyLevel == "EMERGENCY" || result.urgencyLevel == "URGENT") {
                                    Button(
                                        onClick = { navController.navigate("emergency_hospitals") },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Find Nearby Emergency Hospitals with Live Beds →", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        HealthDataManager.addMedicalEvent(
                                            MedicalEvent(
                                                id = "evt_sym_${System.currentTimeMillis() % 10000}",
                                                date = "Today",
                                                type = "Consultation",
                                                title = "AI Symptom Evaluation: ${result.summaryTitle}",
                                                finding = "Reported: $symptomDescription ($selectedDuration). Triage: ${result.urgencyLevel}. ${result.immediateSteps.firstOrNull() ?: ""}",
                                                source = "MedPath AI Symptom Checker (Gemini 3.5)",
                                                relatedCondition = "Symptom Evaluation",
                                                severity = if (result.urgencyLevel == "EMERGENCY") "Critical" else if (result.urgencyLevel == "URGENT") "Attention" else "Routine",
                                                doctorName = result.recommendedDoctorSpecialty,
                                                notes = result.primaryAssessment.take(180) + "..."
                                            )
                                        )
                                        isSavedToHistory = true
                                        Toast.makeText(context, "Saved to your Medical Records Timeline!", Toast.LENGTH_SHORT).show()
                                    },
                                    enabled = !isSavedToHistory,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        if (isSavedToHistory) Icons.Default.Check else Icons.Default.BookmarkAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isSavedToHistory) "Saved to Medical History ✓" else "Save Assessment to Medical Timeline", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        symptomDescription = ""
                                        assessmentResult = null
                                        isSavedToHistory = false
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Check Another Symptom")
                                }
                            }
                        }
                    }
                }
            }

            // Clinical Disclaimer Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Disclaimer: MedPath AI Symptom Checker uses Gemini 3.5 Flash to provide preliminary clinical triage guidance and medication safety awareness. This is not a diagnosis. If you believe you are experiencing a life-threatening medical emergency, call 108 or proceed to the nearest emergency room immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
