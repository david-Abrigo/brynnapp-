package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeviceRole
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkersScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ModernBorderLight
import com.example.ui.theme.ModernMatteBlack
import com.example.ui.theme.ModernNeonLime
import com.example.ui.theme.ModernOnNeonLime
import com.example.ui.theme.ModernSubtleTextLight
import com.example.ui.viewmodel.MainViewModel

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
    WORKERS("Equipo", Icons.Filled.People, Icons.Outlined.People, "tab_workers"),
    BRANCH_PAYMENTS("Validar", Icons.Filled.Storefront, Icons.Outlined.Storefront, "tab_branch_payments"),
    REPORTS("Reportes", Icons.Filled.Assessment, Icons.Outlined.Assessment, "tab_reports"),
    SETTINGS("Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val isSender = storeConfig.deviceRole == DeviceRole.SENDER

    // El tab de Equipo SOLO es visible en modo Emisor (Dueño); Solo Local y Receptor tienen Dashboard, Reportes y Ajustes
    val availableTabs = remember(isSender) {
        if (isSender) {
            listOf(MainTab.DASHBOARD, MainTab.WORKERS, MainTab.REPORTS, MainTab.SETTINGS)
        } else {
            listOf(MainTab.DASHBOARD, MainTab.REPORTS, MainTab.SETTINGS)
        }
    }

    var currentTab by rememberSaveable { mutableStateOf(MainTab.DASHBOARD) }

    // Si el usuario cambia el rol o estaba en una pestaña no permitida, redirigir a Dashboard
    LaunchedEffect(isSender) {
        if (!isSender && currentTab == MainTab.WORKERS) {
            currentTab = MainTab.DASHBOARD
        } else if (currentTab == MainTab.BRANCH_PAYMENTS) {
            currentTab = MainTab.DASHBOARD
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Floating Modern Pill Navigation Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = if (AppTheme.isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.08f)
                        )
                        .testTag("main_bottom_navigation"),
                    shape = RoundedCornerShape(32.dp),
                    color = if (AppTheme.isDark) Color(0xFF1E2028) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (AppTheme.isDark) Color(0xFF2E313C) else Color(0xFFE5E7EB)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        availableTabs.forEach { tab ->
                            val isSelected = tab == currentTab

                            if (isSelected) {
                                // Active Tab: Full Vibrant Gradient (#BDF347 -> #35589A) Pill
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFFBDF347), // Verde Lima
                                                    Color(0xFF35589A)  // Azul Profundo
                                                )
                                            )
                                        )
                                        .clickable { currentTab = tab }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag(tab.testTag),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tab.selectedIcon,
                                        contentDescription = tab.title,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tab.title.uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                            } else {
                                // Inactive Tab: Soft Gradient (#BDF347 -> #35589A) Circle Button
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFFBDF347).copy(alpha = 0.22f),
                                                    Color(0xFF35589A).copy(alpha = 0.22f)
                                                )
                                            )
                                        )
                                        .clickable { currentTab = tab }
                                        .padding(horizontal = 11.dp, vertical = 8.dp)
                                        .testTag(tab.testTag),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        tint = if (AppTheme.isDark) Color(0xFFD6DBE7) else Color(0xFF334155),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToWorkers = { currentTab = MainTab.WORKERS }
                )
                MainTab.WORKERS -> WorkersScreen(
                    viewModel = viewModel
                )
                MainTab.BRANCH_PAYMENTS -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToWorkers = { currentTab = MainTab.WORKERS }
                )
                MainTab.REPORTS -> HistoryScreen(
                    viewModel = viewModel
                )
                MainTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToWorkers = { currentTab = MainTab.WORKERS }
                )
            }
        }
    }
}
