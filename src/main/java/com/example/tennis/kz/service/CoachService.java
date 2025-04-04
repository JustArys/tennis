package com.example.tennis.kz.service;

import com.example.tennis.kz.model.City;
import com.example.tennis.kz.model.Coach;
import com.example.tennis.kz.model.Language;
import com.example.tennis.kz.model.User;
import com.example.tennis.kz.model.request.CoachRequest;
import com.example.tennis.kz.repository.CoachRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CoachService {
    private final CoachRepository coachRepository;

    public Coach getCoachById(Long id) {
        return coachRepository.findById(id).orElseThrow(()
                -> new NoSuchElementException(String.format("No Coach found with '%d'", id)));
    }

    public List<Coach> getAllCoaches(Boolean enabled) {
        return coachRepository.findByEnabledOrderByCostAsc(enabled);
    }

    public List<Coach> getAllCoaches() {
        return coachRepository.findAll();
    }
    public Page<Coach> getAllCoaches(Pageable pageable, Boolean enabled) {
        return coachRepository.findAllByEnabled(pageable, enabled);
    }
    public Coach enableCoach(Long id){
        Coach coach = getCoachById(id);
        coach.setEnabled(true);
        return coachRepository.save(coach);
    }

    public Coach addCoach(User user, CoachRequest coach) {
        var newCoach = Coach.builder()
                .enabled(false)
                .city(coach.getCity())
                .languages(coach.getLanguage())
                .cost(coach.getCost())
                .services(coach.getCoachServices())
                .description(coach.getDescription())
                .experience(coach.getExperience())
                .stadium(coach.getStadium())
                .user(user.getUserInfo())
                .build();
        return coachRepository.save(newCoach);
    }

    public Coach updateCoachParams(Long id,
                                   City city,
                                   Set<Language> languages,
                                   Float cost,
                                   Set<com.example.tennis.kz.model.CoachService> coachServices,
                                   String description,
                                   Integer experience,
                                   String stadium) {
        Coach coach = coachRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Coach not found with id " + id));

        if (city != null) {
            coach.setCity(city);
        }
        if (languages != null) {
            coach.setLanguages(languages);
        }
        if (cost != null) {
            coach.setCost(cost);
        }
        if (coachServices != null) {
            coach.setServices(coachServices);
        }
        if (description != null) {
            coach.setDescription(description);
        }
        if (experience != null) {
            coach.setExperience(experience);
        }
        if (stadium != null) {
            coach.setStadium(stadium);
        }
        return coachRepository.save(coach);
    }

    public void deleteCoach(Long id) {
        coachRepository.deleteById(id);
    }
}
