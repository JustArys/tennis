package com.example.tennis.kz.service;

import com.example.tennis.kz.exception.BadRequestException; // Импорт вашего кастомного исключения
import com.example.tennis.kz.model.City;
import com.example.tennis.kz.model.Coach;
import com.example.tennis.kz.model.Language;
import com.example.tennis.kz.model.User;
import com.example.tennis.kz.model.request.CoachRequest;
import com.example.tennis.kz.repository.CoachRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // Pageable был без PageRequest, но он используется для типа параметра
import org.springframework.stereotype.Service;

// import java.time.LocalDateTime; // Не используется в этом классе
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CoachService {
    private final CoachRepository coachRepository;

    public Coach getCoachById(Long id) {
        if (id == null) {
            throw new BadRequestException("ID тренера не может быть null.");
        }
        return coachRepository.findById(id).orElseThrow(()
                -> new NoSuchElementException(String.format("Тренер с ID '%d' не найден.", id)));
    }

    public List<Coach> getAllCoaches(Boolean enabled) {
        // enabled может быть null, если такая логика предусмотрена.
        // Если enabled всегда должен быть true или false, то можно добавить проверку:
        // if (enabled == null) {
        //     throw new BadRequestException("Параметр 'enabled' должен быть указан (true или false).");
        // }
        return coachRepository.findByEnabledOrderByCostAsc(enabled);
    }

    public List<Coach> getAllCoaches() {
        return coachRepository.findAll();
    }

    public Page<Coach> getAllCoaches(Pageable pageable, Boolean enabled) {
        // Аналогично, 'enabled' можно проверить на null, если это не предполагается.
        return coachRepository.findAllByEnabled(pageable, enabled);
    }

    public Coach enableCoach(Long id){
        Coach coach = getCoachById(id); // getCoachById уже выбрасывает исключения, если нужно
        coach.setEnabled(true);
        return coachRepository.save(coach);
    }

    public Coach addCoach(User user, CoachRequest coachRequest) {
        if (user == null || user.getUserInfo() == null) {
            throw new BadRequestException("Пользователь (UserInfo) для создания тренера не может быть null.");
        }
        if (coachRequest == null) {
            throw new BadRequestException("Данные тренера (CoachRequest) не могут быть null.");
        }
        if (coachRequest.getCity() == null) {
            throw new BadRequestException("Город для тренера должен быть указан.");
        }
        if (coachRequest.getCost() == null || coachRequest.getCost() < 0) {
            throw new BadRequestException("Стоимость услуг тренера должна быть указана и не может быть отрицательной.");
        }
        // Дополнительные проверки для других обязательных полей coachRequest можно добавить здесь

        var newCoach = Coach.builder()
                .enabled(false) // По умолчанию false, будет включен отдельно
                .city(coachRequest.getCity())
                .languages(coachRequest.getLanguage())
                .cost(coachRequest.getCost())
                .services(coachRequest.getCoachServices())
                .description(coachRequest.getDescription())
                .experience(coachRequest.getExperience())
                .stadium(coachRequest.getStadium())
                .user(user.getUserInfo()) // Связываем с UserInfo пользователя
                .build();
        return coachRepository.save(newCoach);
    }

    public Coach updateCoachParams(Long id,
                                   City city,
                                   Set<Language> languages,
                                   Float cost,
                                   Set<com.example.tennis.kz.model.CoachService> coachServices, // Уточнил полное имя CoachService
                                   String description,
                                   Integer experience,
                                   String stadium) {
        Coach coach = getCoachById(id); // getCoachById уже обрабатывает случай ненайденного тренера

        if (cost != null && cost < 0) {
            throw new BadRequestException("Стоимость не может быть отрицательной.");
        }
        // Можно добавить другие проверки для параметров, если они имеют ограничения (например, experience >= 0)
        if (experience != null && experience < 0) {
            throw new BadRequestException("Опыт не может быть отрицательным.");
        }


        if (city != null) {
            coach.setCity(city);
        }
        if (languages != null) { // Для Set можно проверить !languages.isEmpty(), если пустой сет невалиден
            coach.setLanguages(languages);
        }
        if (cost != null) {
            coach.setCost(cost);
        }
        if (coachServices != null) {
            coach.setServices(coachServices);
        }
        // Для description можно проверить на пустую строку, если это не разрешено:
        // if (description != null && description.trim().isEmpty()) {
        //    throw new BadRequestException("Описание не может состоять только из пробелов.");
        // }
        if (description != null) {
            coach.setDescription(description);
        }
        if (experience != null) {
            coach.setExperience(experience);
        }
        if (stadium != null) {
            coach.setStadium(stadium);
        }
        return coachRepository.save(coach);
    }

    public void deleteCoach(Long id) {
        if (id == null) {
            throw new BadRequestException("ID тренера для удаления не может быть null.");
        }
        if (!coachRepository.existsById(id)) {
            throw new NoSuchElementException(String.format("Тренер с ID '%d' не найден, удаление невозможно.", id));
        }
        coachRepository.deleteById(id);
    }
}