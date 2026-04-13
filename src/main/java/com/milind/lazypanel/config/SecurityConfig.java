package com.milind.lazypanel.config;

import com.milind.lazypanel.filtler.JwtFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtFilter jwtFilter;
    @Autowired
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        httpSecurity
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(r-> r
                .requestMatchers("/").permitAll()
                .anyRequest().authenticated()).oauth2Login(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2.failureHandler(((request, response, exception) -> {
                            log.error("Oauth2 error: {}", exception.getMessage());
                        })
                ).successHandler(oAuth2SuccessHandler)
                );
        return httpSecurity.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }
}


//login through Google, this is what generates the jwt to access all the BS stored against the user email,
//but the intention here is to use Google's api to change stuff in docs (excel).
//but we also want some stuff like sticky notes and a movie list that we might store, so for this use case using
//Google login becomes necessary (otherwise username/password is just fine but since gmail is going to access
//the docs anyway we might as well use it to store stuff rather than also integrating username/password auth)

//Next step: Implement google login, can use it for all things we'll be storing against it, google keep exists
//so let's try to make something different (not right now necessarily, maybe in the future, like a scheduler)
//keep the sticky notes/movie list simple for now
//The ui gets the auth code from Google gives it to us, and we slap a jwt back after verifying through Google
//if this is a real user (we get all the details actually)

//I realised this is using Oauth2 but not the way it's above, food for thought, btw not a big deal only need to
//copy dependencies from dashboard

//Upcoming scope: Use the Google stuff to access docs