package com.example.tennis.kz.model.request;

import com.example.tennis.kz.model.Category;
import com.example.tennis.kz.model.City;
import com.example.tennis.kz.model.TournamentTier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.DecimalMin;
import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentCreationRequestDTO {

    @NotBlank(message = "Description cannot be blank")
    private String description;

    @NotNull(message = "Start date cannot be null")
    @FutureOrPresent(message = "Start date must be in the present or future")
    private LocalDate startDate;

    @NotNull(message = "End date cannot be null")
    @FutureOrPresent(message = "End date must be in the present or future")
    private LocalDate endDate;
    // Можно добавить кастомную валидацию @AssertTrue, что endDate >= startDate

    @NotNull(message = "Start time cannot be null")
    @Schema(type = "string", example = "14:30")
    private LocalTime startTime;

    @NotNull(message = "Tournament tier cannot be null")
    private TournamentTier tier;

    @NotNull(message = "Tournament category cannot be null")
    private Category category;

    @NotBlank(message = "Location cannot be blank")
    private String location;

    @NotNull(message = "City cannot be null")
    private City city;

    @NotNull(message = "Minimum level cannot be null")
    @DecimalMin(value = "0.0", message = "Minimum level must be non-negative")
    private Float minLevel;

    @NotNull(message = "Maximum level cannot be null")
    @DecimalMin(value = "0.0", message = "Maximum level must be non-negative")
    private Float maxLevel;
    // Можно добавить кастомную валидацию @AssertTrue, что maxLevel >= minLevel

    @NotNull(message = "Cost cannot be null")
    @PositiveOrZero(message = "Cost must be non-negative")
    private Integer cost;
}
