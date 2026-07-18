package com.milind.lazypanel.service;

import com.milind.lazypanel.model.User;
import com.milind.lazypanel.service.implementations.UserDetailsCustomService;
import com.milind.lazypanel.service.implementations.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsCustomServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserDetailsCustomService userDetailsCustomService;

    @Test
    void shouldReturnUserWhenUserExists() {

        User user = User.builder()
                .username("john@gmail.com")
                .build();

        when(userService.getUserByUsername("john@gmail.com"))
                .thenReturn(user);

        UserDetails result =
                userDetailsCustomService.loadUserByUsername("john@gmail.com");

        assertEquals(user, result);
    }

    @Test
    void shouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {

        when(userService.getUserByUsername("john@gmail.com"))
                .thenReturn(null);

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsCustomService.loadUserByUsername("john@gmail.com")
        );
    }
}