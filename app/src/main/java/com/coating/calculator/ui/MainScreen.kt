package com.coating.calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
fun CoatingCalculatorScreen(viewModel: CalculatorViewModel) {
    val strings = LocalizationManager.getStrings(viewModel.currentLanguage)
    var showExportDialog by remember { mutableStateOf(false) }

    val yellowBg = Color(0xFFFFF9C4) // 输入项暖黄色
    val blueBg = Color(0xFFE1F5FE)   // 只读输出项浅蓝色
    val projectResult = viewModel.getProjectCostResult()

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
                                label = { Text(strings.targetDryCoatLabel) },
                                modifier = Modifier.weight(1f).background(yellowBg),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = viewModel.coatingLayersText,
                                onValueChange = { viewModel.coatingLayersText = it },
                                label = { Text(strings.coatingLayersLabel) },
                                modifier = Modifier.weight(1f).background(yellowBg),
                                singleLine = true
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = viewModel.filmLengthText,
                                onValueChange = { viewModel.filmLengthText = it },
                                label = { Text(strings.filmLengthLabel) },
                                modifier = Modifier.weight(1f).background(yellowBg),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = viewModel.filmWidthText,
                                onValueChange = { viewModel.filmWidthText = it },
                                label = { Text(strings.filmWidthLabel) },
                                modifier = Modifier.weight(1f).background(yellowBg),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // 模块 3：计算结果汇总（浅蓝色只读区域）
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
                    label = { Text("组分名称") },
                    modifier = Modifier.weight(1.2f).background(yellowBg),
                    singleLine = true
                )
                OutlinedTextField(
                    value = component.ratioText,
                    onValueChange = { onUpdate(component.copy(ratioText = it)) },
                    label = { Text("质量比") },
                    modifier = Modifier.weight(0.9f).background(yellowBg),
                    singleLine = true
                )
                OutlinedTextField(
                    value = if (component.isAdjustableSolvent) "0%" else component.solidText,
                    onValueChange = { if (!component.isAdjustableSolvent) onUpdate(component.copy(solidText = it)) },
                    label = { Text("固含量%") },
                    modifier = Modifier.weight(0.9f).background(if (component.isAdjustableSolvent) Color(0xFFE0E0E0) else yellowBg),
                    enabled = !component.isAdjustableSolvent,
                    singleLine = true
                )
                OutlinedTextField(
                    value = component.unitPriceText,
                    onValueChange = { onUpdate(component.copy(unitPriceText = it)) },
                    label = { Text("单价/kg") },
                    modifier = Modifier.weight(0.9f).background(yellowBg),
                    singleLine = true
                )
            }
        }
    }
}
