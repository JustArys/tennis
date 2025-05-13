package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.Match; // Используй DTO в реальном проекте
import com.example.tennis.kz.service.BracketService;
import com.example.tennis.kz.service.MatchService;
import com.example.tennis.kz.service.PointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Если используешь Spring Security
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/tournaments")
@RequiredArgsConstructor
@Tag(name = "Admin Tournament Management", description = "Endpoints for managing tournament brackets, results, and points by admins")
@PreAuthorize("hasRole('ADMIN')")
public class TournamentAdminController {

    private final BracketService bracketService; // Прямая инъекция класса
    private final MatchService matchService;     // Прямая инъекция класса
    private final PointsService pointsService;   // Прямая инъекция класса

    @PostMapping("/{tournamentId}/generate-bracket")
    @Operation(summary = "Generate tournament bracket")
    public ResponseEntity<List<Match>> generateBracket(@PathVariable Long tournamentId) {
        // Возвращать List<Match> напрямую может быть тяжело для фронтенда и API.
        // Лучше использовать DTO (Data Transfer Objects).
        List<Match> bracket = bracketService.generateBracket(tournamentId);
        return ResponseEntity.ok(bracket); // Для демонстрации вернем Match, но DTO лучше
    }

    // DTO для ввода результата матча (можно определить как record внутри контроллера или в отдельном файле)
    public record MatchResultInputDto(Long winnerRegistrationId, String score) {}

    @PutMapping("/matches/{matchId}/result")
    @Operation(summary = "Record match result")
    public ResponseEntity<Match> recordMatchResult(@PathVariable Long matchId, @RequestBody MatchResultInputDto input) {
        Match updatedMatch = matchService.recordResult(matchId, input.winnerRegistrationId(), input.score());
        return ResponseEntity.ok(updatedMatch); // DTO здесь также предпочтительнее
    }

    // DTO для ввода walkover
    public record WalkoverInputDto(Long winnerRegistrationId) {}

    @PutMapping("/matches/{matchId}/walkover")
    @Operation(summary = "Record match walkover")
    public ResponseEntity<Match> recordMatchWalkover(@PathVariable Long matchId, @RequestBody WalkoverInputDto input) {
        Match updatedMatch = matchService.recordWalkover(matchId, input.winnerRegistrationId());
        return ResponseEntity.ok(updatedMatch); // DTO
    }

    @PostMapping("/{tournamentId}/calculate-points")
    @Operation(summary = "Calculate and award points for a finished tournament")
    public ResponseEntity<Void> calculateAndAwardPoints(@PathVariable Long tournamentId) {
        pointsService.calculateAndAwardPointsForTournament(tournamentId);
        return ResponseEntity.ok().build();
    }
}