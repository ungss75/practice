package com.example.dogwalk

import android.app.Application
import androidx.room.Room
import com.example.dogwalk.data.AppDatabase
import com.example.dogwalk.data.ProfileStore
import com.example.dogwalk.data.WalkRepository

class DogWalkApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "dog_walk.db").build()
        container = AppContainer(
            walkRepository = WalkRepository(db.walkRecordDao()),
            profileStore = ProfileStore(this)
        )
    }
}

data class AppContainer(
    val walkRepository: WalkRepository,
    val profileStore: ProfileStore
)
