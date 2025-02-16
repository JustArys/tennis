package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.Tournament;
import com.example.tennis.kz.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTournament(@PathVariable Long id, @RequestBody Tournament tournament) {
        return ResponseEntity.ok(tournamentService.updateTournament(id, tournament));
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
    public ResponseEntity<?> findTournamentByPage(@RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(tournamentService.findAllTournaments(page));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
        return ResponseEntity.ok().build();
    }
}
