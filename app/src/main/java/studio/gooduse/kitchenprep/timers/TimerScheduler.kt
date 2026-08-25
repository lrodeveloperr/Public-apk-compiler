package studio.gooduse.kitchenprep.timers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONObject

class TimerScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Schedules a best-effort background alert. The persisted deadline in Room is the
     * source of truth for the native countdown; alarm delivery never completes a task.
     * No exact-alarm special access is required for the core product.
     */
    fun schedule(taskId: String, targetEpoch: Long) {
        if (targetEpoch <= System.currentTimeMillis()) return
        val intent = Intent(appContext, TimerAlarmReceiver::class.java)
            .putExtra(EXTRA_TASK_ID, taskId)
        val pending = PendingIntent.getBroadcast(
            appContext,
            stableRequestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetEpoch, pending)

        val stored = loadStored().toMutableMap()
        stored[taskId] = targetEpoch
        saveStored(stored)
    }

    fun cancel(taskId: String) {
        val intent = Intent(appContext, TimerAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            appContext,
            stableRequestCode(taskId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
        val stored = loadStored().toMutableMap()
        if (stored.remove(taskId) != null) saveStored(stored)
    }

    fun restore() {
        val now = System.currentTimeMillis()
        val stored = loadStored()
        val future = stored.filterValues { it > now + 500L }
        stored.keys.minus(future.keys).forEach(::cancel)
        future.forEach { (taskId, target) ->
            val intent = Intent(appContext, TimerAlarmReceiver::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
            val pending = PendingIntent.getBroadcast(
                appContext,
                stableRequestCode(taskId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pending)
        }
        saveStored(future)
    }

    fun clearAll() {
        loadStored().keys.toList().forEach(::cancel)
        prefs.edit().remove(KEY_TIMERS).apply()
    }

    internal fun onAlarmFired(taskId: String) {
        val stored = loadStored().toMutableMap()
        stored.remove(taskId)
        saveStored(stored)
    }

    private fun loadStored(): Map<String, Long> {
        val raw = prefs.getString(KEY_TIMERS, null) ?: return emptyMap()
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
        val out = linkedMapOf<String, Long>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val epoch = obj.optLong(id, 0L)
            if (epoch > 0L) out[id] = epoch
        }
        return out
    }

    private fun saveStored(values: Map<String, Long>) {
        val obj = JSONObject()
        values.forEach { (id, epoch) -> obj.put(id, epoch) }
        prefs.edit().putString(KEY_TIMERS, obj.toString()).apply()
    }

    private fun stableRequestCode(id: String): Int = id.hashCode() and 0x7fffffff

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        private const val PREFS = "kpb_timer_bridge"
        private const val KEY_TIMERS = "active_timers"
    }
}
