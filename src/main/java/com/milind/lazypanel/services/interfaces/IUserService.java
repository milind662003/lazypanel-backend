package com.milind.lazypanel.services.interfaces;

import com.milind.lazypanel.models.User;

public interface IUserService {

    User getUserByUsername(String email);

    User signUpNewUser(String email, String firstName, String lastName);
}
