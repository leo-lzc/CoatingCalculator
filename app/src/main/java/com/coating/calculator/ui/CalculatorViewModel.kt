package com.coating.calculator.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.coating.calculator.engine.*
import com.coating.calculator.ui.i18n.AppLanguage
import java.math.BigDecimal
import java.math.RoundingMode

data class ComponentUiState(
    val slotIndex: Int,
    val enabled: Boolean,
    val name: String,
    val ratioText: String,
    val solidText: String,
    val unitPriceText: String,
    val isAdjustableSolvent: Boolean
) {
    fun toEngineInput() = ComponentInput(
        slotIndex = slotIndex,
        name = name,
        ratio = ratioText.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        solidContent = (solidText.toBigDecimalOrNull() ?: BigDecimal.ZERO).divide(BigDecimal(100)),
        unitPrice = unitPriceText.toBigDecimalOrNull(),
        isAdjustableSolvent = isAdjustableSolvent
    )
}

// 格式化后的各项明细数据结构（严格控制小数位数）
data class FormattedComponentDetail(
    val slotIndex: Int,
    val name: String,
    val wetWeightPerSqmText: String, // g/m² 保留1位小数
    val costPerSqmText: String,      // 元/m² 保留2位小数
    val totalWeightKgText: String,   // kg 保留1位小数
    val totalCostText: String        // 元 保留2位小数
)

class CalculatorViewModel : ViewModel() {
    var currentLanguage by mutableStateOf(AppLanguage.ZH_CN)
        private set

    fun setLanguage(lang: AppLanguage) {
        currentLanguage = lang
    }

    // 最多 5 个槽位初始化
    val components = mutableStateListOf(
        ComponentUiState(1, true, "主剂", "100", "30", "95", false),
        ComponentUiState(2, true, "固化剂", "10", "50", "60", false),
        ComponentUiState(3, true, "纯溶剂", "50", "0", "8", true), // 默认勾选可调溶剂
        ComponentUiState(4, false, "", "", "0", "", false),
        ComponentUiState(5, false, "", "", "0", "", false)
    )

    var coatingDryWeightText by mutableStateOf("25")
    var filmLengthText by mutableStateOf("1000")
    var filmWidthText by mutableStateOf("1.2")
    var coatingLayersText by mutableStateOf("1")
    var batchUnitText by mutableStateOf("kg")

    // 1.1 & 1.2 实验配胶与配胶计算输入状态
    var expMainAgentText by mutableStateOf("")     // 实验配胶：输入主剂添加量
    var targetTotalMassText by mutableStateOf("")  // 配胶计算：输入工作液目标总量

    fun updateComponent(index: Int, updated: ComponentUiState) {
        if (index in components.indices) {
            components[index] = updated
        }
    }

    fun getProjectCostResult(): ProjectCostResult {
        val inputs = components.filter { it.enabled }.map { it.toEngineInput() }
        val d = coatingDryWeightText.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val l = filmLengthText.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val w = filmWidthText.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val n = coatingLayersText.toBigDecimalOrNull() ?: BigDecimal.ONE
        return CoatingCalculatorEngine.calculateProjectCost(inputs, d, l, w, n)
    }

    /**
     * 获取各组分明细数据（每平米湿重/总重量保留1位小数，每平米成本/总成本保留2位小数）
     */
    fun getFormattedComponentDetails(): List<FormattedComponentDetail> {
        val result = getProjectCostResult()
        val inputs = components.filter { it.enabled }
        if (inputs.isEmpty() || result.workingSolutionSolid.compareTo(BigDecimal.ZERO) == 0) {
            return emptyList()
        }

        val d = coatingDryWeightText.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val l = filmLengthText.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val w = filmWidthText.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val n = coatingLayersText.toBigDecimalOrNull() ?: BigDecimal.ONE
        val area = l.multiply(w).multiply(n) // 总加工面积 m²

        val totalRatio = inputs.sumOf { it.toEngineInput().ratio }
        val totalWetSqm = if (result.workingSolutionSolid.compareTo(BigDecimal.ZERO) > 0) {
            d.divide(result.workingSolutionSolid, 6, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return inputs.map { comp ->
            val engineInput = comp.toEngineInput()
            val ratioFraction = if (totalRatio.compareTo(BigDecimal.ZERO) > 0) {
                engineInput.ratio.divide(totalRatio, 6, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            // 每平米湿重 (g/m²) -> 严格保留 1 位小数
            val wetSqm = totalWetSqm.multiply(ratioFraction).setScale(1, RoundingMode.HALF_UP)
            
            // 每平米成本 (元/m²) = (湿重g/m² / 1000) * 单价元/kg -> 严格保留 2 位小数
            val unitPrice = engineInput.unitPrice ?: BigDecimal.ZERO
            val costSqm = wetSqm.divide(BigDecimal("1000"), 6, RoundingMode.HALF_UP)
                .multiply(unitPrice)
                .setScale(2, RoundingMode.HALF_UP)

            // 整个项目组分总重量 (kg) = (湿重g/m² * 面积m²) / 1000 -> 严格保留 1 位小数
            val totalWeightKg = wetSqm.multiply(area)
                .divide(BigDecimal("1000"), 6, RoundingMode.HALF_UP)
                .setScale(1, RoundingMode.HALF_UP)

            // 整个项目组分总成本 (元) = 总重量kg * 单价元/kg -> 严格保留 2 位小数
            val totalCost = totalWeightKg.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)

            FormattedComponentDetail(
                slotIndex = comp.slotIndex,
                name = comp.name.ifBlank { "组分${comp.slotIndex}" },
                wetWeightPerSqmText = wetSqm.toPlainString(),
                costPerSqmText = costSqm.toPlainString(),
                totalWeightKgText = totalWeightKg.toPlainString(),
                totalCostText = totalCost.toPlainString()
            )
        }
    }

    /**
     * 1.1 实验配胶：输入主剂添加量，计算其他组分添加量（结果保留 1 位小数）
     */
    fun calculateExperimentalMixing(): List<Pair<String, String>> {
        val mainAmount = expMainAgentText.toBigDecimalOrNull() ?: return emptyList()
        val inputs = components.filter { it.enabled }
        if (inputs.isEmpty()) return emptyList()

        val mainComp = inputs.firstOrNull() ?: return emptyList()
        val mainRatio = mainComp.toEngineInput().ratio
        if (mainRatio.compareTo(BigDecimal.ZERO) == 0) return emptyList()

        val scaleFactor = mainAmount.divide(mainRatio, 6, RoundingMode.HALF_UP)
        return inputs.map { comp ->
            val ratio = comp.toEngineInput().ratio
            val amount = ratio.multiply(scaleFactor).setScale(1, RoundingMode.HALF_UP)
            Pair(comp.name.ifBlank { "组分${comp.slotIndex}" }, amount.toPlainString())
        }
    }

    /**
     * 1.2 配胶计算：输入工作液目标总量，计算各组分添加量（结果保留 1 位小数）
     */
    fun calculateTargetTotalMixing(): List<Pair<String, String>> {
        val targetTotal = targetTotalMassText.toBigDecimalOrNull() ?: return emptyList()
        val inputs = components.filter { it.enabled }
        if (inputs.isEmpty()) return emptyList()

        val totalRatio = inputs.sumOf { it.toEngineInput().ratio }
        if (totalRatio.compareTo(BigDecimal.ZERO) == 0) return emptyList()

        val scaleFactor = targetTotal.divide(totalRatio, 6, RoundingMode.HALF_UP)
        return inputs.map { comp ->
            val ratio = comp.toEngineInput().ratio
            val amount = ratio.multiply(scaleFactor).setScale(1, RoundingMode.HALF_UP)
            Pair(comp.name.ifBlank { "组分${comp.slotIndex}" }, amount.toPlainString())
        }
    }
}
