package com.watchvault.data.repository

import com.watchvault.data.dao.MaintenanceRecordDao
import com.watchvault.data.entity.MaintenanceRecord

class MaintenanceRepository(private val maintenanceRecordDao: MaintenanceRecordDao) {
    suspend fun forWatch(watchUuid: String): List<MaintenanceRecord> = maintenanceRecordDao.forWatch(watchUuid)
    suspend fun add(record: MaintenanceRecord) = maintenanceRecordDao.insert(record)
    suspend fun addAll(records: List<MaintenanceRecord>) {
        if (records.isNotEmpty()) maintenanceRecordDao.insertAll(records)
    }
}
