package com.milind.lazypanel.service.interfaces;

import com.milind.lazypanel.dto.UserTokenDto;
import com.milind.lazypanel.model.User;

public interface ITokenService {
    String getAccessTokenFromUserId(Long userId);

    void saveTokens(User user, UserTokenDto userTokenDto);

    void deleteTokens(Long userId);
}
