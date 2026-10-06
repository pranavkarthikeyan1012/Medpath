package com.example.data

object MockData {
    val demoPatient = Patient(
        name = "Arun Kumar",
        age = 52,
        conditions = listOf("Type 2 diabetes"),
        medications = listOf("Metformin")
    )

    val medicalEvents = listOf(
        MedicalEvent(
            id = "1",
            date = "2019",
            type = "Diagnosis",
            title = "Diabetes diagnosed",
            finding = "Fasting glucose elevated",
            source = "Diagnosis Report — City Clinic",
            relatedCondition = "Diabetes",
            documentUrl = "diagnosis_2019.pdf",
            confidence = "High"
        ),
        MedicalEvent(
            id = "2",
            date = "2021",
            type = "Test Result",
            title = "HbA1c increased",
            finding = "HbA1c 8.2%",
            source = "Lab Report — General Hospital",
            relatedCondition = "Diabetes",
            documentUrl = "lab_report_2021.pdf",
            confidence = "High"
        ),
        MedicalEvent(
            id = "3",
            date = "2023",
            type = "Prescription",
            title = "Medication changed",
            finding = "Metformin dose adjusted",
            source = "Prescription — Dr. Smith",
            relatedCondition = "Diabetes",
            documentUrl = "prescription_2023.pdf",
            confidence = "High"
        ),
        MedicalEvent(
            id = "4",
            date = "2025",
            type = "Test Result",
            title = "Abnormal kidney-function marker",
            finding = "Elevated Creatinine",
            source = "Lab Report — Demo Hospital",
            relatedCondition = "Diabetes",
            documentUrl = "lab_report_2025.pdf",
            confidence = "High"
        ),
        MedicalEvent(
            id = "5",
            date = "2026",
            type = "Consultation",
            title = "Current consultation",
            finding = "Routine checkup",
            source = "Consultation Note — Dr. Jones",
            relatedCondition = "None",
            documentUrl = "consultation_2026.pdf",
            confidence = "High"
        )
    ).sortedByDescending { it.date }

    val documents = listOf(
        Document("1", "Prescription.pdf", "Prescription", "2026", "Processed"),
        Document("2", "LabReport.pdf", "Lab Report", "2025", "Processed"),
        Document("3", "Scan.pdf", "Scan Report", "2025", "Processed"),
        Document("4", "Diagnosis.pdf", "Diagnosis Report", "2019", "Processed")
    )

    val facilities = listOf(
        HealthcareFacility("1", "District Hospital B", "Government Hospital", 38.0, listOf("Nephrology", "General Medicine", "Diagnostics"), listOf("Blood tests", "Kidney function tests", "Imaging")),
        HealthcareFacility("2", "City General", "Private Hospital", 12.5, listOf("General Medicine", "Cardiology", "Diagnostics"), listOf("Blood tests", "X-Ray")),
        HealthcareFacility("3", "Metro Specialist Clinic", "Private Clinic", 5.2, listOf("Nephrology", "Endocrinology"), listOf("Consultation", "Basic Labs")),
        HealthcareFacility("4", "Community Health Center", "Government Clinic", 2.1, listOf("General Practice"), listOf("Basic Labs", "Vaccination")),
        HealthcareFacility("5", "Apex Care Hospital", "Private Hospital", 45.0, listOf("Nephrology", "Urology", "Advanced Diagnostics"), listOf("MRI", "CT Scan", "Comprehensive Labs")),
        HealthcareFacility("6", "Sunrise Medical Center", "Private Hospital", 22.4, listOf("Internal Medicine", "Endocrinology"), listOf("Blood tests", "Ultrasound")),
        HealthcareFacility("7", "Regional General Hospital", "Government Hospital", 60.0, listOf("All Major Departments"), listOf("Full Diagnostic Suite")),
        HealthcareFacility("8", "CarePlus Clinic", "Private Clinic", 8.0, listOf("General Medicine"), listOf("Consultation")),
        HealthcareFacility("9", "Kidney Institute", "Specialty Hospital", 110.0, listOf("Nephrology", "Urology"), listOf("Transplant", "Dialysis", "Advanced Labs")),
        HealthcareFacility("10", "Suburban Diagnostic Center", "Diagnostic Center", 15.0, listOf("Diagnostics"), listOf("Blood tests", "Imaging", "Pathology"))
    )

    val careSuggestion = CareSuggestion(
        department = "Nephrology",
        facility = facilities[0], // District Hospital B
        relevantHistory = listOf("Diabetes", "Previous abnormal HbA1c", "Recent kidney-function abnormality"),
        previousTests = listOf("Blood test", "Kidney function test"),
        documentsToCarry = listOf("Previous laboratory reports", "Current prescription", "Previous discharge summary"),
        priority = "Specialist consultation"
    )
}
