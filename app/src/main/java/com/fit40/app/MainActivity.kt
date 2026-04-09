package com.fit40.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Fit40App(this)
            }
        }
    }
}

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
    val notes: String = ""
)

data class AppData(
    val days: List<DayEntry> = emptyList()
)

private const val DATA_FILE = "fit40_data.json"

private fun defaultMeals(): List<MealItem> = listOf(
    MealItem("Breakfast: bread omelette + coffee"),
    MealItem("Post workout: banana"),
    MealItem("Lunch: rice + dal + 2 boiled eggs"),
    MealItem("Evening: tea"),
    MealItem("Dinner: roasted chicken + 2 eggs + curd")
)

private fun defaultWorkout(day: Int): Pair<String, String> {
    return when ((day - 1) % 6) {
        0 -> "Push" to "Bench, incline press, shoulder press, lateral raise, triceps, core"
        1 -> "Pull" to "Rows, pulldowns, curls, rear delts, face pulls, core"
        2 -> "Legs" to "Squats, RDL, lunges, calves, core"
        3 -> "Push" to "Chest, shoulders, triceps, core"
        4 -> "Pull" to "Back, biceps, rear delts, core"
        else -> "Legs" to "Legs + conditioning + core"
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

private fun loadData(context: Context): AppData {
    val file = File(context.filesDir, DATA_FILE)
    if (!file.exists()) {
        val initial = buildDefaultData()
        saveData(context, initial)
        return initial
    }

    return try {
        Gson().fromJson(file.readText(), AppData::class.java) ?: buildDefaultData()
    } catch (_: Exception) {
        buildDefaultData()
    }
}

private fun saveData(context: Context, data: AppData) {
    val file = File(context.filesDir, DATA_FILE)
    file.writeText(Gson().toJson(data))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Fit40App(context: Context) {
    var data by remember { mutableStateOf(AppData()) }
    var selectedDay by remember { mutableIntStateOf(1) }
    var currentTab by remember { mutableStateOf("Today") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val loaded: AppData = withContext(Dispatchers.IO) {
            loadData(context)
        }
        data = loaded
    }

    fun updateDay(updated: DayEntry) {
        val updatedDays = data.days.map { day ->
            if (day.day == updated.day) updated else day
        }
        val newData = AppData(days = updatedDays)
        data = newData
        scope.launch(Dispatchers.IO) {
            saveData(context, newData)
        }
    }

    val dayEntry: DayEntry? = data.days.getOrNull(selectedDay - 1)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Fit40 Tracker") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { currentTab = "Today" }) { Text("Today") }
                TextButton(onClick = { currentTab = "Days" }) { Text("Days") }
                TextButton(onClick = { currentTab = "Progress" }) { Text("Progress") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (currentTab) {
                "Today" -> {
                    if (dayEntry != null) {
                        DayDetail(
                            day = dayEntry,
                            onUpdate = { updateDay(it) }
                        )
                    }
                }
                "Days" -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(data.days) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDay = item.day
                                        currentTab = "Today"
                                    }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Day ${item.day} • ${item.workoutType}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = item.workoutTarget,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val mealDone = item.meals.count { it.done }
                                    Text("Workout: ${if (item.workoutDone) "Done" else "Pending"}")
                                    Text("Meals: $mealDone/${item.meals.size}")
                                }
                            }
                        }
                    }
                }
                "Progress" -> {
                    ProgressScreen(data = data)
                }
            }
        }
    }
}

@Composable
fun DayDetail(
    day: DayEntry,
    onUpdate: (DayEntry) -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Day ${day.day}", style = MaterialTheme.typography.headlineSmall)
        Text("${day.workoutType} day", style = MaterialTheme.typography.titleMedium)
        Text(day.workoutTarget)

        Row {
            Checkbox(
                checked = day.workoutDone,
                onCheckedChange = { checked ->
                    onUpdate(day.copy(workoutDone = checked))
                }
            )
            Text(
                text = "Workout completed",
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Text("Meals", style = MaterialTheme.typography.titleMedium)

        day.meals.forEachIndexed { index, meal ->
            Row {
                Checkbox(
                    checked = meal.done,
                    onCheckedChange = { checked ->
                        val updatedMeals = day.meals.toMutableList()
                        updatedMeals[index] = meal.copy(done = checked)
                        onUpdate(day.copy(meals = updatedMeals))
                    }
                )
                Text(
                    text = meal.name,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        OutlinedTextField(
            value = day.weight,
            onValueChange = { value ->
                onUpdate(day.copy(weight = value))
            },
            label = { Text("Weight") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = day.notes,
            onValueChange = { value ->
                onUpdate(day.copy(notes = value))
            },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ProgressScreen(data: AppData) {
    val workoutDone = data.days.count { it.workoutDone }
    val totalDays = data.days.size
    val totalMeals = data.days.sumOf { it.meals.size }
    val mealsDone = data.days.sumOf { day -> day.meals.count { it.done } }

    val workoutProgress = if (totalDays == 0) 0f else workoutDone.toFloat() / totalDays.toFloat()
    val mealProgress = if (totalMeals == 0) 0f else mealsDone.toFloat() / totalMeals.toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Workout adherence", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { workoutProgress }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                Text("$workoutDone / $totalDays days")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Meal adherence", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { mealProgress }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                Text("$mealsDone / $totalMeals meals")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Plan summary", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("40 day lean cut tracker")
                Text("Push / Pull / Legs rotation")
                Text("Local JSON storage inside app")
            }
        }
    }
}
