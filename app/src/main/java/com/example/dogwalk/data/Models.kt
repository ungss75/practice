package com.example.dogwalk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class DogProfile(
    val dogName: String,
    val goalDistanceMeters: Int,
    val unit: DistanceUnit
)

enum class DistanceUnit { METER, KILOMETER }

@Entity(tableName = "walk_records")
data class WalkRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val durationSec: Long,
    val distanceMeters: Double,
    val pathPointsJson: String
)

data class PathPoint(
    val lat: Double,
    val lng: Double,
    val timestamp: Long
)
