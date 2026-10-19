package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.Emerald700
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SpenderBen
import com.example.ui.theme.SpenderEsim
import com.example.ui.theme.SpenderOrtak
import com.example.ui.viewmodel.BudgetUiState
import com.example.ui.viewmodel.formatCurrency

@Composable
fun AnalyticsScreen(
    state: BudgetUiState,
    onSetPartnerNames: (String, String) -> Unit,
    onResetDemoData: () -> Unit,
    onClearAllData: () -> Unit,
    onLeaveFamily: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPartnerNamesDialog by remember { mutableStateOf(false) }
    var partner1Input by remember { mutableStateOf(state.partner1Name) }
    var partner2Input by remember { mutableStateOf(state.partner2Name) }

    var showResetConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${state.monthTitle} ÖZET RAPORU",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Aylık bütçe ve tasarruf performansı",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        IconButton(
                            onClick = {
                                val shareText = buildString {
                                    appendLine("📊 SAĞLAM AİLE BÜTÇESİ - ${state.monthTitle}")
                                    appendLine("----------------------------------")
                                    appendLine("💰 Toplam Gelir: ${formatCurrency(state.totalIncome)}")
                                    appendLine("💸 Toplam Harcama: ${formatCurrency(state.totalExpense)}")
                                    appendLine("✨ Güncel Kalan Bakiye: ${formatCurrency(state.currentBalance)}")
                                    appendLine("📈 Tasarruf Oranı: %${state.savingsRate.toInt()}")
                                    appendLine("----------------------------------")
                                    appendLine("👥 Eşler Arası Harcama:")
                                    state.spenderDistribution.forEach {
                                        appendLine(" • ${it.spender}: ${formatCurrency(it.amount)} (%${(it.percentage * 100).toInt()})")
                                    }
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Bütçe Raporunu Paylaş"))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Raporu Paylaş",
                                tint = Emerald700
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Net Bakiye", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = formatCurrency(state.currentBalance),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (state.currentBalance >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Tasarruf Oranı", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "%${state.savingsRate.toInt()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Emerald700
                            )
                        }
                    }
                }
            }
        }

        // Detailed Spouse Comparison
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = Emerald700)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EŞLER ARASI HARCAMA DETAYI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    state.spenderDistribution.forEach { item ->
                        val color = when (item.spender) {
                            "Ortak" -> SpenderOrtak
                            state.spenderDistribution.getOrNull(0)?.spender -> SpenderBen
                            else -> SpenderEsim
                        }

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.spender,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${formatCurrency(item.amount)}  (%${(item.percentage * 100).toInt()})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }
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
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category Breakdown Full Table
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoGraph, contentDescription = null, tint = Emerald700)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KATEGORİ HARCAMA DAĞILIMI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    state.categoryProgressList.forEach { item ->
                        val catColor = Color(item.category.defaultColorHex)
                        val pctOfTotal = if (state.totalExpense > 0) ((item.spentAmount / state.totalExpense) * 100).toInt() else 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(catColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(item.category.iconName),
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.category.titleTr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Toplam harcamanın %$pctOfTotal'i",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Text(
                                text = formatCurrency(item.spentAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Family Cloud Sync & Invite Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
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
                                    .background(Emerald700.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = Emerald700, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ORTAK BÜTÇE VE EŞ SENKRONİZASYONU",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.activeFamily?.name ?: "Sağlam Aile Bütçesi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Emerald700,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IncomeGreen.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "🟢 Bulut Aktif",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Aile Paylaşım Kodu:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = state.activeFamily?.familyCode ?: "SAGLAM-26",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald700,
                                letterSpacing = 1.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Güvenlik Şifresi / PIN:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = state.activeFamily?.passcode ?: "••••",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val code = state.activeFamily?.familyCode ?: "SAGLAM-26"
                            val pass = state.activeFamily?.passcode ?: "1234"
                            val inviteText = "Sevgilim, Sağlam Aile Bütçesi ortak bütçemize katılman için kodumuz:\n" +
                                    "🔑 Aile Kodu: $code\n" +
                                    "🔒 Şifre: $pass\n" +
                                    "Uygulamayı açıp 'Bütçeye Katıl' diyerek bu bilgileri girdiğinde tüm harcamalarımız ve bakiyemiz anlık eşitlenecek!"

                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, inviteText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(intent, "Eşini Aile Bütçesine Davet Et"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eşine Aile Kodu ve Şifreyi Gönder")
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onLeaveFamily) {
                            Text("Bütçe Bağlantısını Değiştir")
                        }
                        TextButton(onClick = onSignOut) {
                            Text("Oturumu Kapat", color = ExpenseRed)
                        }
                    }
                }
            }
        }

        // Family Preferences & Data Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ManageAccounts, contentDescription = null, tint = Emerald700)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AİLE BÜTÇESİ AYARLARI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Aktif Eş İsimleri: ${state.partner1Name} & ${state.partner2Name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            partner1Input = state.partner1Name
                            partner2Input = state.partner2Name
                            showPartnerNamesDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Eş İsimlerini Güncelle")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Örnek Verileri Yeniden Yükle")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tüm İşlemleri Sıfırla", color = ExpenseRed)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }

    // Dialog: Update Partner Names
    if (showPartnerNamesDialog) {
        AlertDialog(
            onDismissRequest = { showPartnerNamesDialog = false },
            title = { Text("Eşlerin İsimleri") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Uygulama genelinde harcayan kişi olarak görüntülenecek isimler:")
                    OutlinedTextField(
                        value = partner1Input,
                        onValueChange = { partner1Input = it },
                        label = { Text("1. Eşin Adı (Örn: Emrullah)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = partner2Input,
                        onValueChange = { partner2Input = it },
                        label = { Text("2. Eşin Adı (Örn: Eşim veya Ayşe)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetPartnerNames(partner1Input, partner2Input)
                        showPartnerNamesDialog = false
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPartnerNamesDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Dialog: Confirm Reset Demo Data
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Örnek Veriler Yüklensin mi?") },
            text = { Text("Mevcut işlemler sıfırlanıp zengin örnek aile bütçesi verileri yeniden eklenecektir.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDemoData()
                        showResetConfirm = false
                    }
                ) {
                    Text("Evet, Yükle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Dialog: Confirm Clear All
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Tüm İşlemler Silinsin mi?") },
            text = { Text("Tüm gelir ve gider kayıtları kalıcı olarak temizlenecektir.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Evet, Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
