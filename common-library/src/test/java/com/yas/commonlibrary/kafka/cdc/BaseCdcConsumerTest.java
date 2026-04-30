package com.yas.commonlibrary.kafka.cdc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;

class BaseCdcConsumerTest {

    private final BaseCdcConsumer<String, String> consumer = new BaseCdcConsumer<>() {};

    @Test
    void processMessage_WithConsumer_ShouldCallAccept() {
        Map<String, Object> headerMap = new HashMap<>();
        MessageHeaders headers = new MessageHeaders(headerMap);
        
        @SuppressWarnings("unchecked")
        Consumer<String> mockConsumer = mock(Consumer.class);

        consumer.processMessage("test-record", headers, mockConsumer);

        verify(mockConsumer).accept("test-record");
    }

    @Test
    void processMessage_WithBiConsumer_ShouldCallAccept() {
        Map<String, Object> headerMap = new HashMap<>();
        MessageHeaders headers = new MessageHeaders(headerMap);
        
        @SuppressWarnings("unchecked")
        BiConsumer<String, String> mockBiConsumer = mock(BiConsumer.class);

        consumer.processMessage("test-key", "test-value", headers, mockBiConsumer);

        verify(mockBiConsumer).accept("test-key", "test-value");
    }
}
