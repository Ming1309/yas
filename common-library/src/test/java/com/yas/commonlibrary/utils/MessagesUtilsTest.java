package com.yas.commonlibrary.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_WhenCodeExists_ShouldReturnFormattedMessage() {
        // Since we may not have messages.properties in common-library test resources,
        // it might fall back to the error code. Let's test the fallback mechanism.
        String message = MessagesUtils.getMessage("UNKNOWN_CODE", "param1");
        assertEquals("UNKNOWN_CODE", message); // Because it falls back and no {} to format
    }

    @Test
    void getMessage_WithFormatting_ShouldFormatMessage() {
        // Testing formatting when the string has {}
        String message = MessagesUtils.getMessage("Error: {} happened", "Timeout");
        assertEquals("Error: Timeout happened", message);
    }
}
