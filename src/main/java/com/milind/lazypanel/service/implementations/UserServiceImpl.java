package com.milind.lazypanel.service.implementations;

import com.milind.lazypanel.model.User;
import com.milind.lazypanel.repository.UserRepository;
import com.milind.lazypanel.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getUserByUsername(String email) {
        return userRepository.findByUsername(email);
    }

    @Override
    public User signUpNewUser(String email, String firstName, String lastName) {
        return userRepository.save(User.builder()
                .username(email)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .build());
    }
}
