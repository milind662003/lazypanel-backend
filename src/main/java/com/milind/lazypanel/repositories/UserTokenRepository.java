package com.milind.lazypanel.repositories;

import com.milind.lazypanel.models.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    UserToken findByUserId(Long userId);
}
