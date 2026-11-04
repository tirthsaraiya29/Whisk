package com.whisk.tasks

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.whisk.domain.model.TaskItem
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

enum class TaskPriority(val label: String, val level: Int) {
    P1("P1", 1),
    P2("P2", 2),
    P3("P3", 3)
}

class TaskSchedulerManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private val duePattern = Pattern.compile("(?:due:|⏰|@)\\s*([0-9]{4}-[0-9]{2}-[0-9]{2}(?:\\s+[0-9]{2}:[0-9]{2})?)", Pattern.CASE_INSENSITIVE)
    private val priorityP1Pattern = Pattern.compile("(?i)(#p1|#urgent|!high|🚨)")
    private val priorityP2Pattern = Pattern.compile("(?i)(#p2|#med|#medium|!med|⚡)")
    private val priorityP3Pattern = Pattern.compile("(?i)(#p3|#low|!low)")

    fun extractPriority(text: String): TaskPriority {
        return when {
            priorityP1Pattern.matcher(text).find() -> TaskPriority.P1
            priorityP3Pattern.matcher(text).find() -> TaskPriority.P3
            else -> TaskPriority.P2
        }
    }

    fun extractDueDate(text: String): Long? {
        val matcher = duePattern.matcher(text)
        if (matcher.find()) {
            val dateStr = matcher.group(1)?.trim() ?: return null
            return try {
                if (dateStr.contains(":")) {
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    format.parse(dateStr)?.time
                } else {
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    // Set default reminder time to 9:00 AM
                    val parsed = format.parse(dateStr)?.time ?: return null
                    parsed + (9 * 60 * 60 * 1000)
                }
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    fun scheduleTaskReminder(task: TaskItem, triggerAtMillis: Long) {
        if (alarmManager == null || triggerAtMillis <= System.currentTimeMillis()) return

        val priority = extractPriority(task.title).label
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TASK_REMINDER
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(TaskAlarmReceiver.EXTRA_NOTE_TITLE, task.noteTitle)
            putExtra(TaskAlarmReceiver.EXTRA_PRIORITY, priority)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Android 12+ exact alarm permission fallback
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelTaskReminder(taskId: String) {
        if (alarmManager == null) return

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TASK_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun syncAndScheduleAll(tasks: List<TaskItem>) {
        tasks.forEach { task ->
            if (task.isCompleted) {
                cancelTaskReminder(task.id)
            } else {
                val due = extractDueDate(task.title)
                if (due != null && due > System.currentTimeMillis()) {
                    scheduleTaskReminder(task, due)
                }
            }
        }
    }
}
