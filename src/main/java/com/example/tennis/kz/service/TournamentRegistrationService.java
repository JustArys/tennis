package com.example.tennis.kz.service;

import com.example.tennis.kz.model.*;
import com.example.tennis.kz.model.request.RegistrationRequest;
import com.example.tennis.kz.repository.TournamentRegistrationRepository;
import com.example.tennis.kz.repository.TournamentRepository;
import com.example.tennis.kz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentRegistrationService {

    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final TournamentRegistrationRepository registrationRepository;

    /**
     * Метод регистрации на турнир:
     * - Проверяет уровень/рейтинг (для одиночных и парных).
     * - Проверяет категорию (одиночная / парная).
     * - Проверяет соответствие гендера.
     * - Проверяет свободные места.
     * - Создаёт запись в статусе REGISTERED (одиночная) или PENDING_PARTNER (парная).
     */
    @Transactional
    public TournamentRegistration register(RegistrationRequest request) {
        // 1. Найдём турнир
        Tournament tournament = tournamentRepository.findById(request.getTournamentId())
                .orElseThrow(() -> new RuntimeException("Турнир не найден"));

        // 2. Найдём инициатора регистрации (user)
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Пользователь (инициатор) не найден"));

        // 3. Определяем категорию
        Category category = tournament.getCategory();
        if (category == null) {
            throw new RuntimeException("В турнире не указана категория");
        }

        // Проверяем, одиночная категория (SINGLES_...) или парная (DOUBLE_...)?
        boolean isSingles = isSinglesCategory(category);

        // --- Одиночная ---
        if (isSingles) {
            if (request.getPartnerId() != null) {
                throw new RuntimeException("Для одиночной категории партнёр не нужен");
            }

            // Проверяем гендер (MALE/FEMALE/ALL)
            validateSinglesGender(user, category);

            // 1) **Проверка рейтинга** одиночного игрока
            float rating = (user.getUserInfo().getRating() != null) ? user.getUserInfo().getRating() : 0f;
            if (rating < tournament.getMinLevel() || rating > tournament.getMaxLevel()) {
                throw new RuntimeException("Ваш рейтинг " + rating + " не подходит под условия турнира");
            }

            // 2) Проверка свободных мест (нужно 1 место)
            int participantsNow = getCurrentParticipantCount(tournament.getId(), category);
            if (participantsNow + 1 > tournament.getMaxParticipants()) {
                throw new IllegalArgumentException("Нет свободных мест в турнире (макс: "
                        + tournament.getMaxParticipants() + ")");
            }

            // Создаём запись о регистрации сразу в статусе REGISTERED
            TournamentRegistration registration = TournamentRegistration.builder()
                    .tournament(tournament)
                    .user(user)
                    .partner(null)
                    .status(RegistrationStatus.REGISTERED)
                    .build();

            return registrationRepository.save(registration);
        }

        // --- Парная ---
        else {
            if (request.getPartnerId() == null) {
                throw new RuntimeException("Для парной категории требуется partnerId");
            }

            // Найдём партнёра
            User partner = userRepository.findById(request.getPartnerId())
                    .orElseThrow(() -> new RuntimeException("Партнёр не найден"));

            // Проверяем гендер обоих
            validateDoublesGender(user, partner, category);

            // 1) **Проверка совокупного рейтинга** (предположим, сумма должна попасть в [minLevel, maxLevel])
            float rating1 = user.getUserInfo().getRating() != null ? user.getUserInfo().getRating() : 0f;
            float rating2 = partner.getUserInfo().getRating() != null ? partner.getUserInfo().getRating() : 0f;
            float combinedRating = rating1 + rating2;

            if (combinedRating < tournament.getMinLevel() || combinedRating > tournament.getMaxLevel()) {
                throw new RuntimeException("Суммарный рейтинг " + combinedRating
                        + " не подходит под условия турнира [" + tournament.getMinLevel()
                        + " - " + tournament.getMaxLevel() + "]");
            }

            // 2) Проверяем, хватит ли 2 мест
            int participantsNow = getCurrentParticipantCount(tournament.getId(), category);
            if (participantsNow + 2 > tournament.getMaxParticipants()) {
                throw new RuntimeException("В турнире недостаточно мест для парной регистрации");
            }

            // Создаём запись в статусе "ожидание подтверждения партнёра"
            TournamentRegistration registration = TournamentRegistration.builder()
                    .tournament(tournament)
                    .user(user)
                    .partner(partner)
                    .status(RegistrationStatus.PENDING_PARTNER)
                    .build();

            return registrationRepository.save(registration);
        }
    }

    @Transactional
    public TournamentRegistration confirmPartner(Long registrationId, Long partnerId) {
        TournamentRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Регистрация не найдена"));

        if (registration.getPartner() == null
                || !registration.getPartner().getId().equals(partnerId)) {
            throw new RuntimeException("У вас нет прав на подтверждение данной регистрации");
        }

        if (registration.getStatus() != RegistrationStatus.PENDING_PARTNER) {
            throw new RuntimeException("Регистрация не в статусе ожидания партнёра");
        }

        // Проверим, достаточно ли мест, чтобы занять 2 слота
        Tournament tournament = registration.getTournament();
        Category category = tournament.getCategory();
        int participantsNow = getCurrentParticipantCount(tournament.getId(), category);

        if (participantsNow + 2 > tournament.getMaxParticipants()) {
            throw new RuntimeException("Места закончились. Подтверждение невозможно");
        }

        // Меняем статус на REGISTERED
        registration.setStatus(RegistrationStatus.REGISTERED);
        return registrationRepository.save(registration);
    }

    /**
     * Отклонение приглашения партнёром (PENDING_PARTNER -> REJECTED).
     */
    @Transactional
    public TournamentRegistration rejectPartner(Long registrationId, Long partnerId) {
        TournamentRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Регистрация не найдена"));

        if (registration.getPartner() == null
                || !registration.getPartner().getId().equals(partnerId)) {
            throw new RuntimeException("У вас нет прав на отклонение данной регистрации");
        }

        if (registration.getStatus() != RegistrationStatus.PENDING_PARTNER) {
            throw new RuntimeException("Регистрация не в статусе ожидания партнёра");
        }

        // Меняем статус на REJECTED
        registration.setStatus(RegistrationStatus.REJECTED);
        return registrationRepository.save(registration);
    }

    /**
     * Уйти (отписаться) с турнира, на который зарегистрирован.
     * Можно назвать "cancelRegistration" или "withdrawFromTournament".
     *
     *  - Проверяем, что userId является либо основным user, либо partner.
     *  - Меняем статус на CANCELED, если он был REGISTERED или PENDING_PARTNER.
     *  - Если уже REJECTED или CANCELED, ничего не делаем или выбрасываем ошибку.
     */
    @Transactional
    public TournamentRegistration withdrawFromTournament(Long tournamentId, Long userId) {
        // 1. Найти запись (по tournamentId и userId)
        TournamentRegistration registration = registrationRepository
                .findByTournamentIdAndUserOrPartner(tournamentId, userId)
                .orElseThrow(() -> new RuntimeException(
                        "Регистрация не найдена для этого турнира и пользователя"));

        // 2. Проверить статусы
        if (registration.getStatus() == RegistrationStatus.CANCELED
                || registration.getStatus() == RegistrationStatus.REJECTED) {
            throw new RuntimeException("Регистрация уже отменена или отклонена");
        }

        // 3. Установить статус
        registration.setStatus(RegistrationStatus.CANCELED);

        return registrationRepository.save(registration);
    }

    // ================= ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =================

    /**
     * Подсчитывает, сколько участников (в статусе REGISTERED) уже заняли места в турнире:
     * - Для одиночной категории: каждая регистрация = 1 участник
     * - Для парной категории: каждая регистрация = 2 участника
     * - Статусы REJECTED / PENDING_PARTNER / CANCELED не считаются занятыми местами
     */
    private int getCurrentParticipantCount(Long tournamentId, Category category) {
        var registeredList = registrationRepository.findByTournamentIdAndStatus(
                tournamentId, RegistrationStatus.REGISTERED
        );
        if (isSinglesCategory(category)) {
            return registeredList.size();    // каждая запись = 1 участник
        } else {
            return registeredList.size() * 2; // парная запись = 2 участника
        }
    }

    /** Проверка, относится ли категория к одиночным (SINGLES_...). */
    private boolean isSinglesCategory(Category category) {
        return switch (category) {
            case SINGLES_MALE, SINGLES_FEMALE, SINGLES_ALL -> true;
            default -> false;
        };
    }

    /** Проверяем гендер пользователя для одиночной категории (MALE, FEMALE, ALL). */
    private void validateSinglesGender(User user, Category category) {
        Gender userGender = user.getUserInfo().getGender();
        switch (category) {
            case SINGLES_MALE:
                if (userGender != Gender.MALE) {
                    throw new RuntimeException("Для SINGLES_MALE требуется мужской пол");
                }
                break;
            case SINGLES_FEMALE:
                if (userGender != Gender.FEMALE) {
                    throw new RuntimeException("Для SINGLES_FEMALE требуется женский пол");
                }
                break;
            case SINGLES_ALL:
                // нет ограничений
                break;
            default:
                throw new RuntimeException("Некорректная одиночная категория: " + category);
        }
    }

    /**
     * Проверяем гендер пары (user, partner) для парной категории:
     * - DOUBLE_MALE: оба мужчины
     * - DOUBLE_FEMALE: обе женщины
     * - DOUBLE_MIXED: один мужчина, одна женщина
     * - DOUBLE_ALL: без ограничений
     */
    private void validateDoublesGender(User user, User partner, Category category) {
        Gender userGender = user.getUserInfo().getGender();
        Gender partnerGender = partner.getUserInfo().getGender();

        switch (category) {
            case DOUBLE_MALE:
                if (userGender != Gender.MALE || partnerGender != Gender.MALE) {
                    throw new RuntimeException("Для DOUBLE_MALE оба участника должны быть мужчинами");
                }
                break;
            case DOUBLE_FEMALE:
                if (userGender != Gender.FEMALE || partnerGender != Gender.FEMALE) {
                    throw new RuntimeException("Для DOUBLE_FEMALE обе участницы должны быть женщинами");
                }
                break;
            case DOUBLE_MIXED:
                boolean um_pf = (userGender == Gender.MALE && partnerGender == Gender.FEMALE);
                boolean uf_pm = (userGender == Gender.FEMALE && partnerGender == Gender.MALE);
                if (!(um_pf || uf_pm)) {
                    throw new RuntimeException("Для DOUBLE_MIXED требуется один мужчина и одна женщина");
                }
                break;
            case DOUBLE_ALL:
                // Без ограничений
                break;
            default:
                throw new RuntimeException("Некорректная парная категория: " + category);
        }
    }
}
