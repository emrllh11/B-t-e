package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddTransactionSheet
import com.example.ui.components.ReceiptScanSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.Emerald700
import com.example.ui.theme.SaglamAileButcesiTheme
import com.example.ui.viewmodel.BudgetViewModel
import com.example.util.configureAppCheck
import kotlinx.coroutines.launch

enum class AppTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Genel Bakış", Icons.Default.Dashboard),
    TRANSACTIONS("İşlemler", Icons.Default.ReceiptLong),
    BUDGETS("Bütçe & Hedef", Icons.Default.Savings),
    ANALYTICS("Raporlar", Icons.Default.AutoGraph)
}

class MainActivity : ComponentActivity() {

    private val viewModel: BudgetViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure App Check before UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            SaglamAileButcesiTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
                val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scanSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                var showAddSheet by remember { mutableStateOf(false) }
                var showScanSheet by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                // Handle back press: if not in Dashboard, return to Dashboard
                BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
                    currentTab = AppTab.DASHBOARD
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Emerald700.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = Emerald700,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SAĞLAM AİLE BÜTÇESİ",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { showScanSheet = true },
                                    modifier = Modifier.testTag("topbar_ai_scan_receipt")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Yapay Zeka ile Fiş Tara",
                                        tint = Emerald700
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            AppTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Emerald700,
                                        selectedTextColor = Emerald700,
                                        indicatorColor = Emerald700.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = { showAddSheet = true },
                            containerColor = Emerald700,
                            contentColor = Color.White,
                            icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                            text = { Text("İşlem Ekle", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("fab_add_transaction")
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            AppTab.DASHBOARD -> {
                                DashboardScreen(
                                    state = uiState,
                                    onPreviousMonth = { viewModel.selectPreviousMonth() },
                                    onNextMonth = { viewModel.selectNextMonth() },
                                    onCurrentMonth = { viewModel.selectCurrentMonth() },
                                    onSeeAllTransactions = { currentTab = AppTab.TRANSACTIONS },
                                    onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                    onScanReceipt = { showScanSheet = true }
                                )
                            }

                            AppTab.TRANSACTIONS -> {
                                TransactionsScreen(
                                    state = uiState,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onTypeFilterChange = { viewModel.setTypeFilter(it) },
                                    onSpenderFilterChange = { viewModel.setSpenderFilter(it) },
                                    onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                                )
                            }

                            AppTab.BUDGETS -> {
                                BudgetsScreen(
                                    state = uiState,
                                    onSetCategoryLimit = { cat, limit -> viewModel.setCategoryLimit(cat, limit) },
                                    onAddSavingsGoal = { title, target, initial -> viewModel.addSavingsGoal(title, target, initial) },
                                    onAdjustGoalAmount = { goal, delta -> viewModel.adjustGoalAmount(goal, delta) },
                                    onDeleteSavingsGoal = { viewModel.deleteSavingsGoal(it) },
                                    onAddRecurringBill = { title, amount, day, spender -> viewModel.addRecurringBill(title, amount, day, spender) },
                                    onToggleBillPaid = { viewModel.toggleBillPaid(it) },
                                    onDeleteRecurringBill = { viewModel.deleteRecurringBill(it) }
                                )
                            }

                            AppTab.ANALYTICS -> {
                                AnalyticsScreen(
                                    state = uiState,
                                    onSetPartnerNames = { name1, name2 -> viewModel.setPartnerNames(name1, name2) },
                                    onResetDemoData = { viewModel.resetToDemoData() },
                                    onClearAllData = { viewModel.clearAllTransactions() }
                                )
                            }
                        }
                    }

                    // Add Transaction Bottom Sheet
                    if (showAddSheet) {
                        AddTransactionSheet(
                            partner1Name = uiState.partner1Name,
                            partner2Name = uiState.partner2Name,
                            sheetState = addSheetState,
                            onDismiss = {
                                scope.launch { addSheetState.hide() }.invokeOnCompletion {
                                    showAddSheet = false
                                }
                            },
                            onScanReceipt = {
                                scope.launch { addSheetState.hide() }.invokeOnCompletion {
                                    showAddSheet = false
                                    showScanSheet = true
                                }
                            },
                            onSave = { title, amount, type, category, spender, method, note ->
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    category = category,
                                    spender = spender,
                                    paymentMethod = method,
                                    note = note
                                )
                                scope.launch { addSheetState.hide() }.invokeOnCompletion {
                                    showAddSheet = false
                                }
                            }
                        )
                    }

                    // AI Receipt Scan Bottom Sheet
                    if (showScanSheet) {
                        ReceiptScanSheet(
                            partner1Name = uiState.partner1Name,
                            partner2Name = uiState.partner2Name,
                            sheetState = scanSheetState,
                            onDismiss = {
                                scope.launch { scanSheetState.hide() }.invokeOnCompletion {
                                    showScanSheet = false
                                }
                            },
                            onSaveTransaction = { title, amount, type, category, spender, method, note ->
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    category = category,
                                    spender = spender,
                                    paymentMethod = method,
                                    note = note
                                )
                                scope.launch { scanSheetState.hide() }.invokeOnCompletion {
                                    showScanSheet = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
