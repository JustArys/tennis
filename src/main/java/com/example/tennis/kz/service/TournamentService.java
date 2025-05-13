package com.example.tennis.kz.service;

import com.example.tennis.kz.model.*; // Все твои модели
import com.example.tennis.kz.model.request.TournamentCreationRequestDTO;
import com.example.tennis.kz.repository.TournamentRegistrationRepository;
import com.example.tennis.kz.repository.TournamentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays; // Важный импорт для работы с values() у enum
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Импорт для Collectors.joining

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;

    @Transactional
    public Tournament createTournamentFromDto(TournamentCreationRequestDTO dto, User author) {
        // Валидация DTO должна происходить на уровне контроллера с @Valid
        // Здесь мы можем добавить дополнительную бизнес-логику, если необходимо

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
        if (dto.getMaxLevel() < dto.getMinLevel()){
            throw new IllegalArgumentException("Maximum level cannot be less than minimum level.");
        }

        Tournament tournament = new Tournament();
        tournament.setDescription(dto.getDescription());
        tournament.setStartDate(dto.getStartDate());
        tournament.setEndDate(dto.getEndDate());
        tournament.setStartTime(dto.getStartTime());
        tournament.setTier(dto.getTier());
        tournament.setCategory(dto.getCategory());
        tournament.setLocation(dto.getLocation());
        tournament.setCity(dto.getCity());
        tournament.setMinLevel(dto.getMinLevel());
        tournament.setMaxLevel(dto.getMaxLevel());
        tournament.setCost(dto.getCost());

        tournament.setAuthor(author); // Устанавливаем автора

        // id, createdAt, updatedAt будут установлены автоматически
        return tournamentRepository.save(tournament);
    }
    // --- КОНЕЦ НОВОГО МЕТОДА ---


    @Transactional
    public Tournament createTournament(Tournament tournament, User author) {
        if (tournament.getTier() == null) {
            throw new IllegalArgumentException("Tournament tier must be specified for creation.");
        }
        if (tournament.getCategory() == null) {
            throw new IllegalArgumentException("Tournament category must be specified for creation.");
        }
        if (tournament.getId() != null) { // Это важная проверка для метода, принимающего сущность
            throw new IllegalArgumentException("ID must be null for new tournament creation when passing entity directly.");
        }
        tournament.setAuthor(author);
        return tournamentRepository.save(tournament);
    }

    @Transactional(readOnly = true)
    public Tournament getTournamentById(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Tournament> getAllTournaments() {
        // Для API, возвращающего ВСЕ турниры, стоит подумать о пагинации по умолчанию
        // или ограничении, если их может быть очень много.
        return tournamentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Tournament> findAllTournaments(Pageable pageable) {
        return tournamentRepository.findAll(pageable);
    }

    @Transactional
    public Tournament updateTournamentParams(
            Long id, String description, LocalDate startDate, LocalDate endDate, LocalTime startTime,
            Category category, Integer maxParticipantsFromRequest, String location,
            Float minLevel, Float maxLevel, Integer cost) {

        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found with ID: " + id + " for update."));

        // Проверка: можно ли изменять критичные параметры (tier, category)
        // Это важно, так как изменения могут повлиять на уже зарегистрированных участников или сгенерированную сетку.
        boolean hasRegistrations = tournamentRegistrationRepository.existsByTournamentId(id); // Более эффективный способ проверки
        boolean bracketGenerated = !tournament.getMatches().isEmpty(); // Предполагаем, что getMatches() не вызовет проблему LAZY здесь, т.к. tournament загружен

        if (description != null) {
            tournament.setDescription(description);
        }
        if (startDate != null) {
            // Можно добавить валидацию дат (startDate <= endDate)
            tournament.setStartDate(startDate);
        }
        if (endDate != null) {
            tournament.setEndDate(endDate);
        }
        if (startTime != null) {
            tournament.setStartTime(startTime);
        }
        if (location != null) {
            tournament.setLocation(location);
        }
        if (minLevel != null) {
            // Валидация: minLevel <= maxLevel
            tournament.setMinLevel(minLevel);
        }
        if (maxLevel != null) {
            tournament.setMaxLevel(maxLevel);
        }
        if (cost != null) {
            // Валидация: cost >= 0
            tournament.setCost(cost);
        }

        if (category != null && !category.equals(tournament.getCategory())) {
            if (hasRegistrations || bracketGenerated) {
                throw new IllegalStateException("Cannot change category for a tournament with existing registrations or a generated bracket. Tournament ID: " + id);
            }
            tournament.setCategory(category);
        }

        if (maxParticipantsFromRequest != null) {
            if (hasRegistrations || bracketGenerated) {
                throw new IllegalStateException("Cannot change tier (via maxParticipants) for a tournament with existing registrations or a generated bracket. Tournament ID: " + id);
            }

            Optional<TournamentTier> foundTier = Arrays.stream(TournamentTier.values())
                    .filter(t -> t.getMaxParticipants() == maxParticipantsFromRequest)
                    .findFirst();

            if (foundTier.isPresent()) {
                if (!foundTier.get().equals(tournament.getTier())) { // Обновляем только если tier действительно изменился
                    tournament.setTier(foundTier.get());
                }
            } else {
                String validParticipantCounts = Arrays.stream(TournamentTier.values())
                        .map(t -> String.valueOf(t.getMaxParticipants()))
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException("No TournamentTier found for maxParticipants: " + maxParticipantsFromRequest +
                        ". Valid participant counts are: [" + validParticipantCounts + "].");
            }
        }
        // updatedAt будет обновлен Hibernate автоматически

        return tournamentRepository.save(tournament);
    }

    @Transactional
    public void deleteTournament(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found with ID: " + id + " for deletion."));

        // Дополнительные бизнес-правила перед удалением:
        // Например, если турнир уже начался или есть регистрации, возможно, удаление должно быть "мягким"
        // или должно быть запрещено. Текущая реализация просто удаляет.
        // Каскадное удаление регистраций и матчей настроено в Tournament entity.
        tournamentRepository.delete(tournament);
    }

    @Transactional(readOnly = true)
    public List<TournamentRegistration> getAllParticipants(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new EntityNotFoundException("Tournament not found with ID: " + tournamentId + " when fetching participants.");
        }
        List<TournamentRegistration> registrations = tournamentRegistrationRepository.findByTournamentId(tournamentId);

        // Замечание опытного разработчика:
        // Если сущности User и UserInfo внутри TournamentRegistration мапятся с FetchType.LAZY,
        // то при доступе к ним вне этого транзакционного метода (например, при сериализации в JSON контроллером)
        // может возникнуть LazyInitializationException.
        // Решения:
        // 1. Использовать DTO и маппить данные в них здесь, внутри транзакции.
        // 2. Использовать JOIN FETCH в запросе репозитория findByTournamentId, чтобы подгрузить связанные сущности.
        //    Пример: @Query("SELECT tr FROM TournamentRegistration tr LEFT JOIN FETCH tr.user u LEFT JOIN FETCH u.userInfo WHERE tr.tournament.id = :tournamentId")
        // 3. Изменить FetchType на EAGER (но это может повлиять на производительность в других местах).
        // Текущая реализация возвращает список сущностей; дальнейшая обработка LAZY - ответственность вызывающего кода или DTO-маппера.
        return registrations;
    }
}