package com.antonlappa.rechnungapp.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that {@code ALLOWED_ORIGINS} entries are trimmed and empty entries ignored.
 */
@SpringBootTest(properties = "ALLOWED_ORIGINS=https://a.com, https://b.com, ,")
@AutoConfigureMockMvc
class AllowedOriginsCorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"https://a.com", "https://b.com"})
    void allowsEachConfiguredOrigin(String origin) throws Exception {
        mockMvc.perform(options("/api/v1/customers")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin));
    }
}
