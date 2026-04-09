package com.fit40.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Fit40Root(this) }
    }
}

private val AppBg = Color(0xFF0B1020)
private val AppSurface = Color(0xFF12182B)
private val AppSurface2 = Color(0xFF1A223A)
private val AppCard = Color(0xFF161F35)
private val AppAccent = Color(0xFF7C4DFF)
private val AppAccent2 = Color(0xFF00D4FF)
private val AppSuccess = Color(0xFF28C76F)
private val AppWarning = Color(0xFFFFB020)
private val AppRest = Color(0xFFFF7A59)
private val AppTextSubtle = Color(0xFF9BA7C7)

private val Fit40Colors = darkColorScheme(
    primary = AppAccent,
    secondary = AppAccent2,
    tertiary = AppSuccess,
    background = AppBg,
    surface = AppSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

data class MealItem(
    val name: String,
    val done: Boolean = false
)

data class DayEntry(
    val day: Int,
    val workoutType: String,
    val workoutTarget: String,
    val workoutDone: Boolean = false,
    val meals: List<MealItem> = emptyList(),
    val weight: String = "",
    val notes: String = "",
    val isRestDay: Boolean = false
)

data class AppData(
    val days: List<DayEntry> = emptyList()
)

private const val DATA_FILE = "fit40_data.json"

private fun defaultMeals(): List<MealItem> = listOf(
    MealItem("Breakfast • Bread omelette + coffee"),
    MealItem("Post workout • Banana"),
    MealItem("Lunch • Rice + dal + 2 boiled eggs"),
    MealItem("Evening • Tea"),
    MealItem("Dinner • Roasted chicken + 2 eggs + curd")
)

private fun restMeals(): List<MealItem> = listOf(
    MealItem("Breakfast • Bread omelette + coffee"),
    MealItem("Lunch • Rice + dal + eggs"),
    MealItem("Evening • Tea"),
    MealItem("Dinner • Chicken / eggs / curd")
)

private fun defaultWorkout(day: Int): Pair<String, String> {
    return when ((day - 1) % 6) {
        0 -> "Push" to "Bench press, incline press, shoulder press, lateral raise, triceps, core"
        1 -> "Pull" to "Rows, pulldowns, curls, rear delts, face pulls, core"
        2 -> "Legs" to "Squats, RDL, lunges, calves, core"
        3 -> "Push" to "Chest, shoulders, triceps, core"
        4 -> "Pull" to "Back, biceps, rear delts, core"
        else -> "Legs" to "Legs, conditioning, core"
    }
}

private fun buildDefaultData(): AppData {
    val days = (1..40).map { day ->
        val workout = defaultWorkout(day)
        DayEntry(
            day = day,
            workoutType = workout.first,
            workoutTarget = workout.second,
            meals = defaultMeals()
        )
    }
    return AppData(days = days)
}

private fun normalizeLoadedData(data: AppData): AppData {
    val fixed = data.days.mapIndexed { index, d ->
        d.copy(
            day = index + 1,
            meals = if (d.meals.isEmpty()) {
                if (d.isRestDay) restMeals() else defaultMeals()
            } else d.meals
        )
    }
    return AppData(fixed)
}

private fun saveData(context: Context, data: AppData) {
    File(context.filesDir, DATA_FILE).writeText(Gson().toJson(data))
}

private fun loadData(context: Context): AppData {
    val file = File(context.filesDir, DATA_FILE)
    if (!file.exists()) {
        val initial = buildDefaultData()
        saveData(context, initial)
        return initial
    }
    return try {
        normalizeLoadedData(Gson().fromJson(file.readText(), AppData::class.java) ?: buildDefaultData())
    } catch (_: Exception) {
        buildDefaultData()
    }
}

@Stable
private data class NavItem(val key: String, val label: String)

@Composable
fun Fit40Root(context: Context) {
    MaterialTheme(colorScheme = Fit40Colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = AppBg) {
            Fit40App(context)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Fit40App(context: Context) {
    var data by remember { mutableStateOf(AppData()) }
    var selectedDay by remember { mutableStateOf(1) }
    var tab by remember { mutableStateOf("home") }
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        data = withContext(Dispatchers.IO) { loadData(context) }
    }

    fun persist(newData: AppData, message: String? = null) {
        data = newData
        scope.launch(Dispatchers.IO) { saveData(context, newData) }
        if (message != null) {
            scope.launch { snackbars.showSnackbar(message) }
        }
    }

    fun updateDay(updated: DayEntry, message: String? = null) {
        val updatedDays = data.days.map { if (it.day == updated.day) updated else it }
        persist(AppData(updatedDays), message)
    }

    fun addRestDay(afterDay: Int) {
        val current = data.days.toMutableList()
        val insertIndex = current.indexOfFirst { it.day == afterDay } + 1
        val rest = DayEntry(
            day = 0,
            workoutType = "Rest Day",
            workoutTarget = "Recovery, mobility, stretching, walking, hydration",
            workoutDone = false,
            meals = restMeals(),
            isRestDay = true
        )
        current.add(insertIndex.coerceAtLeast(0), rest)
        val renumbered = current.mapIndexed { index, day -> day.copy(day = index + 1) }
        persist(AppData(renumbered), "Rest day added")
        selectedDay = (insertIndex + 1).coerceAtLeast(1)
    }

    val currentDay = data.days.getOrNull((selectedDay - 1).coerceAtLeast(0))
        ?: buildDefaultData().days.first()

    Scaffold(
        containerColor = AppBg,
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fit40", fontWeight = FontWeight.Bold)
                        Text(
                            "Lean structure tracker",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTextSubtle
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = AppSurface,
                tonalElevation = 0.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val items = listOf(
                    NavItem("home", "Home"),
                    NavItem("days", "Days"),
                    NavItem("progress", "Progress")
                )
                items.forEach { item ->
                    val icon = when (item.key) {
                        "home" -> Icons.Default.Home
                        "days" -> Icons.Default.CalendarMonth
                        else -> Icons.Default.Insights
                    }
                    NavigationBarItem(
                        selected = tab == item.key,
                        onClick = { tab = item.key },
                        icon = { Icon(icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(AppBg, Color(0xFF0F1630), AppBg)
                    )
                )
                .padding(padding)
        ) {
            when (tab) {
                "home" -> HomeScreen(
                    day = currentDay,
                    appData = data,
                    onOpenDay = { dayNo ->
                        selectedDay = dayNo
                        tab = "days"
                    },
                    onUpdateDay = { updateDay(it, "Saved") }
                )
                "days" -> DaysScreen(
                    appData = data,
                    onSelectDay = { selectedDay = it },
                    onUpdateDay = { updateDay(it, "Day updated") },
                    onAddRestDay = { addRestDay(it) }
                )
                else -> ProgressScreen(data = data)
            }
        }
    }
}

@Composable
fun HomeScreen(
    day: DayEntry,
    appData: AppData,
    onOpenDay: (Int) -> Unit,
    onUpdateDay: (DayEntry) -> Unit
) {
    val trackedWorkoutDays = appData.days.filter { !it.isRestDay }
    val workoutDone = trackedWorkoutDays.count { it.workoutDone }
    val totalWorkoutDays = max(1, trackedWorkoutDays.size)
    val totalMeals = max(1, appData.days.sumOf { it.meals.size })
    val mealsDone = appData.days.sumOf { d -> d.meals.count { it.done } }
    val workoutProgress = workoutDone.toFloat() / totalWorkoutDays
    val mealProgress = mealsDone.toFloat() / totalMeals

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroCard(day, onOpenDay)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Workout",
                    value = "$workoutDone/$totalWorkoutDays",
                    subtitle = "Training days",
                    accent = AppAccent,
                    progress = workoutProgress,
                    icon = Icons.Default.SportsGymnastics
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Meals",
                    value = "$mealsDone/$totalMeals",
                    subtitle = "Meals tracked",
                    accent = AppAccent2,
                    progress = mealProgress,
                    icon = Icons.Default.LocalDining
                )
            }
        }
        item {
            SectionTitle("Today")
        }
        item {
            TodayWorkoutCard(day = day, onUpdateDay = onUpdateDay)
        }
        item {
            MealsCard(day = day, onUpdateDay = onUpdateDay)
        }
        item {
            NotesWeightCard(day = day, onUpdateDay = onUpdateDay)
        }
    }
}

@Composable
fun HeroCard(day: DayEntry, onOpenDay: (Int) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = if (day.isRestDay)
                            listOf(Color(0xFF4B2A1F), Color(0xFF4B1F39))
                        else
                            listOf(Color(0xFF241B4B), Color(0xFF0E2B4B))
                    )
                )
                .padding(20.dp)
        ) {
            AssistChip(
                onClick = { onOpenDay(day.day) },
                label = { Text("Day ${day.day}") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color.White.copy(alpha = 0.12f),
                    labelColor = Color.White
                )
            )
            Spacer(Modifier.height(16.dp))
            Text(
                day.workoutType,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                day.workoutTarget,
                color = Color(0xFFD7DDF6),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (day.isRestDay) {
                    Pill("Recovery day", AppRest)
                } else {
                    Pill(if (day.workoutDone) "Workout done" else "Workout pending", if (day.workoutDone) AppSuccess else AppWarning)
                }
                Pill("${day.meals.count { it.done }}/${day.meals.size} meals", AppAccent2)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onOpenDay(day.day) }) {
                Text("Open day details")
            }
        }
    }
}

@Composable
fun Pill(text: String, color: Color) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White)
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    progress: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(accent.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = accent)
                }
                Spacer(Modifier.width(10.dp))
                Text(title, color = AppTextSubtle)
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, color = AppTextSubtle, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = accent,
                trackColor = Color.White.copy(alpha = 0.10f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
fun TodayWorkoutCard(day: DayEntry, onUpdateDay: (DayEntry) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (day.isRestDay) Icons.Default.Hotel else Icons.Default.SportsGymnastics,
                    contentDescription = null,
                    tint = if (day.isRestDay) AppRest else AppAccent
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (day.isRestDay) "Rest day plan" else "Workout target",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(day.workoutTarget, color = AppTextSubtle)
            if (!day.isRestDay) {
                Divider(color = Color.White.copy(alpha = 0.08f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = day.workoutDone,
                        onCheckedChange = { checked ->
                            onUpdateDay(day.copy(workoutDone = checked))
                        }
                    )
                    Text(if (day.workoutDone) "Completed for today" else "Mark workout complete")
                }
            }
        }
    }
}

@Composable
fun MealsCard(day: DayEntry, onUpdateDay: (DayEntry) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalDining, contentDescription = null, tint = AppAccent2)
                Spacer(Modifier.width(10.dp))
                Text("Diet tracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            day.meals.forEachIndexed { index, meal ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = meal.done,
                        onCheckedChange = { checked ->
                            val meals = day.meals.toMutableList()
                            meals[index] = meal.copy(done = checked)
                            onUpdateDay(day.copy(meals = meals))
                        }
                    )
                    Text(meal.name)
                }
            }
        }
    }
}

@Composable
fun NotesWeightCard(day: DayEntry, onUpdateDay: (DayEntry) -> Unit) {
    var weight by remember(day.day, day.weight) { mutableStateOf(day.weight) }
    var notes by remember(day.day, day.notes) { mutableStateOf(day.notes) }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = AppSuccess)
                Spacer(Modifier.width(10.dp))
                Text("Weight and notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Weight") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            )

            Button(
                onClick = { onUpdateDay(day.copy(weight = weight, notes = notes)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save details")
            }
        }
    }
}

@Composable
fun DaysScreen(
    appData: AppData,
    onSelectDay: (Int) -> Unit,
    onUpdateDay: (DayEntry) -> Unit,
    onAddRestDay: (Int) -> Unit
) {
    val expanded = remember { mutableStateMapOf<Int, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionTitle("All days")
        }

        items(appData.days) { day ->
            val doneMeals = day.meals.count { it.done }
            val isExpanded = expanded[day.day] == true

            Card(
                colors = CardDefaults.cardColors(containerColor = AppCard),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expanded[day.day] = !(expanded[day.day] ?: false)
                                onSelectDay(day.day)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Day ${day.day}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(10.dp))
                                Pill(
                                    if (day.isRestDay) "Rest Day" else day.workoutType,
                                    if (day.isRestDay) AppRest else AppAccent
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                day.workoutTarget,
                                color = AppTextSubtle,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SmallStatus(
                            if (day.isRestDay) "Recovery" else "Workout",
                            if (day.isRestDay) "Planned" else if (day.workoutDone) "Done" else "Pending",
                            if (day.isRestDay) AppRest else if (day.workoutDone) AppSuccess else AppWarning
                        )
                        SmallStatus("Meals", "$doneMeals/${day.meals.size}", AppAccent2)
                    }

                    if (isExpanded) {
                        Spacer(Modifier.height(16.dp))
                        TodayWorkoutCard(day, onUpdateDay)
                        Spacer(Modifier.height(12.dp))
                        MealsCard(day, onUpdateDay)
                        Spacer(Modifier.height(12.dp))
                        NotesWeightCard(day, onUpdateDay)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onAddRestDay(day.day) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add rest day after this")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmallStatus(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = AppTextSubtle, style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ProgressScreen(data: AppData) {
    val trackedWorkoutDays = data.days.filter { !it.isRestDay }
    val workoutDone = trackedWorkoutDays.count { it.workoutDone }
    val totalWorkoutDays = max(1, trackedWorkoutDays.size)
    val totalMeals = max(1, data.days.sumOf { it.meals.size })
    val mealsDone = data.days.sumOf { d -> d.meals.count { it.done } }
    val workoutProgress = workoutDone.toFloat() / totalWorkoutDays.toFloat()
    val mealProgress = mealsDone.toFloat() / totalMeals.toFloat()
    val streak = calculateWorkoutStreak(data.days)
    val weightPoints = data.days.mapNotNull { d ->
        d.weight.toFloatOrNull()?.let { d.day to it }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionTitle("Progress")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Streak",
                    value = streak.toString(),
                    subtitle = "Workout days",
                    accent = AppSuccess,
                    progress = (streak / max(1f, totalWorkoutDays.toFloat())).coerceIn(0f, 1f),
                    icon = Icons.Default.CheckCircle
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Completion",
                    value = "${(workoutProgress * 100).toInt()}%",
                    subtitle = "Workout score",
                    accent = AppAccent,
                    progress = workoutProgress,
                    icon = Icons.Default.Insights
                )
            }
        }
        item {
            ModernChartCard(
                title = "Workout completion by day",
                subtitle = "Rest days are skipped from workout completion",
                values = data.days.map {
                    when {
                        it.isRestDay -> 0.06f
                        it.workoutDone -> 1f
                        else -> 0.12f
                    }
                },
                activeColor = AppAccent,
                baselineColor = Color.White.copy(alpha = 0.08f)
            )
        }
        item {
            ModernChartCard(
                title = "Meal adherence by day",
                subtitle = "Includes workout and rest days",
                values = data.days.map {
                    if (it.meals.isEmpty()) 0f else it.meals.count { meal -> meal.done }.toFloat() / it.meals.size.toFloat()
                },
                activeColor = AppAccent2,
                baselineColor = Color.White.copy(alpha = 0.08f)
            )
        }
        item {
            WeightCard(weightPoints)
        }
        item {
            SummaryCard(workoutDone, totalWorkoutDays, mealsDone, totalMeals, streak, workoutProgress, mealProgress, data.days.count { it.isRestDay })
        }
    }
}

private fun calculateWorkoutStreak(days: List<DayEntry>): Int {
    var streak = 0
    for (day in days) {
        if (day.isRestDay) continue
        if (day.workoutDone) streak++ else break
    }
    return streak
}

@Composable
fun ModernChartCard(
    title: String,
    subtitle: String,
    values: List<Float>,
    activeColor: Color,
    baselineColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = AppTextSubtle, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                val count = max(1, values.size)
                val gap = 8f
                val chartWidth = size.width
                val barWidth = (chartWidth - gap * (count - 1)) / count.toFloat()
                val chartHeight = size.height

                values.forEachIndexed { index, value ->
                    val v = value.coerceIn(0f, 1f)
                    val left = index * (barWidth + gap)
                    val top = chartHeight - (chartHeight * v)
                    drawRoundRect(
                        color = baselineColor,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(24f, 24f)
                    )
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, chartHeight - top),
                        cornerRadius = CornerRadius(24f, 24f)
                    )
                }
            }
        }
    }
}

@Composable
fun WeightCard(weightPoints: List<Pair<Int, Float>>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = AppSuccess)
                Spacer(Modifier.width(10.dp))
                Text("Weight trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            if (weightPoints.size < 2) {
                Text("Add weight on at least 2 days to see a trend line.", color = AppTextSubtle)
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(top = 10.dp)
                ) {
                    val maxY = weightPoints.maxOf { it.second }
                    val minY = weightPoints.minOf { it.second }
                    val range = max(0.1f, maxY - minY)
                    val stepX = size.width / (weightPoints.size - 1).toFloat()

                    for (i in 0 until weightPoints.lastIndex) {
                        val startVal = weightPoints[i].second
                        val endVal = weightPoints[i + 1].second
                        val x1 = i * stepX
                        val x2 = (i + 1) * stepX
                        val y1 = size.height - ((startVal - minY) / range) * size.height
                        val y2 = size.height - ((endVal - minY) / range) * size.height

                        drawLine(
                            color = AppSuccess,
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 8f,
                            cap = StrokeCap.Round
                        )
                    }

                    weightPoints.forEachIndexed { index, point ->
                        val x = index * stepX
                        val y = size.height - ((point.second - minY) / range) * size.height
                        drawCircle(
                            color = Color.White,
                            radius = 10f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = AppSuccess,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    workoutDone: Int,
    totalDays: Int,
    mealsDone: Int,
    totalMeals: Int,
    streak: Int,
    workoutProgress: Float,
    mealProgress: Float,
    restDays: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NoteAlt, contentDescription = null, tint = AppWarning)
                Spacer(Modifier.width(10.dp))
                Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            SummaryRow("Workout completion", "$workoutDone / $totalDays")
            SummaryRow("Meal adherence", "$mealsDone / $totalMeals")
            SummaryRow("Current streak", "$streak days")
            SummaryRow("Rest days", restDays.toString())
            SummaryRow("Workout rate", "${(workoutProgress * 100).toInt()}%")
            SummaryRow("Meal rate", "${(mealProgress * 100).toInt()}%")
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AppTextSubtle)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
