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

data class FormattedComponentDetail(
    val slotIndex: Int,
    val name: String,
    val wetWeightPerSqmText: String, // g/m² 保留1位小数
    val costPerSqmText: String,      // 元/m² 保留2位小数
    val totalWeightKgText: String,   // kg 保留2位小数
    val totalCostText: String        // 元 保留2位小数
)

data class FormattedProjectResult(
    val workingSolutionSolid: String,
    val totalWetWeightPerSqm: String,
    val totalCostPerSqm: String,
    val processingAreaSqm: String,
    val totalRollCost: String
)

class CalculatorViewModel : ViewModel() {
    var currentLanguage by mutableStateOf(AppLanguage.ZH_CN)
        private set

    fun setLanguage(lang: AppLanguage) {
        currentLanguage = lang
    }

    val components = mutableStateListOf(
        ComponentUiState(1, true, "主剂", "100", "30", "95", false),
        ComponentUiState(2, true, "固化剂", "10", "50", "60", false),
        ComponentUiState(3, true, "纯溶剂", "50", "0", "8", true),
        ComponentUiState(4, false, "", "", "0", "", false),
        ComponentUiState(5, false, "", "", "0", "", false)
    )

    var coatingDryWeightText by mutableStateOf("25")
    var filmLengthText by mutableStateOf("1000")
    var filmWidthText by mutableStateOf("1.2")
    var coatingLayersText by mutableStateOf("1")
    var batchUnitText by mutableStateOf("kg")

    // 1.1 实验配胶 & 1.2 配胶计算 & 1.3 工作液浓度反算溶剂
    var expMainAgentText by mutableStateOf("")
    var targetTotalMassText by mutableStateOf("")
    var targetSolidContentText by mutableStateOf("") // 新增：目标工作液固含量反算（%）

    fun updateComponent(index: Int, updated: ComponentUiState) {
        if (index in components.indices) {
            components[index] = updated
        }
    }

    private fun getRawProjectResult(): ProjectCostResult {
        val inputs = components.filter { it.enabled }.map { it.toEngineInput() }
        val d = coatingDryWeightText.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val l = filmLengthText.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val w = filmWidthText.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val n = coatingLayersText.toBigDecimalOrNull() ?: BigDecimal.ONE
        return CoatingCalculatorEngine.calculateProjectCost(inputs, d, l, w, n)
    }

    fun getProjectCostResult(): FormattedProjectResult {
        val raw = getRawProjectResult()
        
        val solidPercent = raw.workingSolutionSolid.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)
        val wetWeightFormatted = raw.totalWetWeightPerSqm.setScale(1, RoundingMode.HALF_UP).toPlainString()
        val costPerSqmFormatted = raw.totalCostPerSqm.setScale(2, RoundingMode.HALF_UP).toPlainString()
        val areaFormatted = raw.processingAreaSqm.setScale(1, RoundingMode.HALF_UP).toPlainString()
        val totalRollCostFormatted = raw.totalRollCost.setScale(2, RoundingMode.HALF_UP).toPlainString()

        return FormattedProjectResult(
            workingSolutionSolid = "$solidPercent%",
            totalWetWeightPerSqm = wetWeightFormatted,
            totalCostPerSqm = costPerSqmFormatted,
            processingAreaSqm = areaFormatted,
            totalRollCost = totalRollCostFormatted
        )
    }

    fun getFormattedComponentDetails(): List<FormattedComponentDetail> {
        val raw = getRawProjectResult()
        val inputs = components.filter { it.enabled }
        if (inputs.isEmpty() || raw.workingSolutionSolid.compareTo(BigDecimal.ZERO) == 0) {
            return emptyList()
        }

        val d = coatingDryWeightText.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val l = filmLengthText.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val w = filmWidthText.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val n = coatingLayersText.toBigDecimalOrNull() ?: BigDecimal.ONE
        val area = l.multiply(w).multiply(n)

        val totalRatio = inputs.sumOf { it.toEngineInput().ratio }
        val totalWetSqm = if (raw.workingSolutionSolid.compareTo(BigDecimal.ZERO) > 0) {
            d.divide(raw.workingSolutionSolid, 6, RoundingMode.HALF_UP)
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

            val wetSqm = totalWetSqm.multiply(ratioFraction).setScale(1, RoundingMode.HALF_UP)
            val unitPrice = engineInput.unitPrice ?: BigDecimal.ZERO
            val costSqm = wetSqm.divide(BigDecimal("1000"), 6, RoundingMode.HALF_UP)
                .multiply(unitPrice)
                .setScale(2, RoundingMode.HALF_UP)

            // kg 单位严格保留 2 位小数
            val totalWeightKg = wetSqm.multiply(area)
                .divide(BigDecimal("1000"), 6, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP)

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
            val amount = ratio.multiply(scaleFactor).setScale(2, RoundingMode.HALF_UP) // kg/g显示2位
            Pair(comp.name.ifBlank { "组分${comp.slotIndex}" }, amount.toPlainString())
        }
    }

    fun calculateTargetTotalMixing(): List<Pair<String, String>> {
        val targetTotal = targetTotalMassText.toBigDecimalOrNull() ?: return emptyList()
        val inputs = components.filter { it.enabled }
        if (inputs.isEmpty()) return emptyList()

        val totalRatio = inputs.sumOf { it.toEngineInput().ratio }
        if (totalRatio.compareTo(BigDecimal.ZERO) == 0) return emptyList()

        val scaleFactor = targetTotal.divide(totalRatio, 6, RoundingMode.HALF_UP)
        return inputs.map { comp ->
            val ratio = comp.toEngineInput().ratio
            val amount = ratio.multiply(scaleFactor).setScale(2, RoundingMode.HALF_UP)
            Pair(comp.name.ifBlank { "组分${comp.slotIndex}" }, amount.toPlainString())
        }
    }

    /**
     * 根据目标工作液固含量反算可调溶剂的配比及调整方案
     */
    fun calculateSolventAdjustmentByTargetSolid(): String {
        val targetSolidPercent = targetSolidContentText.toBigDecimalOrNull() ?: return ""
        val targetSolid = targetSolidPercent.divide(BigDecimal("100"), 6, RoundingMode.HALF_UP)
        if (targetSolid.compareTo(BigDecimal.ZERO) <= 0 || targetSolid.compareTo(BigDecimal.ONE) >= 0) {
            return "请输入有效的固含量百分比 (0~100)"
        }

        // 找到勾选了“可调溶剂”的组分
        val solventComp = components.firstOrNull { it.enabled && it.isAdjustableSolvent }
            ?: return "请先在下方勾选一个组分作为“可调溶剂”"

        // 计算非溶剂组分的总固体质量或基准量
        val nonSolventComps = components.filter { it.enabled && !it.isAdjustableSolvent }
        if (nonSolventComps.isEmpty()) return "没有找到基础配方组分"

        // 计算非溶剂组分的总配比质量
        val nonSolventRatioSum = nonSolventComps.sumOf { it.ratioText.toBigDecimalOrNull() ?: BigDecimal.ZERO }
        // 计算非溶剂组分携带的干固分总量折算基数 (Ratio * SolidContent)
        val totalSolidMassBase = nonSolventComps.sumOf { c ->
            val r = c.ratioText.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val s = (c.solidText.toBigDecimalOrNull() ?: BigDecimal.ZERO).divide(BigDecimal("100"), 6, RoundingMode.HALF_UP)
            r.multiply(s)
        }

        if (totalSolidMassBase.compareTo(BigDecimal.ZERO) == 0) return "基础固分质量为0，无法反算"

        // 公式：目标总配比 = 总固分质量基数 / 目标固含量
        val requiredTotalRatio = totalSolidMassBase.divide(targetSolid, 6, RoundingMode.HALF_UP)
        // 所需的可调溶剂配比 = 目标总配比 - 非溶剂组分总配比
        val requiredSolventRatio = requiredTotalRatio.subtract(nonSolventRatioSum).setScale(2, RoundingMode.HALF_UP)

        if (requiredSolventRatio.compareTo(BigDecimal.ZERO) < 0) {
            return "目标固含量过高，现有主剂固含已超过该数值，无法通过增加溶剂实现"
        }

        return "反算建议：【${solventComp.name.ifBlank { "组分${solventComp.slotIndex}" }}】配比应调整为 ${requiredSolventRatio.toPlainString()}"
    }
}
