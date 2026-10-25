package com.just_sri.suvidha.expenses

/**
 * Standardizes phone numbers to clean E.164 (+91XXXXXXXXXX) and
 * human-friendly display formats (+91 XXXXX XXXXX).
 */
object PhoneUtils {
    /**
     * Cleans and normalizes a phone number.
     * Returns a pair of (e164Format, displayFormat), or null if the number is invalid.
     *
     * Example conversions:
     * - "9899034904"        -> ("+919899034904", "+91 98990 34904")
     * - "09583067151"       -> ("+919583067151", "+91 95830 67151")
     * - "+91 85958 77293"   -> ("+918595877293", "+91 85958 77293")
     * - "+919899034904"     -> ("+919899034904", "+91 98990 34904")
     * - "8603483869"        -> ("+918603483869", "+91 86034 83869")
     */
    fun clean(raw: String?): Pair<String, String>? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val digitsOnly = trimmed.filter(Char::isDigit)
        if (digitsOnly.length < 10) return null

        val (countryCode, nationalNumber) = when {
            // Numbers with +91 country prefix
            trimmed.startsWith("+91") && digitsOnly.startsWith("91") && digitsOnly.length >= 12 -> {
                "91" to digitsOnly.substring(2).takeLast(10)
            }
            // 12-digit Indian numbers starting with 91 (e.g. 919899034904)
            digitsOnly.length == 12 && digitsOnly.startsWith("91") -> {
                "91" to digitsOnly.substring(2)
            }
            // 11-digit numbers with leading 0 (e.g. 09583067151)
            digitsOnly.length == 11 && digitsOnly.startsWith("0") -> {
                "91" to digitsOnly.substring(1)
            }
            // Standard 10-digit mobile number
            digitsOnly.length == 10 -> {
                "91" to digitsOnly
            }
            // Other international number starting with +
            trimmed.startsWith("+") -> {
                val cleaned = "+" + digitsOnly
                return cleaned to formatInternational(cleaned)
            }
            // Fallback for numbers > 10 digits
            else -> {
                "91" to digitsOnly.takeLast(10)
            }
        }

        if (nationalNumber.length != 10) return null

        val e164 = "+$countryCode$nationalNumber"
        val display = "+$countryCode ${nationalNumber.substring(0, 5)} ${nationalNumber.substring(5)}"
        return e164 to display
    }

    /**
     * Formats any phone number into a clean display format "+91 XXXXX XXXXX".
     */
    fun formatDisplay(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return clean(raw)?.second ?: raw.trim()
    }

    /**
     * Returns the strict E.164 format "+91XXXXXXXXXX" for storage and Firestore querying.
     */
    fun normalizeToE164(raw: String?): String? {
        return clean(raw)?.first
    }

    private fun formatInternational(number: String): String {
        return if (number.length > 5) {
            number.chunked(4).joinToString(" ")
        } else {
            number
        }
    }
}
