package com.milind.lazypanel.services.interfaces;

import com.milind.lazypanel.dto.UserTokenDto;
import com.milind.lazypanel.models.User;

public interface ITokenService {
    String getAccessTokenFromUserId(Long userId);

    void saveTokens(User user, UserTokenDto userTokenDto);

    void deleteTokens(Long userId);
}
