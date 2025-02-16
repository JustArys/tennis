package com.example.tennis.kz.model.request;

import lombok.Data;

@Data
public class RegistrationRequest {
    private Long tournamentId;
    private Long userId;
    private Long partnerId;
}
