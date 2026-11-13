package com.coating.calculator.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coating.calculator.export.PdfExportAndShareHelper
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: CalculatorViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val projectResult = viewModel.getProjectCostResult()
    val componentDetails = viewModel.getFormattedComponentDetails()
    val expMixingResults = viewModel.calculateExperimentalMixing()
    val targetMixingResults = viewModel.calculateTargetTotalMixing()

    // SAF 存储访问框架：用于选择本地文件夹保存 PDF
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri) as? FileOutputStream
                if (outputStream != null) {
                    PdfExportAndShareHelper.writePdfToOutputStream(context, viewModel, outputStream)
                    Toast.makeText(context, "PDF 报告已成功导出到本地", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "保存失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "薄膜涂布配方与成本计算器",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================= 1. 配方组分设置 =================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. 配方组分设置",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    viewModel.components.forEachIndexed { index, comp ->
                        ComponentInputRow(
                            comp = comp,
                            onUpdate = { updated -> viewModel.updateComponent(index, updated) }
                        )
                        if (index < viewModel.components.size - 1) {
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // ================= 2. 涂布与规格参数 =================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "2. 涂布工艺与基材规格",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = viewModel.coatingDryWeightText,
                            onValueChange = { viewModel.coatingDryWeightText = it },
                            label = { Text("目标干涂重 (g/m²)", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.coatingLayersText,
                            onValueChange = { viewModel.coatingLayersText = it },
                            label = { Text("涂布层数", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = viewModel.filmLengthText,
                            onValueChange = { viewModel.filmLengthText = it },
                            label = { Text("薄膜长度 (m)", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.filmWidthText,
                            onValueChange = { viewModel.filmWidthText = it },
                            label = { Text("薄膜幅宽 (m)", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            // ================= 3. 配胶反算工具 =================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "配胶反算助手",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF8D6E63)
                    )

                    // 1.1 实验配胶：按主剂添加量
                    OutlinedTextField(
                        value = viewModel.expMainAgentText,
                        onValueChange = { viewModel.expMainAgentText = it },
                        label = { Text("实验配胶：输入主剂添加量 (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (expMixingResults.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            expMixingResults.forEach { (name, amount) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = name, fontSize = 13.sp)
                                    Text(text = "$amount g", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // 1.2 配胶计算：按工作液目标总量
                    OutlinedTextField(
                        value = viewModel.targetTotalMassText,
                        onValueChange = { viewModel.targetTotalMassText = it },
                        label = { Text("配胶计算：输入工作液目标总量 (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (targetMixingResults.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            targetMixingResults.forEach { (name, amount) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = name, fontSize = 13.sp)
                                    Text(text = "$amount g", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ================= 4. 计算结果与成本汇总 =================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "3. 计算结果与成本汇总",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1565C0)
                    )

                    SummaryInfoRow("工作液固含量：", projectResult.workingSolutionSolid)
                    SummaryInfoRow("每平米总湿重：", "${projectResult.totalWetWeightPerSqm} g/m²")
                    SummaryInfoRow("每平米总成本：", "${projectResult.totalCostPerSqm} 元/m²")
                    SummaryInfoRow("加工面积：", "${projectResult.processingAreaSqm} m²")
                    SummaryInfoRow("整卷总成本：", "${projectResult.totalRollCost} 元", isHighlight = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "各组分明细汇总（湿重保留1位小数，成本保留2位小数）：",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1976D2)
                    )

                    componentDetails.forEach { detail ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "#${detail.slotIndex} ${detail.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("每平米湿重: ${detail.wetWeightPerSqmText} g/m²", fontSize = 12.sp)
                                    Text("每平米成本: ${detail.costPerSqmText} 元", fontSize = 12.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("项目总重量: ${detail.totalWeightKgText} kg", fontSize = 12.sp)
                                    Text("项目总成本: ${detail.totalCostText} 元", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ================= 5. 导出与分享操作区 =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 按钮 1：保存到本地（SAF 系统保存面板）
                OutlinedButton(
                    onClick = {
                        savePdfLauncher.launch("CoatingCalculator_Report.pdf")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "导出到本地",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("导出到本地", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                // 按钮 2：直接调用系统分享面板（如发送给微信/钉钉/邮箱）
                Button(
                    onClick = {
                        PdfExportAndShareHelper.generateAndSharePdf(context, viewModel)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "直接分享",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("直接分享", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryInfoRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isHighlight) 16.sp else 14.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = Color.DarkGray
        )
        Text(
            text = value,
            fontSize = if (isHighlight) 17.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) Color(0xFFD32F2F) else Color.Black
        )
    }
}

@Composable
private fun ComponentInputRow(
    comp: ComponentUiState,
    onUpdate: (ComponentUiState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = comp.enabled,
                onCheckedChange = { onUpdate(comp.copy(enabled = it)) }
            )
            OutlinedTextField(
                value = comp.name,
                onValueChange = { onUpdate(comp.copy(name = it)) },
                label = { Text("组分#${comp.slotIndex}名称", fontSize = 12.sp) },
                enabled = comp.enabled,
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = comp.isAdjustableSolvent,
                    onCheckedChange = { onUpdate(comp.copy(isAdjustableSolvent = it)) },
                    enabled = comp.enabled
                )
                Text("可调溶剂", fontSize = 12.sp)
            }
        }

        if (comp.enabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = comp.ratioText,
                    onValueChange = { onUpdate(comp.copy(ratioText = it)) },
                    label = { Text("配比", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = comp.solidText,
                    onValueChange = { onUpdate(comp.copy(solidText = it)) },
                    label = { Text("固含(%)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = comp.unitPriceText,
                    onValueChange = { onUpdate(comp.copy(unitPriceText = it)) },
                    label = { Text("单价(元)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}
