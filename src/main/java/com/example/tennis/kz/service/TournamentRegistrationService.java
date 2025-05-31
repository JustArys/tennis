package com.example.tennis.kz.service;

import com.example.tennis.kz.exception.BadRequestException; // Наш кастомный BadRequestException
import com.example.tennis.kz.model.*;
import com.example.tennis.kz.model.request.RegistrationRequest;
import com.example.tennis.kz.repository.TournamentRegistrationRepository;
import com.example.tennis.kz.repository.TournamentRepository;
import com.example.tennis.kz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
// import org.apache.coyote.BadRequestException; // Неправильный импорт, будет удален если был
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List; // Импорт List может быть нужен для findByTournamentIdAndStatus
import java.util.NoSuchElementException; // Стандартный NoSuchElementException

@Service
@RequiredArgsConstructor
public class TournamentRegistrationService {

    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final TournamentRegistrationRepository registrationRepository;

    @Transactional
    public TournamentRegistration register(RegistrationRequest request) {
        if (request == null) {
            throw new BadRequestException("Запрос на регистрацию не может быть null.");
        }
        if (request.getTournamentId() == null) {
            throw new BadRequestException("ID турнира в запросе не может быть null.");
        }
        if (request.getUserId() == null) {
            throw new BadRequestException("ID пользователя (инициатора) в запросе не может быть null.");
        }

        Tournament tournament = tournamentRepository.findById(request.getTournamentId())
                .orElseThrow(() -> new NoSuchElementException("Турнир с ID " + request.getTournamentId() + " не найден."));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NoSuchElementException("Пользователь (инициатор) с ID " + request.getUserId() + " не найден."));

        if (user.getUserInfo() == null) {
            throw new BadRequestException("UserInfo для пользователя (инициатора) ID " + request.getUserId() + " не найден. Необходим для проверки рейтинга/гендера.");
        }

        Category category = tournament.getCategory();
        if (category == null) {
            throw new IllegalStateException("В турнире ID " + tournament.getId() + " не указана категория. Регистрация невозможна.");
        }

        boolean isSingles = isSinglesCategory(category);

        if (isSingles) {
            if (request.getPartnerId() != null) {
                throw new BadRequestException("Для одиночной категории партнер не указывается.");
            }
            validateSinglesGender(user, category);

            // Проверка null для min/max Level чтобы избежать NPE
            Float minLevel = tournament.getMinLevel();
            Float maxLevel = tournament.getMaxLevel();
            float rating = (user.getUserInfo().getRating() != null) ? user.getUserInfo().getRating() : 0f;

            if ((minLevel != null && rating < minLevel) || (maxLevel != null && rating > maxLevel)) {
                throw new BadRequestException("Ваш рейтинг " + rating + " не соответствует уровню турнира ("
                        + (minLevel != null ? minLevel : "N/A") + " - " + (maxLevel != null ? maxLevel : "N/A") + ").");
            }

            int participantsNow = getCurrentParticipantCount(tournament.getId(), category);
            if (participantsNow >= tournament.getMaxParticipants()) {
                throw new BadRequestException("Нет свободных мест в турнире (макс: " + tournament.getMaxParticipants() + ", текущих: " + participantsNow + ").");
            }

            TournamentRegistration registration = TournamentRegistration.builder()
                    .tournament(tournament)
                    .user(user)
                    .partner(null)
                    .status(RegistrationStatus.REGISTERED)
                    .build();
            return registrationRepository.save(registration);
        } else { // Парная категория
            if (request.getPartnerId() == null) {
                throw new BadRequestException("Для парной категории необходимо указать ID партнера.");
            }
            if (request.getPartnerId().equals(request.getUserId())) {
                throw new BadRequestException("Игрок не может быть партнером самому себе.");
            }

            User partner = userRepository.findById(request.getPartnerId())
                    .orElseThrow(() -> new NoSuchElementException("Партнер с ID " + request.getPartnerId() + " не найден."));
            if (partner.getUserInfo() == null) {
                throw new BadRequestException("UserInfo для партнера ID " + request.getPartnerId() + " не найден. Необходим для проверки рейтинга/гендера.");
            }

            validateDoublesGender(user, partner, category);

            Float minLevel = tournament.getMinLevel();
            Float maxLevel = tournament.getMaxLevel();
            float rating1 = user.getUserInfo().getRating() != null ? user.getUserInfo().getRating() : 0f;
            float rating2 = partner.getUserInfo().getRating() != null ? partner.getUserInfo().getRating() : 0f;
            float combinedRating = rating1 + rating2;

            if ((minLevel != null && combinedRating < minLevel) || (maxLevel != null && combinedRating > maxLevel)) {
                throw new BadRequestException("Суммарный рейтинг пары " + combinedRating
                        + " не соответствует уровню турнира (" + (minLevel != null ? minLevel : "N/A")
                        + " - " + (maxLevel != null ? maxLevel : "N/A") + ").");
            }

            int participantsNow = getCurrentParticipantCount(tournament.getId(), category);
            // Для парной категории, каждая регистрация (которая в статусе REGISTERED) занимает 2 места.
            // Проверяем, хватит ли места для новой пары (т.е. еще +2 места).
            if (participantsNow + 2 > tournament.getMaxParticipants()) {
                throw new BadRequestException("В турнире недостаточно мест для регистрации пары (макс: " + tournament.getMaxParticipants() + ", текущих занято: " + participantsNow + ").");
            }

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
        if (registrationId == null || partnerId == null) {
            throw new BadRequestException("ID регистрации и ID партнера не могут быть null.");
        }
        TournamentRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NoSuchElementException("Регистрация с ID " + registrationId + " не найдена."));

        if (registration.getPartner() == null || !registration.getPartner().getId().equals(partnerId)) {
            throw new BadRequestException("У вас нет прав на подтверждение данной регистрации или партнер не соответствует.");
        }
        if (registration.getStatus() != RegistrationStatus.PENDING_PARTNER) {
            throw new BadRequestException("Регистрация не находится в статусе ожидания подтверждения от партнера.");
        }

        Tournament tournament = registration.getTournament();
        Category category = tournament.getCategory(); // Категория нужна для getCurrentParticipantCount
        int participantsNow = getCurrentParticipantCount(tournament.getId(), category);

        if (participantsNow + 2 > tournament.getMaxParticipants()) {
            throw new BadRequestException("Места в турнире закончились. Подтверждение невозможно (макс: " + tournament.getMaxParticipants() + ", текущих занято: " + participantsNow + ").");
        }

        registration.setStatus(RegistrationStatus.REGISTERED);
        return registrationRepository.save(registration);
    }

    @Transactional
    public TournamentRegistration rejectPartner(Long registrationId, Long partnerId) {
        if (registrationId == null || partnerId == null) {
            throw new BadRequestException("ID регистрации и ID партнера не могут быть null.");
        }
        TournamentRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NoSuchElementException("Регистрация с ID " + registrationId + " не найдена."));

        if (registration.getPartner() == null || !registration.getPartner().getId().equals(partnerId)) {
            throw new BadRequestException("У вас нет прав на отклонение данной регистрации или партнер не соответствует.");
        }
        if (registration.getStatus() != RegistrationStatus.PENDING_PARTNER) {
            throw new BadRequestException("Регистрация не находится в статусе ожидания подтверждения от партнера.");
        }

        registration.setStatus(RegistrationStatus.REJECTED);
        return registrationRepository.save(registration);
    }

    @Transactional
    public TournamentRegistration withdrawFromTournament(Long tournamentId, Long userId) {
        if (tournamentId == null || userId == null) {
            throw new BadRequestException("ID турнира и ID пользователя не могут быть null.");
        }
        // Ищем регистрацию, где пользователь является либо user, либо partner.
        // Убедитесь, что такой метод есть в репозитории и он корректно работает.
        // Например: findByTournament_IdAndUser_IdOrTournament_IdAndPartner_Id
        TournamentRegistration registration = registrationRepository
                .findByTournamentIdAndUserOrPartner(tournamentId, userId) // Используем метод как в вашем коде
                .orElseThrow(() -> new NoSuchElementException(
                        "Регистрация не найдена для турнира ID " + tournamentId + " и пользователя ID " + userId));

        if (registration.getStatus() == RegistrationStatus.CANCELED || registration.getStatus() == RegistrationStatus.REJECTED) {
            throw new BadRequestException("Регистрация уже отменена или отклонена.");
        }

        registration.setStatus(RegistrationStatus.CANCELED);
        return registrationRepository.save(registration);
    }

    private int getCurrentParticipantCount(Long tournamentId, Category category) {
        List<TournamentRegistration> registeredList = registrationRepository.findByTournamentIdAndStatus(
                tournamentId, RegistrationStatus.REGISTERED
        );
        if (isSinglesCategory(category)) {
            return registeredList.size();
        } else {
            return registeredList.size() * 2;
        }
    }

    private boolean isSinglesCategory(Category category) {
        // Проверка на null для category добавлена в вызывающем методе
        return category == Category.SINGLES_MALE || category == Category.SINGLES_FEMALE || category == Category.SINGLES_ALL;
    }

    private void validateSinglesGender(User user, Category category) {
        if (user.getUserInfo() == null) {
            throw new BadRequestException("UserInfo для пользователя ID " + user.getId() + " не найден для проверки гендера.");
        }
        Gender userGender = user.getUserInfo().getGender();
        if (userGender == null) {
            throw new BadRequestException("Гендер не указан для пользователя ID " + user.getId() + ". Проверка невозможна.");
        }

        switch (category) {
            case SINGLES_MALE:
                if (userGender != Gender.MALE) {
                    throw new BadRequestException("Для категории SINGLES_MALE требуется мужской пол.");
                }
                break;
            case SINGLES_FEMALE:
                if (userGender != Gender.FEMALE) {
                    throw new BadRequestException("Для категории SINGLES_FEMALE требуется женский пол.");
                }
                break;
            case SINGLES_ALL:
                break;
            default:
                throw new IllegalStateException("Некорректная одиночная категория (" + category + ") передана для валидации гендера.");
        }
    }

    private void validateDoublesGender(User user, User partner, Category category) {
        if (user.getUserInfo() == null) {
            throw new BadRequestException("UserInfo для пользователя (инициатора) ID " + user.getId() + " не найден для проверки гендера.");
        }
        if (partner.getUserInfo() == null) {
            throw new BadRequestException("UserInfo для партнера ID " + partner.getId() + " не найден для проверки гендера.");
        }

        Gender userGender = user.getUserInfo().getGender();
        Gender partnerGender = partner.getUserInfo().getGender();

        if (userGender == null || partnerGender == null) {
            throw new BadRequestException("Гендер не указан для одного или обоих игроков пары. Проверка невозможна.");
        }

        switch (category) {
            case DOUBLE_MALE:
                if (userGender != Gender.MALE || partnerGender != Gender.MALE) {
                    throw new BadRequestException("Для категории DOUBLE_MALE оба участника должны быть мужчинами.");
                }
                break;
            case DOUBLE_FEMALE:
                if (userGender != Gender.FEMALE || partnerGender != Gender.FEMALE) {
                    throw new BadRequestException("Для категории DOUBLE_FEMALE обе участницы должны быть женщинами.");
                }
                break;
            case DOUBLE_MIXED:
                boolean isMixedPair = (userGender == Gender.MALE && partnerGender == Gender.FEMALE) ||
                        (userGender == Gender.FEMALE && partnerGender == Gender.MALE);
                if (!isMixedPair) {
                    throw new BadRequestException("Для категории DOUBLE_MIXED требуется один мужчина и одна женщина.");
                }
                break;
            case DOUBLE_ALL:
                break;
            default:
                throw new IllegalStateException("Некорректная парная категория (" + category + ") передана для валидации гендера.");
        }
    }
}