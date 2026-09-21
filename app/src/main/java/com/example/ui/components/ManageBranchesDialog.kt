package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.supabase.SupabaseReceiverDto
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.SleekErrorRed
import com.example.ui.theme.SleekSuccessGreen

private val QUICK_BRANCH_SUGGESTIONS = listOf(
    "Caja 2",
    "Caja 3",
    "Sucursal Norte",
    "Sucursal Sur",
    "Delivery / Envíos",
    "Barra / Salón"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageBranchesDialog(
    currentBranchesString: String,
    storeReceivers: List<SupabaseReceiverDto>,
    onDismiss: () -> Unit,
    onSaveBranches: (newBranches: List<String>, reassignments: Map<String, String>) -> Unit
) {
    val initialBranches = remember(currentBranchesString) {
        val parsed = currentBranchesString.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableList()
        if (!parsed.contains("Principal")) {
            parsed.add(0, "Principal")
        }
        parsed
    }

    val branches = remember { mutableStateListOf<String>().apply { addAll(initialBranches) } }
    val workerReassignments = remember { mutableStateMapOf<String, String>() }

    var newBranchInput by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingText by remember { mutableStateOf("") }
    var branchToDelete by remember { mutableStateOf<String?>(null) }

    fun addBranch(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !branches.any { it.equals(trimmed, ignoreCase = true) }) {
            branches.add(trimmed)
            newBranchInput = ""
        }
    }

    // Modal de confirmación para eliminar sucursal
    if (branchToDelete != null) {
        val target = branchToDelete!!
        val affectedCount = storeReceivers.count {
            val effectiveBranch = workerReassignments[it.id] ?: it.branchName.ifBlank { "Principal" }
            effectiveBranch == target
        }

        AlertDialog(
            onDismissRequest = { branchToDelete = null },
            containerColor = AppTheme.cardBackground,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = SleekErrorRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("¿Eliminar '$target'?", fontWeight = FontWeight.Black, fontSize = 16.sp, color = AppTheme.textPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Esta acción eliminará la sucursal '$target' de la lista del negocio.",
                        fontSize = 13.sp,
                        color = AppTheme.textSecondary
                    )
                    if (affectedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SleekErrorRed.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ $affectedCount trabajador(es) asignados a '$target' serán transferidos automáticamente a la sucursal 'Principal'.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekErrorRed,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        branches.remove(target)
                        // Reasignar trabajadores a Principal
                        storeReceivers.forEach { receiver ->
                            val currentAssigned = workerReassignments[receiver.id] ?: receiver.branchName.ifBlank { "Principal" }
                            if (currentAssigned == target && receiver.id != null) {
                                workerReassignments[receiver.id] = "Principal"
                            }
                        }
                        branchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekErrorRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Eliminar y Reasignar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { branchToDelete = null }) {
                    Text("Cancelar", color = AppTheme.textSecondary)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.cardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Sucursales y Cajas",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = AppTheme.textPrimary
                    )
                    Text(
                        text = "Locales y puntos de cobro del negocio",
                        fontSize = 11.sp,
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
                Text(
                    text = "Crea sucursales o cajas para organizar tus puntos de venta. Los pagos y trabajadores asignados filtrarán las alertas por local.",
                    fontSize = 12.sp,
                    color = AppTheme.textSecondary,
                    lineHeight = 16.sp
                )

                // 1. Campo para agregar nueva sucursal
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AppTheme.pillBackground,
                    border = BorderStroke(1.dp, AppTheme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newBranchInput,
                                onValueChange = { newBranchInput = it },
                                label = { Text("Nueva Sucursal / Caja") },
                                placeholder = { Text("Ej: Caja 2, Salón...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = { addBranch(newBranchInput) },
                                enabled = newBranchInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                                    contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar", modifier = Modifier.size(18.dp))
                            }
                        }

                        // Sugerencias rápidas (1-tap)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "💡 Sugerencias Rápidas:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.textSecondary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QUICK_BRANCH_SUGGESTIONS.forEach { suggestion ->
                                    val alreadyExists = branches.any { it.equals(suggestion, ignoreCase = true) }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (alreadyExists) AppTheme.cardBorder.copy(alpha = 0.3f) else Color(0xFF3B82F6).copy(alpha = 0.10f),
                                        border = BorderStroke(
                                            0.8.dp,
                                            if (alreadyExists) Color.Transparent else Color(0xFF3B82F6).copy(alpha = 0.30f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(enabled = !alreadyExists) { addBranch(suggestion) }
                                    ) {
                                        Text(
                                            text = if (alreadyExists) "✓ $suggestion" else "+ $suggestion",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (alreadyExists) AppTheme.textSecondary else Color(0xFF3B82F6),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Lista de Sucursales Actuales
                Text(
                    text = "Sucursales Activas (${branches.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.textPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    branches.forEachIndexed { index, branch ->
                        val isPrincipal = branch.equals("Principal", ignoreCase = true)
                        val isEditing = editingIndex == index
                        val assignedWorkers = storeReceivers.filter {
                            val eff = workerReassignments[it.id] ?: it.branchName.ifBlank { "Principal" }
                            eff.equals(branch, ignoreCase = true)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPrincipal) Color(0xFF3B82F6).copy(alpha = 0.08f) else AppTheme.pillBackground
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isPrincipal) Color(0xFF3B82F6).copy(alpha = 0.35f) else AppTheme.cardBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (isEditing) {
                                    // Modo edición de nombre
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = editingText,
                                            onValueChange = { editingText = it },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                val trimmed = editingText.trim()
                                                if (trimmed.isNotBlank() && !branches.any { it.equals(trimmed, ignoreCase = true) }) {
                                                    val oldName = branches[index]
                                                    branches[index] = trimmed
                                                    // Migrar trabajadores asignados
                                                    storeReceivers.forEach { receiver ->
                                                        val curr = workerReassignments[receiver.id] ?: receiver.branchName.ifBlank { "Principal" }
                                                        if (curr.equals(oldName, ignoreCase = true) && receiver.id != null) {
                                                            workerReassignments[receiver.id] = trimmed
                                                        }
                                                    }
                                                }
                                                editingIndex = null
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Guardar", tint = SleekSuccessGreen)
                                        }
                                        IconButton(
                                            onClick = { editingIndex = null },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = AppTheme.textSecondary)
                                        }
                                    }
                                } else {
                                    // Modo visualización de sucursal
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isPrincipal) Icons.Default.Store else Icons.Default.Storefront,
                                                contentDescription = null,
                                                tint = if (isPrincipal) Color(0xFF3B82F6) else AppTheme.textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = branch,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 14.sp,
                                                        color = AppTheme.textPrimary
                                                    )
                                                    if (isPrincipal) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFF3B82F6).copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = "Predeterminada",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF3B82F6),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.People,
                                                        contentDescription = null,
                                                        tint = if (assignedWorkers.isNotEmpty()) SleekSuccessGreen else AppTheme.textSecondary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (assignedWorkers.isNotEmpty()) {
                                                            "${assignedWorkers.size} trabajador(es) asignado(s)"
                                                        } else {
                                                            "Sin trabajadores"
                                                        },
                                                        fontSize = 10.sp,
                                                        color = if (assignedWorkers.isNotEmpty()) SleekSuccessGreen else AppTheme.textSecondary
                                                    )
                                                }
                                            }
                                        }

                                        // Acciones
                                        if (!isPrincipal) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        editingIndex = index
                                                        editingText = branch
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Renombrar",
                                                        tint = AppTheme.textSecondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { branchToDelete = branch },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Eliminar",
                                                        tint = SleekErrorRed,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
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
                    val finalBranches = if (branches.isEmpty()) listOf("Principal") else branches.toList()
                    onSaveBranches(finalBranches, workerReassignments.toMap())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (AppTheme.isDark) ModernNeonLime else ModernMatteBlack,
                    contentColor = if (AppTheme.isDark) ModernOnNeonLime else ModernNeonLime
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar Sucursales", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = AppTheme.textSecondary)
            }
        }
    )
}
