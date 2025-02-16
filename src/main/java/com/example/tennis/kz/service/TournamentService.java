package com.example.tennis.kz.service;

import com.example.tennis.kz.model.RegistrationStatus;
import com.example.tennis.kz.model.Tournament;
import com.example.tennis.kz.model.TournamentRegistration;
import com.example.tennis.kz.model.User;
import com.example.tennis.kz.repository.TournamentRegistrationRepository;
import com.example.tennis.kz.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TournamentService {
    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;

    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }

    public Tournament getTournamentById(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No tournament found with id " + id));
    }

    public Page<Tournament> findAllTournaments(int page) {
        Pageable pageable = PageRequest.of(page,10);
        return tournamentRepository.findAll(pageable);
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
            tournament.setCategory(tournament.getCategory());
            tournament.setMaxParticipants(tournamentDetails.getMaxParticipants());
            tournament.setLocation(tournamentDetails.getLocation());
            tournament.setMinLevel(tournamentDetails.getMinLevel());
            tournament.setMaxLevel(tournamentDetails.getMaxLevel());
            tournament.setCost(tournamentDetails.getCost());
            return tournamentRepository.save(tournament);
        }).orElseThrow(() -> new NoSuchElementException("Tournament not found with id " + id));
    }

    public Set<List<User>> getAllParticipants(Long id) {
        var tournament = getTournamentById(id);

        List<TournamentRegistration> regs = registrationRepository.findByTournamentIdAndStatus(
                id, RegistrationStatus.REGISTERED
        );

        Set<List<User>> participants = new HashSet<>();

        for (TournamentRegistration reg : regs) {
            // Для каждой регистрации создаём новый список
            List<User> team = new ArrayList<>();

            // Добавляем основного игрока (user)
            team.add(reg.getUser());

            // Если есть партнёр, добавляем и его
            if (reg.getPartner() != null) {
                team.add(reg.getPartner());
            }

            // Добавляем отдельный список (команду) в набор
            participants.add(team);
        }

        return participants;
    }


    public void deleteTournament(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No tournament found with id " + id));
        tournamentRepository.delete(tournament);
    }


}
