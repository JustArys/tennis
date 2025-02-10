package com.example.tennis.kz.service;

import com.example.tennis.kz.model.Coach;
import com.example.tennis.kz.model.User;
import com.example.tennis.kz.model.request.CoachRequest;
import com.example.tennis.kz.repository.CoachRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

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


    public Coach enableCoach(Long id){
        Coach coach = getCoachById(id);
        coach.setEnabled(true);
        return coachRepository.save(coach);
    }

    public Coach addCoach(User user, CoachRequest coach) {
        var newCoach = Coach.builder()
                .enabled(false)
                .city(coach.getCity())
                .language(coach.getLanguage())
                .cost(coach.getCost())
                .service(coach.getService())
                .description(coach.getDescription())
                .experience(coach.getExperience())
                .stadium(coach.getStadium())
                .user(user.getUserInfo())
                .createdAt(LocalDateTime.now())
                .build();
        return coachRepository.save(newCoach);
    }

    public Coach updateCoach(Long id,CoachRequest coach) {
        return coachRepository.findById(id).map(coachNew -> {
            coachNew.setCity(coach.getCity());
            coachNew.setDescription(coach.getDescription());
            coachNew.setService(coach.getService());
            coachNew.setCost(coach.getCost());
            coachNew.setEnabled(true);
            coachNew.setLanguage(coach.getLanguage());
            coachNew.setStadium(coach.getStadium());
            coachNew.setExperience(coach.getExperience());
            return coachRepository.save(coachNew);
        }).orElseThrow(() -> new NoSuchElementException(String.format("No Coach found with '%d'", id)));
    }

    public void deleteCoach(Long id) {
        coachRepository.deleteById(id);
    }
}
