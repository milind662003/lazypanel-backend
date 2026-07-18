package com.milind.lazypanel.repository;

import com.milind.lazypanel.model.UserSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SheetRepository extends JpaRepository<UserSheet, Long> {

    UserSheet findByUserId(Long userId);
}
