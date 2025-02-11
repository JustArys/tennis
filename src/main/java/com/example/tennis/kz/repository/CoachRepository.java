package com.example.tennis.kz.repository;

import com.example.tennis.kz.model.Coach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachRepository extends JpaRepository<Coach, Long> {
    List<Coach> findByEnabledOrderByCostAsc(Boolean enabled);
    Page<Coach> findAllBy(Pageable pageable);
}
