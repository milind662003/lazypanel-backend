package com.milind.lazypanel.services.interfaces;

import com.milind.lazypanel.dto.UserTokenDto;
import com.milind.lazypanel.models.User;

public interface ITokenService {
    String getAccessTokenFromUserId(Long userId);

    void saveToken(User user, UserTokenDto userTokenDto);
}
