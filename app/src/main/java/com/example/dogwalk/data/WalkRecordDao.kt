package com.example.dogwalk.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkRecordDao {
    @Insert
    suspend fun insert(record: WalkRecordEntity): Long

    @Query("SELECT * FROM walk_records ORDER BY startTime DESC")
    fun getAll(): Flow<List<WalkRecordEntity>>

    @Query("SELECT * FROM walk_records ORDER BY startTime DESC LIMIT :count")
    fun getRecent(count: Int): Flow<List<WalkRecordEntity>>

    @Query("SELECT * FROM walk_records WHERE id = :id")
    fun getById(id: Long): Flow<WalkRecordEntity?>
}
