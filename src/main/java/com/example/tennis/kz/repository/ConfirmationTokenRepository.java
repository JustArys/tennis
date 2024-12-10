package com.example.tennis.kz.repository;

import com.example.tennis.kz.model.ConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.LinkedList;
import java.util.Optional;

@Repository
public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {
    Optional<ConfirmationToken> findConfirmationTokenByConfirmationToken(String confirmationToken);
    LinkedList<ConfirmationToken> findAllByExpirationDateBefore(Date date);

}