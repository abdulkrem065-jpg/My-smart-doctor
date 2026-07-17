package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.utils.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val medications = db.medicationDao().getAllMedications().map { list -> list }.first()

        // تذكير بأدوية اليوم
        medications.forEach { med ->
            val message = "حان وقت تناول دواء ${med.name} (${med.dosage})"
            NotificationHelper.showNotification(
                applicationContext,
                "💊 تذكير دواء",
                message
            )
        }
        return Result.success()
    }
}
