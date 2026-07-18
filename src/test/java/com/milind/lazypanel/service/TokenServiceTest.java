package com.milind.lazypanel.service;

import com.milind.lazypanel.exception.ResourceNotFoundException;
import com.milind.lazypanel.repository.UserTokenRepository;
import com.milind.lazypanel.service.implementations.TokenService;
import com.milind.lazypanel.service.interfaces.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {

        when(restClientBuilder.baseUrl(anyString()))
                .thenReturn(restClientBuilder);

        when(restClientBuilder.build())
                .thenReturn(restClient);

        tokenService = new TokenService(restClientBuilder);

        ReflectionTestUtils.setField(
                tokenService,
                "userTokenRepository",
                userTokenRepository
        );

        ReflectionTestUtils.setField(
                tokenService,
                "stringRedisTemplate",
                stringRedisTemplate
        );

        ReflectionTestUtils.setField(
                tokenService,
                "encryptionService",
                encryptionService
        );
    }

    @Test
    void shouldReturnAccessTokenFromRedis() {

        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.get("1_at"))
                .thenReturn("cached-token");

        String token = tokenService.getAccessTokenFromUserId(1L);

        assertEquals("cached-token", token);

        verify(userTokenRepository, never()).findByUserId(anyLong());
        verify(encryptionService, never()).decrypt(anyString());
    }

    @Test
    void shouldThrowResourceNotFoundWhenUserTokenDoesNotExist() {

        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.get("1_at"))
                .thenReturn(null);

        when(userTokenRepository.findByUserId(1L))
                .thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> tokenService.getAccessTokenFromUserId(1L)
        );
    }
}