package com.example.focusflow

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Paper = Color(0xFFF7F4EE)
private val Ink = Color(0xFF1C1B1A)
private val Terra = Color(0xFFC25E3A)
private val Muted = Color(0xFF8A8680)
private val Hairline = Color(0xFFE5E0D8)
private val BarIdle = Color(0xFFDCD5C9)

@Composable
fun FocusFlowApp(onToggleSession: () -> Unit) {
    val ctx = LocalContext.current
    val locked = FocusState.isFocusing && FocusState.guardEnabled
    fun remindToFinish() {
        Toast.makeText(ctx, "请先结束本次专注", Toast.LENGTH_SHORT).show()
    }
    BackHandler(enabled = locked) { remindToFinish() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var targetMinutes by rememberSaveable { mutableIntStateOf(45) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Terra,
            background = Paper,
            surface = Paper,
            onBackground = Ink,
            onSurface = Ink
        )
    ) {
        Scaffold(
            containerColor = Paper,
            bottomBar = {
                FocusNav(if (locked) 0 else selectedTab) { tab ->
                    if (locked && tab != 0) remindToFinish() else selectedTab = tab
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (if (locked) 0 else selectedTab) {
                    0 -> FocusScreen(targetMinutes, onToggleSession) { targetMinutes = it }
                    1 -> StatsScreen()
                    else -> SettingsScreen()
                }
            }
        }
    }
}

@Composable
private fun BrandBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            Text(
                "FOCUS",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = Ink
            )
            Text(
                "FLOW",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = Terra
            )
        }
        Text("●", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun PageHeader(eyebrow: String, title: String, subtitle: String = "") {
    Text(eyebrow, fontSize = 11.sp, letterSpacing = 3.sp, color = Muted)
    Spacer(Modifier.height(6.dp))
    Text(title, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    if (subtitle.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 12.sp, color = Muted, lineHeight = 20.sp)
    }
}

@Composable
fun FocusScreen(targetMinutes: Int, onToggleSession: () -> Unit, onTargetChange: (Int) -> Unit) {
    val ctx = LocalContext.current
    val isFocusing = FocusState.isFocusing
    val seconds = FocusState.elapsedSeconds
    val todaySeconds = FocusState.todaySeconds

    LaunchedEffect(Unit) { FocusController.refreshStats(ctx) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.height(22.dp))
        BrandBar()
        Spacer(Modifier.height(22.dp))
        PageHeader("FOCUS SESSION", "专注模式")

        Spacer(Modifier.height(26.dp))
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onToggleSession,
                modifier = Modifier.size(188.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFocusing) Ink else Terra,
                    contentColor = Paper
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(24.dp)) {
                        if (isFocusing) {
                            drawRect(Paper)
                        } else {
                            drawPath(Path().apply {
                                moveTo(size.width * .15f, 0f)
                                lineTo(size.width, size.height / 2)
                                lineTo(size.width * .15f, size.height)
                                close()
                            }, Paper)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(if (isFocusing) "结束专注" else "开始专注", fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(formatTimer(seconds), fontFamily = FontFamily.Serif,
                fontSize = 54.sp, lineHeight = 60.sp, color = Ink)
            if (isFocusing && targetMinutes > 0 && seconds >= targetMinutes * 60L) {
                Text("已达到 ${targetMinutes} 分钟目标", fontSize = 12.sp, color = Terra)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "今日已专注 ${formatDuration(todaySeconds)}",
            fontSize = 12.sp,
            color = Muted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            DurationChip("25 分钟", targetMinutes == 25) { onTargetChange(25) }
            DurationChip("45 分钟", targetMinutes == 45) { onTargetChange(45) }
            DurationChip("自由计时", targetMinutes == 0) { onTargetChange(0) }
        }

        Spacer(Modifier.height(24.dp))
        HairlineDivider()
        GuardOption()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun GuardOption() {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("防切出保护", fontSize = 14.sp, color = Ink)
            Text("切出后返回专注页，来电时暂停保护", fontSize = 11.sp, color = Muted)
        }
        Switch(
            checked = FocusState.guardEnabled,
            enabled = !FocusState.isFocusing,
            onCheckedChange = { FocusGuard.setEnabled(ctx, it) },
            colors = SwitchDefaults.colors(checkedTrackColor = Terra, checkedThumbColor = Paper)
        )
    }
}

@Composable
private fun DurationChip(label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = !FocusState.isFocusing, onClick = onClick)
    ) {
        Text(label, fontSize = 14.sp, color = if (active) Ink else Muted)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .width(32.dp)
                .height(2.dp)
                .background(if (active) Terra else Color.Transparent)
        )
    }
}

@Composable
fun StatsScreen() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { FocusController.refreshStats(ctx) }

    val dailyTotals = FocusState.dailyTotals
    val totalSeconds = FocusState.totalSeconds
    val todaySeconds = FocusState.todaySeconds
    val sessions = FocusState.sessions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.height(22.dp))
        BrandBar()
        Spacer(Modifier.height(22.dp))
        PageHeader("STATISTICS", "专注统计", "时间花在哪里，看得见了。")

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("累计专注", fontSize = 12.sp, color = Muted)
                Spacer(Modifier.height(10.dp))
                Text(
                    formatHours(totalSeconds),
                    fontFamily = FontFamily.Serif,
                    fontSize = 38.sp,
                    color = Ink
                )
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(72.dp)
                    .background(Hairline)
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 22.dp)
            ) {
                Text("今日专注", fontSize = 12.sp, color = Muted)
                Spacer(Modifier.height(10.dp))
                Text(
                    formatHours(todaySeconds),
                    fontFamily = FontFamily.Serif,
                    fontSize = 38.sp,
                    color = Ink
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        HairlineDivider()

        Spacer(Modifier.height(20.dp))
        SectionHead("近 7 天", "单位：小时")
        Spacer(Modifier.height(14.dp))
        WeekBarChart(dailyTotals)

        Spacer(Modifier.height(20.dp))
        SectionHead("历史记录", "共 ${sessions.size} 条")
        if (sessions.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("还没有记录，先开始一次专注吧", color = Muted, fontSize = 12.sp)
        } else {
            sessions.take(20).forEach { session ->
                HistoryRow(session)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHead(title: String, meta: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(meta, fontSize = 11.sp, color = Muted)
    }
}

@Composable
private fun WeekBarChart(data: Map<String, Long>) {
    val entries = data.toList()
    val hours = entries.map { it.second / 3600000.0 }
    val max = (hours.maxOrNull() ?: 0.0).coerceAtLeast(0.1)
    val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        entries.forEachIndexed { index, entry ->
            val value = hours[index]
            val isToday = index == entries.lastIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.height(140.dp)
            ) {
                Text(
                    "%.1f".format(value),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Serif,
                    color = if (isToday) Terra else Muted
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .width(14.dp)
                        .height((value / max * 78).dp.coerceAtLeast(4.dp))
                        .background(if (isToday) Terra else BarIdle)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    dayLabelOf(entry.first, index, dayLabels),
                    fontSize = 11.sp,
                    color = Muted
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(session: FocusSession) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val range = "${timeFormat.format(Date(session.startTime))} - ${timeFormat.format(Date(session.endTime))}"
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                dayLabelOfTime(session.startTime),
                fontSize = 13.sp,
                color = Ink,
                modifier = Modifier.width(36.dp)
            )
            Text(
                range,
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                color = Muted
            )
            Spacer(Modifier.weight(1f))
            Text(
                formatDuration(session.durationMillis / 1000),
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                color = Ink
            )
        }
        HairlineDivider()
    }
}

@Composable
private fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.height(22.dp))
        BrandBar()
        Spacer(Modifier.height(22.dp))
        PageHeader("SETTINGS", "设置", "专注之外的杂项。")
        Spacer(Modifier.height(24.dp))
        HairlineDivider()
        GuardOption()
        HairlineDivider()
        Spacer(Modifier.height(16.dp))
        Text("FocusFlow ${BuildConfig.VERSION_NAME} · 本地 SQLite 存储", fontSize = 12.sp, color = Muted)
    }
}

@Composable
private fun FocusNav(selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.navigationBarsPadding()) {
        HairlineDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("专注", "统计", "设置").forEachIndexed { index, label ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelect(index) }
                ) {
                    Box(
                        Modifier
                            .size(4.dp)
                            .background(if (selected == index) Terra else Color.Transparent)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        label,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = if (selected == index) Ink else Muted
                    )
                }
            }
        }
    }
}

@Composable
private fun HairlineDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Hairline)
    )
}

private fun formatTimer(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%02d:%02d".format(minutes, secs)
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}小时${minutes}分"
        hours > 0 -> "${hours}小时"
        minutes > 0 -> "${minutes}分钟"
        else -> "${seconds}秒"
    }
}

private fun formatHours(totalSeconds: Long): String {
    val hours = totalSeconds / 3600.0
    return if (hours >= 1) "%.1f 小时".format(hours) else "${totalSeconds / 60} 分钟"
}

private fun dayLabelOf(dateKey: String, index: Int, fallback: List<String>): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = format.parse(dateKey) ?: return fallback.getOrElse(index) { "" }
        val calendar = java.util.Calendar.getInstance().apply { time = date }
        fallback[(calendar.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7]
    } catch (e: Exception) {
        fallback.getOrElse(index) { "" }
    }
}

private fun dayLabelOfTime(timeMillis: Long): String {
    val today = FocusDatabase.dateKeyOf(System.currentTimeMillis())
    val yesterday = FocusDatabase.dateKeyOf(System.currentTimeMillis() - 24 * 3600 * 1000)
    return when (FocusDatabase.dateKeyOf(timeMillis)) {
        today -> "今天"
        yesterday -> "昨天"
        else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timeMillis))
    }
}
