package com.milind.lazypanel.service;


import com.milind.lazypanel.dto.UserTokenDto;
import com.milind.lazypanel.model.User;
import com.milind.lazypanel.service.implementations.AuthService;
import com.milind.lazypanel.service.implementations.JwtService;
import com.milind.lazypanel.service.implementations.TokenService;
import com.milind.lazypanel.service.implementations.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenService tokenService;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldAuthenticateExistingUser() {

        User user = User.builder()
                .id(1L)
                .username("john@gmail.com")
                .build();

        UserTokenDto tokenDto = createUserTokenDto();

        when(oAuth2User.getAttribute("email"))
                .thenReturn("john@gmail.com");

        when(userService.getUserByUsername("john@gmail.com"))
                .thenReturn(user);

        when(jwtService.generateToken("john@gmail.com"))
                .thenReturn("jwt-token");

        String jwt = authService.authenticate(oAuth2User, tokenDto);

        assertEquals("jwt-token", jwt);

        verify(userService)
                .getUserByUsername("john@gmail.com");

        verify(userService, never())
                .signUpNewUser(anyString(), anyString(), anyString());

        verify(tokenService)
                .saveTokens(user, tokenDto);

        verify(jwtService)
                .generateToken("john@gmail.com");
    }

    @Test
    void shouldCreateNewUserWhenUserDoesNotExist() {

        User user = User.builder()
                .id(1L)
                .username("john@gmail.com")
                .build();

        UserTokenDto tokenDto = createUserTokenDto();

        when(oAuth2User.getAttribute("email"))
                .thenReturn("john@gmail.com");

        when(oAuth2User.getAttribute("given_name"))
                .thenReturn("John");

        when(oAuth2User.getAttribute("family_name"))
                .thenReturn("Doe");

        when(userService.getUserByUsername("john@gmail.com"))
                .thenReturn(null);

        when(userService.signUpNewUser(
                "john@gmail.com",
                "John",
                "Doe"))
                .thenReturn(user);

        when(jwtService.generateToken("john@gmail.com"))
                .thenReturn("jwt-token");

        String jwt = authService.authenticate(oAuth2User, tokenDto);

        assertEquals("jwt-token", jwt);

        verify(userService)
                .getUserByUsername("john@gmail.com");

        verify(userService)
                .signUpNewUser(
                        "john@gmail.com",
                        "John",
                        "Doe");

        verify(tokenService)
                .saveTokens(user, tokenDto);

        verify(jwtService)
                .generateToken("john@gmail.com");
    }

    @Test
    void shouldLogoutUser() {

        authService.logout(1L);

        verify(tokenService)
                .deleteTokens(1L);
    }

    private UserTokenDto createUserTokenDto() {
        return new UserTokenDto(
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(3600)
        );
    }
}