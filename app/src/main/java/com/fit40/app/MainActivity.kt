package com.fit40.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Fit40App() }
    }
}

data class MealEntry(
    var label: String,
    var done: Boolean = false
)

data class DayEntry(
    var day: Int,
    var title: String,
    var workoutType: String,
    var workoutTarget: String,
    var workoutDone: Boolean = false,
    var meals: MutableList<MealEntry> = mutableListOf(),
    var weightKg: String = "",
    var notes: String = ""
)

data class Fit40Data(
    var appName: String = "Fit40 Tracker",
    var startDate: String = LocalDate.now().toString(),
    var totalDays: Int = 40,
    var days: MutableList<DayEntry> = mutableListOf()
)

private object Fit40Storage {
    private const val FILE_NAME = "fit40_data.json"
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun load(context: Context): Fit40Data {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            val seed = buildInitialData()
            save(context, seed)
            return seed
        }
        return try {
            file.reader().use { reader ->
                gson.fromJson(reader, Fit40Data::class.java) ?: buildInitialData().also { save(context, it) }
            }
        } catch (_: Exception) {
            buildInitialData().also { save(context, it) }
        }
    }

    fun save(context: Context, data: Fit40Data) {
        val file = File(context.filesDir, FILE_NAME)
        file.writer().use { it.write(gson.toJson(data)) }
    }

    fun exportJson(data: Fit40Data): String = gson.toJson(data)

    fun import(context: Context, json: String): Result<Fit40Data> {
        return try {
            val parsed = gson.fromJson(json, Fit40Data::class.java)
            if (parsed == null || parsed.days.size != 40) {
                Result.failure(IllegalArgumentException("Invalid data file"))
            } else {
                save(context, parsed)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun buildInitialData(): Fit40Data {
    val workouts = listOf(
        Triple("Push A", "Push", "Bench press, overhead press, incline press, lateral raises, triceps, plank"),
        Triple("Pull A", "Pull", "Deadlift, pull ups or pulldown, barbell row, face pulls, curls, cable crunch"),
        Triple("Legs A", "Legs", "Squat, Romanian deadlift, leg press, lunges, calves, hanging raises"),
        Triple("Push B", "Push", "Incline bench, dumbbell shoulder press, chest fly, lateral raises, dips or pushdown, core"),
        Triple("Pull B", "Pull", "Chest supported row, lat pulldown, seated row, rear delts, curls, Russian twists"),
        Triple("Legs B", "Legs", "Front squat or goblet squat, hamstring curl, split squat, calves, ab wheel, incline walk")
    )
    val defaultMeals = listOf(
        "Breakfast - Bread omelette (2 eggs + 1 slice bread) + coffee",
        "Post workout - Banana",
        "Lunch - Rice + dal + 2 boiled eggs",
        "Evening - Tea",
        "Dinner - Roasted chicken + 2 eggs + curd"
    )
    val days = (1..40).map { dayNum ->
        val plan = workouts[(dayNum - 1) % workouts.size]
        DayEntry(
            day = dayNum,
            title = plan.first,
            workoutType = plan.second,
            workoutTarget = plan.third,
            meals = defaultMeals.map { MealEntry(it) }.toMutableList()
        )
    }.toMutableList()
    return Fit40Data(days = days)
}

enum class AppScreen { HOME, DAYS, PROGRESS, DETAIL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Fit40App() {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf(Fit40Storage.load(context)) }
    var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var selectedDayIndex by rememberSaveable { mutableStateOf(todayIndex(data)) }
    var showImportDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberFileExporter { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    OutputStreamWriter(out).use { writer -> writer.write(Fit40Storage.exportJson(data)) }
                }
            }.onSuccess {
                scope.launch { snackbarHostState.showSnackbar("Data exported") }
            }.onFailure {
                scope.launch { snackbarHostState.showSnackbar("Export failed") }
            }
        }
    }

    val importLauncher = rememberFileImporter { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    InputStreamReader(input).use { reader -> reader.readText() }
                }
            }.onSuccess { json ->
                val result = Fit40Storage.import(context, json)
                result.onSuccess {
                    data = it
                    selectedDayIndex = todayIndex(it)
                    scope.launch { snackbarHostState.showSnackbar("Data imported") }
                }.onFailure {
                    scope.launch { snackbarHostState.showSnackbar("Invalid import file") }
                }
            }.onFailure {
                scope.launch { snackbarHostState.showSnackbar("Import failed") }
            }
        }
    }

    val saveAndRefresh: () -> Unit = {
        Fit40Storage.save(context, data)
        data = data.copy(days = data.days.toMutableList())
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            when (screen) {
                                AppScreen.HOME -> "Fit40 Tracker"
                                AppScreen.DAYS -> "All 40 Days"
                                AppScreen.PROGRESS -> "Progress"
                                AppScreen.DETAIL -> "Day ${selectedDayIndex + 1}"
                            }
                        )
                    },
                    navigationIcon = {
                        if (screen == AppScreen.DETAIL) {
                            IconButton(onClick = { screen = AppScreen.DAYS }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (screen == AppScreen.PROGRESS) {
                            IconButton(onClick = { exportLauncher.launch("fit40_data_backup.json") }) {
                                Icon(Icons.Default.Share, contentDescription = "Export")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (screen != AppScreen.DETAIL) {
                    BottomAppBar {
                        NavigationBarItem(
                            selected = screen == AppScreen.HOME,
                            onClick = { screen = AppScreen.HOME },
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = screen == AppScreen.DAYS,
                            onClick = { screen = AppScreen.DAYS },
                            icon = { Icon(Icons.Default.List, null) },
                            label = { Text("Days") }
                        )
                        NavigationBarItem(
                            selected = screen == AppScreen.PROGRESS,
                            onClick = { screen = AppScreen.PROGRESS },
                            icon = { Icon(Icons.Default.InsertChart, null) },
                            label = { Text("Progress") }
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            when (screen) {
                AppScreen.HOME -> HomeScreen(
                    modifier = Modifier.padding(innerPadding),
                    data = data,
                    onOpenToday = {
                        selectedDayIndex = todayIndex(data)
                        screen = AppScreen.DETAIL
                    },
                    onOpenProgress = { screen = AppScreen.PROGRESS }
                )
                AppScreen.DAYS -> DaysScreen(
                    modifier = Modifier.padding(innerPadding),
                    data = data,
                    onOpenDay = {
                        selectedDayIndex = it
                        screen = AppScreen.DETAIL
                    }
                )
                AppScreen.PROGRESS -> ProgressScreen(
                    modifier = Modifier.padding(innerPadding),
                    data = data,
                    onExport = { exportLauncher.launch("fit40_data_backup.json") },
                    onImport = { showImportDialog = true },
                    onReset = {
                        data = buildInitialData()
                        saveAndRefresh()
                        scope.launch { snackbarHostState.showSnackbar("Plan reset") }
                    }
                )
                AppScreen.DETAIL -> DayDetailScreen(
                    modifier = Modifier.padding(innerPadding),
                    day = data.days[selectedDayIndex],
                    onDayChanged = { updatedDay ->
                        data.days[selectedDayIndex] = updatedDay
                        saveAndRefresh()
                    }
                )
            }
        }

        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import backup") },
                text = { Text("Pick a previously exported Fit40 JSON file. This will replace your current data.") },
                confirmButton = {
                    Button(onClick = {
                        showImportDialog = false
                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }) {
                        Text("Choose file")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showImportDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    data: Fit40Data,
    onOpenToday: () -> Unit,
    onOpenProgress: () -> Unit
) {
    val todayIdx = todayIndex(data)
    val day = data.days[todayIdx]
    val completedMeals = day.meals.count { it.done }
    val completedWorkouts = data.days.count { it.workoutDone }
    val totalMealsDone = data.days.sumOf { d -> d.meals.count { it.done } }
    val totalMeals = data.days.sumOf { it.meals.size }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Day ${day.day} of ${data.totalDays}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(day.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(day.workoutTarget)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onOpenToday) { Text("Open today") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Workout done", if (day.workoutDone) "Yes" else "No", Modifier.weight(1f))
                MetricCard("Meals done", "$completedMeals/${day.meals.size}", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Workout adherence", "${percent(completedWorkouts, data.totalDays)}%", Modifier.weight(1f))
                MetricCard("Meal adherence", "${percent(totalMealsDone, totalMeals)}%", Modifier.weight(1f))
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Meals for today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    day.meals.forEach {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (it.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(it.label)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
        item {
            Button(onClick = onOpenProgress, modifier = Modifier.fillMaxWidth()) {
                Text("View progress report")
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DaysScreen(modifier: Modifier = Modifier, data: Fit40Data, onOpenDay: (Int) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(data.days) { index, day ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpenDay(index) }) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Day ${day.day}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        FilterChip(
                            selected = day.workoutDone,
                            onClick = {},
                            label = { Text(if (day.workoutDone) "Workout done" else "Pending") }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(day.title)
                    Text(day.workoutTarget, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text("Meals: ${day.meals.count { it.done }}/${day.meals.size}")
                }
            }
        }
    }
}

@Composable
fun DayDetailScreen(modifier: Modifier = Modifier, day: DayEntry, onDayChanged: (DayEntry) -> Unit) {
    var localDay by remember(day.day) { mutableStateOf(day.copy(meals = day.meals.map { it.copy() }.toMutableList())) }

    LaunchedEffect(localDay) {
        onDayChanged(localDay)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(localDay.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(localDay.workoutType)
                Spacer(Modifier.height(8.dp))
                Text(localDay.workoutTarget)
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = localDay.workoutDone,
                        onCheckedChange = { checked -> localDay = localDay.copy(workoutDone = checked) }
                    )
                    Text("Mark workout complete", fontWeight = FontWeight.Medium)
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Diet tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                localDay.meals.forEachIndexed { mealIndex, meal ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = meal.done,
                            onCheckedChange = { checked ->
                                val updatedMeals = localDay.meals.toMutableList()
                                updatedMeals[mealIndex] = updatedMeals[mealIndex].copy(done = checked)
                                localDay = localDay.copy(meals = updatedMeals)
                            }
                        )
                        Text(meal.label, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bodyweight and notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = localDay.weightKg,
                    onValueChange = { localDay = localDay.copy(weightKg = it.filter { ch -> ch.isDigit() || ch == '.' }) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Weight in kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = localDay.notes,
                    onValueChange = { localDay = localDay.copy(notes = it) },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    label = { Text("Notes") }
                )
            }
        }
    }
}

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    data: Fit40Data,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit
) {
    val workoutSeries = data.days.map { if (it.workoutDone) 1f else 0f }
    val mealSeries = data.days.map { if (it.meals.isEmpty()) 0f else it.meals.count { meal -> meal.done }.toFloat() / it.meals.size }
    val weightSeries = data.days.mapNotNullIndexed { index, day -> day.weightKg.toFloatOrNull()?.let { index to it } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Workout completion", "${percent(data.days.count { it.workoutDone }, data.totalDays)}%", Modifier.weight(1f))
                MetricCard("Meal completion", "${percent(data.days.sumOf { it.meals.count { m -> m.done } }, data.days.sumOf { it.meals.size })}%", Modifier.weight(1f))
            }
        }
        item {
            MetricCard("Current streak", "${currentStreak(data.days)} days")
        }
        item {
            ChartCard("Workout completion by day") { SimpleBarChart(workoutSeries, maxValue = 1f) }
        }
        item {
            ChartCard("Meal adherence by day") { SimpleBarChart(mealSeries, maxValue = 1f) }
        }
        item {
            ChartCard("Weight trend") {
                if (weightSeries.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        Text("Add weight entries on day pages to see the trend")
                    }
                } else {
                    SimpleLineChart(weightSeries)
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Data tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Export JSON backup") }
                    OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Import JSON backup") }
                    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Reset full 40 day plan") }
                    Text(
                        "The app stores everything in one file called fit40_data.json inside app storage.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SimpleBarChart(values: List<Float>, maxValue: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val leftPad = 8.dp.toPx()
        val bottomPad = 16.dp.toPx()
        val chartWidth = size.width - leftPad
        val chartHeight = size.height - bottomPad
        val barGap = 4.dp.toPx()
        val barWidth = max(2f, (chartWidth - (values.size - 1) * barGap) / values.size)

        drawLine(
            color = surfaceVariant,
            start = Offset(leftPad, chartHeight),
            end = Offset(size.width, chartHeight),
            strokeWidth = 2.dp.toPx()
        )

        values.forEachIndexed { index, value ->
            val normalized = (value / maxValue).coerceIn(0f, 1f)
            val barHeight = chartHeight * normalized
            val x = leftPad + index * (barWidth + barGap)
            drawRoundRect(
                color = primary,
                topLeft = Offset(x, chartHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

@Composable
fun SimpleLineChart(points: List<Pair<Int, Float>>) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        if (points.size == 1) {
            drawCircle(primary, radius = 6.dp.toPx(), center = Offset(size.width / 2f, size.height / 2f))
            return@Canvas
        }
        val leftPad = 24.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val chartWidth = size.width - leftPad - rightPad
        val chartHeight = size.height - topPad - bottomPad
        val minY = points.minOf { it.second }
        val maxY = points.maxOf { it.second }
        val rangeY = max(maxY - minY, 1f)
        val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth

        drawLine(surfaceVariant, Offset(leftPad, size.height - bottomPad), Offset(size.width - rightPad, size.height - bottomPad), 2.dp.toPx())
        drawLine(surfaceVariant, Offset(leftPad, topPad), Offset(leftPad, size.height - bottomPad), 2.dp.toPx())

        var previous: Offset? = null
        points.forEachIndexed { i, (_, yVal) ->
            val x = leftPad + i * stepX
            val normalized = (yVal - minY) / rangeY
            val y = topPad + (chartHeight * (1f - normalized))
            val current = Offset(x, y)
            previous?.let { drawLine(primary, it, current, 3.dp.toPx(), cap = StrokeCap.Round) }
            drawCircle(primary, radius = 4.dp.toPx(), center = current)
            previous = current
        }

        drawContext.canvas.nativeCanvas.apply {
            drawText(minY.toString(), 4f, size.height - bottomPad, android.graphics.Paint().apply {
                color = onSurface.toArgb()
                textSize = 28f
            })
            drawText(maxY.toString(), 4f, topPad + 20f, android.graphics.Paint().apply {
                color = onSurface.toArgb()
                textSize = 28f
            })
        }
    }
}

private fun todayIndex(data: Fit40Data): Int {
    val start = runCatching { LocalDate.parse(data.startDate) }.getOrElse { LocalDate.now() }
    val diff = ChronoUnit.DAYS.between(start, LocalDate.now()).toInt()
    return diff.coerceIn(0, data.totalDays - 1)
}

private fun percent(done: Int, total: Int): Int {
    if (total == 0) return 0
    return ((done * 100f) / total).roundToInt()
}

private fun currentStreak(days: List<DayEntry>): Int {
    var streak = 0
    for (day in days) {
        if (day.workoutDone) streak++ else break
    }
    return streak
}

private fun <T, R : Any> List<T>.mapNotNullIndexed(transform: (Int, T) -> R?): List<R> {
    val destination = ArrayList<R>()
    forEachIndexed { index, item -> transform(index, item)?.let(destination::add) }
    return destination
}

@Composable
fun rememberFileExporter(onResult: (Uri?) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json"), onResult)

@Composable
fun rememberFileImporter(onResult: (Uri?) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), onResult)

