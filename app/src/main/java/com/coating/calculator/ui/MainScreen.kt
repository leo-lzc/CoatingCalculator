package com.coating.calculator.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
    val solventAdvice = viewModel.calculateSolventAdjustmentByTargetSolid()

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
                        text = "CoatCalc 薄膜涂布配方计算",
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    modifier = Modifier.padding(14.dp),
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
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
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
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "2. 涂布工艺与基材规格",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = viewModel.coatingDryWeightText,
                            onValueChange = { viewModel.coatingDryWeightText = it },
                            label = { Text("目标干涂重 (g/m²)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.coatingLayersText,
                            onValueChange = { viewModel.coatingLayersText = it },
                            label = { Text("涂布层数", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = viewModel.filmLengthText,
                            onValueChange = { viewModel.filmLengthText = it },
                            label = { Text("薄膜长度 (m)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.filmWidthText,
                            onValueChange = { viewModel.filmWidthText = it },
                            label = { Text("薄膜幅宽 (m)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            // ================= 3. 配胶与浓度反算助手 =================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "配胶与浓度反算助手",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF8D6E63)
                    )

                    OutlinedTextField(
                        value = viewModel.expMainAgentText,
                        onValueChange = { viewModel.expMainAgentText = it },
                        label = { Text("实验配胶：输入主剂添加量 (kg/g)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (expMixingResults.isNotEmpty()) {
                        ResultDisplayBox(expMixingResults)
                    }

                    OutlinedTextField(
                        value = viewModel.targetTotalMassText,
                        onValueChange = { viewModel.targetTotalMassText = it },
                        label = { Text("配胶计算：输入工作液目标总量 (kg/g)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (targetMixingResults.isNotEmpty()) {
                        ResultDisplayBox(targetMixingResults)
                    }

                    OutlinedTextField(
                        value = viewModel.targetSolidContentText,
                        onValueChange = { viewModel.targetSolidContentText = it },
                        label = { Text("反算溶剂：输入目标工作液固含量 (%)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (solventAdvice.isNotBlank()) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = solventAdvice,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD32F2F),
                                modifier = Modifier.padding(10.dp)
                            )
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        text = "各组分明细汇总（小数保留仅为显示值，后台计算精确度为6位）：",
                        fontSize = 12.sp,
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
                OutlinedButton(
                    onClick = {
                        savePdfLauncher.launch("CoatCalc_Report.pdf")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "导出到本地",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("导出到本地", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

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
private fun ResultDisplayBox(results: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        results.forEach { (name, amount) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = name, fontSize = 13.sp)
                Text(text = "$amount kg/g", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
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
                onCheckedChange = { onUpdate(comp.copy(enabled = it)) },
                modifier = Modifier.size(24.dp)
            )
            OutlinedTextField(
                value = comp.name,
                onValueChange = { onUpdate(comp.copy(name = it)) },
                label = { Text("组分#${comp.slotIndex}名称", fontSize = 11.sp) },
                enabled = comp.enabled,
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = comp.isAdjustableSolvent,
                    onCheckedChange = { onUpdate(comp.copy(isAdjustableSolvent = it)) },
                    enabled = comp.enabled,
                    modifier = Modifier.size(24.dp)
                )
                Text("可调溶剂", fontSize = 11.sp)
            }
        }

        if (comp.enabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = comp.ratioText,
                    onValueChange = { onUpdate(comp.copy(ratioText = it)) },
                    label = { Text("配比", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = comp.solidText,
                    onValueChange = { onUpdate(comp.copy(solidText = it)) },
                    label = { Text("固含(%)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = comp.unitPriceText,
                    onValueChange = { onUpdate(comp.copy(unitPriceText = it)) },
                    label = { Text("单价(元)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}
