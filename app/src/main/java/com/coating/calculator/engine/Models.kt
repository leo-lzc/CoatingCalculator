package com.coating.calculator.engine

import java.math.BigDecimal

data class ComponentInput(
    val slotIndex: Int,
    val name: String,
    val ratio: BigDecimal,
    val solidContent: BigDecimal, // 0~1 之间，如 0.30 代表 30%
    val unitPrice: BigDecimal?,   // 选填单价 (元/kg)
    val isAdjustableSolvent: Boolean // 是否勾选“可调纯溶剂”
) {
    fun isCompletelyEmpty(): Boolean {
        return name.isBlank() && ratio.compareTo(BigDecimal.ZERO) == 0 && unitPrice == null
    }
}

enum class CalculationStatus {
    COMPLETED,       // 计算完整
    PARTIAL_COST,    // 部分计算（部分单价缺失）
    INSUFFICIENT_DATA // 数据不足（如全为0或未配置有效组分）
}

data class ProjectCostResult(
    val workingSolutionSolid: BigDecimal,     // 工作液浓度 S = K / R
    val totalWetWeightPerSqm: BigDecimal,     // 每平米总湿重 (g/m²)
    val totalCostPerSqm: BigDecimal,          // 每平米总成本 (元/m²)
    val processingAreaSqm: BigDecimal,        // 加工总面积 (m²)
    val totalRollCost: BigDecimal,            // 整卷总成本 (元)
    val componentDetails: List<ComponentCostDetail>,
    val status: CalculationStatus,
    val isPerSqmCostIncomplete: Boolean,
    val isRollCostIncomplete: Boolean
)

data class ComponentCostDetail(
    val slotIndex: Int,
    val name: String,
    val wetWeightPerSqm: BigDecimal,          // 每平米湿重 (g/m²)
    val costPerSqm: BigDecimal,               // 每平米成本 (元/m²)
    val rollConsumptionKg: BigDecimal,        // 整卷消耗量 (kg)
    val rollCost: BigDecimal,                 // 整卷成本 (元)
    val isPriceMissing: Boolean
)

data class BatchCalculationResult(
    val items: List<BatchItemResult>,
    val totalAmount: BigDecimal,
    val status: CalculationStatus
)

data class BatchItemResult(
    val slotIndex: Int,
    val name: String,
    val amount: BigDecimal
)
