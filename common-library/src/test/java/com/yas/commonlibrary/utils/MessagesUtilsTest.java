package com.yas.commonlibrary.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_WhenCodeExists_ShouldReturnFormattedMessage() {
        String message = MessagesUtils.getMessage("UNKNOWN_CODE", "param1");
        assertEquals("UNKNOWN_CODE", message);
    }

    @Test
    void getMessage_WithFormatting_ShouldFormatMessage() {
        String message = MessagesUtils.getMessage("Error: {} happened", "Timeout");
        assertEquals("Error: Timeout happened", message);
    }
}
