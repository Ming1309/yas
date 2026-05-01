package com.yas.search.constant;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstantsTest {

    @Test
    void testProductFieldConstructor() throws Exception {
        Constructor<ProductField> constructor = ProductField.class.getDeclaredConstructor();
        assertTrue(constructor.trySetAccessible());
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
    }

    @Test
    void testMessageCodeConstructor() throws Exception {
        Constructor<MessageCode> constructor = MessageCode.class.getDeclaredConstructor();
        assertTrue(constructor.trySetAccessible());
        constructor.newInstance();
    }

    @Test
    void testActionConstructor() throws Exception {
        Constructor<Action> constructor = Action.class.getDeclaredConstructor();
        assertTrue(constructor.trySetAccessible());
        constructor.newInstance();
    }
}
