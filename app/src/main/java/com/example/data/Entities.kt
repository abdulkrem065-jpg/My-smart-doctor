package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val doctorName: String,
    val specialty: String,
    val dateTime: Long,
    val notes: String
)

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val timesPerDay: Int,
    val timeToTake: Long // Time of day in millis or next dose
)
