package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.data.ChatMessage
import com.example.data.HealthDataManager
import com.example.ui.components.AccessibilityDialog
import com.example.ui.components.AccessibilityTopBarAction
import com.example.ui.theme.status_taken_bg
import com.example.ui.theme.status_taken_green
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatbotScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val chatMessages by HealthDataManager.chatMessages.collectAsStateWithLifecycle()
    val userProfile by HealthDataManager.userProfile.collectAsStateWithLifecycle()
    val medications by HealthDataManager.medications.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("English") }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var speakingMessageId by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Text to Speech
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Initialize default
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    val indianLanguages = listOf(
        "English",
        "हिन्दी (Hindi)",
        "தமிழ் (Tamil)",
        "తెలుగు (Telugu)",
        "ಕನ್ನಡ (Kannada)",
        "മലയാളം (Malayalam)",
        "বাংলা (Bengali)",
        "मराठी (Marathi)",
        "ગુજરાતી (Gujarati)",
        "ਪੰਜਾਬੀ (Punjabi)",
        "ଓଡ଼ିଆ (Odia)"
    )

    // Speech-To-Text Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
            }
        }
    }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your symptom or health question...")
            val langTag = when (selectedLanguage) {
                "हिन्दी (Hindi)" -> "hi-IN"
                "தமிழ் (Tamil)" -> "ta-IN"
                "తెలుగు (Telugu)" -> "te-IN"
                "ಕನ್ನಡ (Kannada)" -> "kn-IN"
                "മലയാളം (Malayalam)" -> "ml-IN"
                "বাংলা (Bengali)" -> "bn-IN"
                "मराठी (Marathi)" -> "mr-IN"
                "ગુજરાતી (Gujarati)" -> "gu-IN"
                "ਪੰਜਾਬੀ (Punjabi)" -> "pa-IN"
                "ଓଡ଼ିଆ (Odia)" -> "or-IN"
                else -> "en-IN"
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            // Voice recognition not available on some devices
        }
    }

    fun sanitizeTextForTts(rawText: String, lang: String): String {
        var clean = rawText
        // Fix TTS glitches on medical units like 1.6 mg/dL across all languages
        when {
            lang.contains("Tamil") -> {
                clean = clean
                    .replace("1.6 mg/dL", "1.6 மில்லிகிராம்")
                    .replace("1.6mg/dL", "1.6 மில்லிகிராம்")
                    .replace("1.6 mg/dl", "1.6 மில்லிகிராம்")
                    .replace("mg/dL", "மில்லிகிராம்")
                    .replace("mg/dl", "மில்லிகிராம்")
                    .replace("mg", "மில்லிகிராம்")
            }
            lang.contains("Hindi") -> {
                clean = clean
                    .replace("1.6 mg/dL", "1.6 मिलीग्राम प्रति डेसीलीटर")
                    .replace("1.6mg/dL", "1.6 मिलीग्राम प्रति डेसीलीटर")
                    .replace("1.6 mg/dl", "1.6 मिलीग्राम प्रति डेसीलीटर")
                    .replace("mg/dL", "मिलीग्राम प्रति डेसीलीटर")
                    .replace("mg/dl", "मिलीग्राम प्रति डेसीलीटर")
                    .replace("mg", "मिलीग्राम")
            }
            lang.contains("Telugu") -> {
                clean = clean
                    .replace("1.6 mg/dL", "1.6 మిల్లీగ్రాములు")
                    .replace("1.6mg/dL", "1.6 మిల్లీగ్రాములు")
                    .replace("mg/dL", "మిల్లీగ్రాములు")
                    .replace("mg", "మిల్లీగ్రాములు")
            }
            lang.contains("Kannada") -> {
                clean = clean
                    .replace("1.6 mg/dL", "1.6 ಮಿಲಿಗ್ರಾಂ")
                    .replace("1.6mg/dL", "1.6 ಮಿಲಿಗ್ರಾಂ")
                    .replace("mg/dL", "ಮಿಲಿಗ್ರಾಂ")
                    .replace("mg", "ಮಿಲಿಗ್ರಾಂ")
            }
            lang.contains("Malayalam") -> {
                clean = clean
                    .replace("1.6 mg/dL", "1.6 മില്ലിഗ്രാം")
                    .replace("1.6mg/dL", "1.6 മില്ലിഗ്രാം")
                    .replace("mg/dL", "മില്ലിഗ്രാം")
                    .replace("mg", "മില്ലിഗ്രാം")
            }
            lang.contains("Bengali") -> {
                clean = clean
                    .replace("1.6 mg/dL", "১.৬ মিলিগ্রাম")
                    .replace("1.6mg/dL", "১.৬ মিলিগ্রাম")
                    .replace("mg/dL", "মিলিগ্রাম")
            }
            lang.contains("Marathi") -> {
                clean = clean
                    .replace("1.6 mg/dL", "१.६ मिलीग्रॅम")
                    .replace("1.6mg/dL", "१.६ मिलीग्रॅम")
                    .replace("mg/dL", "मिलीग्रॅम")
            }
            lang.contains("Gujarati") -> {
                clean = clean
                    .replace("1.6 mg/dL", "૧.૬ મિલિગ્રામ")
                    .replace("1.6mg/dL", "૧.૬ મિલિગ્રામ")
                    .replace("mg/dL", "મિલિગ્રામ")
            }
            else -> {
                clean = clean
                    .replace("1.6 mg/dL", "1.6 milligrams per deciliter")
                    .replace("1.6mg/dL", "1.6 milligrams per deciliter")
                    .replace("1.6 mg/dl", "1.6 milligrams per deciliter")
                    .replace("mg/dL", "milligrams per deciliter")
                    .replace("mg/dl", "milligrams per deciliter")
                    .replace("mg", "milligram")
            }
        }
        // Clean symbols, markdown asterisks, brackets, and emojis which break TTS engines
        clean = clean
            .replace("⚠️", "Caution: ")
            .replace("🏥", "")
            .replace("🧪", "")
            .replace("📋", "")
            .replace("✅", "")
            .replace("💊", "")
            .replace(Regex("[*#•_~`\\[\\](){}<>|/\\\\^=+]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return clean
    }

    fun speakText(id: String, text: String) {
        if (speakingMessageId == id) {
            tts?.stop()
            speakingMessageId = null
        } else {
            tts?.stop()
            val locale = when (selectedLanguage) {
                "हिन्दी (Hindi)" -> Locale("hi", "IN")
                "தமிழ் (Tamil)" -> Locale("ta", "IN")
                "తెలుగు (Telugu)" -> Locale("te", "IN")
                "ಕನ್ನಡ (Kannada)" -> Locale("kn", "IN")
                "മലയാളം (Malayalam)" -> Locale("ml", "IN")
                "বাংলা (Bengali)" -> Locale("bn", "IN")
                "मराठी (Marathi)" -> Locale("mr", "IN")
                "ગુજરાતી (Gujarati)" -> Locale("gu", "IN")
                else -> Locale.ENGLISH
            }
            tts?.language = locale
            val spokenText = sanitizeTextForTts(text, selectedLanguage)
            tts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, id)
            speakingMessageId = id
        }
    }

    fun sendMessage(msg: String) {
        val trimmed = msg.trim()
        if (trimmed.isBlank() || isSending) return

        val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val userMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            text = trimmed,
            isFromUser = true,
            timestamp = nowTime,
            language = selectedLanguage
        )
        HealthDataManager.addChatMessage(userMsg)
        inputText = ""
        isSending = true

        coroutineScope.launch {
            val response = GeminiSymptomCheckerService.chatWithAi(
                userMessage = trimmed,
                language = selectedLanguage,
                chatHistory = chatMessages,
                profile = userProfile,
                activeMedications = medications
            )

            val aiMsg = ChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                text = response,
                isFromUser = false,
                timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                language = selectedLanguage
            )
            HealthDataManager.addChatMessage(aiMsg)
            isSending = false
            listState.animateScrollToItem(chatMessages.size)
        }
    }

    if (showAccessibilityDialog) {
        AccessibilityDialog(onDismissRequest = { showAccessibilityDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("MedPath AI Companion", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Context-Aware Clinical Chatbot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        val welcome = GeminiSymptomCheckerService.getWelcomeMessageInLanguage(lang)
                                        HealthDataManager.updateWelcomeMessage(welcome, lang)
                                        Toast.makeText(context, "AI language set to $lang", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    AccessibilityTopBarAction(onOpenDialog = { showAccessibilityDialog = true })
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Quick Prompts Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        item {
                            SuggestionChip(
                                onClick = { sendMessage("I have a mild headache and feel dizzy. Is this related to my diabetes?") },
                                label = { Text("Headache & Dizziness?", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = { sendMessage("What food should I eat to protect my kidneys with creatinine 1.6?") },
                                label = { Text("Diet for Creatinine 1.6", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = { sendMessage("Can I take Paracetamol with Metformin and Glimepiride?") },
                                label = { Text("Paracetamol + Metformin safe?", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = { navController.navigate("symptom_checker") },
                                label = { Text("Full Symptom Triage 🩺", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Voice Input Button
                        IconButton(
                            onClick = { startVoiceInput() },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask about symptoms, meds, diet...") },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { sendMessage(inputText) },
                            enabled = inputText.isNotBlank() && !isSending,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Patient Context Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Active Patient Context Loaded", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Arun Kumar (52M) • Type 2 Diabetes • Creatinine 1.6 • Allergies: Penicillin, Sulfa", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Language Selector Chips
                Text(
                    text = "AI Language (select to reply in native script):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(indianLanguages) { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = {
                                selectedLanguage = lang
                                val welcome = GeminiSymptomCheckerService.getWelcomeMessageInLanguage(lang)
                                HealthDataManager.updateWelcomeMessage(welcome, lang)
                                Toast.makeText(context, "AI language switched to $lang", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text(lang, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            items(chatMessages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    isSpeaking = speakingMessageId == message.id,
                    onSpeakToggle = { speakText(message.id, message.text) },
                    onNavigateTriage = { navController.navigate("symptom_checker") }
                )
            }

            if (isSending) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("MedPath AI is thinking in $selectedLanguage...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    onSpeakToggle: () -> Unit,
    onNavigateTriage: () -> Unit
) {
    val isUser = message.isFromUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
            tonalElevation = if (!isUser) 2.dp else 0.dp,
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isUser) {
                        IconButton(
                            onClick = onSpeakToggle,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Listen to response",
                                tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
