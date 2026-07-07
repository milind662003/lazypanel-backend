package com.milind.lazypanel.config;

import com.milind.lazypanel.dto.UserTokenDto;
import com.milind.lazypanel.services.implementations.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        OAuth2AuthorizedClient client = oAuth2AuthorizedClientService.loadAuthorizedClient(
                token.getAuthorizedClientRegistrationId(), token.getName());
        String accessToken = client.getAccessToken().getTokenValue();
        Instant expiry = client.getAccessToken().getExpiresAt();
        String refreshToken = client.getRefreshToken().getTokenValue();
        UserTokenDto userTokenDto = new UserTokenDto(accessToken, refreshToken, expiry);
        String jwt = authService.authenticate(oAuth2User, userTokenDto);

        ResponseCookie responseCookie = ResponseCookie.from("access_token", jwt)
                .httpOnly(true).secure(false).path("/").sameSite("Lax").maxAge(Duration.ofDays(7)).build();

        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());  
        response.sendRedirect("http://localhost:3000/");
    }
}
