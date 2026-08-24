package studio.gooduse.kitchenprep.timers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONObject


data class NativeSessionState(
    val keepAwake: Boolean = false,
    val shouldPromptNotifications: Boolean = false,
)

class TimerScheduler(private val context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun syncSession(json: String): NativeSessionState {
        val root = runCatching { JSONObject(json) }.getOrNull()
            ?: return NativeSessionState()

        val settings = root.optJSONObject("settings")
        val alerts = settings?.optBoolean("alerts", true) ?: true
        val awake = settings?.optBoolean("awake", true) ?: true
        val currentView = root.optString("currentView", "home")
        val paused = root.optBoolean("paused", false)
        val timers = root.optJSONObject("taskTimers") ?: JSONObject()
        val now = System.currentTimeMillis()

        val active = linkedMapOf<String, Long>()
        if (alerts && !paused) {
            val keys = timers.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val timer = timers.optJSONObject(id) ?: continue
                val target = timer.optLong("targetEpoch", 0L)
                if (target > now + 500L) active[id] = target
            }
        }

        syncAlarms(active)

        return NativeSessionState(
            keepAwake = awake && currentView == "live",
            shouldPromptNotifications = alerts && currentView == "live" && active.isNotEmpty(),
        )
    }

    fun restore() {
        val stored = loadStored()
        val now = System.currentTimeMillis()
        val future = stored.filterValues { it > now + 500L }
        syncAlarms(future)
    }

    fun clearAll() {
        val stored = loadStored()
        stored.keys.forEach(::cancel)
        prefs.edit().remove(KEY_TIMERS).apply()
    }

    internal fun onAlarmFired(taskId: String) {
        val stored = loadStored().toMutableMap()
        stored.remove(taskId)
        saveStored(stored)
    }

    private fun syncAlarms(active: Map<String, Long>) {
        val old = loadStored()
        (old.keys - active.keys).forEach(::cancel)
        active.forEach { (id, target) ->
            if (old[id] != target) schedule(id, target)
        }
        saveStored(active)
    }

    private fun schedule(taskId: String, targetEpoch: Long) {
        val intent = Intent(appContext, TimerAlarmReceiver::class.java)
            .putExtra(EXTRA_TASK_ID, taskId)
        val pending = PendingIntent.getBroadcast(
            appContext,
            stableRequestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Exact UI timing remains driven by targetEpoch in the frozen app. Background
        // notifications use the policy-light alarm path and may be slightly delayed by
        // Android power management; no exact-alarm special access is required.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetEpoch, pending)
    }

    private fun cancel(taskId: String) {
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
