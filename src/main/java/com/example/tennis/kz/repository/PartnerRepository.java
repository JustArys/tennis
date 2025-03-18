package com.example.tennis.kz.repository;

import com.example.tennis.kz.model.Partner;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerRepository extends JpaRepository<Partner, Long> {
    List<Partner> findByEnabled(boolean enabled);
    Page<Partner> findAllBy(Pageable pageable);

    Page<Partner> findAllByEnabled(Boolean enabled, Pageable pageable);
}
