package com.yas.search.kafka.config.consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class AppKafkaListenerConfigurerTest {

    @Test
    void configureKafkaListeners_SetsValidator() {
        LocalValidatorFactoryBean validator = mock(LocalValidatorFactoryBean.class);
        AppKafkaListenerConfigurer configurer = new AppKafkaListenerConfigurer(validator);
        KafkaListenerEndpointRegistrar registrar = mock(KafkaListenerEndpointRegistrar.class);

        configurer.configureKafkaListeners(registrar);

        verify(registrar).setValidator(validator);
    }
}
