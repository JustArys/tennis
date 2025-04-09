package com.example.tennis.kz.repository;

import com.example.tennis.kz.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findAllBy(Pageable pageable);
    @Query("SELECT u FROM User u JOIN u.userInfo ui " +
            "WHERE LOWER(ui.firstName) LIKE LOWER(CONCAT(:query, '%')) " +
            "OR LOWER(ui.lastName) LIKE LOWER(CONCAT(:query, '%'))")
    Page<User> searchByFirstNameOrLastNameStartsWith(
            @Param("query") String query,
            Pageable pageable
    );
}