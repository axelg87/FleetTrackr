package com.fleetmanager.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.concurrent.TimeUnit

class InputValidatorTest {

    private lateinit var validator: InputValidator

    @Before
    fun setUp() {
        validator = InputValidator()
    }

    @Test
    fun `validateText returns error when required field is blank`() {
        // Arrange
        val fieldName = "Email"

        // Act
        val result = validator.validateText("   ", fieldName, required = true)

        // Assert
        assertTrue(result is ValidationResult.Error)
        assertEquals("AEDfieldName is required", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateText succeeds when trimmed input within max length`() {
        // Arrange
        val input = "   Fleet Tracker   "

        // Act
        val result = validator.validateText(input, fieldName = "Project", required = true, maxLength = 20)

        // Assert
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateName returns error when name contains invalid characters`() {
        // Arrange
        val invalidName = "John123"

        // Act
        val result = validator.validateName(invalidName)

        // Assert
        assertTrue(result is ValidationResult.Error)
        assertEquals("Name contains invalid characters", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateEarnings returns error when amount is negative`() {
        // Arrange
        val amount = "-50"

        // Act
        val result = validator.validateEarnings(amount, fieldName = "Weekly earnings")

        // Assert
        assertTrue(result is ValidationResult.Error)
        assertEquals("Weekly earnings cannot be negative", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateYear returns error when year more than one year in future`() {
        // Arrange
        val futureYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 2

        // Act
        val result = validator.validateYear(futureYear)

        // Assert
        assertTrue(result is ValidationResult.Error)
        assertEquals("Year cannot be more than one year in the future", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateLicensePlate uppercases and accepts valid format`() {
        // Arrange
        val licensePlate = "abc-123"

        // Act
        val result = validator.validateLicensePlate(licensePlate)

        // Assert
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateNotes returns error when notes exceed max length`() {
        // Arrange
        val longNotes = "a".repeat(5001)

        // Act
        val result = validator.validateNotes(longNotes)

        // Assert
        assertTrue(result is ValidationResult.Error)
        assertEquals("Notes must be 5000 characters or less", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateDate returns error when date more than one day ahead`() {
        // Arrange
        val futureDate = Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2))

        // Act
        val result = validator.validateDate(futureDate)

        // Assert
        assertTrue(result is ValidationResult.Error)
        assertEquals("Date cannot be in the future", (result as ValidationResult.Error).message)
    }

    @Test
    fun `sanitizeText trims whitespace removes control characters and normalizes spaces`() {
        // Arrange
        val rawInput = "  Fleet\u0007  Tracker\n App  "

        // Act
        val result = validator.sanitizeText(rawInput)

        // Assert
        assertEquals("Fleet Tracker App", result)
    }

    @Test
    fun `sanitizeNumericInput removes non numeric characters and extra decimals`() {
        // Arrange
        val rawInput = "  \AED1,234.56.78  "

        // Act
        val result = validator.sanitizeNumericInput(rawInput)

        // Assert
        assertEquals("1234.5678", result)
    }

    @Test
    fun `validateAll returns first error without evaluating remaining validations`() {
        // Arrange
        var wasSecondValidationCalled = false
        val firstValidation: () -> ValidationResult = {
            ValidationResult.Error("First error")
        }
        val secondValidation: () -> ValidationResult = {
            wasSecondValidationCalled = true
            ValidationResult.Success
        }

        // Act
        val result = validator.validateAll(firstValidation, secondValidation)

        // Assert
        assertTrue(result is ValidationResult.Error)
        assertEquals("First error", (result as ValidationResult.Error).message)
        assertFalse(wasSecondValidationCalled)
    }
}
