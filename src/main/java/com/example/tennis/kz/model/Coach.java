package com.example.tennis.kz.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
@Builder
public class Coach {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Id
    @Column(name = "coach_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;


    @OneToOne
    @JoinColumn(name = "user_id")
    private UserInfo user;

    private Boolean enabled;
    private String city;
    private String language;
    private Float cost;
    private String service;
    private String description;
    private Integer experience;
    private String stadium;

}
