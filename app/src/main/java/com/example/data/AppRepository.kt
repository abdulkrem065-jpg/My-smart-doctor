package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val appointmentDao: AppointmentDao,
    private val medicationDao: MedicationDao,
    private val diagnosisDao: DiagnosisDao
) {
    val allAppointments: Flow<List<Appointment>> = appointmentDao.getAllAppointments()
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()

    fun getAllDiagnoses(): Flow<List<Diagnosis>> = diagnosisDao.getAllDiagnoses()
    suspend fun saveDiagnosis(diagnosis: Diagnosis) = diagnosisDao.insertDiagnosis(diagnosis)

    suspend fun insertAppointment(appointment: Appointment) = appointmentDao.insertAppointment(appointment)
    suspend fun deleteAppointment(id: Int) = appointmentDao.deleteAppointmentById(id)

    suspend fun insertMedication(medication: Medication) = medicationDao.insertMedication(medication)
    suspend fun deleteMedication(id: Int) = medicationDao.deleteMedicationById(id)
}
