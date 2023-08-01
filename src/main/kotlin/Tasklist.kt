import java.io.File
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.*

val taskList = mutableListOf<Task>()
val jsonAdapter: JsonAdapter<List<Task>> = createJsonAdapter()
val jsonFile = File(Util.FILE_PATH)
class Tasklist {

    fun run() {
        loadFromJson()

        while (true) {
            println("Input an action (add, print, edit, delete, end):")
            when (readln()) {
                "add" -> addTasks()
                "print" -> printTasks()
                "edit" -> editTask()
                "delete" -> deleteTask()
                "end" -> break
                else -> println("The input action is invalid")
            }
        }

        saveToJson()
        println("Tasklist exiting!")
    }

}

fun printTasks() {
    if (taskList.isEmpty()) {
        println("No tasks have been input")
    } else {
        println(Util.HORIZON_LINE + "\n" + Util.HEADER_LINE + "\n" + Util.HORIZON_LINE)
        for (i in taskList.indices) {
            val task = taskList[i]
            val taskLines = taskList[i].tasks.flatMap { it.chunked(44) }.listIterator()
            val firstLineTask = taskLines.next().padEnd(44, ' ')
            val (date, time) = task.dateTime.split(" ")
            val index = (i + 1).toString().padEnd(2, ' ')
            val priority = getPriorityOrDueColor(task.priority)
            val due = getPriorityOrDueColor(task.due)

            println("| $index | $date | $time | $priority | $due |${firstLineTask}|")

            for (taskLine in taskLines) {
                val lineTask = taskLine.padEnd(44, ' ')
                println("|    |            |       |   |   |$lineTask|")
            }
            println(Util.HORIZON_LINE)
        }
    }
}

fun addTasks() {
    val priority = takePriority()
    val dateTime = takeDate()
    val dateWithTime = takeTime(dateTime)
    val dateTimeString = getDateTimeString(dateWithTime)
    val due = takeDue(dateWithTime)
    val currentWrittenTask = takeTask()

    val task = Task(dateTimeString, priority, due, currentWrittenTask)
    taskList.add(task)
}

fun deleteTask() {
    printTasks()
    if (taskList.isNotEmpty()) {

        while (true) {
            println("Input the task number (1-${taskList.size}):")
            val input = (readln().toIntOrNull() ?: 0) - 1
            if (input in taskList.indices) {
                taskList.removeAt(input)
                println("The task is deleted")
                break
            } else {
                println("Invalid task number")
            }
        }
    }
}

fun editTask() {
    printTasks()
    if (taskList.isNotEmpty()) {
        while (true) {
            println("Input the task number (1-${taskList.size}):")
            val input = (readln().toIntOrNull() ?: 0) - 1

            if (input in taskList.indices) {
                val task = taskList[input]
                var controller = true

                while (controller) {
                    println("Input a field to edit (priority, date, time, task):")
                    controller = false
                    when (readln()) {
                        "priority" -> task.priority = takePriority()
                        "date" -> task.dateTime = getDateTimeString(takeDate(), getDateTime(task.dateTime))
                        "time" -> task.dateTime = getDateTimeString(takeTime(getDateTime(task.dateTime).date))
                        "task" -> task.tasks = takeTask()
                        else -> {
                            controller = true
                            println("Invalid Field")
                        }

                    }
                }

                println("The task is changed")
                break
            } else {
                println("Invalid task number")
            }
        }
    }
}

fun getDateTime(dateTime: String) = LocalDateTime.parse(dateTime.replaceFirst(" ", "T"))

fun getDateTimeString(dateTime: LocalDateTime) = dateTime.toString().replace("T", " ")

fun getDateTimeString(date: LocalDate, time: LocalDateTime) = getDateTimeString(date.atTime(time.hour, time.minute))

fun getPriorityOrDueColor(priorityOrDue: String): String {
    return when(priorityOrDue) {
        "C", "O" -> "\u001B[101m \u001B[0m"
        "H", "T" -> "\u001B[103m \u001B[0m"
        "N", "I" -> "\u001B[102m \u001B[0m"
        else -> "\u001B[104m \u001B[0m"
    }
}

fun takePriority(): String {
    while (true) {
        println("Input the task priority (C, H, N, L):")
        val priorities = listOf("C", "H", "N", "L")
        val input = readln().uppercase()
        if (priorities.contains(input)) {
            return input
        }
    }
}

fun takeDate(): LocalDate {
    var date: LocalDate

    while (true) {
        println("Input the date (yyyy-mm-dd):")
        val input = readln()
        try {
            val (year, month, day) = input.split("-").map { it.toInt() }
            date = LocalDate(year, month, day)
            break
        } catch (_: IllegalArgumentException) {
            println("The input date is invalid")
        }
    }

    return date
}

fun takeTime(date: LocalDate): LocalDateTime {
    var dateWithTime: LocalDateTime

    while (true) {
        println("Input the time (hh:mm):")
        val input = readln()
        try {
            val (hours, minutes) = input.split(":").map { it.toInt() }
            dateWithTime = date.atTime(hours, minutes)
            break
        } catch (_: IllegalArgumentException) {
            println("The input time is invalid")
        }
    }

    return dateWithTime
}

fun takeDue(dateTime: LocalDateTime): String {
    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
    val taskDate = LocalDate(dateTime.year, dateTime.month, dateTime.dayOfMonth)
    val numberOfDays = currentDate.daysUntil(taskDate)

    return when {
        0 == numberOfDays -> "T"
        numberOfDays > 0 -> "I"
        else -> "O"
    }
}

fun takeTask(): MutableList<String> {
    val currentWrittenTasks = mutableListOf<String>()
    println("Input a new task (enter a blank line to end):")

    while (true) {
        val input = readln().trim()
        if (input.isBlank()) {
            if (currentWrittenTasks.isEmpty()) {
                println("The task is blank")
            }
            return currentWrittenTasks

        } else {
            currentWrittenTasks.add(input)
        }
    }
}

fun loadFromJson() {

    if (jsonFile.exists()) {
        val fileResult = jsonFile.readText()

        if (fileResult.isNotEmpty()) {
            jsonAdapter.fromJson(fileResult)?.let { taskList.addAll(it) }
        }
    }
}

fun saveToJson() {
    val outputJson = jsonAdapter.toJson(taskList)
    jsonFile.writeText(outputJson)
}

fun createJsonAdapter(): JsonAdapter<List<Task>>{
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val type = Types.newParameterizedType(List::class.java, Task::class.java)
    return moshi.adapter<List<Task>>(type)
}