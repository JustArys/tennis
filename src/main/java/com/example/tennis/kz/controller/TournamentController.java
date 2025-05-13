package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.Category; // Оставляем для PATCH
import com.example.tennis.kz.model.Match;
import com.example.tennis.kz.model.Tournament;
import com.example.tennis.kz.model.User;
import com.example.tennis.kz.model.request.TournamentCreationRequestDTO;
import com.example.tennis.kz.model.response.CustomPageResponse;
import com.example.tennis.kz.service.TournamentQueryService;
import com.example.tennis.kz.service.TournamentService;
import com.example.tennis.kz.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid; // Для валидации DTO
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tournament") // Оставим прежний путь для совместимости
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final UserService userService; // Для получения аутентифицированного пользователя
    private final TournamentQueryService tournamentQueryService;

    @GetMapping
    public ResponseEntity<?> getAllTournaments() {
        return ResponseEntity.ok(tournamentService.getAllTournaments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findTournamentById(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentService.getTournamentById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateTournamentFields(
            @PathVariable Long id,
            @RequestParam(required = false) String description,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Integer maxParticipants, // Это для PATCH, где может меняться tier
            @RequestParam(required = false) String location,
            // Добавим city, если его тоже можно обновлять через PATCH
            @RequestParam(required = false) com.example.tennis.kz.model.City city,
            @RequestParam(required = false) Float minLevel,
            @RequestParam(required = false) Float maxLevel,
            @RequestParam(required = false) Integer cost) {

        Tournament updatedTournament = tournamentService.updateTournamentParams(
                id, description, startDate, endDate, startTime,
                category, maxParticipants, location, minLevel, maxLevel, cost
                // city нужно будет добавить в сигнатуру tournamentService.updateTournamentParams, если оно обновляемо
        );
        return ResponseEntity.ok(updatedTournament); // Рекомендуется возвращать DTO и здесь
    }

    // --- ИЗМЕНЕННЫЙ МЕТОД СОЗДАНИЯ ---
    @PostMapping
    @Operation(summary = "Create a new tournament")
    public ResponseEntity<Tournament> createTournament(@Valid @RequestBody TournamentCreationRequestDTO tournamentDto) {
        User authenticatedUser = userService.getAuthenticatedUser();
        Tournament createdTournament = tournamentService.createTournamentFromDto(tournamentDto, authenticatedUser);
        return new ResponseEntity<>(createdTournament, HttpStatus.CREATED);
    }
    // --- КОНЕЦ ИЗМЕНЕНИЙ ---

    @GetMapping("/{id}/participants")
    public ResponseEntity<?> getAllParticipants(@PathVariable Long id) {
        // Здесь тоже лучше возвращать List<ParticipantDTO>
        return ResponseEntity.ok(tournamentService.getAllParticipants(id));
    }

    @GetMapping("/page")
    public ResponseEntity<?> findTournamentByPage(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page-1, size, Sort.by(Sort.Order.desc("createdAt")));
        // Здесь тоже лучше возвращать Page<TournamentSummaryDTO>
        Page<Tournament> tournaments = tournamentService.findAllTournaments(pageable);
        return ResponseEntity.ok(new CustomPageResponse<>(tournaments.getNumber() + 1, tournaments.getSize(), tournaments.getTotalElements(), tournaments.getContent()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build(); // Стандартный ответ для DELETE
    }

    @GetMapping("/{tournamentId}/bracket")
    @Operation(summary = "Get tournament bracket data")
    public ResponseEntity<List<Match>> getTournamentBracket(@PathVariable Long tournamentId) {
        // Здесь тоже лучше возвращать List<MatchDTO>
        List<Match> bracketData = tournamentQueryService.getTournamentBracket(tournamentId);
        return ResponseEntity.ok(bracketData);
    }
}