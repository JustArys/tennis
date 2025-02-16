package com.example.tennis.kz.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class Partner {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Id
    @Column(name = "partner_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;


    private String phone;
    private String firstName;
    private String lastName;
    private Float rating;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean enabled;
    @Enumerated(EnumType.STRING)
    private City city;
    private String stadium;
    private String description;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
