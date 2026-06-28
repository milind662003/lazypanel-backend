package com.milind.lazypanel.services.implementations;

import com.milind.lazypanel.models.User;
import com.milind.lazypanel.repositories.UserRepository;
import com.milind.lazypanel.services.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

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
