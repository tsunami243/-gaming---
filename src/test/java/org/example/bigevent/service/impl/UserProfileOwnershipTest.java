package org.example.bigevent.service.impl;
import org.example.bigevent.mapper.UserMapper;
import org.example.bigevent.pojo.User;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class UserProfileOwnershipTest {
    @AfterEach void cleanup() { ThreadLocalUtil.remove(); }
    @Test void cannotUpdateAnotherUserEvenBySubmittingTheirId() {
        UserMapper mapper = mock(UserMapper.class); UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service,"userMapper",mapper);
        ThreadLocalUtil.set(Map.of("userId",3));
        User submitted = new User(); submitted.setId(99); submitted.setNickname("changed");
        var error = assertThrows(ResponseStatusException.class, () -> service.update(submitted));
        assertEquals(HttpStatus.FORBIDDEN,error.getStatusCode()); verifyNoInteractions(mapper);
    }
    @Test void absentClaimsCannotUpdateAnyUser() {
        UserServiceImpl service = new UserServiceImpl();
        var error = assertThrows(ResponseStatusException.class, () -> service.update(new User()));
        assertEquals(HttpStatus.UNAUTHORIZED,error.getStatusCode());
    }
    @Test void ownProfileUpdateRemainsAvailable() {
        UserMapper mapper = mock(UserMapper.class); UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service,"userMapper",mapper);
        ThreadLocalUtil.set(Map.of("userId",3));
        User submitted = new User(); submitted.setId(3); submitted.setNickname("self");
        service.update(submitted);
        verify(mapper).update(submitted); assertNotNull(submitted.getUpdateTime());
    }
}
