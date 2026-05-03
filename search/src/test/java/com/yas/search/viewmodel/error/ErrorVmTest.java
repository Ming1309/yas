package com.yas.search.viewmodel.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorVmTest {

    @Test
    void testErrorVm_ConstructorWithThreeParams() {
        ErrorVm errorVm = new ErrorVm("400", "Bad Request", "Detail error");
        assertEquals("400", errorVm.statusCode());
        assertEquals("Bad Request", errorVm.title());
        assertEquals("Detail error", errorVm.detail());
        assertTrue(errorVm.fieldErrors().isEmpty());
    }
}
