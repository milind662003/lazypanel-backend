package com.milind.lazypanel.repositories;

import com.milind.lazypanel.models.UserSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SheetRepository extends JpaRepository<UserSheet, Long> {

    UserSheet findByUserId(Long userId);
}
