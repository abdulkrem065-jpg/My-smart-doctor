package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnoses")
data class Diagnosis(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val symptoms: String,
    val diagnosisText: String,
    val recommendations: String,
    val confidence: String,
    val timestamp: Long = System.currentTimeMillis()
)
