package com.example.tennis.kz.service;

import com.example.tennis.kz.exception.BadRequestException; // Наш кастомный BadRequestException
import com.example.tennis.kz.model.*;
import com.example.tennis.kz.model.request.TournamentCreationRequestDTO;
import com.example.tennis.kz.repository.TournamentRegistrationRepository;
import com.example.tennis.kz.repository.TournamentRepository;
// import jakarta.persistence.EntityNotFoundException; // Заменяем
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException; // Стандартный NoSuchElementException
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;

    @Transactional
    public Tournament createTournamentFromDto(TournamentCreationRequestDTO dto, User author) {
        if (dto == null) {
            throw new BadRequestException("Данные для создания турнира (DTO) не могут быть null.");
        }
        if (author == null) {
            throw new BadRequestException("Автор турнира не может быть null.");
        }
        // Валидация полей DTO
        if (dto.getStartDate() == null) throw new BadRequestException("Дата начала турнира должна быть указана.");
        if (dto.getEndDate() == null) throw new BadRequestException("Дата окончания турнира должна быть указана.");
        if (dto.getTier() == null) throw new BadRequestException("Уровень (Tier) турнира должен быть указан.");
        if (dto.getCategory() == null) throw new BadRequestException("Категория турнира должна быть указана.");
        if (dto.getCity() == null) throw new BadRequestException("Город проведения турнира должен быть указан.");
        if (dto.getCost() != null && dto.getCost() < 0) throw new BadRequestException("Стоимость участия не может быть отрицательной.");
        if (dto.getMinLevel() != null && dto.getMinLevel() < 0) throw new BadRequestException("Минимальный уровень не может быть отрицательным.");
        if (dto.getMaxLevel() != null && dto.getMaxLevel() < 0) throw new BadRequestException("Максимальный уровень не может быть отрицательным.");


        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("Дата окончания турнира не может быть раньше даты начала.");
        }
        if (dto.getMaxLevel() != null && dto.getMinLevel() != null && dto.getMaxLevel() < dto.getMinLevel()){
            throw new BadRequestException("Максимальный уровень не может быть меньше минимального уровня.");
        }

        Tournament tournament = new Tournament();
        tournament.setDescription(dto.getDescription());
        tournament.setStartDate(dto.getStartDate());
        tournament.setEndDate(dto.getEndDate());
        tournament.setStartTime(dto.getStartTime()); // startTime может быть null, если не указано
        tournament.setTier(dto.getTier());
        tournament.setCategory(dto.getCategory());
        tournament.setLocation(dto.getLocation());
        tournament.setCity(dto.getCity());
        tournament.setMinLevel(dto.getMinLevel());
        tournament.setMaxLevel(dto.getMaxLevel());
        tournament.setCost(dto.getCost());
        tournament.setAuthor(author);

        return tournamentRepository.save(tournament);
    }

    @Transactional
    public Tournament createTournament(Tournament tournament, User author) {
        if (tournament == null) {
            throw new BadRequestException("Объект турнира не может быть null.");
        }
        if (author == null) {
            throw new BadRequestException("Автор турнира не может быть null.");
        }
        if (tournament.getTier() == null) {
            throw new BadRequestException("Уровень (Tier) турнира должен быть указан при создании.");
        }
        if (tournament.getCategory() == null) {
            throw new BadRequestException("Категория турнира должна быть указана при создании.");
        }
        if (tournament.getId() != null) {
            throw new BadRequestException("ID должен быть null для создания нового турнира при передаче сущности.");
        }
        // Дополнительные проверки полей tournament, если они обязательны
        if (tournament.getStartDate() == null) throw new BadRequestException("Дата начала турнира должна быть указана.");
        if (tournament.getEndDate() == null) throw new BadRequestException("Дата окончания турнира должна быть указана.");
        if (tournament.getEndDate().isBefore(tournament.getStartDate())) throw new BadRequestException("Дата окончания не может быть раньше даты начала.");
        if (tournament.getCity() == null) throw new BadRequestException("Город турнира должен быть указан.");


        tournament.setAuthor(author);
        return tournamentRepository.save(tournament);
    }

    @Transactional(readOnly = true)
    public Tournament getTournamentById(Long id) {
        if (id == null) {
            throw new BadRequestException("ID турнира не может быть null.");
        }
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Турнир с ID: " + id + " не найден."));
    }

    @Transactional(readOnly = true)
    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Tournament> findAllTournaments(Pageable pageable) {
        if (pageable == null) {
            throw new BadRequestException("Pageable не может быть null.");
        }
        return tournamentRepository.findAll(pageable);
    }

    @Transactional
    public Tournament updateTournamentParams(
            Long id, String description, LocalDate startDate, LocalDate endDate, LocalTime startTime,
            Category category, Integer maxParticipantsFromRequest, String location,
            Float minLevel, Float maxLevel, Integer cost) {

        if (id == null) {
            throw new BadRequestException("ID турнира для обновления не может быть null.");
        }
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Турнир с ID: " + id + " не найден для обновления."));

        // Валидация дат, если обе указаны
        LocalDate effectiveStartDate = (startDate != null) ? startDate : tournament.getStartDate();
        LocalDate effectiveEndDate = (endDate != null) ? endDate : tournament.getEndDate();
        if (effectiveEndDate.isBefore(effectiveStartDate)) {
            throw new BadRequestException("Дата окончания турнира не может быть раньше даты начала.");
        }

        // Валидация уровней, если оба указаны или один из них обновляется
        Float effectiveMinLevel = (minLevel != null) ? minLevel : tournament.getMinLevel();
        Float effectiveMaxLevel = (maxLevel != null) ? maxLevel : tournament.getMaxLevel();
        if (effectiveMinLevel != null && effectiveMaxLevel != null && effectiveMaxLevel < effectiveMinLevel) {
            throw new BadRequestException("Максимальный уровень не может быть меньше минимального.");
        }
        if (cost != null && cost < 0) {
            throw new BadRequestException("Стоимость участия не может быть отрицательной.");
        }


        boolean hasRegistrations = tournamentRegistrationRepository.existsByTournamentId(id);
        boolean bracketGenerated = !tournament.getMatches().isEmpty();

        if (description != null) tournament.setDescription(description);
        if (startDate != null) tournament.setStartDate(startDate);
        if (endDate != null) tournament.setEndDate(endDate);
        if (startTime != null) tournament.setStartTime(startTime);
        if (location != null) tournament.setLocation(location);
        if (minLevel != null) tournament.setMinLevel(minLevel);
        if (maxLevel != null) tournament.setMaxLevel(maxLevel);
        if (cost != null) tournament.setCost(cost);


        if (category != null && !category.equals(tournament.getCategory())) {
            if (hasRegistrations || bracketGenerated) {
                throw new BadRequestException("Нельзя изменять категорию для турнира ID: " + id + " с существующими регистрациями или сгенерированной сеткой.");
            }
            tournament.setCategory(category);
        }

        if (maxParticipantsFromRequest != null) {
            Optional<TournamentTier> foundTier = Arrays.stream(TournamentTier.values())
                    .filter(t -> t.getMaxParticipants() == maxParticipantsFromRequest)
                    .findFirst();

            if (foundTier.isPresent()) {
                if (!foundTier.get().equals(tournament.getTier())) {
                    if (hasRegistrations || bracketGenerated) {
                        throw new BadRequestException("Нельзя изменять уровень (Tier) для турнира ID: " + id + " с существующими регистрациями или сгенерированной сеткой.");
                    }
                    tournament.setTier(foundTier.get());
                }
            } else {
                String validParticipantCounts = Arrays.stream(TournamentTier.values())
                        .map(t -> String.valueOf(t.getMaxParticipants()))
                        .collect(Collectors.joining(", "));
                throw new BadRequestException("Не найден TournamentTier для maxParticipants: " + maxParticipantsFromRequest +
                        ". Допустимые значения: [" + validParticipantCounts + "].");
            }
        }
        return tournamentRepository.save(tournament);
    }

    @Transactional
    public void deleteTournament(Long id) {
        if (id == null) {
            throw new BadRequestException("ID турнира для удаления не может быть null.");
        }
        // Проверка существования перед удалением, чтобы вернуть 404, если не найден
        if (!tournamentRepository.existsById(id)) {
            throw new NoSuchElementException("Турнир с ID: " + id + " не найден для удаления.");
        }
        // Дополнительные бизнес-правила (например, запрет удаления активных турниров) могут быть здесь.
        // Каскадное удаление настроено в Tournament entity.
        tournamentRepository.deleteById(id); // Используем deleteById для эффективности, т.к. сущность уже проверена
    }

    @Transactional(readOnly = true)
    public List<TournamentRegistration> getAllParticipants(Long tournamentId) {
        if (tournamentId == null) {
            throw new BadRequestException("ID турнира не может быть null для получения участников.");
        }
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new NoSuchElementException("Турнир с ID: " + tournamentId + " не найден при получении списка участников.");
        }
        return tournamentRegistrationRepository.findByTournamentId(tournamentId);
    }
}