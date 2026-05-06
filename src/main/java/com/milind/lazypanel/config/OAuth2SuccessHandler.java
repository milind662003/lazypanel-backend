package com.milind.lazypanel.config;

import com.milind.lazypanel.dto.LoginResponseDto;
import com.milind.lazypanel.dto.UserTokenDto;
import com.milind.lazypanel.services.implementations.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private AuthService authService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        OAuth2AuthorizedClient client = oAuth2AuthorizedClientService.loadAuthorizedClient(
                token.getAuthorizedClientRegistrationId(), token.getName());
        String accessToken = client.getAccessToken().getTokenValue();
//        String refreshToken = client.getRefreshToken().getTokenValue();
        Instant expiry = client.getAccessToken().getExpiresAt();
        UserTokenDto userTokenDto = new UserTokenDto(accessToken, "", expiry);
        ResponseEntity<LoginResponseDto> loginResponse = authService.handleLogin(oAuth2User, userTokenDto);

        response.setStatus(loginResponse.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(loginResponse.getBody()));
    }
}
