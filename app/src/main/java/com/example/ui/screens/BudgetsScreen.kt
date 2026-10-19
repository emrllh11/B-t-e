package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecurringBillEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionCategory
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.Emerald700
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldYellow
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SpenderOrtak
import com.example.ui.viewmodel.BudgetUiState
import com.example.ui.viewmodel.formatCurrency

@Composable
fun BudgetsScreen(
    state: BudgetUiState,
    onSetCategoryLimit: (TransactionCategory, Double) -> Unit,
    onAddSavingsGoal: (String, Double, Double) -> Unit,
    onAdjustGoalAmount: (SavingsGoalEntity, Double) -> Unit,
    onDeleteSavingsGoal: (SavingsGoalEntity) -> Unit,
    onAddRecurringBill: (String, Double, Int, String) -> Unit,
    onToggleBillPaid: (RecurringBillEntity) -> Unit,
    onDeleteRecurringBill: (RecurringBillEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Limitler", "Kumbaralar", "Faturalar")

    // Dialog states
    var editingCategory by remember { mutableStateOf<TransactionCategory?>(null) }
    var editLimitText by remember { mutableStateOf("") }

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var newGoalTitle by remember { mutableStateOf("") }
    var newGoalTarget by remember { mutableStateOf("") }
    var newGoalInitial by remember { mutableStateOf("") }

    var adjustingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var adjustDeltaText by remember { mutableStateOf("") }
    var isDepositMode by remember { mutableStateOf(true) }

    var showAddBillDialog by remember { mutableStateOf(false) }
    var newBillTitle by remember { mutableStateOf("") }
    var newBillAmount by remember { mutableStateOf("") }
    var newBillDay by remember { mutableStateOf("15") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("budgets_screen")
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Emerald700
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Tab 0: Kategori Limitleri
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Aylık Toplam Bütçe Limiti",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = formatCurrency(state.totalBudgetLimit),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald700
                                    )
                                }
                                Text(
                                    text = "Her kategori için bütçe sınırı belirleyin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }
                    }

                    items(state.categoryProgressList, key = { it.category.name }) { item ->
                        val catColor = Color(item.category.defaultColorHex)
                        val isOver = item.limitAmount > 0 && item.spentAmount > item.limitAmount
                        val remaining = item.limitAmount - item.spentAmount

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(catColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getCategoryIcon(item.category.iconName),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = item.category.titleTr,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (item.limitAmount > 0) {
                                                    if (isOver) "Aşım: ${formatCurrency(-remaining)}"
                                                    else "Kalan: ${formatCurrency(remaining)}"
                                                } else "Limit belirlenmedi",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            editingCategory = item.category
                                            editLimitText = if (item.limitAmount > 0) item.limitAmount.toInt().toString() else ""
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Düzenle",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Harcanan: ${formatCurrency(item.spentAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (item.limitAmount > 0) "Limit: ${formatCurrency(item.limitAmount)}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                if (item.limitAmount > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFE2E8F0))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(item.percentage.coerceIn(0f, 1f))
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isOver) ExpenseRed else catColor)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(60.dp)) }
                }
            }

            1 -> {
                // Tab 1: Ortak Birikim Kumbaraları
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Button(
                            onClick = {
                                newGoalTitle = ""
                                newGoalTarget = ""
                                newGoalInitial = ""
                                showAddGoalDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Yeni Birikim Hedefi Ekle")
                        }
                    }

                    items(state.savingsGoals, key = { it.id }) { goal ->
                        val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                        val pctInt = (progress * 100).toInt()

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(GoldYellow.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Savings,
                                                contentDescription = null,
                                                tint = GoldYellow,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = goal.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Hedef: ${formatCurrency(goal.targetAmount)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    IconButton(onClick = { onDeleteSavingsGoal(goal) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Sil",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Biriken: ${formatCurrency(goal.currentAmount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald700
                                    )
                                    Text(
                                        text = "%$pctInt Tamamlandı",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldYellow
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFFE2E8F0))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Emerald700)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            adjustingGoal = goal
                                            isDepositMode = true
                                            adjustDeltaText = ""
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("+ Para Ekle")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    TextButton(
                                        onClick = {
                                            adjustingGoal = goal
                                            isDepositMode = false
                                            adjustDeltaText = ""
                                        }
                                    ) {
                                        Text("- Çek")
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(60.dp)) }
                }
            }

            2 -> {
                // Tab 2: Sabit Faturalar & Ödemeler
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Button(
                            onClick = {
                                newBillTitle = ""
                                newBillAmount = ""
                                newBillDay = "15"
                                showAddBillDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sabit Fatura / Aidat Ekle")
                        }
                    }

                    val totalBills = state.recurringBills.sumOf { it.amount }
                    val paidBills = state.recurringBills.filter { it.isPaid }.sumOf { it.amount }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Toplam Fatura Yükü",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = formatCurrency(totalBills),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }
                                Text(
                                    text = "Ödenen: ${formatCurrency(paidBills)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IncomeGreen
                                )
                            }
                        }
                    }

                    items(state.recurringBills, key = { it.id }) { bill ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { onToggleBillPaid(bill) }) {
                                    Icon(
                                        imageVector = if (bill.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Ödendi Durumu",
                                        tint = if (bill.isPaid) IncomeGreen else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bill.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (bill.isPaid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Son Gün: Ayın ${bill.dueDay}'i",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = " • ${bill.spender}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Emerald700
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatCurrency(bill.amount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (bill.isPaid) IncomeGreen else ExpenseRed
                                    )
                                    Text(
                                        text = if (bill.isPaid) "Ödendi" else "Ödenecek",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (bill.isPaid) IncomeGreen else GoldYellow
                                    )
                                }

                                IconButton(onClick = { onDeleteRecurringBill(bill) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Sil",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(60.dp)) }
                }
            }
        }
    }

    // Dialog: Edit Category Limit
    if (editingCategory != null) {
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("${editingCategory!!.titleTr} Bütçe Limiti") },
            text = {
                Column {
                    Text("Bu kategori için aylık harcama tavanını belirleyin:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editLimitText,
                        onValueChange = { editLimitText = it },
                        label = { Text("Aylık Limit (₺)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = editLimitText.toDoubleOrNull() ?: 0.0
                        onSetCategoryLimit(editingCategory!!, limit)
                        editingCategory = null
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // Dialog: Add Savings Goal
    if (showAddGoalDialog) {
        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Yeni Birikim Hedefi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newGoalTitle,
                        onValueChange = { newGoalTitle = it },
                        label = { Text("Hedef Adı (Örn: Yaz Tatili)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newGoalTarget,
                        onValueChange = { newGoalTarget = it },
                        label = { Text("Hedef Tutar (₺)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newGoalInitial,
                        onValueChange = { newGoalInitial = it },
                        label = { Text("Mevcut Biriken Tutar (İsteğe bağlı)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = newGoalTarget.toDoubleOrNull() ?: 0.0
                        val initial = newGoalInitial.toDoubleOrNull() ?: 0.0
                        if (newGoalTitle.isNotBlank() && target > 0) {
                            onAddSavingsGoal(newGoalTitle, target, initial)
                            showAddGoalDialog = false
                        }
                    }
                ) {
                    Text("Oluştur")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Dialog: Adjust Goal Amount
    if (adjustingGoal != null) {
        AlertDialog(
            onDismissRequest = { adjustingGoal = null },
            title = { Text(if (isDepositMode) "Kumbara Para Yatır" else "Kumbaradan Para Çek") },
            text = {
                Column {
                    Text("${adjustingGoal!!.title} için tutar:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = adjustDeltaText,
                        onValueChange = { adjustDeltaText = it },
                        label = { Text("Tutar (₺)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = adjustDeltaText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val delta = if (isDepositMode) amount else -amount
                            onAdjustGoalAmount(adjustingGoal!!, delta)
                            adjustingGoal = null
                        }
                    }
                ) {
                    Text("Onayla")
                }
            },
            dismissButton = {
                TextButton(onClick = { adjustingGoal = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // Dialog: Add Recurring Bill
    if (showAddBillDialog) {
        AlertDialog(
            onDismissRequest = { showAddBillDialog = false },
            title = { Text("Yeni Fatura / Sabit Ödeme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newBillTitle,
                        onValueChange = { newBillTitle = it },
                        label = { Text("Fatura Adı (Örn: Elektrik, Kira)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newBillAmount,
                        onValueChange = { newBillAmount = it },
                        label = { Text("Tutar (₺)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newBillDay,
                        onValueChange = { newBillDay = it },
                        label = { Text("Son Ödeme Günü (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = newBillAmount.toDoubleOrNull() ?: 0.0
                        val day = newBillDay.toIntOrNull() ?: 15
                        if (newBillTitle.isNotBlank() && amount > 0) {
                            onAddRecurringBill(newBillTitle, amount, day, "Ortak")
                            showAddBillDialog = false
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBillDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
