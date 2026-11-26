package com.abihaniisa.hani.data

import java.math.BigDecimal
import java.math.RoundingMode

object ProductCalculator {
    fun costForPackage(
        purchasePriceKobo: Long,
        packageQuantity: String,
        purchaseQuantity: String
    ): Long {
        val numerator = BigDecimal.valueOf(purchasePriceKobo)
            .multiply(BigDecimal(packageQuantity))
        return numerator.divide(BigDecimal(purchaseQuantity), 0, RoundingMode.HALF_UP).longValueExact()
    }

    fun suggestedSellingPriceKobo(costKobo: Long): Long =
        (Math.floorDiv(costKobo, 5000L) + 1L) * 5000L

    fun naira(kobo: Long): String {
        val amount = BigDecimal.valueOf(kobo).divide(BigDecimal(100))
        return "₦" + String.format("%,.2f", amount)
    }

    fun parseNaira(text: String): Long {
        val value = BigDecimal(text.trim().replace(",", ""))
            .multiply(BigDecimal(100))
        return value.setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    }
}
