package com.example.tennis.kz.model.request;

import com.example.tennis.kz.model.City;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachRequest {
    private City city;
    private String language;
    private Float cost;
    private String service;
    private String description;
    private Integer experience;
    private String stadium;
}
