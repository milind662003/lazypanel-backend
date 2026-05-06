package com.milind.lazypanel.services.implementations;

import com.milind.lazypanel.dto.LoginResponseDto;
import com.milind.lazypanel.dto.SignUpRequestDto;
import com.milind.lazypanel.dto.UserTokenDto;
import com.milind.lazypanel.models.User;
import com.milind.lazypanel.models.UserSheet;
import com.milind.lazypanel.models.UserToken;
import com.milind.lazypanel.repositories.SheetRepository;
import com.milind.lazypanel.repositories.UserRepository;
import com.milind.lazypanel.repositories.UserTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserTokenRepository userTokenRepository;

    @Autowired
    private SheetRepository sheetRepository;

    @Transactional
    public ResponseEntity<LoginResponseDto> handleLogin(OAuth2User oAuth2User, UserTokenDto userTokenDto) {
        String email = oAuth2User.getAttribute("email");
        User user = userRepository.findByUsername(email);

        if(user == null) {
            String firstName = oAuth2User.getAttribute("given_name");
            String lastName = oAuth2User.getAttribute("family_name");
            user = signUp(new SignUpRequestDto(email, firstName, lastName));
        }
        LoginResponseDto loginResponse =  new LoginResponseDto(jwtService.generateToken(user.getUsername()),
                user.getUsername(), "Login Successful");
        userTokenRepository.save(UserToken.builder()
                .accessToken(userTokenDto.getAccessToken())
                .expiresAt(userTokenDto.getExpiresAt())
                .user(user)
                .build());

        return ResponseEntity.ok(loginResponse);
    }

    private User signUp(SignUpRequestDto signUpRequestDto) {
        return userRepository.save(User.builder()
                .username(signUpRequestDto.getUsername())
                .email(signUpRequestDto.getUsername())
                .firstName(signUpRequestDto.getFirstName())
                .lastName(signUpRequestDto.getLastName())
                .build());
    }
}
