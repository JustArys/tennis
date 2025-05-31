package com.example.tennis.kz.repository;

import com.example.tennis.kz.model.UserInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    Page<UserInfo> findAllByPointsIsNotNull(Pageable pageable);
}
