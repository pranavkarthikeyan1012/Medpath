package com.example.data

import android.content.Context
import com.example.data.db.MedicationRepository
import com.example.notifications.MedicationNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object HealthDataManager {
    private var repository: MedicationRepository? = null
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        val repo = MedicationRepository(app)
        repository = repo

        MedicationNotificationManager.createNotificationChannel(app)

        CoroutineScope(Dispatchers.IO).launch {
            repo.allMedicationsFlow.collect { dbMeds ->
                if (dbMeds.isNotEmpty()) {
                    _medications.value = dbMeds
                    MedicationNotificationManager.scheduleAllReminders(app, dbMeds)
                }
            }
        }
    }

    // Current Active Patient Profile
    private val _userProfile = MutableStateFlow(
        UserProfile(
            id = "MP-89241",
            name = "Arun Kumar",
            email = "arun.kumar@medpath.org",
            phone = "+91 98765 43210",
            age = 52,
            gender = "Male",
            bloodGroup = "B+ Positive",
            heightCm = 174,
            weightKg = 78.5,
            primaryCareFacility = "District Hospital B",
            primaryPhysician = "Dr. Sarah Vance, MD",
            emergencyContactName = "Priya Kumar",
            emergencyContactPhone = "+91 98765 12345",
            emergencyContactRelation = "Spouse",
            allergies = listOf("Penicillin (Mild rash)", "Sulfa drugs"),
            memberSince = "March 2024",
            isVerified = true
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Auth Session State
    private val _authSession = MutableStateFlow(
        AuthSession(
            isLoggedIn = true,
            authMethod = "Email",
            userIdentifier = "arun.kumar@medpath.org"
        )
    )
    val authSession: StateFlow<AuthSession> = _authSession.asStateFlow()

    // Initialized with realistic mock data tailored for patient Arun Kumar
    private val _medications = MutableStateFlow(
        listOf(
            MedicationItem(
                id = "med_1",
                name = "Metformin Hydrochloride",
                dosage = "500 mg",
                frequency = "Twice daily",
                timeSlot = "Morning",
                scheduledTime = "08:00 AM",
                instruction = "Take with or immediately after breakfast",
                purpose = "Type 2 Diabetes glucose regulation",
                pillAppearance = "White oval tablet with '500' imprint",
                pillsRemaining = 18,
                refillDays = 9,
                isTaken = true,
                takenTimestamp = "08:14 AM"
            ),
            MedicationItem(
                id = "med_2",
                name = "Glimepiride",
                dosage = "1 mg",
                frequency = "Once daily",
                timeSlot = "Morning",
                scheduledTime = "08:00 AM",
                instruction = "Take with first main meal of the day",
                purpose = "Stimulates insulin secretion",
                pillAppearance = "Small pink oblong tablet",
                pillsRemaining = 8,
                refillDays = 4,
                isTaken = true,
                takenTimestamp = "08:15 AM"
            ),
            MedicationItem(
                id = "med_3",
                name = "Atorvastatin",
                dosage = "10 mg",
                frequency = "Once daily at bedtime",
                timeSlot = "Night",
                scheduledTime = "09:30 PM",
                instruction = "Take at bedtime with a glass of water",
                purpose = "Cholesterol & cardiovascular risk reduction",
                pillAppearance = "White round film-coated tablet",
                pillsRemaining = 24,
                refillDays = 24,
                isTaken = false,
                takenTimestamp = null
            ),
            MedicationItem(
                id = "med_4",
                name = "Vitamin D3 + Calcium",
                dosage = "60,000 IU / 500 mg",
                frequency = "After lunch",
                timeSlot = "Afternoon",
                scheduledTime = "01:30 PM",
                instruction = "Take after lunch with plain water",
                purpose = "Bone health & metabolic support",
                pillAppearance = "Yellow softgel capsule",
                pillsRemaining = 14,
                refillDays = 14,
                isTaken = false,
                takenTimestamp = null
            )
        )
    )
    val medications: StateFlow<List<MedicationItem>> = _medications.asStateFlow()

    private val _appointments = MutableStateFlow(
        listOf(
            AppointmentReminder(
                id = "apt_1",
                title = "Nephrology Specialist Follow-up",
                doctorName = "Dr. Sarah Vance, MD",
                specialty = "Nephrologist",
                facilityName = "District Hospital B",
                departmentLocation = "OPD Block 2, Room 204",
                date = "Monday, Sep 28, 2026",
                time = "10:30 AM",
                relativeDays = "In 4 days",
                preparationChecklist = listOf(
                    "Fasting for 8-10 hours prior to visit (water allowed)",
                    "Carry physical printout of Creatinine & HbA1c lab reports",
                    "Bring current medicine blister strips (Metformin & Glimepiride)",
                    "Log blood pressure or blood glucose readings from past 3 days"
                ),
                notes = "Review elevated serum creatinine marker (1.6 mg/dL) and evaluate Metformin dosage safety.",
                reminderEnabled = true,
                isCompleted = false
            ),
            AppointmentReminder(
                id = "apt_2",
                title = "Routine Comprehensive Metabolic Panel (CMP)",
                doctorName = "Diagnostic Lab",
                specialty = "Pathology & Biochemistry",
                facilityName = "Suburban Diagnostic Center",
                departmentLocation = "Sample Collection Counter 1",
                date = "Friday, Oct 02, 2026",
                time = "07:45 AM",
                relativeDays = "In 8 days",
                preparationChecklist = listOf(
                    "Strict 10-hour fasting required",
                    "Do not take morning diabetes medication until after blood draw",
                    "Drink 1-2 glasses of plain water"
                ),
                notes = "Fasting glucose, Serum Creatinine, eGFR, and microalbuminuria screening.",
                reminderEnabled = true,
                isCompleted = false
            ),
            AppointmentReminder(
                id = "apt_3",
                title = "Quarterly Diabetes Consultation",
                doctorName = "Dr. Ramesh Patel",
                specialty = "Endocrinologist",
                facilityName = "Metro Specialist Clinic",
                departmentLocation = "Suite 301",
                date = "Wednesday, Aug 12, 2026",
                time = "04:00 PM",
                relativeDays = "Completed",
                preparationChecklist = listOf(
                    "Past HbA1c review",
                    "Dietary recall"
                ),
                notes = "Patient reported mild fatigue. Advised kidney function retest in 6 weeks.",
                reminderEnabled = false,
                isCompleted = true
            )
        )
    )
    val appointments: StateFlow<List<AppointmentReminder>> = _appointments.asStateFlow()

    // Synthesized Profile Health Analysis computed from records & history
    private val _profileAnalysis = MutableStateFlow(
        ProfileAnalysis(
            continuityScore = 86,
            scoreGrade = "Strong Continuity & Monitored",
            adherenceScore = 85,
            recordCompletenessScore = 92,
            preventiveCareScore = 78,
            keyFindings = listOf(
                AnalysisFinding(
                    category = "Glycemic Health",
                    severity = "Moderate",
                    title = "Type 2 Diabetes — Medicated & Stable",
                    description = "Diagnosed in 2019. HbA1c currently around 7.4% on dual-therapy (Metformin 500mg + Glimepiride 1mg). Fasting sugar averages 138-145 mg/dL.",
                    actionItem = "Maintain consistent morning dosing with meals; monitor for hypoglycemia when skipping lunch."
                ),
                AnalysisFinding(
                    category = "Renal Function",
                    severity = "High",
                    title = "Serum Creatinine Elevation (1.6 mg/dL)",
                    description = "Detected in 2025 lab screening. Mild renal filtration marker elevation requires clinical surveillance to ensure Metformin excretion safety.",
                    actionItem = "Prioritize upcoming Nephrology consult on Sep 28; repeat Serum Creatinine & eGFR panel."
                ),
                AnalysisFinding(
                    category = "Cardiovascular Risk",
                    severity = "Low",
                    title = "Preventive Lipid Protection Active",
                    description = "Atorvastatin 10mg taken nightly. Total cholesterol within target boundary (195 mg/dL). Blood pressure stable at 128/82 mmHg.",
                    actionItem = "Continue bedtime statin adherence and 30-minute daily light cardiovascular walks."
                )
            ),
            careGaps = listOf(
                CareGap(
                    title = "Annual Diabetic Retinopathy Eye Exam",
                    dueDate = "Overdue (Due July 2026)",
                    urgency = "Overdue",
                    reason = "Standard annual microvascular screening for patients diagnosed with Type 2 diabetes over 5 years."
                ),
                CareGap(
                    title = "Nephrology Specialist Review",
                    dueDate = "Sep 28, 2026 (In 4 days)",
                    urgency = "Recommended Soon",
                    reason = "Required follow-up for 2025 abnormal renal marker before renewing prescription."
                ),
                CareGap(
                    title = "Microalbuminuria Urine Screening",
                    dueDate = "Oct 02, 2026",
                    urgency = "Routine",
                    reason = "Early detection of diabetic nephropathy protein leakage."
                )
            ),
            biomarkers = listOf(
                HealthBiomarker("Fasting Blood Glucose", "142", "mg/dL", "70 - 99", "Elevated", "Feb 2026", "Stable"),
                HealthBiomarker("HbA1c Glycated Hemoglobin", "7.4", "%", "< 5.7 (Normal), < 7.0 (Target)", "Attention", "Oct 2025", "Improving"),
                HealthBiomarker("Serum Creatinine", "1.6", "mg/dL", "0.7 - 1.3", "Elevated", "Dec 2025", "Attention"),
                HealthBiomarker("Blood Pressure", "128 / 82", "mmHg", "< 120/80", "Optimal", "Sep 2026", "Stable"),
                HealthBiomarker("Estimated GFR", "56", "mL/min", "> 60", "Attention", "Dec 2025", "Stable"),
                HealthBiomarker("Total Cholesterol", "195", "mg/dL", "< 200", "Normal", "Oct 2025", "Improving")
            ),
            lifestyleRecommendations = listOf(
                "Maintain hydration: Drink at least 2.2L of water daily, especially before lab draws.",
                "Sodium moderation: Keep daily sodium intake under 2,000 mg to preserve kidney health.",
                "Foot inspection: Perform daily visual checks for pressure sores or numbness."
            ),
            synthesizedSummary = "Patient Arun Kumar demonstrates robust health engagement with 85% medication adherence across 4 active prescriptions and a complete digital history spanning 2019 to 2026. The primary clinical imperative is evaluating mild renal marker elevation at the scheduled Nephrology follow-up, and closing the overdue annual diabetic retinopathy screening."
        )
    )
    val profileAnalysis: StateFlow<ProfileAnalysis> = _profileAnalysis.asStateFlow()

    // Medical History Records (Chronological tracking for patient Arun Kumar)
    private val _medicalEvents = MutableStateFlow(
        listOf(
            MedicalEvent(
                id = "evt_7",
                date = "March 15, 2026",
                type = "Consultation",
                title = "Nephrology Pre-Evaluation & Vitals Check",
                finding = "Blood Pressure 128/82 mmHg. Vitals stable. Advised repeat renal function test before prescription renewal.",
                source = "District Hospital B — OPD Block 2",
                relatedCondition = "Renal Surveillance",
                severity = "Routine",
                doctorName = "Dr. Sarah Vance, MD",
                notes = "Scheduled formal follow-up for late September 2026."
            ),
            MedicalEvent(
                id = "evt_6",
                date = "December 14, 2025",
                type = "Lab Result",
                title = "Abnormal Kidney-Function Marker Detected",
                finding = "Serum Creatinine elevated at 1.6 mg/dL (Normal: 0.7-1.3 mg/dL). Estimated GFR: 56 mL/min.",
                source = "Suburban Diagnostic Center — Biochemistry Lab",
                relatedCondition = "Renal Function",
                severity = "Attention",
                doctorName = "Dr. M. K. Rao (Pathologist)",
                notes = "Mild renal filtration impairment. Requires close monitoring of Metformin safety."
            ),
            MedicalEvent(
                id = "evt_5",
                date = "June 22, 2024",
                type = "Hospitalization",
                title = "Emergency Observation — Acute Dehydration",
                finding = "Presented with acute viral gastroenteritis and weakness. Administered 2L IV Normal Saline and antiemetics.",
                source = "City General Hospital — Emergency Dept",
                relatedCondition = "General Medicine",
                severity = "Attention",
                doctorName = "Dr. R. Gupta (ER Physician)",
                notes = "Admitted 24 hours for observation. Discharged with oral rehydration protocol."
            ),
            MedicalEvent(
                id = "evt_4",
                date = "October 10, 2023",
                type = "Prescription",
                title = "Diabetes Medication Regimen Optimization",
                finding = "Dual therapy initiated: Metformin 500mg twice daily with meals + Glimepiride 1mg once daily with breakfast.",
                source = "Metro Specialist Clinic — Endocrinology",
                relatedCondition = "Type 2 Diabetes",
                severity = "Routine",
                doctorName = "Dr. Ramesh Patel",
                notes = "Target HbA1c reduction below 7.2%."
            ),
            MedicalEvent(
                id = "evt_3",
                date = "November 05, 2022",
                type = "Procedure",
                title = "Dilated Fundoscopy / Retinopathy Screening",
                finding = "Normal macula bilaterally. Mild retinal arteriolar narrowing without proliferative retinopathy.",
                source = "Eye Care Specialists Hospital",
                relatedCondition = "Diabetic Microvascular Health",
                severity = "Routine",
                doctorName = "Dr. Anita Desai",
                notes = "Advised annual follow-up. Last due July 2026."
            ),
            MedicalEvent(
                id = "evt_2",
                date = "August 18, 2021",
                type = "Lab Result",
                title = "Elevated Glycated Hemoglobin (HbA1c)",
                finding = "HbA1c reached 8.2% (Target < 7.0%). Fasting plasma glucose: 162 mg/dL.",
                source = "General Hospital Central Lab",
                relatedCondition = "Type 2 Diabetes",
                severity = "Attention",
                doctorName = "Dr. S. Nair",
                notes = "Lifestyle intervention plus pharmacotherapy adjustment advised."
            ),
            MedicalEvent(
                id = "evt_1",
                date = "April 04, 2019",
                type = "Diagnosis",
                title = "Type 2 Diabetes Mellitus Diagnosed",
                finding = "Confirmed Type 2 Diabetes following repeated fasting glucose (> 145 mg/dL) and symptoms of polyuria and fatigue.",
                source = "City Health Clinic — Internal Medicine",
                relatedCondition = "Type 2 Diabetes",
                severity = "Routine",
                doctorName = "Dr. A. Sharma",
                notes = "Initiated single-agent Metformin 500mg once daily."
            )
        )
    )
    val medicalEvents: StateFlow<List<MedicalEvent>> = _medicalEvents.asStateFlow()

    // Real-Time Emergency Facilities with Live Beds & Slots
    private val _facilities = MutableStateFlow(
        listOf(
            HealthcareFacility(
                id = "fac_ramapuram_uphc",
                name = "Government Urban Primary Health Centre (UPHC), Ramapuram",
                type = "Government Hospital & Primary Emergency Unit",
                distanceKm = 0.4,
                departments = listOf("Emergency First Aid", "Outpatient Medicine", "Maternal Care", "Diabetic Clinic", "Immunization"),
                services = listOf("24/7 First Aid Casualty", "100% Free Govt Pharmacy", "108 Ambulance Dispatch", "Free Blood Sugar & BP Tests"),
                isEmergency24x7 = true,
                emergencyPhone = "108 / 044 2249 1100",
                travelTimeMinutes = 2,
                traumaLevel = "Government Local Urgent Care Unit",
                icuBedsAvailable = 2,
                icuBedsTotal = 5,
                emergencyBedsAvailable = 8,
                emergencyBedsTotal = 12,
                generalBedsAvailable = 25,
                generalBedsTotal = 40,
                oxygenBedsAvailable = 6,
                erWaitTimeMinutes = 1,
                availableEmergencySlots = listOf("Immediate Triage Bed", "Observation Room 1"),
                address = "Rayala Nagar Main Road, Ramapuram, Chennai - 600089",
                hasAmbulanceService = true,
                isRecommendedEmergency = true,
                latitude = 13.0325,
                longitude = 80.1812
            ),
            HealthcareFacility(
                id = "fac_nesapakkam_govt",
                name = "Government Peripheral Hospital, Nesapakkam",
                type = "Government Community Hospital",
                distanceKm = 1.1,
                departments = listOf("Casualty & Trauma", "General Medicine", "Pediatrics", "Emergency Stabilization"),
                services = listOf("24/7 Emergency Casualty", "Free State Medicines", "Oxygen Wards", "CMCHIS Cashless Support"),
                isEmergency24x7 = true,
                emergencyPhone = "108 / 044 2471 2233",
                travelTimeMinutes = 3,
                traumaLevel = "Government Community Trauma Center",
                icuBedsAvailable = 4,
                icuBedsTotal = 10,
                emergencyBedsAvailable = 14,
                emergencyBedsTotal = 20,
                generalBedsAvailable = 50,
                generalBedsTotal = 80,
                oxygenBedsAvailable = 10,
                erWaitTimeMinutes = 2,
                availableEmergencySlots = listOf("Casualty Bed 3", "Emergency Observation Bay"),
                address = "Pillaiyar Koil Street, Nesapakkam (Bordering Ramapuram), Chennai - 600078",
                hasAmbulanceService = true,
                isRecommendedEmergency = true,
                latitude = 13.0360,
                longitude = 80.1910
            ),
            HealthcareFacility(
                id = "fac_kknagar_govt",
                name = "Government Peripheral Hospital & ESIC Emergency Center (K.K. Nagar)",
                type = "Government Apex Secondary Hospital & Trauma Center",
                distanceKm = 1.8,
                departments = listOf("24/7 Polytrauma", "General Medicine", "Nephrology & Dialysis", "Cardiology", "Surgery"),
                services = listOf("24/7 Emergency Casualty", "Free Dialysis Unit", "Advanced ICU", "Government Blood Bank", "108 Ambulance Fleet"),
                isEmergency24x7 = true,
                emergencyPhone = "108 / 044 2474 1102",
                travelTimeMinutes = 5,
                traumaLevel = "Government Apex Trauma & Secondary Care Hospital",
                icuBedsAvailable = 12,
                icuBedsTotal = 26,
                emergencyBedsAvailable = 24,
                emergencyBedsTotal = 40,
                generalBedsAvailable = 160,
                generalBedsTotal = 300,
                oxygenBedsAvailable = 20,
                erWaitTimeMinutes = 3,
                availableEmergencySlots = listOf("Acute Trauma Resuscitation Bay", "Free Govt ER Bed", "Dialysis Emergency Ready"),
                address = "11th Avenue, Ashok Nagar / K.K. Nagar (Near Kasi Theater), Chennai - 600078",
                hasAmbulanceService = true,
                isRecommendedEmergency = true,
                latitude = 13.0385,
                longitude = 80.2030
            ),
            HealthcareFacility(
                id = "fac_guindy_super_govt",
                name = "Kalaignar Centenary Government Multi Super Speciality Hospital (Guindy)",
                type = "Government Multi Super Speciality Apex Hospital",
                distanceKm = 2.6,
                departments = listOf("Apex Trauma Center", "Nephrology & Renal Transplant", "Cardiology & Cath Lab", "Neurology", "Surgical Oncology"),
                services = listOf("1000-Bed Govt Super Speciality", "24/7 Apex Emergency", "Free Dialysis & ICU", "State Helipad & Ambulance Hub"),
                isEmergency24x7 = true,
                emergencyPhone = "108 / 044 2250 4400",
                travelTimeMinutes = 6,
                traumaLevel = "State Level 1 Multi-Super Speciality Apex Center",
                icuBedsAvailable = 35,
                icuBedsTotal = 80,
                emergencyBedsAvailable = 45,
                emergencyBedsTotal = 75,
                generalBedsAvailable = 420,
                generalBedsTotal = 1000,
                oxygenBedsAvailable = 50,
                erWaitTimeMinutes = 2,
                availableEmergencySlots = listOf("Immediate Super Speciality Triage", "Nephrology Resuscitation Unit", "Cardiac ER Slot"),
                address = "King Institute Campus, Anna Salai (Mount Road), Guindy, Chennai - 600032",
                hasAmbulanceService = true,
                isRecommendedEmergency = true,
                latitude = 13.0110,
                longitude = 80.2105
            ),
            HealthcareFacility(
                id = "fac_saidapet_govt",
                name = "Government Taluk Hospital (Saidapet)",
                type = "Government Taluk General Hospital",
                distanceKm = 3.2,
                departments = listOf("Casualty & Emergency", "General Outpatient", "Pediatrics", "Maternity Wards"),
                services = listOf("24/7 Casualty", "Free Generic Medicines", "Maternity Emergency", "Ambulance Hub"),
                isEmergency24x7 = true,
                emergencyPhone = "108 / 044 2435 4422",
                travelTimeMinutes = 7,
                traumaLevel = "Government Taluk Emergency Center",
                icuBedsAvailable = 6,
                icuBedsTotal = 14,
                emergencyBedsAvailable = 16,
                emergencyBedsTotal = 25,
                generalBedsAvailable = 80,
                generalBedsTotal = 150,
                oxygenBedsAvailable = 12,
                erWaitTimeMinutes = 3,
                availableEmergencySlots = listOf("Casualty Bay 2", "Observation Ward"),
                address = "Bazaar Road, Saidapet, Chennai - 600015",
                hasAmbulanceService = true,
                isRecommendedEmergency = true,
                latitude = 13.0220,
                longitude = 80.2220
            ),
            HealthcareFacility(
                id = "fac_miot",
                name = "MIOT International Hospital",
                type = "Private Multispecialty & Trauma Center",
                distanceKm = 1.2,
                departments = listOf("Orthopedics", "Nephrology", "Emergency & Trauma", "Cardiology", "Critical Care", "Neurology"),
                services = listOf("24/7 Level 1 Trauma", "Dialysis Unit", "Coronary Care", "Blood Bank", "Helipad & Ambulances"),
                isEmergency24x7 = true,
                emergencyPhone = "044 4200 2288 / 108",
                travelTimeMinutes = 4,
                traumaLevel = "Level 1 Comprehensive Trauma Center",
                icuBedsAvailable = 14,
                icuBedsTotal = 35,
                emergencyBedsAvailable = 24,
                emergencyBedsTotal = 40,
                generalBedsAvailable = 110,
                generalBedsTotal = 250,
                oxygenBedsAvailable = 18,
                erWaitTimeMinutes = 4,
                availableEmergencySlots = listOf("Acute Trauma Resuscitation Bay", "Nephrology Emergency Dialysis Ready", "Cardiac ICU Slot"),
                address = "4/112, Mount-Poonamallee Road, Manapakkam, Chennai - 600089",
                hasAmbulanceService = true,
                isRecommendedEmergency = true,
                latitude = 13.0185,
                longitude = 80.1865
            ),
            HealthcareFacility(
                id = "fac_srm",
                name = "SRM Prime Hospital & Research Institute",
                type = "Multispecialty Teaching Hospital",
                distanceKm = 3.4,
                departments = listOf("General Medicine", "Emergency Medicine", "Nephrology", "Cardiology", "Surgery"),
                services = listOf("24/7 Casualty", "ICU / CCU", "Diagnostic Labs", "Ambulance Fleet", "Pharmacy"),
                isEmergency24x7 = true,
                emergencyPhone = "044 4743 2345 / 108",
                travelTimeMinutes = 8,
                traumaLevel = "Level 1 Emergency Care Center",
                icuBedsAvailable = 10,
                icuBedsTotal = 28,
                emergencyBedsAvailable = 18,
                emergencyBedsTotal = 30,
                generalBedsAvailable = 85,
                generalBedsTotal = 180,
                oxygenBedsAvailable = 12,
                erWaitTimeMinutes = 6,
                availableEmergencySlots = listOf("Emergency Triage Bed 2", "Dialysis Bay Available", "Observation Room 4"),
                address = "SRM Medical Campus, Bharathi Salai, Ramapuram / Kattankulathur, Chennai",
                hasAmbulanceService = true,
                isRecommendedEmergency = true,
                latitude = 13.0310,
                longitude = 80.1800
            ),
            HealthcareFacility(
                id = "fac_apollo",
                name = "Apollo Hospitals (Greams Road & Vanagaram)",
                type = "Private Multispecialty Super Speciality",
                distanceKm = 2.8,
                departments = listOf("Emergency Medicine", "Interventional Cardiology", "Nephrology", "Critical Care", "Neurology"),
                services = listOf("JCI Accredited", "24/7 Chest Pain Clinic", "Cath Lab", "Advanced ICU", "Air Ambulance"),
                isEmergency24x7 = true,
                emergencyPhone = "1066 / 044 2829 0200",
                travelTimeMinutes = 7,
                traumaLevel = "Level 1 Comprehensive Emergency Center",
                icuBedsAvailable = 16,
                icuBedsTotal = 45,
                emergencyBedsAvailable = 26,
                emergencyBedsTotal = 45,
                generalBedsAvailable = 140,
                generalBedsTotal = 320,
                oxygenBedsAvailable = 22,
                erWaitTimeMinutes = 5,
                availableEmergencySlots = listOf("Cardiac Resuscitation Bay", "Immediate Stroke Triage Unit", "ICU Ventilator Bed"),
                address = "21 Greams Lane, Thousand Lights / Vanagaram Campus, Chennai",
                hasAmbulanceService = true,
                isRecommendedEmergency = true,
                latitude = 13.0610,
                longitude = 80.2520
            ),
            HealthcareFacility(
                id = "fac_kmch_govt",
                name = "Government Kilpauk Medical College Hospital (KMCH)",
                type = "Government Hospital & Medical College",
                distanceKm = 2.4,
                departments = listOf("Emergency & Trauma", "General Medicine", "Nephrology", "Burns & Plastic Surgery", "Pediatrics"),
                services = listOf("24/7 Free Emergency", "Government Free Dialysis", "ICU Beds", "PM-JAY / CMCHIS Cashless", "Blood Bank"),
                isEmergency24x7 = true,
                emergencyPhone = "108 / 044 2836 4951",
                travelTimeMinutes = 6,
                traumaLevel = "Government Apex Trauma & Emergency Center",
                icuBedsAvailable = 12,
                icuBedsTotal = 32,
                emergencyBedsAvailable = 28,
                emergencyBedsTotal = 50,
                generalBedsAvailable = 190,
                generalBedsTotal = 450,
                oxygenBedsAvailable = 25,
                erWaitTimeMinutes = 3,
                availableEmergencySlots = listOf("Immediate Govt Casualty Triage", "Free ER Bed 5", "Acute Nephrology On-Call"),
                address = "822 Poonamallee High Road, Kilpauk, Chennai - 600010",
                hasAmbulanceService = true,
                isRecommendedEmergency = true,
                latitude = 13.0800,
                longitude = 80.2410
            ),
            HealthcareFacility(
                id = "fac_rgggh_govt",
                name = "Rajiv Gandhi Government General Hospital (RGGGH)",
                type = "Government Apex Tertiary Hospital",
                distanceKm = 4.8,
                departments = listOf("Multi-Trauma Center", "Emergency Medicine", "Nephrology & Urology", "Cardiology", "Neurology"),
                services = listOf("24/7 Emergency Triage", "Free State Dialysis", "Advanced ICU", "State Blood Bank", "Ambulance Hub"),
                isEmergency24x7 = true,
                emergencyPhone = "108 / 044 2530 5000",
                travelTimeMinutes = 11,
                traumaLevel = "State Level 1 Multi-Trauma Apex Hospital",
                icuBedsAvailable = 25,
                icuBedsTotal = 60,
                emergencyBedsAvailable = 45,
                emergencyBedsTotal = 80,
                generalBedsAvailable = 350,
                generalBedsTotal = 900,
                oxygenBedsAvailable = 40,
                erWaitTimeMinutes = 4,
                availableEmergencySlots = listOf("Govt Polytrauma Resuscitation", "Acute Dialysis Bay Ready", "Observation Unit 2"),
                address = "EVR Periyar Salai, Park Town (Near Central Station), Chennai - 600003",
                hasAmbulanceService = true,
                isRecommendedEmergency = true,
                latitude = 13.0815,
                longitude = 80.2760
            ),
            HealthcareFacility(
                id = "fac_peripheral_govt",
                name = "Government Peripheral Hospital & Urgent Care",
                type = "Government Community Hospital",
                distanceKm = 3.1,
                departments = listOf("Emergency Stabilization", "General Outpatient", "First Aid & Triage"),
                services = listOf("24/7 Casualty", "Free Generic Medicines", "Oxygen Support", "Ambulance Transfer"),
                isEmergency24x7 = true,
                emergencyPhone = "108 / 044 2474 1102",
                travelTimeMinutes = 7,
                traumaLevel = "Government Urgent Care Center",
                icuBedsAvailable = 4,
                icuBedsTotal = 10,
                emergencyBedsAvailable = 12,
                emergencyBedsTotal = 20,
                generalBedsAvailable = 48,
                generalBedsTotal = 80,
                oxygenBedsAvailable = 8,
                erWaitTimeMinutes = 2,
                availableEmergencySlots = listOf("Walk-in Casualty Bed 1", "Observation Bay"),
                address = "Near Anna Main Road, K.K. Nagar / Ashok Nagar, Chennai",
                hasAmbulanceService = true,
                isRecommendedEmergency = false,
                latitude = 13.0380,
                longitude = 80.2010
            ),
            HealthcareFacility(
                id = "fac_sims",
                name = "SIMS Hospital (SRM Institutes for Medical Science)",
                type = "Private Multispecialty Tertiary Hospital",
                distanceKm = 3.8,
                departments = listOf("Emergency & Trauma", "Cardiology", "Neurology", "Orthopedics", "Critical Care"),
                services = listOf("24/7 Trauma Care", "Cath Lab", "ICU / CCU", "Blood Bank", "Advanced Diagnostics"),
                isEmergency24x7 = true,
                emergencyPhone = "044 2000 2001 / 108",
                travelTimeMinutes = 9,
                traumaLevel = "Level 1 Multispecialty Trauma",
                icuBedsAvailable = 11,
                icuBedsTotal = 30,
                emergencyBedsAvailable = 20,
                emergencyBedsTotal = 35,
                generalBedsAvailable = 95,
                generalBedsTotal = 200,
                oxygenBedsAvailable = 15,
                erWaitTimeMinutes = 5,
                availableEmergencySlots = listOf("Cardiac Emergency Bay", "Acute Surgical ICU"),
                address = "No. 1, Jawaharlal Nehru Salai, Vadapalani (Near Metro), Chennai - 600026",
                hasAmbulanceService = true,
                isRecommendedEmergency = false,
                latitude = 13.0515,
                longitude = 80.2085
            ),
            HealthcareFacility(
                id = "fac_kauvery",
                name = "Kauvery Hospital",
                type = "Private Multispecialty Hospital",
                distanceKm = 4.6,
                departments = listOf("Emergency Medicine", "Heart & Vascular", "Geriatrics", "Nephrology"),
                services = listOf("24/7 Emergency Care", "Emergency Dialysis", "ICU Beds", "Home Care Support"),
                isEmergency24x7 = true,
                emergencyPhone = "044 4000 6000 / 108",
                travelTimeMinutes = 11,
                traumaLevel = "Level 2 Emergency Center",
                icuBedsAvailable = 8,
                icuBedsTotal = 22,
                emergencyBedsAvailable = 15,
                emergencyBedsTotal = 25,
                generalBedsAvailable = 70,
                generalBedsTotal = 150,
                oxygenBedsAvailable = 10,
                erWaitTimeMinutes = 6,
                availableEmergencySlots = listOf("Emergency ICU Bed 3", "Vascular Bay"),
                address = "199 Luz Church Road, Mylapore / Alwarpet, Chennai - 600004",
                hasAmbulanceService = true,
                isRecommendedEmergency = false,
                latitude = 13.0350,
                longitude = 80.2570
            ),
            HealthcareFacility(
                id = "fac_sri_ramachandra",
                name = "Sri Ramachandra Hospital & Medical Center",
                type = "Super Speciality & Tertiary Hospital",
                distanceKm = 2.2,
                departments = listOf("Trauma & Emergency", "Nephrology & Urology", "Diabetology", "Cardiology", "Neurology"),
                services = listOf("24/7 Level 1 Emergency", "State-of-the-art Dialysis Bay", "Cardiac Catheterization", "Blood Bank", "Fleet Ambulances"),
                isEmergency24x7 = true,
                emergencyPhone = "044 4592 8500 / 108",
                travelTimeMinutes = 5,
                traumaLevel = "Level 1 Tertiary Emergency Center",
                icuBedsAvailable = 18,
                icuBedsTotal = 40,
                emergencyBedsAvailable = 25,
                emergencyBedsTotal = 45,
                generalBedsAvailable = 130,
                generalBedsTotal = 300,
                oxygenBedsAvailable = 20,
                erWaitTimeMinutes = 4,
                availableEmergencySlots = listOf("Acute Dialysis Bay", "Emergency Resuscitation Unit"),
                address = "No. 1 Ramachandra Nagar, Porur (Opp. DLF Sector), Chennai - 600116",
                hasAmbulanceService = true,
                isRecommendedEmergency = true,
                latitude = 13.0360,
                longitude = 80.1450
            ),
            HealthcareFacility(
                id = "fac_govt_royapettah",
                name = "Government Royapettah Hospital & Trauma Center",
                type = "Government Apex Trauma & Emergency Hospital",
                distanceKm = 4.2,
                departments = listOf("Emergency & Disaster Medicine", "General Surgery", "Nephrology", "General Medicine"),
                services = listOf("24/7 Free Government Casualty", "Free Emergency Dialysis", "CMCHIS / PM-JAY Cashless", "State Ambulance Base"),
                isEmergency24x7 = true,
                emergencyPhone = "108 / 044 2848 3051",
                travelTimeMinutes = 10,
                traumaLevel = "Government Apex Multi-Trauma Center",
                icuBedsAvailable = 15,
                icuBedsTotal = 36,
                emergencyBedsAvailable = 32,
                emergencyBedsTotal = 60,
                generalBedsAvailable = 210,
                generalBedsTotal = 500,
                oxygenBedsAvailable = 30,
                erWaitTimeMinutes = 3,
                availableEmergencySlots = listOf("Govt Free Emergency Bay", "Trauma ICU Bed 2"),
                address = "1 West Cott Road, Royapettah, Chennai - 600014",
                hasAmbulanceService = true,
                isRecommendedEmergency = true,
                latitude = 13.0530,
                longitude = 80.2600
            ),
            HealthcareFacility(
                id = "fac_govt_stanley",
                name = "Government Stanley Medical College Hospital",
                type = "Government Tertiary Teaching Hospital",
                distanceKm = 7.1,
                departments = listOf("Trauma Care", "Liver & Renal Transplant", "General Medicine", "Cardiology"),
                services = listOf("24/7 Apex Emergency", "Free Dialysis & Medication", "State Blood Bank", "Free Inpatient Care"),
                isEmergency24x7 = true,
                emergencyPhone = "108 / 044 2528 1351",
                travelTimeMinutes = 16,
                traumaLevel = "State Level 1 Multi-Speciality Government Hospital",
                icuBedsAvailable = 20,
                icuBedsTotal = 50,
                emergencyBedsAvailable = 35,
                emergencyBedsTotal = 70,
                generalBedsAvailable = 320,
                generalBedsTotal = 800,
                oxygenBedsAvailable = 35,
                erWaitTimeMinutes = 4,
                availableEmergencySlots = listOf("State Resuscitation Unit", "Free Observation Bay"),
                address = "Old Jail Road, Royapuram, Chennai - 600001",
                hasAmbulanceService = true,
                isRecommendedEmergency = false,
                latitude = 13.1070,
                longitude = 80.2870
            )
        )
    )
    val facilities: StateFlow<List<HealthcareFacility>> = _facilities.asStateFlow()

    // Doctors who previously treated the patient
    private val _previousDoctors = MutableStateFlow(
        listOf(
            Doctor(
                id = "doc_1",
                name = "Dr. Sarah Vance, MD",
                specialty = "Nephrologist",
                facilityName = "District Hospital B — OPD Block 2",
                qualification = "MBBS, MD (Nephrology), DNB",
                experienceYears = 16,
                pastTreatedCondition = "Renal Filtration Surveillance & Creatinine Elevation (1.6 mg/dL)",
                lastTreatedDate = "March 15, 2026",
                phone = "+91 80 2299 1080",
                availableDays = "Mon, Wed, Fri (10:00 AM - 01:30 PM)",
                consultationFee = "₹600",
                doctorNotes = "Advised monitoring of Metformin excretion safety and repeat KFT in September."
            ),
            Doctor(
                id = "doc_2",
                name = "Dr. Ramesh Patel",
                specialty = "Endocrinologist & Diabetologist",
                facilityName = "Metro Specialist Clinic — Suite 301",
                qualification = "MBBS, MD, DM (Endocrinology)",
                experienceYears = 21,
                pastTreatedCondition = "Type 2 Diabetes Mellitus & Statin Therapy",
                lastTreatedDate = "August 12, 2026",
                phone = "+91 80 4567 9200",
                availableDays = "Tue, Thu, Sat (03:00 PM - 07:00 PM)",
                consultationFee = "₹800",
                doctorNotes = "Initiated Metformin 500mg BD + Glimepiride 1mg OD. Stable glycemic response."
            ),
            Doctor(
                id = "doc_3",
                name = "Dr. Anita Desai",
                specialty = "Ophthalmologist (Retina Specialist)",
                facilityName = "Eye Care Specialists Hospital",
                qualification = "MBBS, MS (Ophthalmology), Fellow Vitreo-Retinal",
                experienceYears = 14,
                pastTreatedCondition = "Dilated Fundoscopy / Diabetic Microvascular Screening",
                lastTreatedDate = "November 05, 2022",
                phone = "+91 80 3344 8877",
                availableDays = "Mon - Sat (09:30 AM - 02:00 PM)",
                consultationFee = "₹500",
                doctorNotes = "No proliferative diabetic retinopathy detected. Annual repeat screening is overdue."
            ),
            Doctor(
                id = "doc_4",
                name = "Dr. R. Gupta",
                specialty = "Emergency Medicine Physician",
                facilityName = "City General Hospital — Trauma Triage",
                qualification = "MBBS, MEM (Emergency Medicine)",
                experienceYears = 11,
                pastTreatedCondition = "Acute Dehydration & Viral Gastroenteritis (IV Hydration)",
                lastTreatedDate = "June 22, 2024",
                phone = "+91 80 4567 9111",
                availableDays = "24x7 ER Rotation",
                consultationFee = "Covered under ER",
                doctorNotes = "Discharged after 24hr observation with oral rehydration protocol."
            ),
            Doctor(
                id = "doc_5",
                name = "Dr. A. Sharma",
                specialty = "Senior Physician (Internal Medicine)",
                facilityName = "City Health Clinic — Room 102",
                qualification = "MBBS, MD (General Medicine)",
                experienceYears = 25,
                pastTreatedCondition = "Initial Type 2 Diabetes Diagnosis & Lifestyle Protocol",
                lastTreatedDate = "April 04, 2019",
                phone = "+91 80 2211 4455",
                availableDays = "Mon - Fri (09:00 AM - 12:30 PM)",
                consultationFee = "₹400",
                doctorNotes = "Initial fasting glucose 145 mg/dL. Primary care physician since 2019."
            )
        )
    )
    val previousDoctors: StateFlow<List<Doctor>> = _previousDoctors.asStateFlow()

    // Available Diagnostic Labs & Tests
    private val _labTests = MutableStateFlow(
        listOf(
            LabTest(
                id = "lab_1",
                name = "Kidney Function Test (KFT / RFT)",
                category = "Renal Health",
                testDescription = "Serum Creatinine, Blood Urea Nitrogen (BUN), eGFR, Uric Acid, Electrolytes (Sodium, Potassium).",
                fastingRequired = "10-12 hours overnight fasting required",
                reportTurnaround = "Same Day (6 hours)",
                price = "₹650",
                homeSampleAvailable = true,
                isRecommendedForPatient = true,
                recommendedReason = "Crucial surveillance for your 1.6 mg/dL Creatinine marker before your Nephrology consult."
            ),
            LabTest(
                id = "lab_2",
                name = "HbA1c (Glycated Hemoglobin) + Fasting Glucose",
                category = "Diabetes",
                testDescription = "3-month average blood glucose control and current fasting plasma sugar level.",
                fastingRequired = "8-10 hours fasting required for glucose",
                reportTurnaround = "4 hours",
                price = "₹450",
                homeSampleAvailable = true,
                isRecommendedForPatient = true,
                recommendedReason = "Quarterly diabetes management tracking (last tested HbA1c: 7.4%)."
            ),
            LabTest(
                id = "lab_3",
                name = "Microalbuminuria Urine Test",
                category = "Renal Health",
                testDescription = "Spot urine albumin-to-creatinine ratio (ACR) to detect early diabetic nephropathy.",
                fastingRequired = "First morning urine sample preferred",
                reportTurnaround = "Same Day (8 hours)",
                price = "₹350",
                homeSampleAvailable = true,
                isRecommendedForPatient = true,
                recommendedReason = "Recommended annual kidney protection screening for diabetic patients."
            ),
            LabTest(
                id = "lab_4",
                name = "Comprehensive Lipid Profile",
                category = "Cardiovascular",
                testDescription = "Total Cholesterol, HDL (Good), LDL (Bad), VLDL, and Triglycerides.",
                fastingRequired = "10-12 hours strict fasting required",
                reportTurnaround = "Same Day (6 hours)",
                price = "₹550",
                homeSampleAvailable = true,
                isRecommendedForPatient = false,
                recommendedReason = "Monitors preventive statin efficacy (Atorvastatin 10mg)."
            ),
            LabTest(
                id = "lab_5",
                name = "Complete Blood Count (CBC) with ESR",
                category = "Routine",
                testDescription = "Hemoglobin, RBC, WBC count, Platelets, Hematocrit, and Erythrocyte Sedimentation Rate.",
                fastingRequired = "No fasting required",
                reportTurnaround = "3 hours",
                price = "₹300",
                homeSampleAvailable = true,
                isRecommendedForPatient = false,
                recommendedReason = "General immune and oxygen carrying capacity baseline."
            ),
            LabTest(
                id = "lab_6",
                name = "Liver Function Test (LFT)",
                category = "Routine",
                testDescription = "Bilirubin, SGOT/AST, SGPT/ALT, Alkaline Phosphatase, Total Protein, Albumin.",
                fastingRequired = "No fasting required",
                reportTurnaround = "Same Day (6 hours)",
                price = "₹600",
                homeSampleAvailable = true,
                isRecommendedForPatient = false,
                recommendedReason = "Periodic hepatic safety check."
            )
        )
    )
    val labTests: StateFlow<List<LabTest>> = _labTests.asStateFlow()

    // Nearby Pharmacies & Jan Aushadhi Kendras
    private val _pharmacies = MutableStateFlow(
        listOf(
            Pharmacy(
                id = "ph_apollo_manapakkam",
                name = "Apollo Pharmacy 24x7 (Manapakkam)",
                type = "24x7 Emergency Pharmacy & Home Delivery",
                address = "Mount-Poonamallee High Road, Opposite MIOT Hospital, Manapakkam, Chennai",
                distanceKm = 0.4,
                phone = "044 2249 1122",
                openHours = "Open 24 Hours • 7 Days a week",
                homeDelivery = true,
                discountOffer = "Flat 15% off on chronic diabetes & statin refills • Cold chain insulin storage",
                latitude = 13.0195,
                longitude = 80.1870
            ),
            Pharmacy(
                id = "ph_medplus_manapakkam",
                name = "MedPlus Pharmacy 24x7",
                type = "Retail Chain & 24/7 Express Delivery",
                address = "No. 12, Manapakkam Main Road, Near DLF IT Park, Chennai",
                distanceKm = 0.7,
                phone = "044 2252 3344",
                openHours = "Open 24 Hours • Doorstep Delivery in 30 mins",
                homeDelivery = true,
                discountOffer = "Buy monthly supply & get flat 20% discount on branded generics",
                latitude = 13.0210,
                longitude = 80.1840
            ),
            Pharmacy(
                id = "ph_janaushadhi_guindy",
                name = "Pradhan Mantri Bhartiya Jan Aushadhi Kendra (Govt)",
                type = "Government Subsidized Generic Medicine Store",
                address = "Shop 4, Near Guindy / Porur Junction, Chennai",
                distanceKm = 1.1,
                phone = "1800 180 8080 / 044 2235 6677",
                openHours = "08:00 AM - 10:00 PM (All days)",
                homeDelivery = false,
                discountOffer = "Up to 80% subsidized prices for generic Metformin (₹12/strip), Glimepiride & Atorvastatin",
                latitude = 13.0140,
                longitude = 80.1910
            ),
            Pharmacy(
                id = "ph_apollo_miot_gate",
                name = "Apollo Pharmacy Express (MIOT Gate)",
                type = "24x7 Emergency Nocturnal Chemist",
                address = "Ground Floor, Adjoining MIOT Casualty Entrance, Manapakkam",
                distanceKm = 1.3,
                phone = "044 4200 2299",
                openHours = "Open 24 Hours • Specialized Emergency Drugs",
                homeDelivery = true,
                discountOffer = "Emergency injectables, IV fluids, and dialysis care supplies",
                latitude = 13.0180,
                longitude = 80.1860
            ),
            Pharmacy(
                id = "ph_wellness_forever",
                name = "Wellness Forever 24x7 Day/Night Chemist",
                type = "24x7 Emergency Pharmacy & Surgical Supplies",
                address = "Arcot Road, Near Porur Roundtana, Chennai",
                distanceKm = 2.1,
                phone = "044 2476 5588",
                openHours = "Open 24 Hours • Surgical & Oxygen Supply",
                homeDelivery = true,
                discountOffer = "Nebulizers, glucometer strips & blood pressure monitors on discount",
                latitude = 13.0345,
                longitude = 80.1600
            ),
            Pharmacy(
                id = "ph_govt_jan_aushadhi_kmch",
                name = "Jan Aushadhi Medical Store (KMCH Govt Gate)",
                type = "Government Generic Drug Outlet",
                address = "Opposite Kilpauk Govt Hospital Main Gate, Chennai",
                distanceKm = 2.3,
                phone = "044 2836 1234",
                openHours = "08:00 AM - 09:30 PM",
                homeDelivery = false,
                discountOffer = "Official Govt subsidized diabetes, cardiac & renal supportive medicines",
                latitude = 13.0805,
                longitude = 80.2415
            ),
            Pharmacy(
                id = "ph_netmeds_porur",
                name = "Netmeds Pharmacy Store",
                type = "Digital & Retail Pharmacy Chain",
                address = "Mount Poonamallee Rd, Porur, Chennai",
                distanceKm = 2.8,
                phone = "044 6699 8800",
                openHours = "08:00 AM - 11:00 PM",
                homeDelivery = true,
                discountOffer = "Up to 25% off with prescription upload",
                latitude = 13.0370,
                longitude = 80.1580
            ),
            Pharmacy(
                id = "ph_apollo_greams",
                name = "Apollo 24x7 Superstore (Greams Road)",
                type = "24x7 Apex Hospital Pharmacy",
                address = "Greams Lane, Off Greams Road, Chennai",
                distanceKm = 3.6,
                phone = "044 2829 3333",
                openHours = "Open 24 Hours • Comprehensive Critical Care Stock",
                homeDelivery = true,
                discountOffer = "Complete oncology, nephrology & cardiology medications",
                latitude = 13.0615,
                longitude = 80.2525
            ),
            Pharmacy(
                id = "ph_srm_prime_pharmacy",
                name = "SRM Prime 24/7 Pharmacy",
                type = "24x7 Emergency Hospital Chemist",
                address = "SRM Medical Campus, Ground Floor OPD Block, Ramapuram, Chennai",
                distanceKm = 1.6,
                phone = "044 4743 2399",
                openHours = "Open 24 Hours • All Days",
                homeDelivery = true,
                discountOffer = "Direct hospital discount on diabetes test strips, insulin cartridges & statins",
                latitude = 13.0315,
                longitude = 80.1805
            ),
            Pharmacy(
                id = "ph_ramachandra_pharmacy",
                name = "Sri Ramachandra In-House 24x7 Pharmacy",
                type = "24x7 Apex Hospital Chemist",
                address = "Near Main Casualty Entrance, Ramachandra Medical Center, Porur",
                distanceKm = 2.2,
                phone = "044 4592 8555",
                openHours = "Open 24 Hours • Specialized Nephro & Diabeto Care",
                homeDelivery = true,
                discountOffer = "Full cold chain maintenance for sensitive medications & vaccines",
                latitude = 13.0365,
                longitude = 80.1455
            ),
            Pharmacy(
                id = "ph_noble_chemist_guindy",
                name = "Noble Chemists & Surgical 24/7",
                type = "24/7 Emergency Medical Supplies & Pharmacy",
                address = "Race Course Road, Near Guindy Metro Station, Chennai",
                distanceKm = 1.9,
                phone = "044 2235 4488",
                openHours = "Open 24 Hours",
                homeDelivery = true,
                discountOffer = "Continuous glucose monitors, BP machines & generic medications",
                latitude = 13.0080,
                longitude = 80.2110
            ),
            Pharmacy(
                id = "ph_govt_free_dispensary",
                name = "Government Free Medicine Dispensing Center (KMCH)",
                type = "Government Hospital Free Drug Outlet",
                address = "KMCH Hospital Ground Floor OPD Counter, Kilpauk, Chennai",
                distanceKm = 2.4,
                phone = "108 / 044 2836 4951",
                openHours = "08:00 AM - 08:00 PM • Free Govt Supplies",
                homeDelivery = false,
                discountOffer = "100% Free Government provided essential diabetes & blood pressure medicines",
                latitude = 13.0802,
                longitude = 80.2412
            ),
            Pharmacy(
                id = "ph_frank_ross_porur",
                name = "Frank Ross Pharmacy & Day/Night Chemist",
                type = "Retail & Emergency Pharmacy",
                address = "Mount-Poonamallee High Road, Porur Junction, Chennai",
                distanceKm = 2.1,
                phone = "044 2476 1122",
                openHours = "Open 24 Hours",
                homeDelivery = true,
                discountOffer = "Special monthly subscriber discount for chronic patients",
                latitude = 13.0355,
                longitude = 80.1610
            )
        )
    )
    val pharmacies: StateFlow<List<Pharmacy>> = _pharmacies.asStateFlow()

    // User's Real / Current GPS Location (Ramapuram Medical Zone, Chennai Default)
    private val _userLatitude = MutableStateFlow(13.0315)
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()

    private val _userLongitude = MutableStateFlow(80.1805)
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    private val _locationName = MutableStateFlow("Ramapuram, Chennai (Near SRM Prime & MIOT)")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    fun getDistanceKm(lat: Double, lon: Double): Double {
        val userLat = _userLatitude.value
        val userLon = _userLongitude.value
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat - userLat)
        val dLon = Math.toRadians(lon - userLon)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(lat)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val dist = r * c
        return Math.round(dist * 10.0) / 10.0
    }

    // Conversational AI Chat Messages
    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "msg_welcome",
                text = "Hello Arun! I'm your MedPath AI Clinical Care Companion. I'm aware of your Type 2 Diabetes history, mild renal surveillance (Creatinine 1.6 mg/dL), active medications (Metformin, Glimepiride, Atorvastatin), and drug allergies (Penicillin, Sulfa).\n\nYou can ask me anything about your symptoms, lab reports, medications, diet, or emergency triage in English or any Indian language (हिन्दी, தமிழ், తెలుగు, ಕನ್ನಡ, etc.). How can I help you today?",
                isFromUser = false,
                timestamp = "Just now",
                language = "English"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    fun addChatMessage(message: ChatMessage) {
        _chatMessages.update { it + message }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                id = "msg_welcome_${System.currentTimeMillis()}",
                text = "Hello Arun! I'm your MedPath AI Clinical Care Companion. How can I help you today?",
                isFromUser = false,
                timestamp = "Just now",
                language = "English"
            )
        )
    }

    fun updateWelcomeMessage(welcomeText: String, language: String) {
        _chatMessages.update { list ->
            if (list.isNotEmpty() && list[0].id.startsWith("msg_welcome")) {
                listOf(list[0].copy(text = welcomeText, language = language)) + list.drop(1)
            } else {
                listOf(
                    ChatMessage(
                        id = "msg_welcome_${System.currentTimeMillis()}",
                        text = welcomeText,
                        isFromUser = false,
                        timestamp = "Just now",
                        language = language
                    )
                ) + list
            }
        }
    }

    // Dynamic Location Update & Distance Calculation
    fun updateUserLocation(lat: Double, lng: Double, name: String = "Current GPS Location") {
        _userLatitude.value = lat
        _userLongitude.value = lng
        _locationName.value = name

        // Recalculate dynamic distance to all facilities
        _facilities.update { list ->
            list.map { fac ->
                val distance = calculateDistanceInKm(lat, lng, fac.latitude, fac.longitude)
                val rounded = Math.round(distance * 10.0) / 10.0
                val driveTime = (rounded * 2.5).toInt().coerceAtLeast(3)
                fac.copy(
                    distanceKm = rounded,
                    travelTimeMinutes = driveTime
                )
            }.sortedBy { it.distanceKm }
        }

        // Recalculate distance to all pharmacies
        _pharmacies.update { list ->
            list.map { ph ->
                val distance = calculateDistanceInKm(lat, lng, ph.latitude, ph.longitude)
                val rounded = Math.round(distance * 10.0) / 10.0
                ph.copy(distanceKm = rounded)
            }.sortedBy { it.distanceKm }
        }
    }

    private fun calculateDistanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    // Book Lab Test
    fun bookLabTest(test: LabTest, preferredDate: String, homeSample: Boolean): AppointmentReminder {
        val appointment = AppointmentReminder(
            id = "apt_lab_${System.currentTimeMillis() % 10000}",
            title = "${test.name} — ${if (homeSample) "Home Collection" else "Walk-in Lab"}",
            doctorName = "Suburban Diagnostic Center & Pathology",
            specialty = "Pathology & Biochemistry",
            facilityName = "Suburban Diagnostic Center",
            departmentLocation = if (homeSample) "Patient Home (Sector 4)" else "Counter 2, Main Lab",
            date = preferredDate,
            time = "07:30 AM",
            relativeDays = "Upcoming",
            preparationChecklist = listOf(test.fastingRequired, "Keep previous medical ID ready", "Drink 1 glass of water"),
            notes = "Test Fee: ${test.price}. Turnaround: ${test.reportTurnaround}. ${test.recommendedReason}",
            reminderEnabled = true,
            isCompleted = false
        )
        addAppointment(appointment)
        return appointment
    }

    // Add Extracted OCR Document to Records
    fun addExtractedRecordToHistory(data: ExtractedDocumentData, addToMeds: Boolean): MedicalEvent {
        val newEvent = MedicalEvent(
            id = "evt_ocr_${System.currentTimeMillis() % 10000}",
            date = data.date.ifBlank { "October 05, 2026" },
            type = data.documentType,
            title = data.title,
            finding = data.findings.joinToString(" • ").ifBlank { data.clinicalSummary },
            source = data.doctorOrClinic,
            relatedCondition = if (data.title.contains("Renal", ignoreCase = true) || data.clinicalSummary.contains("Creatinine", ignoreCase = true)) "Renal Function" else "Type 2 Diabetes",
            severity = if (data.allergyWarnings.isNotEmpty() || data.findings.any { it.contains("High", ignoreCase = true) || it.contains("Elevated", ignoreCase = true) }) "Attention" else "Routine",
            doctorName = data.doctorOrClinic,
            notes = "${data.clinicalSummary}\n\nDoctor's Advice: ${data.doctorAdvice}"
        )
        addMedicalEvent(newEvent)

        // Optionally add extracted medications to active tracker
        if (addToMeds && data.prescribedMeds.isNotEmpty()) {
            data.prescribedMeds.forEach { med ->
                val medItem = MedicationItem(
                    id = "med_ocr_${System.currentTimeMillis() % 10000}_${med.name.take(3)}",
                    name = med.name,
                    dosage = med.dosage.ifBlank { "As prescribed" },
                    frequency = med.frequency.ifBlank { "Once daily" },
                    timeSlot = "Morning",
                    scheduledTime = "08:00 AM",
                    instruction = med.instructions.ifBlank { "Take after food with water" },
                    purpose = "Extracted from verified clinical record (${data.title})",
                    pillAppearance = "Clinical tablet",
                    pillsRemaining = 30,
                    refillDays = 30,
                    isTaken = false
                )
                addMedication(medItem)
            }
        }

        return newEvent
    }

    // Active Emergency Bed Reservation State
    private val _activeEmergencyReservation = MutableStateFlow<EmergencyReservation?>(null)
    val activeEmergencyReservation: StateFlow<EmergencyReservation?> = _activeEmergencyReservation.asStateFlow()

    private val _accessibility = MutableStateFlow(
        AccessibilityPreferences(
            fontScale = 1.15f,
            highContrast = false,
            largeTouchTargets = true
        )
    )
    val accessibility: StateFlow<AccessibilityPreferences> = _accessibility.asStateFlow()

    fun updateProfile(newProfile: UserProfile) {
        _userProfile.value = newProfile
    }

    fun loginWithEmail(email: String, pass: String): Boolean {
        if (email.isNotBlank() && pass.length >= 4) {
            _authSession.value = AuthSession(
                isLoggedIn = true,
                authMethod = "Email",
                userIdentifier = email.trim()
            )
            // Update profile email if different
            _userProfile.update { it.copy(email = email.trim()) }
            return true
        }
        return false
    }

    fun loginWithPhone(phone: String, otp: String): Boolean {
        if (phone.isNotBlank() && otp.length >= 4) {
            _authSession.value = AuthSession(
                isLoggedIn = true,
                authMethod = "Phone",
                userIdentifier = phone.trim()
            )
            _userProfile.update { it.copy(phone = phone.trim()) }
            return true
        }
        return false
    }

    fun loginAsDemo() {
        _authSession.value = AuthSession(
            isLoggedIn = true,
            authMethod = "Demo Account",
            userIdentifier = "arun.kumar@medpath.org"
        )
    }

    fun logout() {
        _authSession.value = AuthSession(
            isLoggedIn = false,
            authMethod = "None",
            userIdentifier = ""
        )
    }

    fun toggleMedicationTaken(id: String) {
        var updatedItem: MedicationItem? = null
        _medications.update { list ->
            list.map { med ->
                if (med.id == id) {
                    val newTaken = !med.isTaken
                    val updated = med.copy(
                        isTaken = newTaken,
                        takenTimestamp = if (newTaken) "Today at 09:12 AM" else null
                    )
                    updatedItem = updated
                    updated
                } else med
            }
        }
        updatedItem?.let { item ->
            CoroutineScope(Dispatchers.IO).launch {
                repository?.toggleTaken(item.id, item.isTaken, item.takenTimestamp)
            }
        }
    }

    fun toggleMedicationReminder(id: String) {
        var updatedItem: MedicationItem? = null
        _medications.update { list ->
            list.map { med ->
                if (med.id == id) {
                    val newEnabled = !med.reminderEnabled
                    val updated = med.copy(reminderEnabled = newEnabled)
                    updatedItem = updated
                    updated
                } else med
            }
        }
        updatedItem?.let { item ->
            appContext?.let { ctx ->
                if (item.reminderEnabled) {
                    MedicationNotificationManager.scheduleMedicationReminder(ctx, item)
                } else {
                    MedicationNotificationManager.cancelReminder(ctx, item.id)
                }
            }
            CoroutineScope(Dispatchers.IO).launch {
                repository?.toggleReminder(item.id, item.reminderEnabled)
            }
        }
    }

    fun addMedication(item: MedicationItem) {
        _medications.update { it + item }
        appContext?.let { ctx ->
            MedicationNotificationManager.scheduleMedicationReminder(ctx, item)
        }
        CoroutineScope(Dispatchers.IO).launch {
            repository?.insertMedication(item)
        }
    }

    fun deleteMedication(id: String) {
        _medications.update { list -> list.filter { it.id != id } }
        appContext?.let { ctx ->
            MedicationNotificationManager.cancelReminder(ctx, id)
        }
        CoroutineScope(Dispatchers.IO).launch {
            repository?.deleteMedication(id)
        }
    }

    fun toggleAppointmentReminder(id: String) {
        _appointments.update { list ->
            list.map { apt ->
                if (apt.id == id) apt.copy(reminderEnabled = !apt.reminderEnabled) else apt
            }
        }
    }

    fun toggleAppointmentCompleted(id: String) {
        _appointments.update { list ->
            list.map { apt ->
                if (apt.id == id) apt.copy(isCompleted = !apt.isCompleted) else apt
            }
        }
    }

    fun addAppointment(item: AppointmentReminder) {
        _appointments.update { listOf(item) + it }
    }

    // Medical History Event CRUD
    fun addMedicalEvent(event: MedicalEvent) {
        _medicalEvents.update { listOf(event) + it }
    }

    fun updateMedicalEvent(updated: MedicalEvent) {
        _medicalEvents.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
    }

    fun deleteMedicalEvent(id: String) {
        _medicalEvents.update { list -> list.filter { it.id != id } }
    }

    // Emergency Bed & Slot Reservation
    fun reserveEmergencyBed(facilityId: String, bedType: String, complaint: String): EmergencyReservation {
        val facility = _facilities.value.firstOrNull { it.id == facilityId }
        val reservation = EmergencyReservation(
            id = "emg_res_${System.currentTimeMillis() % 10000}",
            facilityId = facilityId,
            facilityName = facility?.name ?: "Emergency Hospital",
            bedType = bedType,
            etaMinutes = facility?.travelTimeMinutes ?: 10,
            patientName = _userProfile.value.name,
            chiefComplaint = complaint,
            timestamp = "Just now",
            status = "Bed Held • ER Notified & En Route"
        )
        _activeEmergencyReservation.value = reservation

        // Decrement available bed count in facility
        _facilities.update { list ->
            list.map { fac ->
                if (fac.id == facilityId) {
                    when (bedType) {
                        "ICU Bed" -> fac.copy(icuBedsAvailable = (fac.icuBedsAvailable - 1).coerceAtLeast(0))
                        else -> fac.copy(emergencyBedsAvailable = (fac.emergencyBedsAvailable - 1).coerceAtLeast(0))
                    }
                } else fac
            }
        }
        return reservation
    }

    fun cancelEmergencyReservation() {
        val current = _activeEmergencyReservation.value
        if (current != null) {
            // Restore bed count
            _facilities.update { list ->
                list.map { fac ->
                    if (fac.id == current.facilityId) {
                        when (current.bedType) {
                            "ICU Bed" -> fac.copy(icuBedsAvailable = fac.icuBedsAvailable + 1)
                            else -> fac.copy(emergencyBedsAvailable = fac.emergencyBedsAvailable + 1)
                        }
                    } else fac
                }
            }
            _activeEmergencyReservation.value = null
        }
    }

    // Drug Allergies Editing
    fun updateAllergies(allergies: List<String>) {
        _userProfile.update { it.copy(allergies = allergies.filter { a -> a.isNotBlank() }) }
    }

    fun addAllergy(allergy: String) {
        val trimmed = allergy.trim()
        if (trimmed.isNotBlank()) {
            _userProfile.update { prof ->
                if (!prof.allergies.any { it.equals(trimmed, ignoreCase = true) }) {
                    prof.copy(allergies = prof.allergies + trimmed)
                } else prof
            }
        }
    }

    fun removeAllergy(allergy: String) {
        _userProfile.update { prof ->
            prof.copy(allergies = prof.allergies.filter { !it.equals(allergy, ignoreCase = true) })
        }
    }

    fun updateFontScale(scale: Float) {
        _accessibility.update { it.copy(fontScale = scale) }
    }

    fun toggleHighContrast(enabled: Boolean) {
        _accessibility.update { it.copy(highContrast = enabled) }
    }
}
