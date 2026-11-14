package com.coating.calculator.ui.utils

import java.math.BigDecimal
import java.math.RoundingMode

object DisplayFormatter {
    fun formatRatio(value: BigDecimal): String = value.setScale(2, RoundingMode.HALF_UP).toPlainString()
    fun formatSolidPercent(value: BigDecimal): String =
        value.multiply(BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%"
    fun formatCostInteger(value: BigDecimal): String = value.setScale(0, RoundingMode.HALF_UP).toPlainString()
    fun formatWeightOrArea(value: BigDecimal): String = value.setScale(3, RoundingMode.HALF_UP).toPlainString()
}
