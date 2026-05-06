package com.milind.lazypanel.services.implementations;

import com.milind.lazypanel.models.User;
import com.milind.lazypanel.repositories.UserRepository;
import com.milind.lazypanel.services.interfaces.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements IUserService {
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;

    @Override
    public Long getUserIdFromJwt(String jwt) {
        try {
            String username = jwtService.extractUserName(jwt);
            User user = userRepository.findByUsername(username);
            return user.getId();
        } catch (Exception e) {
            throw new UsernameNotFoundException("User does not exist", e);
        }
    }
}
