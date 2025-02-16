package com.example.tennis.kz.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Tournament {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Id
    @Column(name = "tournament_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String description;

    @JsonFormat(pattern="yyyy-MM-dd")
    @Temporal(TemporalType.DATE)
    private LocalDate startDate;

    @JsonFormat(pattern="yyyy-MM-dd")
    @Temporal(TemporalType.DATE)
    private LocalDate endDate;

    @JsonFormat(pattern = "HH:mm")
    @Temporal(TemporalType.TIME)
    @Schema(type = "string", example = "14:30")
    private LocalTime startTime;


    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Category category;
    private int maxParticipants;
    private String location;
    @Enumerated(EnumType.STRING)
    private City city;
    private float minLevel;
    private float maxLevel;
    private int cost;
    @JsonIgnore
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentRegistration> registrations = new ArrayList<>();
/*
    @JsonIgnore
    @OneToMany
    private ArrayList<User> users = new ArrayList<>();*/
}
