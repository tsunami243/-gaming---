package org.example.bigevent.controller;
import org.example.bigevent.mapper.FavoriteReadMapper;
import org.example.bigevent.pojo.dto.FavoriteSummary;

import org.example.bigevent.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FavoriteV1ControllerTest {
    FavoriteReadMapper mapper = mock(FavoriteReadMapper.class);
    FavoriteV1Controller controller = new FavoriteV1Controller(mapper);
    @AfterEach void clear() { ThreadLocalUtil.remove(); }
    @Test void usesAuthenticatedUserAndBoundedPage() {
        ThreadLocalUtil.set(Map.of("userId", 3));
        when(mapper.page(3,20,20L)).thenReturn(List.of(new FavoriteSummary(7,"title","summary",null)));
        when(mapper.count(3)).thenReturn(21L);
        var page = controller.list(2,20).data();
        assertEquals(21,page.total()); assertEquals(7,page.items().get(0).id());
        verify(mapper).page(3,20,20L);
    }
    @Test void noClaimsOrInvalidPagingCannotQueryFavorites() {
        var error = assertThrows(ResponseStatusException.class, () -> controller.list(1,20));
        assertEquals(HttpStatus.UNAUTHORIZED,error.getStatusCode());
        ThreadLocalUtil.set(Map.of("userId",3));
        assertThrows(ResponseStatusException.class, () -> controller.list(0,20));
        assertThrows(ResponseStatusException.class, () -> controller.list(1,51));
        verifyNoInteractions(mapper);
    }
}
