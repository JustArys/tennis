package com.example.tennis.kz.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.deser.DataFormatReaders;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "tournaments") // Рекомендую использовать множественное число для таблиц
public class Tournament {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Id
    @Column(name = "tournament_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Column(nullable = false)
    private TournamentTier tier; // Уровень (Futures, Challenger, Masters)

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category; // Тип (одиночный/парный) и пол

    private String location;

    @Enumerated(EnumType.STRING)
    private City city; // Убедитесь, что Enum City существует

    private float minLevel; // Минимальный игровой уровень NTRS/UTR для допуска
    private float maxLevel; // Максимальный игровой уровень
    private int cost; // Стоимость участия

    @CreationTimestamp
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @ManyToOne(fetch = FetchType.EAGER) // Автор турнира
    @JoinColumn(name = "author_user_id") // Явное имя колонки
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private User author;

    @JsonIgnore
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("roundNumber ASC, matchNumberInBracket ASC") // Для упорядоченного получения матчей
    private List<Match> matches = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude // Avoid issues with bidirectional relationships in toString
    @EqualsAndHashCode.Exclude // Avoid issues with bidirectional relationships
    private List<TournamentRegistration> registrations = new ArrayList<>();


    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Transient // Не сохранять в БД, вычисляется на лету
    public int getMaxParticipants() {
        return this.tier != null ? this.tier.getMaxParticipants() : 0;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Transient
    public int getNumberOfSeeds() {
        return this.tier != null ? this.tier.getNumberOfSeeds() : 0;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Transient
    public TournamentType getTournamentType() {
        if (this.category == null) return null;
        return this.category.isDoubles() ? TournamentType.DOUBLES : TournamentType.SINGLES;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Transient
    public int getTotalRounds() {
        int participants = getMaxParticipants();
        if (participants <= 1) return 0; // Если 0 или 1 участник, раундов нет
        // log base 2 of participants
        return (int) (Math.log(participants) / Math.log(2));
    }
}