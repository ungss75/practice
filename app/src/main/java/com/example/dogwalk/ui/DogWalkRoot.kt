package com.example.dogwalk.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dogwalk.AppContainer
import com.example.dogwalk.data.DistanceUnit
import com.example.dogwalk.data.DogProfile
import com.example.dogwalk.data.PathPoint
import com.example.dogwalk.data.WalkRecordEntity
import com.example.dogwalk.location.LocationTracker
import com.example.dogwalk.location.WalkTrackingService
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

@Composable
fun DogWalkRoot(container: AppContainer) {
    val navController = rememberNavController()
    val appScope = rememberCoroutineScope()
    val startDestination = if (container.profileStore.loadProfile() == null) "onboarding" else "main"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen {
                container.profileStore.saveProfile(it)
                navController.navigate("main") {
                    popUpTo("onboarding") { inclusive = true }
                }
            }
        }
        composable("main") {
            MainScreen(container, onStartWalk = { navController.navigate("tracking") }, onOpenRecords = { navController.navigate("records") })
        }
        composable("tracking") {
            TrackingScreen(profile = container.profileStore.loadProfile() ?: DogProfile("멍멍이", 1000, DistanceUnit.METER)) { entity ->
                appScope.launch {
                    val id = container.walkRepository.insert(entity)
                    navController.navigate("summary/$id")
                }
            }
        }
        composable("summary/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            SummaryScreen(id = it.arguments?.getLong("id") ?: 0L, container = container, onConfirm = { navController.navigate("records") })
        }
        composable("records") {
            RecordListScreen(container = container, onOpenDetail = { navController.navigate("record/$it") })
        }
        composable("record/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            SummaryScreen(id = it.arguments?.getLong("id") ?: 0L, container = container, onConfirm = {})
        }
    }
}

@Composable
private fun OnboardingScreen(onSave: (DogProfile) -> Unit) {
    var dogName by remember { mutableStateOf("") }
    var goalText by remember { mutableStateOf("1000") }
    var unit by remember { mutableStateOf(DistanceUnit.METER) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🐶 환영해요!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = dogName, onValueChange = { dogName = it }, label = { Text("강아지 이름") })
        OutlinedTextField(value = goalText, onValueChange = { goalText = it.filter(Char::isDigit) }, label = { Text("목표 거리") })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { unit = DistanceUnit.METER }) { Text(if (unit == DistanceUnit.METER) "✅ m" else "m") }
            Button(onClick = { unit = DistanceUnit.KILOMETER }) { Text(if (unit == DistanceUnit.KILOMETER) "✅ km" else "km") }
        }
        Button(onClick = {
            val raw = goalText.toIntOrNull() ?: 1000
            onSave(DogProfile(dogName.ifBlank { "멍멍이" }, if (unit == DistanceUnit.KILOMETER) raw * 1000 else raw, unit))
        }, modifier = Modifier.fillMaxWidth()) { Text("저장하고 시작") }
    }
}

@Composable
private fun MainScreen(container: AppContainer, onStartWalk: () -> Unit, onOpenRecords: () -> Unit) {
    val recent by container.walkRepository.recentRecords().collectAsStateWithLifecycle(initialValue = emptyList())
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("오늘도 산책 갈까요?", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onStartWalk, modifier = Modifier.fillMaxWidth()) { Text("산책 시작") }
        Text("최근 기록", fontWeight = FontWeight.Bold)
        recent.forEach {
            Card(Modifier.fillMaxWidth()) {
                Text("${formatDate(it.startTime)} · ${"%.2f".format(it.distanceMeters / 1000)}km · ${it.durationSec}s", Modifier.padding(12.dp))
            }
        }
        Button(onClick = onOpenRecords) { Text("전체 기록") }
    }
}

@Composable
private fun TrackingScreen(profile: DogProfile, onEnd: (WalkRecordEntity) -> Unit) {
    val context = LocalContext.current
    val tracker = remember { LocationTracker(context) }
    val pathPoints = remember { mutableStateListOf<PathPoint>() }
    var totalDistance by remember { mutableDoubleStateOf(0.0) }
    var elapsedSec by remember { mutableLongStateOf(0L) }
    val startTime = remember { System.currentTimeMillis() }

    LaunchedEffect(Unit) {
        context.startForegroundService(Intent(context, WalkTrackingService::class.java))
        launch { while (true) { delay(1000); elapsedSec++ } }
        var last: PathPoint? = null
        tracker.streamLocations().collect { loc ->
            val p = PathPoint(loc.latitude, loc.longitude, System.currentTimeMillis())
            last?.let { totalDistance += distanceBetween(it, p) }
            pathPoints.add(p)
            last = p
        }
    }

    val progress = (totalDistance / profile.goalDistanceMeters).coerceAtLeast(0.0)
    val face = when {
        progress < 0.3 -> "😐"
        progress < 0.7 -> "🙂"
        progress < 1.0 -> "😄"
        else -> "🤩"
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(12.dp)) {
        Text("거리 ${"%.1f".format(totalDistance)}m · 시간 ${elapsedSec}s")
        Text("행복도 $face", style = MaterialTheme.typography.headlineSmall)
        LinearProgressIndicator(progress = { progress.toFloat().coerceAtMost(1f) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        val latLng = pathPoints.map { LatLng(it.lat, it.lng) }
        GoogleMap(modifier = Modifier.weight(1f).fillMaxWidth(), properties = MapProperties(isMyLocationEnabled = true)) {
            if (latLng.isNotEmpty()) {
                Polyline(points = latLng)
                Marker(state = MarkerState(position = latLng.last()))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("일시정지") }
            Button(onClick = {
                context.stopService(Intent(context, WalkTrackingService::class.java))
                onEnd(
                    WalkRecordEntity(
                        startTime = startTime,
                        endTime = System.currentTimeMillis(),
                        durationSec = elapsedSec,
                        distanceMeters = totalDistance,
                        pathPointsJson = toJson(pathPoints)
                    )
                )
            }, modifier = Modifier.weight(1f)) { Text("산책 종료") }
        }
    }
}

@Composable
private fun SummaryScreen(id: Long, container: AppContainer, onConfirm: () -> Unit) {
    val profile = container.profileStore.loadProfile()
    val record by container.walkRepository.recordById(id).collectAsStateWithLifecycle(initialValue = null)
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("산책 요약", style = MaterialTheme.typography.headlineSmall)
        record?.let {
            Text("거리 ${"%.2f".format(it.distanceMeters / 1000)} km")
            Text("시간 ${it.durationSec} 초")
            val rate = if (profile != null) (it.distanceMeters / profile.goalDistanceMeters) * 100 else 0.0
            Text("달성률 ${"%.0f".format(rate)}%")
            Button(onClick = onConfirm) { Text("확인") }
        }
    }
}

@Composable
private fun RecordListScreen(container: AppContainer, onOpenDetail: (Long) -> Unit) {
    val records by container.walkRepository.allRecords().collectAsStateWithLifecycle(initialValue = emptyList())
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(records) { record ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(formatDate(record.startTime), fontWeight = FontWeight.Bold)
                    Text("${"%.2f".format(record.distanceMeters / 1000)} km / ${record.durationSec}s")
                    Button(onClick = { onOpenDetail(record.id) }) { Text("상세") }
                }
            }
        }
    }
}

private fun formatDate(time: Long): String = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.KOREA).format(java.util.Date(time))

private fun distanceBetween(a: PathPoint, b: PathPoint): Double {
    val result = FloatArray(1)
    android.location.Location.distanceBetween(a.lat, a.lng, b.lat, b.lng, result)
    return result[0].toDouble()
}

private fun toJson(points: List<PathPoint>): String {
    val array = JSONArray()
    points.forEach {
        val item = JSONArray()
        item.put(it.lat)
        item.put(it.lng)
        item.put(it.timestamp)
        array.put(item)
    }
    return array.toString()
}
