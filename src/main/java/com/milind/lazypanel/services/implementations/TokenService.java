package com.milind.lazypanel.services.implementations;

import com.milind.lazypanel.models.User;
import com.milind.lazypanel.models.UserToken;
import com.milind.lazypanel.repositories.UserRepository;
import com.milind.lazypanel.repositories.UserTokenRepository;
import com.milind.lazypanel.services.interfaces.ITokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TokenService implements ITokenService {

    @Autowired
    private UserTokenRepository userTokenRepository;
    @Override
    public String getAccessTokenFromUserId(Long userId) {
        UserToken userToken = userTokenRepository.findByUserId(userId);
        return userToken.getAccessToken();
    }
}
