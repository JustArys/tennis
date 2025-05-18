package com.example.tennis.kz.model.response;

import com.example.tennis.kz.model.MatchStatus; // Убедитесь, что enum MatchStatus доступен
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchDto {
    private Long id;
    private Integer roundNumber;
    private String roundName; // Будет вычисляться
    private Integer matchNumberInBracket;

    // Информация об участнике 1
    private Long participant1RegistrationId; // ID из TournamentRegistration
    private String participant1Name;         // Имя из TournamentRegistration.getParticipantName()
    private Integer participant1SeedNumber;  // Номер посева из TournamentRegistration

    // Информация об участнике 2
    private Long participant2RegistrationId;
    private String participant2Name;
    private Integer participant2SeedNumber;

    // Информация о победителе
    private Long winnerRegistrationId;
    private String winnerName;

    private String score;
    private MatchStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedTime;

    private Long nextMatchId;     // ID следующего матча (если есть)
    private Integer nextMatchSlot; // Слот в следующем матче (1 или 2)

    private Long participant1Player1UserId; // ID основного игрока (User)
    private Long participant1Player2UserId; // ID партнера (User), если есть

    // Для участника 2
    private Long participant2Player1UserId;
    private Long participant2Player2UserId;
}