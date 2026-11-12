package com.coating.calculator.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.coating.calculator.engine.*
import com.coating.calculator.ui.i18n.AppLanguage
import java.math.BigDecimal

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

class CalculatorViewModel : ViewModel() {
    var currentLanguage by mutableStateOf(AppLanguage.ZH_CN)
        private set

    fun setLanguage(lang: AppLanguage) {
        currentLanguage = lang
    }

    // 最多 5 个槽位初始化
    val components = mutableStateListOf(
        ComponentUiState(1, true, "主剂", "100", "30", "", false),
        ComponentUiState(2, true, "固化剂", "10", "50", "", false),
        ComponentUiState(3, true, "纯溶剂", "50", "10", "", true), // 默认勾选可调溶剂
        ComponentUiState(4, false, "", "", "0", "", false),
        ComponentUiState(5, false, "", "", "0", "", false)
    )

    var coatingDryWeightText by mutableStateOf("5.0")
    var filmLengthText by mutableStateOf("1000")
    var filmWidthText by mutableStateOf("1.2")
    var coatingLayersText by mutableStateOf("1")
    var batchUnitText by mutableStateOf("kg")

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

    fun getBatchResult(): BatchCalculationResult? = null // 可按需扩展配胶反算
}
