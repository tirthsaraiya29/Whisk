package com.whisk.tasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.whisk.MainActivity
import com.whisk.R
import com.whisk.data.local.WhiskDatabase
import com.whisk.data.repository.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
        val noteTitle = intent.getStringExtra(EXTRA_NOTE_TITLE) ?: "Whisk Vault"
        val priority = intent.getStringExtra(EXTRA_PRIORITY) ?: "P2"

        if (action == ACTION_COMPLETE_TASK) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = WhiskDatabase.getDatabase(context)
                val repo = NotesRepository(
                    noteDao = db.noteDao(),
                    folderDao = db.folderDao(),
                    taskDao = db.taskDao()
                )
                repo.toggleTaskStatus(taskId, targetStatus = true)
                // Dismiss notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(taskId.hashCode())
            }
            return
        }

        // Show Brutalist Task Reminder Notification
        showTaskNotification(context, taskId, taskTitle, noteTitle, priority)
    }

    private fun showTaskNotification(
        context: Context,
        taskId: String,
        taskTitle: String,
        noteTitle: String,
        priority: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "whisk_tasks_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Whisk Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications for scheduled knowledge vault tasks"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open app intent
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TARGET", "tasks")
            putExtra("EXTRA_TASK_ID", taskId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Complete action intent
        val completeIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_COMPLETE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("WHISK // TASK ALERT [$priority]")
            .setContentText("$taskTitle • $noteTitle")
            .setStyle(NotificationCompat.BigTextStyle().bigText("[$priority] $taskTitle\nIn note: $noteTitle"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "[DONE]", completePendingIntent)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }

    companion object {
        const val ACTION_TASK_REMINDER = "com.whisk.ACTION_TASK_REMINDER"
        const val ACTION_COMPLETE_TASK = "com.whisk.ACTION_COMPLETE_TASK"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_NOTE_TITLE = "extra_note_title"
        const val EXTRA_PRIORITY = "extra_priority"
    }
}
