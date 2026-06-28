package com.milind.lazypanel.services.implementations;

import com.milind.lazypanel.dto.UserTokenDto;
import com.milind.lazypanel.models.User;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private TokenService tokenService;

    @Transactional
    public String authenticate(OAuth2User oAuth2User, UserTokenDto userTokenDto) {
        String email = oAuth2User.getAttribute("email");
        User user = userService.getUserByUsername(email);

        if(user == null) {
            String firstName = oAuth2User.getAttribute("given_name");
            String lastName = oAuth2User.getAttribute("family_name");
            user = userService.signUpNewUser(email, firstName, lastName);
        }
        tokenService.saveToken(user, userTokenDto);
        return jwtService.generateToken(user.getUsername());
    }
}
