package com.yas.commonlibrary.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DateTimeUtilsTest {

    @Test
    void format_WithDefaultPattern_ShouldReturnFormattedString() {
        LocalDateTime dateTime = LocalDateTime.of(2023, 10, 15, 14, 30, 45);
        String formatted = DateTimeUtils.format(dateTime);
        assertEquals("15-10-2023_14-30-45", formatted);
    }

    @Test
    void format_WithCustomPattern_ShouldReturnFormattedString() {
        LocalDateTime dateTime = LocalDateTime.of(2023, 10, 15, 14, 30, 45);
        String formatted = DateTimeUtils.format(dateTime, "yyyy/MM/dd HH:mm");
        assertEquals("2023/10/15 14:30", formatted);
    }
}
