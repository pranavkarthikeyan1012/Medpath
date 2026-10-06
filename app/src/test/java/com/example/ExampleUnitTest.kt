package com.example

import com.example.data.db.MedicationEntity
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMedicationTimeParsing() {
    val (h1, m1) = MedicationEntity.parseTime("08:00 AM", "Morning")
    assertEquals(8, h1)
    assertEquals(0, m1)

    val (h2, m2) = MedicationEntity.parseTime("01:30 PM", "Afternoon")
    assertEquals(13, h2)
    assertEquals(30, m2)

    val (h3, m3) = MedicationEntity.parseTime("09:30 PM", "Night")
    assertEquals(21, h3)
    assertEquals(30, m3)
  }
}
