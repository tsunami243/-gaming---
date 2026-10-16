package org.example.bigevent.service.impl;
import org.example.bigevent.mapper.IngestSourcePolicyMapper;
import org.example.bigevent.pojo.dto.IngestSourcePolicyResponse;
import org.example.bigevent.service.IngestSourcePolicyService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class IngestSourcePolicyServiceTest {
    @Test void missingAndUnverifiedSourcesAreDisabledRatherThanThrowing() {
        var mapper = mock(IngestSourcePolicyMapper.class);
        var service = new IngestSourcePolicyServiceImpl(mapper);
        assertEquals(new IngestSourcePolicyResponse(false,"MISSING"), service.policy("unknown"));
        for (String status : java.util.List.of("PAUSED","CANDIDATE","FETCH_VERIFIED","unexpected")) {
            when(mapper.verificationStatus("source")).thenReturn(status);
            assertFalse(service.policy("source").enabled());
        }
    }
    @Test void onlyShadowAndAutoEnabledCanRun() {
        var mapper = mock(IngestSourcePolicyMapper.class);
        var service = new IngestSourcePolicyServiceImpl(mapper);
        for (String status : java.util.List.of("SHADOW","AUTO_ENABLED")) {
            when(mapper.verificationStatus("source")).thenReturn(status);
            assertTrue(service.policy("source").enabled());
        }
    }
}
