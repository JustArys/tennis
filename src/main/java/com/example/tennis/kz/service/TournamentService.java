package com.example.tennis.kz.service;

import com.example.tennis.kz.model.*;
import com.example.tennis.kz.repository.TournamentRegistrationRepository;
import com.example.tennis.kz.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import specification.TournamentSpecification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    public Page<Tournament> findAllTournaments(Pageable pageable) {
        return tournamentRepository.findAll(pageable);
    }

    public Tournament createTournament(Tournament tournament, User user) {
        tournament.setAuthor(user);
        return tournamentRepository.save(tournament);
    }

    public Tournament updateTournamentParams(
            Long id,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            Category category,
            Integer maxParticipants,
            String location,
            Float minLevel,
            Float maxLevel,
            Integer cost) {

        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tournament not found with id " + id));

        if (description != null) {
            tournament.setDescription(description);
        }
        if (startDate != null) {
            tournament.setStartDate(startDate);
        }
        if (endDate != null) {
            tournament.setEndDate(endDate);
        }
        if (startTime != null) {
            tournament.setStartTime(startTime);
        }
        if (category != null) {
            tournament.setCategory(category);
        }
        if (maxParticipants != null) {
            tournament.setMaxParticipants(maxParticipants);
        }
        if (location != null) {
            tournament.setLocation(location);
        }
        if (minLevel != null) {
            tournament.setMinLevel(minLevel);
        }
        if (maxLevel != null) {
            tournament.setMaxLevel(maxLevel);
        }
        if (cost != null) {
            tournament.setCost(cost);
        }

        return tournamentRepository.save(tournament);
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


    public List<Tournament> filterTournaments(String location, Category category,
                                              Float minLevel, Float maxLevel,
                                              LocalDate startDate, LocalDate endDate) {

        Specification<Tournament> spec = Specification
                .where(TournamentSpecification.hasLocation(location))
                .and(TournamentSpecification.hasCategory(category))
                .and(TournamentSpecification.hasMinLevel(minLevel))
                .and(TournamentSpecification.hasMaxLevel(maxLevel))
                .and(TournamentSpecification.isBetweenDates(startDate, endDate));

        return tournamentRepository.findAll(spec);
    }

}
