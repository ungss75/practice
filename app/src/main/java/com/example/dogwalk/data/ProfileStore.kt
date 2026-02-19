package com.example.dogwalk.data

import android.content.Context
import android.content.SharedPreferences

class ProfileStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("profile", Context.MODE_PRIVATE)

    fun saveProfile(profile: DogProfile) {
        prefs.edit()
            .putString("dogName", profile.dogName)
            .putInt("goal", profile.goalDistanceMeters)
            .putString("unit", profile.unit.name)
            .apply()
    }

    fun loadProfile(): DogProfile? {
        val dogName = prefs.getString("dogName", null) ?: return null
        val goal = prefs.getInt("goal", 1000)
        val unit = prefs.getString("unit", DistanceUnit.METER.name)?.let { DistanceUnit.valueOf(it) }
            ?: DistanceUnit.METER
        return DogProfile(dogName, goal, unit)
    }
}
