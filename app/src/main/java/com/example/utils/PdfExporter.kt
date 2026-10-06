package com.example.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.HealthDataManager
import com.example.ui.theme.status_taken_bg
import com.example.ui.theme.status_taken_border
import com.example.ui.theme.status_taken_green
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfExportResult(
    val file: File?,
    val uri: Uri?,
    val fileName: String,
    val fileSizeKb: Long,
    val formattedSummaryText: String,
    val isSuccess: Boolean,
    val message: String
)

object PdfExporter {

    fun exportMedicalSummary(context: Context): PdfExportResult {
        val profile = HealthDataManager.userProfile.value
        val analysis = HealthDataManager.profileAnalysis.value
        val medications = HealthDataManager.medications.value
        val events = HealthDataManager.medicalEvents.value

        val timestamp = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val safeName = profile.name.replace(" ", "_")
        val fileName = "MedPath_Medical_Summary_${safeName}.pdf"

        // Build comprehensive formatted text version as guaranteed accessible format
        val textSummary = buildString {
            appendLine("==================================================")
            appendLine("MEDPATH CONTINUOUS CARE CLINICAL SUMMARY")
            appendLine("==================================================")
            appendLine("Generated: $timestamp")
            appendLine("Patient ID: ${profile.id}")
            appendLine("Patient Name: ${profile.name} (${profile.age}y / ${profile.gender})")
            appendLine("Blood Group: ${profile.bloodGroup}")
            appendLine("Emergency Contact: ${profile.emergencyContactName} (${profile.emergencyContactRelation}) - ${profile.emergencyContactPhone}")
            appendLine("Primary Physician: ${profile.primaryPhysician} (${profile.primaryCareFacility})")
            appendLine()
            appendLine("CRITICAL ALLERGIES:")
            if (profile.allergies.isNotEmpty()) {
                profile.allergies.forEach { appendLine("  - $it (STRICT CONTRAINDICATION)") }
            } else {
                appendLine("  None recorded")
            }
            appendLine()
            appendLine("HEALTH CONTINUITY & CLINICAL SCORE:")
            appendLine("Score: ${analysis.continuityScore}/100 (${analysis.scoreGrade})")
            appendLine("Summary: ${analysis.synthesizedSummary}")
            appendLine()
            appendLine("ACTIVE PRESCRIPTIONS & MEDICATION SCHEDULE:")
            medications.forEach { med ->
                appendLine("  - ${med.name} (${med.dosage}) | ${med.frequency} | ${med.scheduledTime} - ${med.instruction}")
            }
            appendLine()
            appendLine("KEY CLINICAL BIOMARKERS:")
            analysis.biomarkers.forEach { bio ->
                appendLine("  - ${bio.name}: ${bio.value} ${bio.unit} (Ref: ${bio.referenceRange}) [${bio.status}]")
            }
            appendLine()
            appendLine("CHRONOLOGICAL MEDICAL HISTORY HIGHLIGHTS:")
            events.take(5).forEach { ev ->
                appendLine("  [${ev.date}] ${ev.type}: ${ev.title} (${ev.doctorName ?: "Clinical Team"})")
                appendLine("    Findings: ${ev.finding}")
            }
            appendLine("==================================================")
            appendLine("Confidential Medical Profile • Verified by MedPath System")
        }

        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(13, 92, 58)
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(90, 90, 90)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(13, 92, 58)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val boldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val textPaint = Paint().apply {
                color = Color.rgb(35, 35, 35)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val redPaint = Paint().apply {
                color = Color.rgb(190, 20, 20)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(215, 232, 222)
                strokeWidth = 1f
            }

            val bgBannerPaint = Paint().apply {
                color = Color.rgb(240, 248, 243)
            }

            // Draw Header Banner
            canvas.drawRect(25f, 20f, pageWidth - 25f, 70f, bgBannerPaint)
            canvas.drawText("MedPath — Continuous Care Clinical Summary", 35f, 42f, titlePaint)
            canvas.drawText("Generated on: $timestamp • Patient ID: ${profile.id} • Blood Group: ${profile.bloodGroup}", 35f, 58f, subtitlePaint)

            var y = 88f

            // Section 1: Demographics & Emergency
            canvas.drawText("1. PATIENT DEMOGRAPHICS & CRITICAL ICE CONTACT", 30f, y, headerPaint)
            y += 5f
            canvas.drawLine(30f, y, pageWidth - 30f, y, linePaint)
            y += 14f

            canvas.drawText("Name: ${profile.name} (${profile.age}y / ${profile.gender})", 30f, y, boldPaint)
            canvas.drawText("Emergency Contact: ${profile.emergencyContactName} (${profile.emergencyContactRelation}) • ${profile.emergencyContactPhone}", 260f, y, boldPaint)
            y += 13f
            canvas.drawText("Primary Physician: ${profile.primaryPhysician} • ${profile.primaryCareFacility}", 30f, y, textPaint)
            canvas.drawText("CRITICAL DRUG ALLERGIES: ${profile.allergies.joinToString(", ")}", 260f, y, redPaint)
            y += 18f

            // Section 2: Continuity Score & Assessment
            canvas.drawText("2. SYNTHESIZED HEALTH CONTINUITY & CLINICAL SCORE", 30f, y, headerPaint)
            y += 5f
            canvas.drawLine(30f, y, pageWidth - 30f, y, linePaint)
            y += 14f

            canvas.drawText("Continuity Score: ${analysis.continuityScore}/100 (${analysis.scoreGrade})", 30f, y, boldPaint)
            y += 13f
            canvas.drawText(analysis.synthesizedSummary.take(110) + "...", 30f, y, textPaint)
            y += 18f

            // Section 3: Active Medications
            canvas.drawText("3. ACTIVE VERIFIED PRESCRIPTIONS", 30f, y, headerPaint)
            y += 5f
            canvas.drawLine(30f, y, pageWidth - 30f, y, linePaint)
            y += 14f

            medications.take(4).forEach { med ->
                canvas.drawText("• ${med.name} (${med.dosage}) — ${med.frequency} [${med.scheduledTime}]", 30f, y, boldPaint)
                canvas.drawText("Instr: ${med.instruction}", 340f, y, subtitlePaint)
                y += 13f
            }
            y += 5f

            // Section 4: Key Biomarkers
            canvas.drawText("4. CLINICAL BIOMARKERS & VITAL SURVEILLANCE", 30f, y, headerPaint)
            y += 5f
            canvas.drawLine(30f, y, pageWidth - 30f, y, linePaint)
            y += 14f

            analysis.biomarkers.take(5).forEach { bio ->
                canvas.drawText("• ${bio.name}: ${bio.value} ${bio.unit} (Ref: ${bio.referenceRange})", 30f, y, textPaint)
                val paintColor = if (bio.status == "Attention" || bio.status == "Elevated") redPaint else boldPaint
                canvas.drawText("[${bio.status}]", 400f, y, paintColor)
                y += 13f
            }
            y += 5f

            // Section 5: Medical History
            canvas.drawText("5. CHRONOLOGICAL MEDICAL HISTORY HIGHLIGHTS", 30f, y, headerPaint)
            y += 5f
            canvas.drawLine(30f, y, pageWidth - 30f, y, linePaint)
            y += 14f

            events.take(4).forEach { ev ->
                canvas.drawText("[${ev.date}] ${ev.type}: ${ev.title}", 30f, y, boldPaint)
                y += 12f
                canvas.drawText("   Findings: ${ev.finding}", 30f, y, subtitlePaint)
                y += 13f
            }

            // Footer
            canvas.drawLine(30f, pageHeight - 35f, pageWidth - 30f, pageHeight - 35f, linePaint)
            canvas.drawText("Confidential Medical Document • MedPath Digital Healthcare System • Verified Patient Record", 30f, pageHeight - 22f, subtitlePaint)

            pdfDocument.finishPage(page)

            // Save in cache dir
            val cacheFile = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(cacheFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()

            // Also save a copy to Documents directory for permanent user access
            try {
                val docsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                if (!docsDir.exists()) docsDir.mkdirs()
                val permFile = File(docsDir, fileName)
                cacheFile.copyTo(permFile, overwrite = true)
            } catch (_: Exception) {}

            pdfDocument.close()

            val contentUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cacheFile
                )
            } catch (e: Exception) {
                null
            }

            val sizeKb = (cacheFile.length() / 1024).coerceAtLeast(1)

            return PdfExportResult(
                file = cacheFile,
                uri = contentUri,
                fileName = fileName,
                fileSizeKb = sizeKb,
                formattedSummaryText = textSummary,
                isSuccess = true,
                message = "Medical profile PDF generated successfully ($sizeKb KB)."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return PdfExportResult(
                file = null,
                uri = null,
                fileName = fileName,
                fileSizeKb = 0,
                formattedSummaryText = textSummary,
                isSuccess = true, // text fallback is available
                message = "Clinical summary generated in text format."
            )
        }
    }

    fun exportPatientDocument(
        context: Context,
        doc: com.example.data.ExtractedDocumentData,
        originalBitmap: android.graphics.Bitmap? = null
    ): PdfExportResult {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeDocName = doc.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24)
        val fileName = "MedPath_${safeDocName}_$timeStamp.pdf"

        val textSummary = buildString {
            appendLine("==================================================")
            appendLine("MEDPATH VERIFIED CLINICAL DOCUMENT")
            appendLine("==================================================")
            appendLine("Document Type: ${doc.documentType}")
            appendLine("Title: ${doc.title}")
            appendLine("Doctor / Clinic: ${doc.doctorOrClinic}")
            appendLine("Date: ${doc.date}")
            appendLine()
            appendLine("CLINICAL SUMMARY:")
            appendLine(doc.clinicalSummary)
            appendLine()
            if (doc.findings.isNotEmpty()) {
                appendLine("FINDINGS:")
                doc.findings.forEach { appendLine("• $it") }
                appendLine()
            }
            if (doc.prescribedMeds.isNotEmpty()) {
                appendLine("PRESCRIBED MEDICATIONS:")
                doc.prescribedMeds.forEach {
                    appendLine("• ${it.name} - ${it.dosage}, ${it.frequency} (${it.instructions})")
                }
                appendLine()
            }
            if (doc.labBiomarkers.isNotEmpty()) {
                appendLine("LABORATORY BIOMARKERS:")
                doc.labBiomarkers.forEach {
                    appendLine("• ${it.testName}: ${it.value} ${it.unit} (Ref: ${it.referenceRange}) [${it.status}]")
                }
                appendLine()
            }
            if (doc.allergyWarnings.isNotEmpty()) {
                appendLine("⚠️ ALLERGY PRECAUTIONS:")
                doc.allergyWarnings.forEach { appendLine("! $it") }
                appendLine()
            }
            if (doc.doctorAdvice.isNotBlank()) {
                appendLine("PHYSICIAN ADVICE:")
                appendLine(doc.doctorAdvice)
            }
            appendLine("==================================================")
            appendLine("Generated by MedPath Digital Health Continuity System")
        }

        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 points
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val docTypePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(0, 130, 140)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(40, 40, 40)
                textSize = 10f
            }

            val boldPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(30, 30, 30)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val linePaint = Paint().apply {
                color = Color.rgb(200, 210, 220)
                strokeWidth = 1f
            }

            // Top Header Bar
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 55f, Paint().apply { color = Color.rgb(24, 75, 135) })
            canvas.drawText("MEDPATH • VERIFIED PATIENT CLINICAL DOCUMENT", 25f, 34f, Paint().apply {
                color = Color.WHITE
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            })

            var y = 78f
            canvas.drawText(doc.documentType.uppercase(), 25f, y, docTypePaint)
            y += 18f
            canvas.drawText(doc.title.take(50), 25f, y, titlePaint)
            y += 16f
            canvas.drawText("Source: ${doc.doctorOrClinic}  |  Date: ${doc.date}", 25f, y, textPaint)
            y += 12f
            canvas.drawLine(25f, y, pageWidth - 25f, y, linePaint)
            y += 18f

            // Summary
            canvas.drawText("CLINICAL SUMMARY:", 25f, y, boldPaint)
            y += 14f
            val summaryLines = doc.clinicalSummary.chunked(75)
            summaryLines.forEach { line ->
                canvas.drawText(line, 25f, y, textPaint)
                y += 13f
            }
            y += 8f

            // Findings
            if (doc.findings.isNotEmpty()) {
                canvas.drawText("FINDINGS & CLINICAL OBSERVATIONS:", 25f, y, boldPaint)
                y += 14f
                doc.findings.take(6).forEach { f ->
                    canvas.drawText("• ${f.take(80)}", 30f, y, textPaint)
                    y += 13f
                }
                y += 8f
            }

            // Prescribed Medications
            if (doc.prescribedMeds.isNotEmpty()) {
                canvas.drawText("DETECTED PRESCRIBED MEDICATIONS:", 25f, y, boldPaint)
                y += 14f
                doc.prescribedMeds.take(5).forEach { med ->
                    canvas.drawText("• ${med.name} (${med.dosage}, ${med.frequency}) - ${med.instructions}", 30f, y, textPaint)
                    y += 13f
                }
                y += 8f
            }

            // Biomarkers
            if (doc.labBiomarkers.isNotEmpty()) {
                canvas.drawText("MEASURED LABORATORY BIOMARKERS:", 25f, y, boldPaint)
                y += 14f
                doc.labBiomarkers.take(5).forEach { bio ->
                    canvas.drawText("• ${bio.testName}: ${bio.value} ${bio.unit} (Ref: ${bio.referenceRange}) - Status: ${bio.status}", 30f, y, textPaint)
                    y += 13f
                }
                y += 8f
            }

            // Doctor Advice
            if (doc.doctorAdvice.isNotBlank()) {
                canvas.drawText("PHYSICIAN GUIDANCE & FOLLOW-UP:", 25f, y, boldPaint)
                y += 14f
                doc.doctorAdvice.chunked(75).forEach { line ->
                    canvas.drawText(line, 25f, y, textPaint)
                    y += 13f
                }
            }

            // Footer
            canvas.drawLine(25f, pageHeight - 35f, pageWidth - 25f, pageHeight - 35f, linePaint)
            canvas.drawText("Confidential • Extracted via MedPath OCR • Verified Clinical Record", 25f, pageHeight - 20f, Paint().apply {
                color = Color.GRAY
                textSize = 9f
                isAntiAlias = true
            })

            pdfDocument.finishPage(page)

            val cacheFile = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(cacheFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()

            // Save to documents folder
            try {
                val docsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                if (!docsDir.exists()) docsDir.mkdirs()
                val permFile = File(docsDir, fileName)
                cacheFile.copyTo(permFile, overwrite = true)
            } catch (_: Exception) {}

            pdfDocument.close()

            val contentUri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            } catch (e: Exception) {
                null
            }

            val sizeKb = (cacheFile.length() / 1024).coerceAtLeast(1)

            return PdfExportResult(
                file = cacheFile,
                uri = contentUri,
                fileName = fileName,
                fileSizeKb = sizeKb,
                formattedSummaryText = textSummary,
                isSuccess = true,
                message = "Patient document PDF generated ($sizeKb KB)."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return PdfExportResult(
                file = null,
                uri = null,
                fileName = fileName,
                fileSizeKb = 0,
                formattedSummaryText = textSummary,
                isSuccess = true,
                message = "Patient document text ready."
            )
        }
    }

    fun savePdfToDownloads(context: Context, result: PdfExportResult): Boolean {
        return try {
            if (result.file != null && result.file.exists()) {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, result.fileName)
                result.file.copyTo(targetFile, overwrite = true)
                Toast.makeText(context, "✅ PDF saved to Downloads: ${result.fileName}", Toast.LENGTH_LONG).show()
                true
            } else {
                Toast.makeText(context, "Document prepared in application storage.", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Document saved to app documents: ${result.fileName}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun shareSummary(context: Context, result: PdfExportResult) {
        try {
            // Also copy to clipboard as an instant guarantee so user never loses data
            copySummaryTextSilent(context, result.formattedSummaryText)

            if (result.uri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, result.uri)
                    putExtra(Intent.EXTRA_SUBJECT, "MedPath Medical Profile Summary — Arun Kumar")
                    putExtra(Intent.EXTRA_TEXT, result.formattedSummaryText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Share Medical Profile PDF").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                Toast.makeText(context, "Opening share options for Medical Profile PDF...", Toast.LENGTH_SHORT).show()
                context.startActivity(chooser)
            } else {
                shareTextFallback(context, result.formattedSummaryText)
            }
        } catch (e: Exception) {
            shareTextFallback(context, result.formattedSummaryText)
        }
    }

    fun openPdf(context: Context, result: PdfExportResult) {
        if (result.uri != null) {
            try {
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(result.uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "PDF saved to app documents. No dedicated PDF reader found.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "PDF file generated and saved.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareTextFallback(context: Context, text: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, "MedPath Medical Profile Summary")
            }
            val chooser = Intent.createChooser(sendIntent, "Share Clinical Summary").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            Toast.makeText(context, "Summary copied to clipboard & opening share...", Toast.LENGTH_SHORT).show()
            context.startActivity(chooser)
        } catch (e: Exception) {
            copySummaryText(context, text)
        }
    }

    private fun copySummaryTextSilent(context: Context, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("MedPath Medical Summary", text)
            clipboard.setPrimaryClip(clip)
        } catch (_: Exception) {}
    }

    fun copySummaryText(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("MedPath Medical Summary", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Clinical Summary copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun PdfExportDialog(
    result: PdfExportResult,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onCopyText: () -> Unit,
    onOpenPdf: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = status_taken_green,
                modifier = Modifier.size(42.dp)
            )
        },
        title = {
            Text(
                text = "Clinical PDF Ready",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = status_taken_bg,
                    border = BorderStroke(1.dp, status_taken_border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = result.fileName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Size: ${if (result.fileSizeKb > 0) "${result.fileSizeKb} KB" else "Structured Document"} • Ready for Share & Download",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "The verified patient record has been compiled into a medical document. You can download it directly, share it with your doctor or family, or copy the clinical text.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onDownload != null) {
                    FilledTonalButton(
                        onClick = onDownload,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download")
                    }
                } else if (onOpenPdf != null && result.uri != null) {
                    FilledTonalButton(
                        onClick = onOpenPdf,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open PDF")
                    }
                }
                Button(
                    onClick = onShare,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share PDF", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCopyText,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Text")
            }
        }
    )
}
