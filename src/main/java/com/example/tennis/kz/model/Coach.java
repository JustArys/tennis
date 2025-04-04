package com.example.tennis.kz.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private String description;
    private Integer experience;
    private String stadium;

    @ElementCollection(targetClass = CoachService.class, fetch = FetchType.EAGER) // EAGER, если сервисы нужны часто
    @CollectionTable(name = "coach_services", joinColumns = @JoinColumn(name = "coach_id"))
    @Enumerated(EnumType.STRING) // Храним enum как строки в БД
    @Column(name = "service_name", nullable = false) // Имя колонки для enum в таблице coach_services
    @Singular("service") // Lombok: Позволяет добавлять элементы по одному в Builder (coachBuilder.service(CoachService.SPARRING_PARTNER))
    @Schema(description = "Набор предоставляемых тренером услуг.", // Опциональное описание для Swagger
            example = "[\"CHILDREN_TRAINING\"]")
    private Set<CoachService> services = new HashSet<>();

    @CreationTimestamp
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
