package com.npst.loggingapi.controller;

import com.npst.loggingapi.exception.UnsupportedEventTypeException;
import com.npst.loggingapi.service.LoggingService;
import com.npst.observability.contract.EventType;
import com.npst.observability.contract.LogIngestRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// checks the responses of the log ingest endpoint
@WebMvcTest(LoggingController.class)
class LoggingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoggingService service;

    @Test
    void storesAValidApplicationLog() throws Exception {

        given(service.store(any(LogIngestRequest.class))).willReturn(42L);

        mockMvc.perform(post("/api/v1/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.traceId").value("TRACE-001"))
                .andExpect(jsonPath("$.data.logId").value(42))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void namesEveryMissingFieldRatherThanFailingOpaquely() throws Exception {

        String missingEnrichment = """
                {
                  "schemaVersion": "1.0",
                  "traceId": "TRACE-002",
                  "eventType": "APPLICATION",
                  "level": "INFO",
                  "message": "Balance fetched successfully",
                  "timestamp": "2026-09-08T14:38:29Z"
                }
                """;

        mockMvc.perform(post("/api/v1/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingEnrichment))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[0]").value("bankCode: bankCode is required"))
                .andExpect(jsonPath("$.errors[1]").value("environment: environment is required"))
                .andExpect(jsonPath("$.errors[2]").value("service: service is required"));
    }

    @Test
    void rejectsABlankMessage() throws Exception {

        mockMvc.perform(post("/api/v1/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("\"Balance fetched successfully\"", "\"  \"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("message: message is required"));
    }

    @Test
    void rejectsALevelOutsideTheContract() throws Exception {

        mockMvc.perform(post("/api/v1/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("\"INFO\"", "\"CRITICAL\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Log rejected: request body could not be read"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {

        mockMvc.perform(post("/api/v1/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void refusesAnAuditEventWithUnprocessableEntity() throws Exception {

        willThrow(new UnsupportedEventTypeException(EventType.AUDIT))
                .given(service).store(any(LogIngestRequest.class));

        mockMvc.perform(post("/api/v1/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("\"APPLICATION\"", "\"AUDIT\"")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Log rejected: unsupported event type"))
                .andExpect(jsonPath("$.errors[0]").value(
                        org.hamcrest.Matchers.containsString("AUDIT")));
    }

    @Test
    void reportsAnInternalFailureInTheSameEnvelope() throws Exception {

        willThrow(new IllegalStateException("database unreachable"))
                .given(service).store(any(LogIngestRequest.class));

        mockMvc.perform(post("/api/v1/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Log ingest failed"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    private static String validRequest() {
        return """
                {
                  "schemaVersion": "1.0",
                  "traceId": "TRACE-001",
                  "bankCode": "NPST",
                  "environment": "DEV",
                  "service": "sample-bank-app",
                  "eventType": "APPLICATION",
                  "level": "INFO",
                  "message": "Balance fetched successfully",
                  "timestamp": "2026-09-08T14:38:29Z",
                  "metadata": { "channel": "MOBILE", "module": "BALANCE" }
                }
                """;
    }
}
