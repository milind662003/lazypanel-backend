package com.milind.lazypanel.services.implementations;

import com.milind.lazypanel.dto.UserTokenDto;
import com.milind.lazypanel.models.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserService userService;
    private final JwtService jwtService;
    private final TokenService tokenService;

    @Transactional
    public String authenticate(OAuth2User oAuth2User, UserTokenDto userTokenDto) {
        String email = oAuth2User.getAttribute("email");
        log.info("Authenticating user {}", email);
        User user = userService.getUserByUsername(email);

        if (user == null) {
            log.info("Creating new user account for {}", email);
            String firstName = oAuth2User.getAttribute("given_name");
            String lastName = oAuth2User.getAttribute("family_name");
            user = userService.signUpNewUser(email, firstName, lastName);
        }
        tokenService.saveTokens(user, userTokenDto);
        log.info("Successfully authenticated userId {}", user.getId());
        return jwtService.generateToken(user.getUsername());
    }

    public void logout(Long userId) {
        log.info("Logging out userId {}", userId);
        tokenService.deleteTokens(userId);
    }
}
