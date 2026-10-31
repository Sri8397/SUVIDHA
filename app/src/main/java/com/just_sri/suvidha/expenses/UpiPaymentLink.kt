package com.just_sri.suvidha.expenses

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Everything needed to open a UPI payment in any installed UPI app. Amount is in the currency's minor unit. */
data class UpiPaymentRequest(
    val payeeAddress: String,
    val payeeName: String,
    val amountMinor: Long,
    val currencyCode: String,
    val note: String,
)

sealed interface UpiPaymentLinkResult {
    data class Ready(val uri: String) : UpiPaymentLinkResult

    data class Rejected(val reason: String) : UpiPaymentLinkResult
}

/** Builds `upi://pay` deep links (NPCI UPI linking spec). Validation runs before any URI is produced. */
internal object UpiPaymentLink {
    private const val UPI_PREFIX: String = "upi://pay?"
    private const val RUPEE_CODE: String = "INR"
    private const val MAX_NAME_LENGTH: Int = 50
    private const val MAX_NOTE_LENGTH: Int = 50
    private const val URL_PLUS: String = "+"
    private const val URL_SPACE: String = "%20"
    private const val EMPTY_TEXT: String = ""
    private val payeeAddressPattern: Regex = Regex("[A-Za-z0-9._-]{2,256}@[A-Za-z]{2,64}")
    private val controlCharacterPattern: Regex = Regex("\\p{Cntrl}")

    fun buildPaymentLink(request: UpiPaymentRequest): UpiPaymentLinkResult {
        val normalizedRequest: UpiPaymentRequest = normalize(request)
        val rejectionReason: String? = findRejectionReason(normalizedRequest)
        return rejectionReason?.let { reason: String -> UpiPaymentLinkResult.Rejected(reason) }
            ?: UpiPaymentLinkResult.Ready(composeUri(normalizedRequest))
    }

    private fun normalize(request: UpiPaymentRequest): UpiPaymentRequest = request.copy(
        payeeAddress = request.payeeAddress.trim(),
        payeeName = sanitizeText(request.payeeName, MAX_NAME_LENGTH),
        note = sanitizeText(request.note, MAX_NOTE_LENGTH),
        currencyCode = request.currencyCode.trim().uppercase(),
    )

    /** Removes control characters and caps length. Names and notes are free text, so they are trimmed rather than rejected. */
    private fun sanitizeText(input: String, maxLength: Int): String =
        input.replace(controlCharacterPattern, " ").trim().take(maxLength)

    private fun findRejectionReason(request: UpiPaymentRequest): String? = when {
        request.currencyCode != RUPEE_CODE -> "UPI payments are only available in INR."
        request.amountMinor <= 0L -> "Payment amount must be greater than zero."
        !payeeAddressPattern.matches(request.payeeAddress) -> "Payee UPI ID is not valid."
        request.payeeName.isEmpty() -> "Payee name is required."
        else -> null
    }

    private fun composeUri(request: UpiPaymentRequest): String {
        val parameters: List<String> = listOf(
            "pa" to request.payeeAddress,
            "pn" to request.payeeName,
            "am" to SharedSplitMoney.format(request.amountMinor, RUPEE_CODE),
            "cu" to RUPEE_CODE,
            "tn" to request.note,
        ).filter { (_, value: String) -> value != EMPTY_TEXT }
            .map { (key: String, value: String) -> "$key=${encodeComponent(value)}" }
        return UPI_PREFIX + parameters.joinToString("&")
    }

    private fun encodeComponent(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace(URL_PLUS, URL_SPACE)
}
