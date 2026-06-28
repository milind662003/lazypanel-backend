package com.milind.lazypanel.services.implementations;

import com.milind.lazypanel.dto.UserTokenDto;
import com.milind.lazypanel.models.User;
import com.milind.lazypanel.models.UserToken;
import com.milind.lazypanel.repositories.UserTokenRepository;
import com.milind.lazypanel.services.interfaces.ITokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService implements ITokenService {

    private final UserTokenRepository userTokenRepository;

    private final StringRedisTemplate stringRedisTemplate;
    @Override
    public String getAccessTokenFromUserId(Long userId) {
        String token = stringRedisTemplate.opsForValue().get(userId+"_at");
        if(token != null) {
            return token;
        }
        //need to add refresh token logic here
        return "";
    }

    @Override
    public void saveToken(User user, UserTokenDto userTokenDto) {
        //need to add token encryption here
        userTokenRepository.save(UserToken.builder().refreshToken(userTokenDto.getRefreshToken()).build());
        Duration ttl = Duration.between(Instant.now(), userTokenDto.getExpiresAt());
        stringRedisTemplate.opsForValue().set(user.getId()+"_at", userTokenDto.getAccessToken(), ttl);
    }
}
