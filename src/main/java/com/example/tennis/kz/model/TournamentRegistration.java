package com.example.tennis.kz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tournament_registrations")
public class TournamentRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER) // EAGER здесь оправдан, часто нужен User с регистрацией
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER) // EAGER для партнера тоже
    @JoinColumn(name = "partner_id") // Nullable для одиночных турниров
    private User partner;

    @ManyToOne(fetch = FetchType.EAGER) // LAZY, т.к. турнир может быть большим объектом
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus status; // Enum RegistrationStatus должен существовать

    // Рейтинг, используемый для посева (индивидуальный или суммарный/средний для пары)
    // Рассчитывается и записывается перед генерацией сетки
    @Column(name = "seeding_rating")
    private Float seedingRating;

    // Номер посева (null если не сеяный)
    // Присваивается во время генерации сетки
    @Column(name = "seed_number")
    private Integer seedNumber;

    // Вспомогательный метод для получения имени участника(ов) для отображения
    @Transient
    public String getParticipantName() {
        if (user == null || user.getUserInfo() == null) return "N/A";

        String userDisplay = user.getUserInfo().getLastName() + " " +
                (user.getUserInfo().getFirstName() != null && !user.getUserInfo().getFirstName().isEmpty() ?
                        user.getUserInfo().getFirstName().substring(0, 1).toUpperCase() + "." : "");

        if (tournament != null && tournament.getTournamentType() == TournamentType.DOUBLES && partner != null && partner.getUserInfo() != null) {
            String partnerDisplay = partner.getUserInfo().getLastName() + " " +
                    (partner.getUserInfo().getFirstName() != null && !partner.getUserInfo().getFirstName().isEmpty() ?
                            partner.getUserInfo().getFirstName().substring(0, 1).toUpperCase() + "." : "");
            return userDisplay + " / " + partnerDisplay;
        }
        return userDisplay;
    }

    // Вспомогательный метод для получения главного игрока (для одиночек или первого в паре)
    @Transient
    public User getPrimaryPlayer() {
        return this.user;
    }
}