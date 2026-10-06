package com.example.data

data class Patient(
    val name: String,
    val age: Int,
    val conditions: List<String>,
    val medications: List<String>
)

data class MedicalEvent(
    val id: String,
    val date: String,
    val type: String, // "Diagnosis", "Lab Result", "Prescription", "Procedure", "Consultation", "Hospitalization"
    val title: String,
    val finding: String,
    val source: String,
    val relatedCondition: String,
    val documentUrl: String = "",
    val confidence: String = "High",
    val severity: String = "Routine", // "Routine", "Attention", "Critical"
    val doctorName: String = "",
    val notes: String = ""
)

data class Document(
    val id: String,
    val name: String,
    val type: String,
    val date: String,
    val status: String
)

data class HealthcareFacility(
    val id: String,
    val name: String,
    val type: String,
    val distanceKm: Double,
    val departments: List<String>,
    val services: List<String>,
    // Emergency & Bed/Slot Availability attributes (with robust defaults)
    val isEmergency24x7: Boolean = true,
    val emergencyPhone: String = "108 / 080-2299-8800",
    val travelTimeMinutes: Int = 10,
    val traumaLevel: String = "Level 1 Trauma Center",
    val icuBedsAvailable: Int = 4,
    val icuBedsTotal: Int = 20,
    val emergencyBedsAvailable: Int = 12,
    val emergencyBedsTotal: Int = 25,
    val generalBedsAvailable: Int = 30,
    val generalBedsTotal: Int = 75,
    val oxygenBedsAvailable: Int = 6,
    val erWaitTimeMinutes: Int = 8,
    val availableEmergencySlots: List<String> = listOf("ER Acute Bed Ready", "Specialist On-Call"),
    val address: String = "Main Medical Corridor, Sector 4",
    val hasAmbulanceService: Boolean = true,
    val isRecommendedEmergency: Boolean = false,
    val latitude: Double = 12.9716,
    val longitude: Double = 77.5946
)

data class Doctor(
    val id: String,
    val name: String,
    val specialty: String,
    val facilityName: String,
    val qualification: String = "MBBS, MD",
    val experienceYears: Int = 14,
    val pastTreatedCondition: String,
    val lastTreatedDate: String,
    val phone: String = "+91 80 2299 8800",
    val availableDays: String = "Mon - Fri, 10:00 AM - 02:00 PM",
    val consultationFee: String = "₹600",
    val doctorNotes: String = ""
)

data class LabTest(
    val id: String,
    val name: String,
    val category: String, // "Renal Health", "Diabetes", "Lipid Profile", "Routine"
    val testDescription: String,
    val fastingRequired: String,
    val reportTurnaround: String,
    val price: String,
    val homeSampleAvailable: Boolean = true,
    val isRecommendedForPatient: Boolean = false,
    val recommendedReason: String = ""
)

data class Pharmacy(
    val id: String,
    val name: String,
    val type: String, // "24x7 Emergency Pharmacy", "Pradhan Mantri Jan Aushadhi Kendra (Generic)", "Retail Chain"
    val address: String,
    val distanceKm: Double,
    val phone: String,
    val openHours: String,
    val homeDelivery: Boolean = true,
    val discountOffer: String = "Up to 70% off on generic medicines",
    val latitude: Double = 12.9716,
    val longitude: Double = 77.5946
)

data class ExtractedDocumentData(
    val documentType: String, // "Diagnosis", "Lab Result", "Prescription", "Hospitalization"
    val title: String,
    val doctorOrClinic: String,
    val date: String,
    val clinicalSummary: String,
    val findings: List<String>,
    val prescribedMeds: List<ExtractedMedication> = emptyList(),
    val labBiomarkers: List<ExtractedBiomarker> = emptyList(),
    val doctorAdvice: String = "",
    val allergyWarnings: List<String> = emptyList()
)

data class ExtractedMedication(
    val name: String,
    val dosage: String,
    val frequency: String,
    val instructions: String
)

data class ExtractedBiomarker(
    val testName: String,
    val value: String,
    val unit: String,
    val referenceRange: String,
    val status: String // "Normal", "Elevated", "Attention"
)

data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: String,
    val language: String = "English",
    val clinicalUrgency: String? = null // "EMERGENCY", "URGENT", "ROUTINE"
)

data class EmergencyReservation(
    val id: String,
    val facilityId: String,
    val facilityName: String,
    val bedType: String, // "Emergency / ER Bed", "ICU Bed", "Trauma Bay"
    val etaMinutes: Int,
    val patientName: String,
    val chiefComplaint: String,
    val timestamp: String,
    val status: String = "Bed Held • ER Notified"
)

data class CareSuggestion(
    val department: String,
    val facility: HealthcareFacility,
    val relevantHistory: List<String>,
    val previousTests: List<String>,
    val documentsToCarry: List<String>,
    val priority: String
)

data class MedicationItem(
    val id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val timeSlot: String, // "Morning", "Afternoon", "Evening", "Night"
    val scheduledTime: String,
    val instruction: String,
    val purpose: String,
    val pillAppearance: String, // e.g., "White oval scored tablet"
    val pillsRemaining: Int,
    val refillDays: Int,
    val isTaken: Boolean = false,
    val takenTimestamp: String? = null,
    val reminderEnabled: Boolean = true
)

data class AppointmentReminder(
    val id: String,
    val title: String,
    val doctorName: String,
    val specialty: String,
    val facilityName: String,
    val departmentLocation: String,
    val date: String,
    val time: String,
    val relativeDays: String,
    val preparationChecklist: List<String>,
    val notes: String,
    val reminderEnabled: Boolean = true,
    val isCompleted: Boolean = false
)

data class AccessibilityPreferences(
    val fontScale: Float = 1.0f, // 1.0f, 1.15f, 1.30f, 1.45f
    val highContrast: Boolean = false,
    val largeTouchTargets: Boolean = true
)

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val age: Int,
    val gender: String,
    val bloodGroup: String,
    val heightCm: Int,
    val weightKg: Double,
    val primaryCareFacility: String,
    val primaryPhysician: String,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val emergencyContactRelation: String,
    val allergies: List<String>,
    val memberSince: String = "September 2024",
    val isVerified: Boolean = true
) {
    val bmi: Double
        get() {
            val heightInMeters = heightCm / 100.0
            return if (heightInMeters > 0) Math.round((weightKg / (heightInMeters * heightInMeters)) * 10.0) / 10.0 else 0.0
        }
    val bmiCategory: String
        get() = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
}

data class HealthBiomarker(
    val name: String,
    val value: String,
    val unit: String,
    val referenceRange: String,
    val status: String, // "Normal", "Elevated", "Attention", "Optimal"
    val lastTestedDate: String,
    val trend: String // "Stable", "Increasing", "Improving"
)

data class ProfileAnalysis(
    val continuityScore: Int, // e.g. 86 / 100
    val scoreGrade: String, // "Strong Continuity"
    val adherenceScore: Int, // 85%
    val recordCompletenessScore: Int, // 92%
    val preventiveCareScore: Int, // 78%
    val keyFindings: List<AnalysisFinding>,
    val careGaps: List<CareGap>,
    val biomarkers: List<HealthBiomarker>,
    val lifestyleRecommendations: List<String>,
    val synthesizedSummary: String
)

data class AnalysisFinding(
    val category: String, // "Glycemic Health", "Renal Function", "Cardiovascular"
    val severity: String, // "Low", "Moderate", "High"
    val title: String,
    val description: String,
    val actionItem: String
)

data class CareGap(
    val title: String,
    val dueDate: String,
    val urgency: String, // "Routine", "Recommended Soon", "Overdue"
    val reason: String
)

data class AuthSession(
    val isLoggedIn: Boolean = true,
    val authMethod: String = "Email", // "Email" or "Phone"
    val userIdentifier: String = "arun.kumar@medpath.org"
)
