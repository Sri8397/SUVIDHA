package com.just_sri.suvidha.expenses

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiPaymentLinkTest {
    @Test
    fun buildsLinkWithEncodedPayeeNameAndNoteAndTwoDecimalAmount() {
        // Arrange
        val inputRequest: UpiPaymentRequest = UpiPaymentRequest(
            payeeAddress = "ravi@okaxis",
            payeeName = "Ravi Kumar",
            amountMinor = 12_550L,
            currencyCode = "INR",
            note = "Dinner & drinks",
        )
        val expectedUri: String =
            "upi://pay?pa=ravi%40okaxis&pn=Ravi%20Kumar&am=125.50&cu=INR&tn=Dinner%20%26%20drinks"

        // Act
        val actualResult: UpiPaymentLinkResult = UpiPaymentLink.buildPaymentLink(inputRequest)

        // Assert
        assertEquals(UpiPaymentLinkResult.Ready(expectedUri), actualResult)
    }

    @Test
    fun omitsEmptyNoteAndKeepsOnePaiseAsZeroPointZeroOne() {
        // Arrange
        val inputRequest: UpiPaymentRequest = UpiPaymentRequest(
            payeeAddress = "priya.s@ybl",
            payeeName = "Priya",
            amountMinor = 1L,
            currencyCode = "INR",
            note = "   ",
        )
        val expectedUri: String = "upi://pay?pa=priya.s%40ybl&pn=Priya&am=0.01&cu=INR"

        // Act
        val actualResult: UpiPaymentLinkResult = UpiPaymentLink.buildPaymentLink(inputRequest)

        // Assert
        assertEquals(UpiPaymentLinkResult.Ready(expectedUri), actualResult)
    }

    @Test
    fun trimsAddressAndStripsControlCharactersFromNote() {
        // Arrange
        val inputRequest: UpiPaymentRequest = UpiPaymentRequest(
            payeeAddress = "  ravi@okaxis  ",
            payeeName = "Ravi",
            amountMinor = 100L,
            currencyCode = "INR",
            note = "Trip\n\tfare",
        )

        // Act
        val actualResult: UpiPaymentLinkResult = UpiPaymentLink.buildPaymentLink(inputRequest)

        // Assert
        assertEquals(UpiPaymentLinkResult.Ready("upi://pay?pa=ravi%40okaxis&pn=Ravi&am=1.00&cu=INR&tn=Trip%20%20fare"), actualResult)
    }

    @Test
    fun capsNoteAtFiftyCharactersWithoutSplittingTheWholeRequest() {
        // Arrange
        val inputNote: String = "x".repeat(80)
        val inputRequest: UpiPaymentRequest = UpiPaymentRequest("ravi@okaxis", "Ravi", 100L, "INR", inputNote)

        // Act
        val actualResult: UpiPaymentLinkResult = UpiPaymentLink.buildPaymentLink(inputRequest)

        // Assert
        val actualUri: String = (actualResult as UpiPaymentLinkResult.Ready).uri
        assertTrue(actualUri.endsWith("&tn=" + "x".repeat(50)))
    }

    @Test
    fun rejectsAnythingOtherThanRupees() {
        // Arrange
        val inputRequest: UpiPaymentRequest = UpiPaymentRequest("ravi@okaxis", "Ravi", 100L, "USD", "Lunch")

        // Act
        val actualResult: UpiPaymentLinkResult = UpiPaymentLink.buildPaymentLink(inputRequest)

        // Assert
        assertEquals(UpiPaymentLinkResult.Rejected("UPI payments are only available in INR."), actualResult)
    }

    @Test
    fun rejectsZeroAndNegativeAmounts() {
        // Arrange
        val inputZeroRequest: UpiPaymentRequest = UpiPaymentRequest("ravi@okaxis", "Ravi", 0L, "INR", "")
        val inputNegativeRequest: UpiPaymentRequest = UpiPaymentRequest("ravi@okaxis", "Ravi", -5L, "INR", "")

        // Act
        val actualZeroResult: UpiPaymentLinkResult = UpiPaymentLink.buildPaymentLink(inputZeroRequest)
        val actualNegativeResult: UpiPaymentLinkResult = UpiPaymentLink.buildPaymentLink(inputNegativeRequest)

        // Assert
        assertEquals(UpiPaymentLinkResult.Rejected("Payment amount must be greater than zero."), actualZeroResult)
        assertEquals(UpiPaymentLinkResult.Rejected("Payment amount must be greater than zero."), actualNegativeResult)
    }

    @Test
    fun rejectsMalformedUpiIds() {
        // Arrange
        val inputAddresses: List<String> = listOf("ravi", "@okaxis", "ravi@", "ravi@b", "ra vi@okaxis", "ravi@ok@axis")

        // Act
        val actualResults: List<UpiPaymentLinkResult> = inputAddresses.map { address: String ->
            UpiPaymentLink.buildPaymentLink(UpiPaymentRequest(address, "Ravi", 100L, "INR", ""))
        }

        // Assert
        assertTrue(actualResults.all { result: UpiPaymentLinkResult -> result == UpiPaymentLinkResult.Rejected("Payee UPI ID is not valid.") })
    }

    @Test
    fun rejectsBlankPayeeName() {
        // Arrange
        val inputRequest: UpiPaymentRequest = UpiPaymentRequest("ravi@okaxis", "   ", 100L, "INR", "Lunch")

        // Act
        val actualResult: UpiPaymentLinkResult = UpiPaymentLink.buildPaymentLink(inputRequest)

        // Assert
        assertEquals(UpiPaymentLinkResult.Rejected("Payee name is required."), actualResult)
    }
}
