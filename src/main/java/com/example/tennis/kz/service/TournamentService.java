package com.example.tennis.kz.service;

import com.example.tennis.kz.model.Tournament;
import com.example.tennis.kz.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class TournamentService {
    private final TournamentRepository tournamentRepository;

    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }

    public Tournament getTournamentById(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No tournament found with id " + id));
    }

    public Tournament createTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    public Tournament updateTournament(Long id, Tournament tournamentDetails) {
        return tournamentRepository.findById(id).map(tournament -> {
            tournament.setDescription(tournamentDetails.getDescription());
            tournament.setStartDate(tournamentDetails.getStartDate());
            tournament.setEndDate(tournamentDetails.getEndDate());
            tournament.setStartTime(tournamentDetails.getStartTime());
            tournament.setCategories(tournamentDetails.getCategories());
            tournament.setLocation(tournamentDetails.getLocation());
            tournament.setMinLevel(tournamentDetails.getMinLevel());
            tournament.setMaxLevel(tournamentDetails.getMaxLevel());
            tournament.setCost(tournamentDetails.getCost());
            return tournamentRepository.save(tournament);
        }).orElseThrow(() -> new NoSuchElementException("Tournament not found with id " + id));
    }


    public void deleteTournament(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No tournament found with id " + id));
        tournamentRepository.delete(tournament);
    }


}
