package com.example.tennis.kz.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Tournament {
    @JsonIgnore
    @Id
    @Column(name = "tournament_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String description;


}
