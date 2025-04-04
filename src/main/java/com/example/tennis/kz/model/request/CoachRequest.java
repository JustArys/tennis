package com.example.tennis.kz.model.request;

import com.example.tennis.kz.model.City;
import com.example.tennis.kz.model.CoachService;
import com.example.tennis.kz.model.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachRequest {
    private City city;
    private Set<Language> language;
    private Float cost;
    private Set<CoachService> coachServices;
    private String description;
    private Integer experience;
    private String stadium;
}
