package com.example.dogwalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay

data class WalkRecord(val date: String, val distanceMeters: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DogWalkApp()
                }
            }
        }
    }
}

@Composable
fun DogWalkApp() {
    val navController = rememberNavController()
    val mockRecords = remember {
        listOf(
            WalkRecord("2026-02-15", 850),
            WalkRecord("2026-02-16", 1020),
            WalkRecord("2026-02-17", 740)
        )
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onStartWalk = { navController.navigate("walk") })
        }
        composable("walk") {
            WalkScreen(onEndWalk = { totalDistance ->
                navController.navigate("result/$totalDistance")
            })
        }
        composable(
            route = "result/{distance}",
            arguments = listOf(navArgument("distance") { type = NavType.IntType })
        ) { backStackEntry ->
            val distance = backStackEntry.arguments?.getInt("distance") ?: 0
            ResultScreen(
                totalDistance = distance,
                onViewHistory = { navController.navigate("history") }
            )
        }
        composable("history") {
            HistoryScreen(records = mockRecords)
        }
    }
}

@Composable
fun HomeScreen(onStartWalk: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "강아지 산책", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onStartWalk) {
            Text("산책 시작")
        }
    }
}

@Composable
fun WalkScreen(onEndWalk: (Int) -> Unit) {
    var distance by remember { mutableIntStateOf(0) }
    val targetDistance = 1000

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            distance += 50
        }
    }

    val progressPercent = (distance.toFloat() / targetDistance.toFloat()) * 100f
    val emoji = when {
        progressPercent < 30f -> "😐"
        progressPercent < 70f -> "🙂"
        progressPercent <= 100f -> "😄"
        else -> "🤩"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "산책 중...", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "거리: ${distance}m / ${targetDistance}m", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "행복도: $emoji", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { onEndWalk(distance) }) {
            Text("산책 종료")
        }
    }
}

@Composable
fun ResultScreen(totalDistance: Int, onViewHistory: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "산책 결과", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "총 거리: ${totalDistance}m", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onViewHistory) {
            Text("기록 보기")
        }
    }
}

@Composable
fun HistoryScreen(records: List<WalkRecord>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "산책 기록",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(records) { record ->
                Text(text = "${record.date} - ${record.distanceMeters}m")
            }
        }
    }
}
