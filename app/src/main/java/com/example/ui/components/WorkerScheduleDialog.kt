package com.example.ui.components

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.supabase.SupabaseReceiverDto
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.SleekErrorRed
import com.example.ui.theme.SleekSuccessGreen
import com.example.util.WorkerScheduleHelper
import java.util.Locale

private data class ShiftPreset(
    val title: String,
    val emoji: String,
    val startTime: String,
    val endTime: String
)

private val SHIFT_PRESETS = listOf(
    ShiftPreset("Mañana", "☀️", "08:00", "16:00"),
    ShiftPreset("Tarde", "⛅", "14:00", "22:00"),
    ShiftPreset("Noche", "🌙", "18:00", "02:00"),
    ShiftPreset("Completo", "🏪", "08:00", "20:00")
)

private data class DayInfo(val code: String, val shortLabel: String, val fullLabel: String)

private val WEEK_DAYS = listOf(
    DayInfo("LUN", "L", "Lunes"),
    DayInfo("MAR", "M", "Martes"),
    DayInfo("MIE", "M", "Miércoles"),
    DayInfo("JUE", "J", "Jueves"),
    DayInfo("VIE", "V", "Viernes"),
    DayInfo("SAB", "S", "Sábado"),
    DayInfo("DOM", "D", "Domingo")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkerScheduleDialog(
    worker: SupabaseReceiverDto,
    onDismiss: () -> Unit,
    onSave: (
        scheduleEnabled: Boolean,
        startTime: String,
        endTime: String,
        days: String,
        onComplete: (Boolean, String?) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    var scheduleEnabled by remember { mutableStateOf(worker.scheduleEnabled) }
    var startTimeInput by remember { mutableStateOf(worker.scheduleStartTime.ifBlank { "08:00" }) }
    var endTimeInput by remember { mutableStateOf(worker.scheduleEndTime.ifBlank { "20:00" }) }
    var selectedDays by remember { mutableStateOf(worker.scheduleDays.ifBlank { "ALL" }) }
    var isSaving by remember { mutableStateOf(false) }

    fun openTimePicker(initialTime: String, onSelected: (String) -> Unit) {
        val parts = initialTime.split(":").mapNotNull { it.trim().toIntOrNull() }
        val hour = if (parts.isNotEmpty()) parts[0] else 8
        val minute = if (parts.size > 1) parts[1] else 0

        TimePickerDialog(
            context,
            { _, h, m ->
                val formatted = String.format(Locale.US, "%02d:%02d", h, m)
                onSelected(formatted)
            },
            hour,
            minute,
            true
        ).show()
    }

    fun adjustTime(time: String, deltaMinutes: Int): String {
        val parts = time.split(":").mapNotNull { it.trim().toIntOrNull() }
        val h = if (parts.isNotEmpty()) parts[0] else 8
        val m = if (parts.size > 1) parts[1] else 0
        var totalMin = (h * 60 + m + deltaMinutes) % (24 * 60)
        if (totalMin < 0) totalMin += (24 * 60)
        val newH = totalMin / 60
        val newM = totalMin % 60
        return String.format(Locale.US, "%02d:%02d", newH, newM)
    }

    val isAllDays = selectedDays.equals("ALL", ignoreCase = true)
    val activeDaysList = remember(selectedDays) {
        if (isAllDays) {
            WEEK_DAYS.map { it.code }
        } else {
            selectedDays.uppercase().split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = AppTheme.cardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack).copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Horario Laboral",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = AppTheme.textPrimary
                    )
                    Text(
                        text = worker.customName,
                        fontSize = 12.sp,
                        color = AppTheme.textSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Interruptor Maestro
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AppTheme.pillBackground,
                    border = BorderStroke(
                        1.dp,
                        if (scheduleEnabled) (if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.4f) else ModernMatteBlack.copy(alpha = 0.2f))
                        else Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Activar Límite de Horario",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AppTheme.textPrimary
                            )
                            Text(
                                text = if (scheduleEnabled) "Solo escuchará alertas durante su turno laboral" else "Recibe alertas 24/7 sin límite de hora",
                                fontSize = 11.sp,
                                color = AppTheme.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = scheduleEnabled,
                            onCheckedChange = { scheduleEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (AppTheme.isDark) ModernMatteBlack else Color.White,
                                checkedTrackColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack
                            )
                        )
                    }
                }

                AnimatedVisibility(
                    visible = scheduleEnabled,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // 2. Presets de Turnos Rápidos (1-Tap)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "⚡ Turnos Rápidos Predefinidos:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.textSecondary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SHIFT_PRESETS.forEach { preset ->
                                    val isMatch = startTimeInput == preset.startTime && endTimeInput == preset.endTime
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isMatch) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.pillBackground,
                                        border = BorderStroke(1.dp, if (isMatch) Color.Transparent else AppTheme.cardBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                startTimeInput = preset.startTime
                                                endTimeInput = preset.endTime
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(preset.emoji, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${preset.title} (${preset.startTime}-${preset.endTime})",
                                                fontSize = 11.sp,
                                                fontWeight = if (isMatch) FontWeight.Black else FontWeight.Medium,
                                                color = if (isMatch) (if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime) else AppTheme.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Tarjetas Interactivas de Horas (Entrada y Salida)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Hora Inicio
                            TimeSelectorCard(
                                title = "Entrada",
                                time = startTimeInput,
                                modifier = Modifier.weight(1f),
                                onTimeClick = {
                                    openTimePicker(startTimeInput) { startTimeInput = it }
                                },
                                onAdjust = { delta ->
                                    startTimeInput = adjustTime(startTimeInput, delta)
                                }
                            )

                            // Hora Fin
                            TimeSelectorCard(
                                title = "Salida",
                                time = endTimeInput,
                                modifier = Modifier.weight(1f),
                                onTimeClick = {
                                    openTimePicker(endTimeInput) { endTimeInput = it }
                                },
                                onAdjust = { delta ->
                                    endTimeInput = adjustTime(endTimeInput, delta)
                                }
                            )
                        }

                        // 4. Selector de Días de Trabajo
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📅 Días Laborales:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.textPrimary
                                )
                            }

                            // Presets rápidos de días
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DayPresetChip(
                                    label = "Lun - Sáb",
                                    isSelected = selectedDays == "LUN,MAR,MIE,JUE,VIE,SAB",
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedDays = "LUN,MAR,MIE,JUE,VIE,SAB" }
                                )
                                DayPresetChip(
                                    label = "Lun - Vie",
                                    isSelected = selectedDays == "LUN,MAR,MIE,JUE,VIE",
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedDays = "LUN,MAR,MIE,JUE,VIE" }
                                )
                                DayPresetChip(
                                    label = "7 Días",
                                    isSelected = isAllDays || activeDaysList.size == 7,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedDays = "ALL" }
                                )
                            }

                            // Botones circulares para cada día L M M J V S D
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                WEEK_DAYS.forEach { day ->
                                    val isSelected = activeDaysList.contains(day.code)
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.pillBackground,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) Color.Transparent else AppTheme.cardBorder
                                        ),
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                val nextList = activeDaysList.toMutableList()
                                                if (isSelected) {
                                                    if (nextList.size > 1) {
                                                        nextList.remove(day.code)
                                                    }
                                                } else {
                                                    nextList.add(day.code)
                                                }
                                                selectedDays = if (nextList.size == 7) "ALL" else nextList.joinToString(",")
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = day.shortLabel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isSelected) (if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime) else AppTheme.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Vista Previa en Vivo y Estado Actual
                        val isNowInShift = remember(startTimeInput, endTimeInput, selectedDays) {
                            WorkerScheduleHelper.isWithinSchedule(
                                scheduleEnabled = true,
                                startTime = startTimeInput,
                                endTime = endTimeInput,
                                days = selectedDays
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isNowInShift) SleekSuccessGreen.copy(alpha = 0.10f) else Color(0xFF64748B).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, if (isNowInShift) SleekSuccessGreen.copy(alpha = 0.35f) else Color(0xFF64748B).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isNowInShift) Icons.Default.CheckCircle else Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = if (isNowInShift) SleekSuccessGreen else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isNowInShift) "🟢 En turno laboral ahora" else "⚪ Fuera de turno en este momento",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isNowInShift) SleekSuccessGreen else AppTheme.textPrimary
                                    )
                                    Text(
                                        text = if (isNowInShift) "Este celular sonará de inmediato ante nuevos cobros."
                                        else "Las notificaciones se mantendrán en silencio hasta que empiece su horario.",
                                        fontSize = 10.sp,
                                        color = AppTheme.textSecondary,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSaving = true
                    onSave(
                        scheduleEnabled,
                        startTimeInput.ifBlank { "08:00" },
                        endTimeInput.ifBlank { "20:00" },
                        selectedDays.ifBlank { "ALL" }
                    ) { success, _ ->
                        isSaving = false
                        if (success) {
                            onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                    contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Guardar Horario", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!isSaving) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = AppTheme.textSecondary)
                }
            }
        }
    )
}

@Composable
private fun TimeSelectorCard(
    title: String,
    time: String,
    modifier: Modifier = Modifier,
    onTimeClick: () -> Unit,
    onAdjust: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppTheme.pillBackground,
        border = BorderStroke(1.dp, AppTheme.cardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AppTheme.cardBackground,
                border = BorderStroke(1.dp, AppTheme.cardBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTimeClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = AppTheme.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = time,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = AppTheme.textPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepperChip(label = "-30m", onClick = { onAdjust(-30) })
                StepperChip(label = "+30m", onClick = { onAdjust(30) })
            }
        }
    }
}

@Composable
private fun StepperChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = AppTheme.cardBackground,
        border = BorderStroke(0.8.dp, AppTheme.cardBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.textPrimary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun DayPresetChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) (if (AppTheme.isDark) ModernNeonLime.copy(alpha = 0.2f) else ModernMatteBlack.copy(alpha = 0.1f)) else AppTheme.pillBackground,
        border = BorderStroke(
            1.dp,
            if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.cardBorder
        ),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                color = if (isSelected) (if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack) else AppTheme.textSecondary
            )
        }
    }
}
