package org.example.bigevent.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import static org.junit.jupiter.api.Assertions.*;

class ErrorDisclosureTest {
    @Test void legacyUnexpectedErrorsDoNotEchoSqlOrCredentials() {
        var response = new GlobalExceptionHandler().handleException(new IllegalStateException("SQL password=fixture-sensitive"));
        assertEquals(500, response.getStatusCode().value());
        assertFalse(response.getBody().getMessage().contains("fixture-sensitive"));
        assertEquals("服务暂不可用，请稍后重试", response.getBody().getMessage());
    }
    @Test void v1UnexpectedErrorsKeepEnvelopeAndHideInternalMessage() {
        var response = new ApiV1ExceptionHandler().unexpected(new IllegalStateException("fixture-sensitive"));
        assertEquals(500,response.getStatusCode().value());
        assertFalse(response.getBody().message().contains("fixture-sensitive"));
        assertNotNull(response.getBody().traceId());
    }
    @Test void malformedJsonIsClientErrorWithoutEchoingBody() {
        var error = new HttpMessageNotReadableException("fixture-sensitive malformed JSON", new MockHttpInputMessage(new byte[0]));
        var legacy = new GlobalExceptionHandler().badRequest(error);
        var v1 = new ApiV1ExceptionHandler().badRequest(error);
        assertEquals(400, legacy.getStatusCode().value());
        assertEquals(400, v1.getStatusCode().value());
        assertFalse(legacy.getBody().getMessage().contains("fixture-sensitive"));
        assertFalse(v1.getBody().message().contains("fixture-sensitive"));
    }
}
