package com.coating.calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coating.calculator.ui.i18n.LocalizationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: CalculatorViewModel) {
    val strings = LocalizationManager.getStrings(viewModel.currentLanguage)
    var showExportDialog by remember { mutableStateOf(false) }

    val yellowBg = Color(0xFFFFF9C4) // 输入项暖黄色
    val blueBg = Color(0xFFE1F5FE)   // 只读输出项浅蓝色
    val projectResult = viewModel.getProjectCostResult()
    val formattedDetails = viewModel.getFormattedComponentDetails()

    // 动态计算反算结果
    val experimentalMixingList = remember(viewModel.expMainAgentText, viewModel.components) {
        viewModel.calculateExperimentalMixing()
    }
    val targetTotalMixingList = remember(viewModel.targetTotalMassText, viewModel.components) {
        viewModel.calculateTargetTotalMixing()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.appTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    LanguageSelectorMenu(
                        currentLanguage = viewModel.currentLanguage,
                        onLanguageSelected = { viewModel.setLanguage(it) }
                    )
                    TextButton(onClick = { showExportDialog = true }) {
                        Text(strings.exportShareBtn, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 图例说明
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.legendYellow, fontSize = 11.sp, color = Color(0xFF7F6000), fontWeight = FontWeight.Bold)
                    Text(strings.legendBlue, fontSize = 11.sp, color = Color(0xFF01579B), fontWeight = FontWeight.Bold)
                }
            }

            // 模块 1：基础配方参数（最多 5 个槽位）
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.tabBasicParams, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(strings.formulaHint, fontSize = 11.sp, color = Color.Gray)

                        viewModel.components.forEachIndexed { index, comp ->
                            ComponentRow(
                                component = comp,
                                yellowBg = yellowBg,
                                onUpdate = { updated -> viewModel.updateComponent(index, updated) }
                            )
                        }
                    }
                }
            }

            // 模块 2：项目加工与尺寸参数
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.tabProjectCost, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = viewModel.coatingDryWeightText,
                                onValueChange = { viewModel.coatingDryWeightText = it },
                                label = { Text(strings.targetDryCoatLabel, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).background(yellowBg),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = viewModel.coatingLayersText,
                                onValueChange = { viewModel.coatingLayersText = it },
                                label = { Text(strings.coatingLayersLabel, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).background(yellowBg),
                                singleLine = true
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = viewModel.filmLengthText,
                                onValueChange = { viewModel.filmLengthText = it },
                                label = { Text(strings.filmLengthLabel, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).background(yellowBg),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = viewModel.filmWidthText,
                                onValueChange = { viewModel.filmWidthText = it },
                                label = { Text(strings.filmWidthLabel, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).background(yellowBg),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // 模块 3：配胶与反算功能专区 (1.1 实验配胶 & 1.2 配胶计算)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("2. 配胶计算与反算专区", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF4A148C))

                        // 1.1 实验配胶
                        OutlinedTextField(
                            value = viewModel.expMainAgentText,
                            onValueChange = { viewModel.expMainAgentText = it },
                            label = { Text("实验配胶：输入主剂添加量 (g)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().background(yellowBg),
                            singleLine = true
                        )
                        if (experimentalMixingList.isNotEmpty()) {
                            Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("实验配胶结果（工作液总量与各组分，保留1位小数）：", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                                    experimentalMixingList.forEach { (name, amount) ->
                                        Text("• $name: $amount g", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // 1.2 配胶计算
                        OutlinedTextField(
                            value = viewModel.targetTotalMassText,
                            onValueChange = { viewModel.targetTotalMassText = it },
                            label = { Text("配胶计算：输入工作液目标总量 (g)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().background(yellowBg),
                            singleLine = true
                        )
                        if (targetTotalMixingList.isNotEmpty()) {
                            Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("目标总量称取明细（保留1位小数）：", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                                    targetTotalMixingList.forEach { (name, amount) ->
                                        Text("• $name: $amount g", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 模块 4：计算结果与成本汇总（浅蓝色只读区域，含明细）
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = blueBg)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(strings.summaryOutputHeader, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF01579B))
                        Text("${strings.workingSolutionSolidLevel}: ${(projectResult.workingSolutionSolid.toFloat() * 100).toInt()}%")
                        Text("${strings.totalWetWeightLabel}: ${projectResult.totalWetWeightPerSqm} g/m²")
                        Text("${strings.totalCostPerSqmLabel}: ${projectResult.totalCostPerSqm} 元/m²")
                        Text("${strings.processingAreaLabel}: ${projectResult.processingAreaSqm} m²")
                        Text("${strings.totalRollCostLabel}: ${projectResult.totalRollCost} 元")

                        Divider(color = Color(0xFFB0BEC5), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                        Text("各组分明细汇总（湿重保留1位小数，成本保留2位小数）：", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF01579B))

                        if (formattedDetails.isEmpty()) {
                            Text("暂无有效组分数据", fontSize = 11.sp, color = Color.Gray)
                        } else {
                            formattedDetails.forEach { detail ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("#${detail.slotIndex} ${detail.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("每平米湿重: ${detail.wetWeightPerSqmText} g/m²", fontSize = 11.sp)
                                            Text("每平米成本: ${detail.costPerSqmText} 元", fontSize = 11.sp)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("项目总重量: ${detail.totalWeightKgText} kg", fontSize = 11.sp)
                                            Text("项目总成本: ${detail.totalCostText} 元", fontSize = 11.sp)
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

    if (showExportDialog) {
        ExportAndSharePdfDialog(
            viewModel = viewModel,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun ComponentRow(
    component: ComponentUiState,
    yellowBg: Color,
    onUpdate: (ComponentUiState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (component.enabled) Color.White else Color(0xFFF1F1F1))
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = component.enabled,
                        onCheckedChange = { onUpdate(component.copy(enabled = it)) }
                    )
                    Text(" #${component.slotIndex} ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                // 可调纯溶剂勾选框
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = component.isAdjustableSolvent,
                        onCheckedChange = { onUpdate(component.copy(isAdjustableSolvent = it, solidText = if (it) "0" else component.solidText)) }
                    )
                    Text("可调纯溶剂", fontSize = 12.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = component.name,
                    onValueChange = { onUpdate(component.copy(name = it)) },
                    label = { Text("组分名称", fontSize = 11.sp) },
                    modifier = Modifier.weight(1.1f).background(yellowBg),
                    singleLine = true
                )
                OutlinedTextField(
                    value = component.ratioText,
                    onValueChange = { onUpdate(component.copy(ratioText = it)) },
                    label = { Text("质量比", fontSize = 11.sp) },
                    modifier = Modifier.weight(0.9f).background(yellowBg),
                    singleLine = true
                )
                OutlinedTextField(
                    value = if (component.isAdjustableSolvent) "0" else component.solidText,
                    onValueChange = { if (!component.isAdjustableSolvent) onUpdate(component.copy(solidText = it)) },
                    label = { Text("固含量%", fontSize = 11.sp) },
                    modifier = Modifier.weight(0.9f).background(if (component.isAdjustableSolvent) Color(0xFFE0E0E0) else yellowBg),
                    enabled = !component.isAdjustableSolvent,
                    singleLine = true
                )
                OutlinedTextField(
                    value = component.unitPriceText,
                    onValueChange = { onUpdate(component.copy(unitPriceText = it)) },
                    label = { Text("单价/kg", fontSize = 11.sp) },
                    modifier = Modifier.weight(0.9f).background(yellowBg),
                    singleLine = true
                )
            }
        }
    }
}
