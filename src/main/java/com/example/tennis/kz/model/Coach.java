package com.example.tennis.kz.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean enabled;
    @Enumerated(EnumType.STRING)
    private City city;

    @ElementCollection
    @CollectionTable(name = "coach_languages", joinColumns = @JoinColumn(name = "coach_id"))
    @Column(name = "language")
    private Set<Language> languages;
    private Float cost;
    private String service;
    private String description;
    private Integer experience;
    private String stadium;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
