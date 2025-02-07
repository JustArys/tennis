package com.example.tennis.kz.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachRequest {
    private String city;
    private String language;
    private Float cost;
    private String service;
    private String description;
    private Integer experience;
    private String stadium;
}
