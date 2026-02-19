package com.example.dogwalk.data

import kotlinx.coroutines.flow.Flow

class WalkRepository(private val dao: WalkRecordDao) {
    fun allRecords(): Flow<List<WalkRecordEntity>> = dao.getAll()
    fun recentRecords(): Flow<List<WalkRecordEntity>> = dao.getRecent(3)
    fun recordById(id: Long): Flow<WalkRecordEntity?> = dao.getById(id)
    suspend fun insert(record: WalkRecordEntity): Long = dao.insert(record)
}
