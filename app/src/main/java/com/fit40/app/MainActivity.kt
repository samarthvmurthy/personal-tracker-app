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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    var done: Boolean = false
)

data class DayEntry(
    val day: Int,
    val workoutType: String,
    val workoutTarget: String,
    var workoutDone: Boolean = false,
    val meals: MutableList<MealItem> = mutableListOf(),
    var weight: String = "",
    var notes: String = ""
)

data class AppData(
    val days: MutableList<DayEntry> = mutableListOf()
)

private const val DATA_FILE = "fit40_data.json"

private fun defaultMeals(): MutableList<MealItem> = mutableListOf(
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
    val days = (1..40).map { d ->
        val (type, target) = defaultWorkout(d)
        DayEntry(
            day = d,
            workoutType = type,
            workoutTarget = target,
            meals = defaultMeals()
        )
    }.toMutableList()
    return AppData(days)
}

private fun loadData(context: Context): AppData {
    val file = File(context.filesDir, DATA_FILE)
    return if (file.exists()) {
        runCatching {
            Gson().fromJson(file.readText(), object : TypeToken<AppData>() {}.type)
        }.getOrElse { buildDefaultData() }
    } else {
        buildDefaultData().also { saveData(context, it) }
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
        data = withContext(Dispatchers.IO) { loadData(context) }
    }

    fun persist() {
        scope.launch(Dispatchers.IO) {
            saveData(context, data)
        }
    }

    val dayEntry = data.days.getOrNull(selectedDay - 1)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Fit40 Tracker") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                        DayDetail(dayEntry) {
                            data = data.copy(days = data.days.toMutableList())
                            persist()
                        }
                    }
                }
                "Days" -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(data.days) { _, item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDay = item.day
                                        currentTab = "Today"
                                    }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Day ${item.day} • ${item.workoutType}", style = MaterialTheme.typography.titleMedium)
                                    Text(item.workoutTarget, style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.height(6.dp))
                                    val mealDone = item.meals.count { it.done }
                                    Text("Workout: ${if (item.workoutDone) "Done" else "Pending"}")
                                    Text("Meals: $mealDone/${item.meals.size}")
                                }
                            }
                        }
                    }
                }
                "Progress" -> {
                    ProgressScreen(data)
                }
            }
        }
    }
}

@Composable
fun DayDetail(day: DayEntry, onUpdate: () -> Unit) {
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
                onCheckedChange = {
                    day.workoutDone = it
                    onUpdate()
                }
            )
            Text("Workout completed", modifier = Modifier.padding(top = 12.dp))
        }

        Text("Meals", style = MaterialTheme.typography.titleMedium)
        day.meals.forEachIndexed { index, meal ->
            Row {
                Checkbox(
                    checked = meal.done,
                    onCheckedChange = {
                        day.meals[index] = meal.copy(done = it)
                        onUpdate()
                    }
                )
                Text(meal.name, modifier = Modifier.padding(top = 12.dp))
            }
        }

        OutlinedTextField(
            value = day.weight,
            onValueChange = {
                day.weight = it
                onUpdate()
            },
            label = { Text("Weight") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = day.notes,
            onValueChange = {
                day.notes = it
                onUpdate()
            },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ProgressScreen(data: AppData) {
    val workoutDone = data.days.count { it.workoutDone }
    val totalMeals = data.days.sumOf { it.meals.size }
    val mealsDone = data.days.sumOf { d -> d.meals.count { it.done } }

    val workoutProgress = if (data.days.isEmpty()) 0f else workoutDone.toFloat() / data.days.size
    val mealProgress = if (totalMeals == 0) 0f else mealsDone.toFloat() / totalMeals

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Workout adherence", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { workoutProgress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text("$workoutDone / ${data.days.size} days")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Meal adherence", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { mealProgress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text("$mealsDone / $totalMeals meals")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Plan summary", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("40 day lean cut tracker")
                Text("Push / Pull / Legs rotation")
                Text("Local JSON storage inside app")
            }
        }
    }
}
