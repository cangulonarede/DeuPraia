package br.com.deupraia

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TideWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        ids.forEach { render(context, manager, it) }
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<TideRefreshWorker>().build())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            context.getSharedPreferences("tides", Context.MODE_PRIVATE)
                .edit().putString("status", "Atualizando…").apply()
            refreshAll(context)
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<TideRefreshWorker>().build())
        }
    }

    companion object {
        const val ACTION_REFRESH = "br.com.deupraia.REFRESH"
        private val eventRegex = Regex("(\\d{2}:\\d{2}).*?(Alta|Baixa)", RegexOption.IGNORE_CASE)

        fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences("tides", Context.MODE_PRIVATE)
            val today = Calendar.getInstance()
            val tomorrow = (today.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, 1)
            }

            val dateFormat = SimpleDateFormat("dd/MM (EEE)", Locale("pt", "BR"))
            val todayDate = dateFormat.format(today.time).replaceFirstChar { it.lowercase(Locale("pt", "BR")) }
            val tomorrowDate = dateFormat.format(tomorrow.time).replaceFirstChar { it.lowercase(Locale("pt", "BR")) }

            val todayRows = prefs.getString("today_rows", "") ?: ""
            val tomorrowRows = prefs.getString("tomorrow_rows", "") ?: ""
            val status = prefs.getString("status", "Natal/RN") ?: "Natal/RN"

            val views = RemoteViews(context.packageName, R.layout.tide_widget)
            views.setTextViewText(R.id.title, "Natal · RN")
            views.setTextViewText(R.id.today_heading, todayDate)
            views.setTextViewText(R.id.tomorrow_heading, tomorrowDate)
            views.setTextViewText(R.id.status, status)

            bindEvents(views, "today", todayRows)
            bindEvents(views, "tomorrow", tomorrowRows)

            val intent = Intent(context, TideWidgetProvider::class.java)
                .setAction(ACTION_REFRESH)
            val pending = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.refresh, pending)
            views.setOnClickPendingIntent(R.id.title, pending)
            manager.updateAppWidget(id, views)
        }

        private fun bindEvents(views: RemoteViews, prefix: String, raw: String) {
            val events = eventRegex.findAll(raw).map { match ->
                match.groupValues[1] to match.groupValues[2].equals("Alta", true)
            }.toList().take(4)

            for (index in 1..4) {
                val rowId = viewId(prefix, "row", index)
                val iconId = viewId(prefix, "icon", index)
                val timeId = viewId(prefix, "time", index)
                val badgeId = viewId(prefix, "badge", index)

                if (index > events.size) {
                    views.setViewVisibility(rowId, View.GONE)
                    continue
                }

                val (time, isHigh) = events[index - 1]
                views.setViewVisibility(rowId, View.VISIBLE)
                views.setImageViewResource(
                    iconId,
                    if (isHigh) R.drawable.tide_high else R.drawable.tide_low
                )
                views.setTextViewText(timeId, time)
                views.setTextViewText(badgeId, if (isHigh) "MARÉ ALTA" else "MARÉ BAIXA")
                views.setInt(
                    badgeId,
                    "setBackgroundResource",
                    if (isHigh) R.drawable.pill_high else R.drawable.pill_low
                )
                views.setTextColor(
                    badgeId,
                    android.graphics.Color.parseColor(
                        if (isHigh) "#76B5FF" else "#FF8294"
                    )
                )
            }

            if (events.isEmpty()) {
                val firstTimeId = viewId(prefix, "time", 1)
                views.setTextViewText(firstTimeId, "Sem dados")
                views.setViewVisibility(viewId(prefix, "row", 1), View.VISIBLE)
            }
        }

        private fun viewId(prefix: String, kind: String, index: Int): Int {
            return when ("$prefix-$kind-$index") {
                "today-row-1" -> R.id.today_row_1
                "today-row-2" -> R.id.today_row_2
                "today-row-3" -> R.id.today_row_3
                "today-row-4" -> R.id.today_row_4
                "today-icon-1" -> R.id.today_icon_1
                "today-icon-2" -> R.id.today_icon_2
                "today-icon-3" -> R.id.today_icon_3
                "today-icon-4" -> R.id.today_icon_4
                "today-time-1" -> R.id.today_time_1
                "today-time-2" -> R.id.today_time_2
                "today-time-3" -> R.id.today_time_3
                "today-time-4" -> R.id.today_time_4
                "today-badge-1" -> R.id.today_badge_1
                "today-badge-2" -> R.id.today_badge_2
                "today-badge-3" -> R.id.today_badge_3
                "today-badge-4" -> R.id.today_badge_4
                "tomorrow-row-1" -> R.id.tomorrow_row_1
                "tomorrow-row-2" -> R.id.tomorrow_row_2
                "tomorrow-row-3" -> R.id.tomorrow_row_3
                "tomorrow-row-4" -> R.id.tomorrow_row_4
                "tomorrow-icon-1" -> R.id.tomorrow_icon_1
                "tomorrow-icon-2" -> R.id.tomorrow_icon_2
                "tomorrow-icon-3" -> R.id.tomorrow_icon_3
                "tomorrow-icon-4" -> R.id.tomorrow_icon_4
                "tomorrow-time-1" -> R.id.tomorrow_time_1
                "tomorrow-time-2" -> R.id.tomorrow_time_2
                "tomorrow-time-3" -> R.id.tomorrow_time_3
                "tomorrow-time-4" -> R.id.tomorrow_time_4
                "tomorrow-badge-1" -> R.id.tomorrow_badge_1
                "tomorrow-badge-2" -> R.id.tomorrow_badge_2
                "tomorrow-badge-3" -> R.id.tomorrow_badge_3
                "tomorrow-badge-4" -> R.id.tomorrow_badge_4
                else -> error("ID de widget desconhecido: $prefix-$kind-$index")
            }
        }

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, TideWidgetProvider::class.java)
            )
            ids.forEach { render(context, manager, it) }
        }
    }
}
