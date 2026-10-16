package org.example.bigevent.service.impl;
import org.example.bigevent.mapper.AdminOperationsMapper;
import org.example.bigevent.pojo.dto.SourcePauseRequest;
import org.example.bigevent.service.AdminOperationsService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminOperationsServiceTest {
    @Test void pauseAndResumeRequireExactlyOneSourceTransition() {
        AdminOperationsMapper mapper = mock(AdminOperationsMapper.class);
        AdminOperationsService service = new AdminOperationsServiceImpl(mapper);
        when(mapper.pauseSource("valorant-official", "manual quality review")).thenReturn(1);
        when(mapper.resumeSource("valorant-official")).thenReturn(1);
        service.pause("valorant-official", new SourcePauseRequest("manual quality review"));
        service.resume("valorant-official");
        verify(mapper).pauseSource("valorant-official", "manual quality review");
        verify(mapper).resumeSource("valorant-official");
    }

    @Test void invalidRunLimitAndNoopPauseAreConflicts() {
        AdminOperationsMapper mapper = mock(AdminOperationsMapper.class);
        AdminOperationsService service = new AdminOperationsServiceImpl(mapper);
        ResponseStatusException badLimit = assertThrows(ResponseStatusException.class, () -> service.latestRuns(101));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, badLimit.getStatusCode());
        when(mapper.pauseSource("paused", "already paused")).thenReturn(0);
        ResponseStatusException conflict = assertThrows(ResponseStatusException.class, () -> service.pause("paused", new SourcePauseRequest("already paused")));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
    }
}
