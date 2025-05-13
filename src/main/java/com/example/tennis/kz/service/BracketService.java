package com.example.tennis.kz.service; // или com.example.tennis.kz.service.impl, если предпочитаешь

import com.example.tennis.kz.model.*;
import com.example.tennis.kz.repository.MatchRepository;
import com.example.tennis.kz.repository.TournamentRegistrationRepository;
import com.example.tennis.kz.repository.TournamentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j; // Логирование убрано
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// @Slf4j // Логирование убрано
public class BracketService { // Класс вместо интерфейса + реализации

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;
    private final MatchRepository matchRepository;

    @Transactional
    public List<Match> generateBracket(Long tournamentId) {
        // System.out.println("Generating bracket for tournament ID: " + tournamentId); // Замена лога
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found with ID: " + tournamentId));

        if (!tournament.getMatches().isEmpty()) {
            // System.out.println("Bracket already generated for tournament ID: " + tournamentId); // Замена лога
            throw new IllegalStateException("Bracket already generated for this tournament.");
        }

        List<TournamentRegistration> approvedRegistrations = tournamentRegistrationRepository
                .findByTournamentIdAndStatus(tournamentId, RegistrationStatus.REGISTERED);

        int maxParticipants = tournament.getMaxParticipants();
        if (approvedRegistrations.size() < 2) {
            // System.err.println("Not enough participants for tournament ID: " + tournamentId); // Замена лога
            throw new IllegalStateException("Not enough participants to generate a bracket (minimum 2).");
        }

        // Важное замечание: Логика для случая, когда approvedRegistrations.size() != maxParticipants
        // (особенно если оно не степень двойки) здесь не обрабатывает "bye" (пропуски).
        // Для корректной работы с неполными сетками потребуется значительное усложнение.
        // Текущий код ожидает, что approvedRegistrations.size() будет равно maxParticipants,
        // и это число будет степенью двойки.
        if (approvedRegistrations.size() != maxParticipants) {
            System.err.println("Warning: Number of participants (" + approvedRegistrations.size() +
                    ") does not match tournament capacity (" + maxParticipants +
                    "). This implementation without 'bye' logic might lead to an incomplete or incorrect bracket.");
            // Можно либо выбросить исключение, либо попытаться продолжить, но с риском.
            // Для демонстрации продолжим, но это КРИТИЧЕСКИЙ момент для реального приложения.
            // throw new IllegalStateException("Participant count must match tournament capacity for this simple bracket generation.");
        }


        List<TournamentRegistration> participants = prepareAndSortParticipants(new ArrayList<>(approvedRegistrations), tournament);

        // Сохраняем seedingRating в БД для регистраций
        tournamentRegistrationRepository.saveAll(participants);


        List<Match> allMatchesInBracket = createMatchStructure(tournament);
        placeParticipants(allMatchesInBracket, participants, tournament);

        // System.out.println("Successfully generated " + allMatchesInBracket.size() + " matches for tournament ID: " + tournamentId); // Замена лога
        return matchRepository.saveAll(allMatchesInBracket);
    }

    // Этот метод может быть public, если нужен извне, или private, если только для внутреннего использования.
    // Сделаем его package-private или private, так как он вспомогательный для generateBracket.
    private List<TournamentRegistration> prepareAndSortParticipants(List<TournamentRegistration> registrations, Tournament tournament) {
        // System.out.println("Preparing and sorting " + registrations.size() + " participants for tournament: " + tournament.getId()); // Замена лога
        for (TournamentRegistration reg : registrations) {
            float seedingRating = 0f;
            if (reg.getUser() != null && reg.getUser().getUserInfo() != null && reg.getUser().getUserInfo().getRating() != null) {
                seedingRating += reg.getUser().getUserInfo().getRating();
            } else if (reg.getUser() != null) {
                // System.out.println("Warning: User " + reg.getUser().getId() + " has no rating for seeding."); // Замена лога
            }


            if (tournament.getTournamentType() == TournamentType.DOUBLES) {
                if (reg.getPartner() != null && reg.getPartner().getUserInfo() != null && reg.getPartner().getUserInfo().getRating() != null) {
                    seedingRating += reg.getPartner().getUserInfo().getRating();
                } else if (reg.getPartner() != null) {
                    // System.out.println("Warning: Partner for registration " + reg.getId() + " has no rating, using 0 for partner."); // Замена лога
                } else {
                    // System.err.println("Doubles tournament registration " + reg.getId() + " is missing a partner."); // Замена лога
                    throw new IllegalStateException("Partner missing in doubles registration: " + reg.getId());
                }
            }
            reg.setSeedingRating(seedingRating);
        }

        registrations.sort(Comparator.comparing(TournamentRegistration::getSeedingRating, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TournamentRegistration::getId)); // Стабильная сортировка при равных рейтингах
        // System.out.println("Participants sorted by seeding rating."); // Замена лога
        return registrations;
    }

    private List<Match> createMatchStructure(Tournament tournament) {
        List<Match> allMatches = new ArrayList<>();
        int totalRounds = tournament.getTotalRounds();
        int maxParticipants = tournament.getMaxParticipants();
        int matchNumberInBracketCounter = 1;

        Map<Integer, List<Match>> matchesByRound = new HashMap<>();

        for (int round = 1; round <= totalRounds; round++) {
            matchesByRound.put(round, new ArrayList<>());
            int matchesInThisRound = maxParticipants / (int) Math.pow(2, round); // Например, 16 / 2^1 = 8; 16 / 2^2 = 4
            for (int i = 0; i < matchesInThisRound; i++) {
                Match match = Match.builder()
                        .tournament(tournament)
                        .roundNumber(round)
                        .matchNumberInBracket(matchNumberInBracketCounter++)
                        .status(MatchStatus.PENDING_PARTICIPANTS)
                        .build();
                allMatches.add(match);
                matchesByRound.get(round).add(match);
            }
        }

        for (int round = 1; round < totalRounds; round++) {
            List<Match> currentRoundMatches = matchesByRound.get(round);
            List<Match> nextRoundMatches = matchesByRound.get(round + 1);
            for (int i = 0; i < currentRoundMatches.size(); i++) {
                Match currentMatch = currentRoundMatches.get(i);
                Match nextMatchForWinner = nextRoundMatches.get(i / 2);
                currentMatch.setNextMatch(nextMatchForWinner);
                currentMatch.setNextMatchSlot((i % 2) + 1);
            }
        }
        return allMatches;
    }

    private void placeParticipants(List<Match> allMatches, List<TournamentRegistration> sortedParticipants, Tournament tournament) {
        int numberOfSeeds = tournament.getNumberOfSeeds();
        // ВАЖНО: sortedParticipants.size() должно быть равно tournament.getMaxParticipants()
        // для корректной работы этого упрощенного алгоритма без "bye".
        // Если это не так, поведение не определено и, скорее всего, будет некорректным.

        List<Match> firstRoundMatches = allMatches.stream()
                .filter(m -> m.getRoundNumber() == 1)
                .sorted(Comparator.comparing(Match::getMatchNumberInBracket))
                .collect(Collectors.toList());

        // Если участников меньше, чем слотов в первом раунде, будет ошибка или некорректная сетка.
        if (sortedParticipants.size() > tournament.getMaxParticipants()) {
            // System.out.println("Warning: More participants than tournament capacity. Truncating list."); // Замена лога
            sortedParticipants = sortedParticipants.subList(0, tournament.getMaxParticipants());
        }


        List<TournamentRegistration> seeds = new ArrayList<>();
        List<TournamentRegistration> others = new ArrayList<>();

        for (int i = 0; i < sortedParticipants.size(); i++) {
            TournamentRegistration reg = sortedParticipants.get(i);
            if (i < numberOfSeeds && i < sortedParticipants.size()) { // Доп. проверка на sortedParticipants.size()
                reg.setSeedNumber(i + 1);
                seeds.add(reg);
            } else {
                others.add(reg);
            }
        }
        if (!seeds.isEmpty()) {
            tournamentRegistrationRepository.saveAll(seeds); // Сохраняем номера посева для сеяных
        }


        // Стандартная расстановка сеяных. Очень УПРОЩЕННАЯ.
        // Для корректной теннисной расстановки (8, 16 сеяных) нужна сложная схема.
        // Это только базовый пример для очень малого числа сеяных.
        // --- НАЧАЛО УПРОЩЕННОЙ РАССТАНОВКИ СЕЯНЫХ ---
        TournamentRegistration[] slots = new TournamentRegistration[tournament.getMaxParticipants()]; // Виртуальные слоты первого раунда

        // Seed 1
        if (seeds.size() >= 1) slots[0] = seeds.get(0);
        // Seed 2
        if (seeds.size() >= 2) slots[tournament.getMaxParticipants() - 1] = seeds.get(1);

        // Seed 3 (если есть) - примерно в начало второй половины
        if (seeds.size() >= 3) {
            int slot3Index = tournament.getMaxParticipants() / 2;
            slots[slot3Index] = seeds.get(2);
        }
        // Seed 4 (если есть) - примерно в конец первой половины (но не там где S1)
        if (seeds.size() >= 4) {
            int slot4Index = tournament.getMaxParticipants() / 2 - 1;
            if(slot4Index == 0 && slots[0] != null) slot4Index = 1; // Сдвиг если S1 уже на 0
            slots[slot4Index] = seeds.get(3);
        }
        // ... и так далее для S5, S6, S7, S8 нужно более аккуратно распределять по четвертям/восьмым.
        // Это TODO для полной теннисной логики.

        // Заполняем оставшиеся слоты несеяными и "неразмещенными" сеяными
        List<TournamentRegistration> unplacedParticipants = new ArrayList<>(others);
        for(TournamentRegistration seed : seeds) {
            boolean isPlaced = false;
            for(TournamentRegistration placedSlotUser : slots){
                if(placedSlotUser != null && placedSlotUser.equals(seed)){
                    isPlaced = true;
                    break;
                }
            }
            if(!isPlaced) unplacedParticipants.add(seed);
        }
        // Сортируем "неразмещенных" так, чтобы сеяные (если они там есть) шли первыми.
        unplacedParticipants.sort(Comparator.comparing(TournamentRegistration::getSeedNumber, Comparator.nullsLast(Comparator.naturalOrder())));


        Collections.shuffle(unplacedParticipants.subList( // Перемешиваем только тех, кто действительно несеяный
                (int)unplacedParticipants.stream().filter(p -> p.getSeedNumber() != null).count(), // начало подсписка (после всех сеяных)
                unplacedParticipants.size() // конец подсписка
        ));


        int unplacedIdx = 0;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null && unplacedIdx < unplacedParticipants.size()) {
                slots[i] = unplacedParticipants.get(unplacedIdx++);
            }
        }
        // --- КОНЕЦ УПРОЩЕННОЙ РАССТАНОВКИ СЕЯНЫХ ---

        // Теперь распределяем участников из slots по реальным матчам первого раунда
        int currentSlotIndex = 0;
        for (Match match : firstRoundMatches) {
            if (currentSlotIndex < slots.length) {
                assignParticipantToMatch(match, slots[currentSlotIndex++], 1);
            } else {
                // System.err.println("Warning: Not enough participants for all slots in match " + match.getMatchNumberInBracket()); // Замена лога
            }
            if (currentSlotIndex < slots.length) {
                assignParticipantToMatch(match, slots[currentSlotIndex++], 2);
            } else {
                // System.err.println("Warning: Not enough participants for all slots in match " + match.getMatchNumberInBracket()); // Замена лога
            }

            // Обновляем статус матча
            if (match.getParticipant1() != null && match.getParticipant2() != null) {
                match.setStatus(MatchStatus.SCHEDULED);
            } else if (match.getParticipant1() != null || match.getParticipant2() != null) {
                // Это ситуация "bye". Победитель известен (тот, кто есть).
                // Требует продвижения этого участника и установки статуса COMPLETED/WALKOVER для матча.
                // System.out.println("Match " + match.getMatchNumberInBracket() + " has a 'bye'. Logic for 'bye' advancement not fully implemented here."); // Замена лога
                TournamentRegistration byeWinner = match.getParticipant1() != null ? match.getParticipant1() : match.getParticipant2();
                match.setWinner(byeWinner);
                match.setScore("BYE");
                match.setStatus(MatchStatus.WALKOVER); // или COMPLETED
                match.setCompletedTime(LocalDateTime.now());
                // Для упрощения пока оставим так, advanceWinner вызовется позже или при вводе результата
            } else {
                // System.err.println("Warning: Match " + match.getMatchNumberInBracket() + " has no participants after placement. This indicates an issue."); // Замена лога
            }
        }
    }

    private void assignParticipantToMatch(Match match, TournamentRegistration registration, int slot) {
        if (registration == null) {
            // System.err.println("Attempting to assign null participant to match " + match.getMatchNumberInBracket() + " slot " + slot); // Замена лога
            return; // Не назначаем null
        }
        if (slot == 1) {
            match.setParticipant1(registration);
        } else {
            match.setParticipant2(registration);
        }
    }
}