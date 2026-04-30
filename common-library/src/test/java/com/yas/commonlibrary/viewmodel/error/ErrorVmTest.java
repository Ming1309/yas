package com.yas.commonlibrary.viewmodel.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void constructor_WithAllArgs_ShouldCreateObject() {
        ErrorVm errorVm = new ErrorVm("404", "Not Found", "Item not found", List.of("id"));

        assertEquals("404", errorVm.statusCode());
        assertEquals("Not Found", errorVm.title());
        assertEquals("Item not found", errorVm.detail());
        assertEquals(1, errorVm.fieldErrors().size());
        assertEquals("id", errorVm.fieldErrors().get(0));
    }

    @Test
    void constructor_WithThreeArgs_ShouldCreateObjectWithEmptyFieldErrors() {
        ErrorVm errorVm = new ErrorVm("500", "Server Error", "Something went wrong");

        assertEquals("500", errorVm.statusCode());
        assertEquals("Server Error", errorVm.title());
        assertEquals("Something went wrong", errorVm.detail());
        assertTrue(errorVm.fieldErrors().isEmpty());
    }
}
