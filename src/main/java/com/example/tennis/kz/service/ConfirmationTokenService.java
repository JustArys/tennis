package com.example.tennis.kz.service;
import com.example.tennis.kz.model.ConfirmationToken;
import com.example.tennis.kz.model.User;
import com.example.tennis.kz.repository.ConfirmationTokenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.UUID;

@Service
public class ConfirmationTokenService {
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final EmailService emailService;

    public ConfirmationTokenService(ConfirmationTokenRepository confirmationTokenRepository, EmailService emailService) {
        this.confirmationTokenRepository = confirmationTokenRepository;
        this.emailService = emailService;
    }

    public ConfirmationToken saveConfirmationToken(User user) {
        ConfirmationToken confirmationToken = ConfirmationToken.builder()
                .user(user)
                .confirmationToken(UUID.randomUUID().toString())
                .expirationDate(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 2))
                .build();
        confirmationTokenRepository.save(confirmationToken);
        return confirmationToken;
    }

    public ResponseEntity<?> sendConfirmationToken(User user) {
        ConfirmationToken confirmationToken = saveConfirmationToken(user);
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject("Complete Registration!");
        mailMessage.setText("To confirm your account, please click here : "
                + "https://tennis-p30s.onrender.com/api/v1/user/confirmemail?token=" + confirmationToken.getConfirmationToken());
        emailService.sendEmail(mailMessage);
        return ResponseEntity.ok("confirmation token sent");
    }


}