package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object IndianFormatUtils {

    /**
     * Formats currency in Indian numbering system (Lakhs / Crores or comma separated)
     * e.g. 1349000 -> "₹ 13.49 L" (compact) or "₹ 13,49,000" (full)
     */
    fun formatInr(amount: Double, compact: Boolean = false): String {
        if (amount < 0) {
            return "- " + formatInr(-amount, compact)
        }
        if (compact) {
            return when {
                amount >= 1_00_00_000.0 -> String.format(Locale.ENGLISH, "₹ %.2f Cr", amount / 1_00_00_000.0)
                amount >= 1_00_000.0 -> String.format(Locale.ENGLISH, "₹ %.2f L", amount / 1_00_000.0)
                amount >= 1_000.0 -> "₹ " + formatIndianNumber(amount)
                else -> {
                    if (amount % 1.0 == 0.0) "₹ ${amount.toLong()}" else String.format(Locale.ENGLISH, "₹ %.2f", amount)
                }
            }
        } else {
            return "₹ " + formatIndianNumber(amount)
        }
    }

    /**
     * Converts a number to standard Indian comma separated format (e.g., 1,23,45,678)
     */
    fun formatIndianNumber(number: Double): String {
        val longVal = number.toLong()
        val frac = number - longVal
        val strVal = longVal.toString()
        val len = strVal.length

        if (len <= 3) {
            val base = strVal
            return if (frac > 0.001) String.format(Locale.ENGLISH, "%s.%.2f", base, frac).replace("0.", ".") else base
        }

        val lastThree = strVal.substring(len - 3)
        val remaining = strVal.substring(0, len - 3)

        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i != 0) {
                sb.append(',')
            }
        }
        val formattedRemaining = sb.reverse().toString()
        val result = "$formattedRemaining,$lastThree"
        return if (frac > 0.001) {
            val fracStr = String.format(Locale.ENGLISH, "%.2f", frac).substring(1)
            result + fracStr
        } else {
            result
        }
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "Not Restocked"
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    fun formatDateIndian(timestamp: Long): String = formatDate(timestamp)

    fun formatDateOnlyIndian(timestamp: Long): String {
        if (timestamp <= 0) return "-"
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    fun getCategoryHindiSubtext(category: String): String {
        return when (category.lowercase()) {
            "cars" -> "कार"
            "bikes" -> "दोपहिया / बाइक"
            "spare parts" -> "स्पेयर पार्ट्स"
            else -> "ऑटो पार्ट्स"
        }
    }

    fun getGstRateForCategory(category: String, subcategory: String = ""): Int {
        return when {
            category.equals("Cars", ignoreCase = true) -> 28
            category.equals("Bikes", ignoreCase = true) -> 28
            subcategory.contains("Tyre", ignoreCase = true) -> 28
            subcategory.contains("Fluid", ignoreCase = true) || subcategory.contains("Oil", ignoreCase = true) -> 18
            subcategory.contains("Battery", ignoreCase = true) -> 28
            else -> 18
        }
    }

    fun getHsnCodeForCategory(category: String, subcategory: String = ""): String {
        return when {
            category.equals("Cars", ignoreCase = true) -> "8703"
            category.equals("Bikes", ignoreCase = true) -> "8711"
            subcategory.contains("Tyre", ignoreCase = true) -> "4011"
            subcategory.contains("Fluid", ignoreCase = true) || subcategory.contains("Oil", ignoreCase = true) -> "2710"
            subcategory.contains("Battery", ignoreCase = true) -> "8507"
            else -> "8708"
        }
    }

    fun getGstLabelWithHsn(category: String, subcategory: String = ""): String {
        val rate = getGstRateForCategory(category, subcategory)
        val hsn = getHsnCodeForCategory(category, subcategory)
        return "GST $rate% (HSN $hsn)"
    }
}
