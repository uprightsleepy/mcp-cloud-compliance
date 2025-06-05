package com.cloudsec.compliance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ApplicationConfigTest {

    @Test
    @DisplayName("Should register JavaTimeModule in ObjectMapper")
    void shouldRegisterJavaTimeModule() {
        ApplicationConfig config = new ApplicationConfig();
        ObjectMapper mapper = config.objectMapper();

        assertThat(mapper.getRegisteredModuleIds())
            .anyMatch(id -> id.toString().contains("jsr310"));
    }
}
