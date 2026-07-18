package com.milind.lazypanel.service.implementations;

import com.milind.lazypanel.dto.RefreshTokenResponseDto;
import com.milind.lazypanel.dto.UserTokenDto;
import com.milind.lazypanel.exception.GoogleTokenException;
import com.milind.lazypanel.exception.ResourceNotFoundException;
import com.milind.lazypanel.exception.TokenRefreshException;
import com.milind.lazypanel.model.User;
import com.milind.lazypanel.model.UserToken;
import com.milind.lazypanel.repository.UserTokenRepository;
import com.milind.lazypanel.service.interfaces.EncryptionService;
import com.milind.lazypanel.service.interfaces.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class TokenServiceImpl implements TokenService {
    private final String KEY_SUFFIX = "_at";
    @Autowired
    private UserTokenRepository userTokenRepository;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private EncryptionService encryptionService;
    private final RestClient restClient;
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    public TokenServiceImpl(RestClient.Builder restClient) {
        this.restClient = restClient.baseUrl("https://oauth2.googleapis.com").build();
    }

    @Override
    public String getAccessTokenFromUserId(Long userId) {
        log.debug("Retrieving access token for userId {}", userId);
        String accessToken = stringRedisTemplate.opsForValue().get(userId + KEY_SUFFIX);
        if (accessToken == null) {
            log.info("Access token cache miss for userId {}, refreshing access token", userId);
            UserToken userToken = userTokenRepository.findByUserId(userId);
            if (userToken == null) {
                throw new ResourceNotFoundException("User token not found.");
            }
            String refreshToken = encryptionService.decrypt(userToken.getRefreshToken());
            try {
                RefreshTokenResponseDto refreshTokenResponseDto = refreshAccessToken(refreshToken);
                accessToken = refreshTokenResponseDto.getAccess_token();
                userToken.setExpiry(Instant.now().plusSeconds(refreshTokenResponseDto.getRefresh_token_expires_in()));
                userTokenRepository.save(userToken);
                stringRedisTemplate.opsForValue().set(userId + KEY_SUFFIX, accessToken, Duration.ofSeconds(refreshTokenResponseDto.getExpires_in() - 60));

                log.info("Successfully refreshed access token for userId {}", userId);
            } catch (TokenRefreshException e) {
                userTokenRepository.deleteById(userToken.getId());
                log.warn("Removed stored refresh token for userId {} after refresh failure", userId);
                throw e;
            }
        } else {
            log.debug("Access token cache hit for userId {}", userId);
        }
        return accessToken;
    }

    private RefreshTokenResponseDto refreshAccessToken(String refreshToken) {
        log.info("Requesting new access token from Google");
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", refreshToken);
        map.add("grant_type", "refresh_token");

        try {
            RefreshTokenResponseDto response = this.restClient.post().uri("/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(map)
                    .retrieve()
                    .body(RefreshTokenResponseDto.class);
            log.info("Successfully refreshed Google access token");
            return response;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.warn("Google rejected the refresh token");
                throw new TokenRefreshException("Invalid refresh token.", e);
            } else throw new GoogleTokenException("Failed to refresh Google access token.", e);
        }
    }

    @Override
    public void saveTokens(User user, UserTokenDto userTokenDto) {
        log.debug("Saving OAuth tokens for userId {}", user.getId());
        UserToken userToken = userTokenRepository.findByUserId(user.getId());
        if (userToken == null) {
            log.info("Creating refresh token record for userId {}", user.getId());
            userToken = UserToken.builder().user(user).build();
        }
        String encryptedRefreshToken = encryptionService.encrypt(userTokenDto.getRefreshToken());
        userToken.setRefreshToken(encryptedRefreshToken);
        userTokenRepository.save(userToken);
        Duration ttl = Duration.between(Instant.now(), userTokenDto.getExpiresAt()).minusSeconds(60);
        stringRedisTemplate.opsForValue().set(user.getId() + KEY_SUFFIX, userTokenDto.getAccessToken(), ttl);
        log.info("Successfully stored OAuth2 tokens for userId {}", user.getId());
    }

    @Override
    public void deleteTokens(Long userId) {
        log.debug("Deleting OAuth2 tokens for userId {}", userId);
        stringRedisTemplate.delete(userId + KEY_SUFFIX);
        UserToken userToken = userTokenRepository.findByUserId(userId);
        if (userToken == null) {
            log.warn("Refresh token record does not exist for userId {}", userId);
            return;
        }
        String refreshToken = encryptionService.decrypt(userToken.getRefreshToken());

        revokeRefreshToken(refreshToken);
        userTokenRepository.delete(userToken);
        log.info("Successfully deleted OAuth2 tokens for userId {}", userId);
    }

    private void revokeRefreshToken(String refreshToken) {
        log.debug("Revoking Google refresh token");
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("token", refreshToken);
        try {
            this.restClient.post().uri("/revoke").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(map)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully revoked refresh token");
        } catch (RestClientResponseException e) {
            throw new GoogleTokenException("Failed to revoke Google refresh token.", e);
        }
    }
}
