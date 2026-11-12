package com.coating.calculator.engine

import java.math.BigDecimal
import java.math.RoundingMode

object CoatingCalculatorEngine {
    private val SCALE_INTERNAL = 6
    private val ROUNDING_INTERNAL = RoundingMode.HALF_UP

    /**
     * 计算工作液浓度与项目加工成本
     */
    fun calculateProjectCost(
        components: List<ComponentInput>,
        dryCoatingWeightD: BigDecimal, // g/m²
        lengthM: BigDecimal,           // 米
        widthM: BigDecimal,            // 米
        layersN: BigDecimal            // 层数
    ): ProjectCostResult {
        val activeComps = components.filter { !it.isCompletelyEmpty() && it.ratio.compareTo(BigDecimal.ZERO) > 0 }
        if (activeComps.isEmpty() || dryCoatingWeightD.compareTo(BigDecimal.ZERO) <= 0) {
            return emptyProjectResult()
        }

        // 1. 计算总配比 R = Σ(r_i)，总干重 K = Σ(r_i * s_i)
        var totalRatio = BigDecimal.ZERO
        var totalSolute = BigDecimal.ZERO

        for (comp in activeComps) {
            totalRatio = totalRatio.add(comp.ratio)
            val solute = comp.ratio.multiply(comp.effectiveSolidContent)
            totalSolute = totalSolute.add(solute)
        }

        if (totalRatio.compareTo(BigDecimal.ZERO) == 0 || totalSolute.compareTo(BigDecimal.ZERO) == 0) {
            return emptyProjectResult()
        }

        // 2. 工作液固含量/浓度 S = K / R
        val workingSolutionSolid = totalSolute.divide(totalRatio, SCALE_INTERNAL, ROUNDING_INTERNAL)

        // 3. 每平米总湿重 W_total = D / S
        val totalWetWeightPerSqm = dryCoatingWeightD.divide(workingSolutionSolid, SCALE_INTERNAL, ROUNDING_INTERNAL)

        // 4. 计算总面积 = L * W * N
        val area = lengthM.multiply(widthM).multiply(layersN)

        var totalCostPerSqm = BigDecimal.ZERO
        var totalRollCost = BigDecimal.ZERO
        var isPerSqmIncomplete = false
        var isRollCostIncomplete = false
        val details = mutableListOf<ComponentCostDetail>()

        for (comp in activeComps) {
            // 该组分湿重每平米 = W_total * (r_i / R)
            val wetWeight = totalWetWeightPerSqm.multiply(comp.ratio).divide(totalRatio, SCALE_INTERNAL, ROUNDING_INTERNAL)
            // 整卷消耗量(kg) = (湿重 g/m² * 面积 m²) / 1000
            val rollKg = wetWeight.multiply(area).divide(BigDecimal(1000), SCALE_INTERNAL, ROUNDING_INTERNAL)

            val price = comp.unitPrice
            val isMissing = price == null || price.compareTo(BigDecimal.ZERO) < 0

            val costSqm: BigDecimal
            val rollCostVal: BigDecimal

            if (!isMissing && price != null) {
                // 每平米成本 = (湿重(kg/m²) * 单价) -> 湿重 g/m² 换算为 kg 为 / 1000
                costSqm = wetWeight.divide(BigDecimal(1000), SCALE_INTERNAL, ROUNDING_INTERNAL).multiply(price)
                rollCostVal = rollKg.multiply(price)
                totalCostPerSqm = totalCostPerSqm.add(costSqm)
                totalRollCost = totalRollCost.add(rollCostVal)
            } else {
                isPerSqmIncomplete = true
                isRollCostIncomplete = true
                costSqm = BigDecimal.ZERO
                rollCostVal = BigDecimal.ZERO
            }

            details.add(
                ComponentCostDetail(
                    slotIndex = comp.slotIndex,
                    name = comp.name.ifBlank { "组分${comp.slotIndex}" },
                    wetWeightPerSqm = wetWeight,
                    costPerSqm = costSqm,
                    rollConsumptionKg = rollKg,
                    rollCost = rollCostVal,
                    isPriceMissing = isMissing
                )
            )
        }

        val status = if (isPerSqmIncomplete) CalculationStatus.PARTIAL_COST else CalculationStatus.COMPLETED

        return ProjectCostResult(
            workingSolutionSolid = workingSolutionSolid,
            totalWetWeightPerSqm = totalWetWeightPerSqm,
            totalCostPerSqm = totalCostPerSqm,
            processingAreaSqm = area,
            totalRollCost = totalRollCost,
            componentDetails = details,
            status = status,
            isPerSqmCostIncomplete = isPerSqmIncomplete,
            isRollCostIncomplete = isRollCostIncomplete
        )
    }

    private fun emptyProjectResult() = ProjectCostResult(
        workingSolutionSolid = BigDecimal.ZERO,
        totalWetWeightPerSqm = BigDecimal.ZERO,
        totalCostPerSqm = BigDecimal.ZERO,
        processingAreaSqm = BigDecimal.ZERO,
        totalRollCost = BigDecimal.ZERO,
        componentDetails = emptyList(),
        status = CalculationStatus.INSUFFICIENT_DATA,
        isPerSqmCostIncomplete = false,
        isRollCostIncomplete = false
    )
}

// 扩展属性：固含量自动锁定为 0 如果是可调纯溶剂
val ComponentInput.effectiveSolidContent: BigDecimal
    get() = if (isAdjustableSolvent) BigDecimal.ZERO else solidContent
