package hr.andrijasevic.soundbox.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorrelationIdIntegrationTest extends BaseIntegrationTest {

    @Test
    void response_carriesGeneratedCorrelationId() throws Exception {
        String token = registerAndGetToken("ante", "ante@example.com");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(emptyString())));
    }

    @Test
    void response_echoesInboundCorrelationId() throws Exception {
        String token = registerAndGetToken("ante", "ante@example.com");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Correlation-Id", "trace-abc-123"))
                .andExpect(header().string("X-Correlation-Id", "trace-abc-123"));
    }
}
