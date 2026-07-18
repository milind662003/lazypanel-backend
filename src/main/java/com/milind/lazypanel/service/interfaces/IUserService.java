package com.milind.lazypanel.service.interfaces;

import com.milind.lazypanel.model.User;

public interface IUserService {

    User getUserByUsername(String email);

    User signUpNewUser(String email, String firstName, String lastName);
}
