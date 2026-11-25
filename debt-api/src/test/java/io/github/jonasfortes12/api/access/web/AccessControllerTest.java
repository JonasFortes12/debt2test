package io.github.jonasfortes12.api.access.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.jonasfortes12.api.access.AccessCodeState;
import io.github.jonasfortes12.api.access.AccessService;
import io.github.jonasfortes12.api.access.AuthenticatedCode;
import io.github.jonasfortes12.api.access.TermsNotCurrentException;
import io.github.jonasfortes12.api.access.TooManyRequestsException;
import io.github.jonasfortes12.core.port.PipelineRunStore;
import io.github.jonasfortes12.core.port.ReportSink;

@WebMvcTest(AccessController.class)
@Import(AccessControllerTest.Terms.class)
class AccessControllerTest {

    private static final String CODE = "D2T-AAAA-BBBB-CCCC-DDDD-EEEE-FFFF-GGGG";
    private static final String VALID_REQUEST = """
            {"name":"Ana","email":"ana@example.com","acceptedTermsVersion":"v1"}""";

    static class Terms {
        @org.springframework.context.annotation.Bean
        TermsCatalog termsCatalog() {
            return new TermsCatalog("v1", "# Terms v1\nBody.");
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AccessService accessService;
    @MockitoBean
    private PipelineRunStore runStore;
    @MockitoBean
    private ReportSink databaseReportSink;

    @Test
    void termsReturnsCurrentVersionAndText() throws Exception {
        mockMvc.perform(get("/api/access/terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("v1"))
                .andExpect(jsonPath("$.text").value("# Terms v1\nBody."));
    }

    @Test
    void requestAnswers202WithFixedMessage() throws Exception {
        mockMvc.perform(post("/api/access/requests").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST)
                .with(request -> {
                    request.setRemoteAddr("203.0.113.7");
                    return request;
                }))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message")
                        .value("If the address can receive email, a message has been sent to it."));

        verify(accessService).requestCode("Ana", "ana@example.com", "v1", "203.0.113.7");
    }

    @Test
    void requestAnswers202EvenWhenQuotaReached() throws Exception {
        // The service reports nothing about quota or cooldown: the response cannot
        // differ.
        mockMvc.perform(post("/api/access/requests").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message")
                        .value("If the address can receive email, a message has been sent to it."));
    }

    @Test
    void requestWithInvalidEmailAnswers400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/access/requests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ana\",\"email\":\"not-an-email\",\"acceptedTermsVersion\":\"v1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    void requestWithStaleTermsAnswers400OnAcceptedTermsVersion() throws Exception {
        doThrow(new TermsNotCurrentException("v0", "v1")).when(accessService)
                .requestCode(any(), any(), eq("v0"), any());

        mockMvc.perform(post("/api/access/requests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"acceptedTermsVersion\":\"v0\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("terms version 'v0' is not current; current is 'v1'"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("acceptedTermsVersion"));
    }

    @Test
    void requestOverIpLimitAnswers429() throws Exception {
        doThrow(new TooManyRequestsException()).when(accessService).requestCode(any(), any(), any(), any());

        mockMvc.perform(post("/api/access/requests").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message")
                        .value("too many access-code requests from this address; try again later"));
    }

    @Test
    void databaseFailureAnswers503WithoutDetails() throws Exception {
        doThrow(new org.springframework.dao.DataIntegrityViolationException(
                "Key (email)=(ana@example.com) already exists.")).when(accessService)
                .requestCode(any(), any(), any(), any());

        mockMvc.perform(post("/api/access/requests").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("the database is unavailable; try again later"));
    }

    @Test
    void codeStatusReturnsStateExpiryAndExecutionId() throws Exception {
        UUID run = UUID.fromString("3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40");
        when(accessService.authenticate(CODE)).thenReturn(Optional.of(new AuthenticatedCode(
                "hash", AccessCodeState.USED, Instant.parse("2026-12-06T12:00:00Z"), run)));

        mockMvc.perform(get("/api/access/code").header("Authorization", "Bearer " + CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("USED"))
                .andExpect(jsonPath("$.expiresAt").value("2026-12-06T12:00:00Z"))
                .andExpect(jsonPath("$.executionId").value(run.toString()));
    }

    @Test
    void codeStatusWithoutBearerAnswers401() throws Exception {
        when(accessService.authenticate(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/access/code"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }
}
