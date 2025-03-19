package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.Category;
import com.example.tennis.kz.model.Tournament;
import com.example.tennis.kz.model.response.CustomPageResponse;
import com.example.tennis.kz.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/v1/tournament")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

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
            @RequestParam(required = false) Integer maxParticipants,
             @RequestParam(required = false) String location,
            @RequestParam(required = false) Float minLevel,
            @RequestParam(required = false) Float maxLevel,
            @RequestParam(required = false) Integer cost) {

        Tournament updatedTournament = tournamentService.updateTournamentParams(
                id, description, startDate, endDate, startTime,
                category, maxParticipants, location, minLevel, maxLevel, cost
        );
        return ResponseEntity.ok(updatedTournament);
    }

    @PostMapping
    public Tournament createTournament(@RequestBody Tournament tournament) {
        return tournamentService.createTournament(tournament);
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<?> getAllParticipants(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentService.getAllParticipants(id));
    }

    @GetMapping("/page")
    public ResponseEntity<?> findTournamentByPage(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page-1, size, Sort.by(Sort.Order.desc("createdAt")));

        Page<Tournament> tournaments = tournamentService.findAllTournaments(pageable);
        return ResponseEntity.ok(new CustomPageResponse<>(tournaments.getNumber() + 1, tournaments.getSize(), tournaments.getTotalElements(), tournaments.getContent()));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
        return ResponseEntity.ok().build();
    }
}
