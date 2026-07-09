package com.milind.lazypanel.services.implementations;

import com.milind.lazypanel.dto.RefreshTokenResponseDto;
import com.milind.lazypanel.dto.UserTokenDto;
import com.milind.lazypanel.models.User;
import com.milind.lazypanel.models.UserToken;
import com.milind.lazypanel.repositories.UserTokenRepository;
import com.milind.lazypanel.services.interfaces.EncryptionService;
import com.milind.lazypanel.services.interfaces.ITokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService implements ITokenService {
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

    public TokenService(RestClient.Builder restClient) {
        this.restClient = restClient.baseUrl("https://oauth2.googleapis.com/token").build();
    }

    @Override
    public String getAccessTokenFromUserId(Long userId) {
        String accessToken = stringRedisTemplate.opsForValue().get(userId + "_at");
        if (accessToken == null) {
            UserToken userToken = userTokenRepository.findByUserId(userId);
            String refreshToken = encryptionService.decrypt(userToken.getRefreshToken());

            RefreshTokenResponseDto refreshTokenResponseDto = refreshAccessToken(refreshToken);
            accessToken = refreshTokenResponseDto.getAccess_token();
            userToken.setExpiry(Instant.now().plusSeconds(refreshTokenResponseDto.getRefresh_token_expires_in()));
            userTokenRepository.save(userToken);
            stringRedisTemplate.opsForValue().set(userId + "_at", accessToken, Duration.ofSeconds(refreshTokenResponseDto.getExpires_in() - 60));
        }
        return accessToken;
    }

    private RefreshTokenResponseDto refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", refreshToken);
        map.add("grant_type", "refresh_token");

        return this.restClient.post().contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(map)
                .retrieve()
                .body(RefreshTokenResponseDto.class);
    }

    @Override
    public void saveTokens(User user, UserTokenDto userTokenDto) {
        UserToken userToken = userTokenRepository.findByUserId(user.getId());
        if (userToken == null) {
            userToken = UserToken.builder().user(user).build();
        }
        String encryptedRefreshToken = encryptionService.encrypt(userTokenDto.getRefreshToken());
        userToken.setRefreshToken(encryptedRefreshToken);
        userTokenRepository.save(userToken);
        Duration ttl = Duration.between(Instant.now(), userTokenDto.getExpiresAt()).minusSeconds(60);
        ;
        stringRedisTemplate.opsForValue().set(user.getId() + "_at", userTokenDto.getAccessToken(), ttl);
    }
}
