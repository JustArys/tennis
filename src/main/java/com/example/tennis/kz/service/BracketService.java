package com.example.tennis.kz.service; // или com.example.tennis.kz.service.impl, если предпочитаешь

import com.example.tennis.kz.exception.BadRequestException; // Импортируем наше кастомное исключение
import com.example.tennis.kz.model.*;
import com.example.tennis.kz.repository.MatchRepository;
import com.example.tennis.kz.repository.TournamentRegistrationRepository;
import com.example.tennis.kz.repository.TournamentRepository;
// jakarta.persistence.EntityNotFoundException; // Больше не используется напрямую для выбрасывания
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BracketService {

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;
    private final MatchRepository matchRepository;

    @Transactional
    public List<Match> generateBracket(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new NoSuchElementException("Турнир с ID " + tournamentId + " не найден.")); // Изменено на NoSuchElementException

        if (!tournament.getMatches().isEmpty()) {
            throw new BadRequestException("Сетка для этого турнира уже была сгенерирована."); // Изменено на BadRequestException
        }

        List<TournamentRegistration> approvedRegistrations = tournamentRegistrationRepository
                .findByTournamentIdAndStatus(tournamentId, RegistrationStatus.REGISTERED);

        int maxParticipants = tournament.getMaxParticipants();
        if (approvedRegistrations.size() < 2) {
            throw new BadRequestException("Недостаточно участников для генерации сетки (минимум 2)."); // Изменено на BadRequestException
        }

        if (approvedRegistrations.size() != maxParticipants) {
            System.err.println("Внимание: Количество участников (" + approvedRegistrations.size() +
                    ") не совпадает с вместимостью турнира (" + maxParticipants +
                    "). Текущая реализация без логики 'bye' может привести к неполной или некорректной сетке.");
            // Если бы это было активным исключением:
            // throw new BadRequestException("Количество участников должно соответствовать вместимости турнира для этой упрощенной генерации сетки.");
        }

        List<TournamentRegistration> participants = prepareAndSortParticipants(new ArrayList<>(approvedRegistrations), tournament);
        tournamentRegistrationRepository.saveAll(participants);

        List<Match> allMatchesInBracket = createMatchStructure(tournament);
        placeParticipants(allMatchesInBracket, participants, tournament);

        return matchRepository.saveAll(allMatchesInBracket);
    }

    private List<TournamentRegistration> prepareAndSortParticipants(List<TournamentRegistration> registrations, Tournament tournament) {
        for (TournamentRegistration reg : registrations) {
            float seedingRating = 0f;
            if (reg.getUser() != null && reg.getUser().getUserInfo() != null && reg.getUser().getUserInfo().getRating() != null) {
                seedingRating += reg.getUser().getUserInfo().getRating();
            }

            if (tournament.getTournamentType() == TournamentType.DOUBLES) {
                if (reg.getPartner() != null && reg.getPartner().getUserInfo() != null && reg.getPartner().getUserInfo().getRating() != null) {
                    seedingRating += reg.getPartner().getUserInfo().getRating();
                } else if (reg.getPartner() == null) { // Явно проверяем, если партнер отсутствует
                    throw new BadRequestException("Отсутствует партнер в парной регистрации: ID " + reg.getId() + " для пользователя " + (reg.getUser() != null ? reg.getUser().getEmail() : "N/A")); // Изменено на BadRequestException
                }
                // Если партнер есть, но без рейтинга - это не ошибка, а предупреждение (уже обрабатывается как 0)
            }
            reg.setSeedingRating(seedingRating);
        }

        registrations.sort(Comparator.comparing(TournamentRegistration::getSeedingRating, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TournamentRegistration::getId));
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
            int matchesInThisRound = maxParticipants / (int) Math.pow(2, round);
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

        List<Match> firstRoundMatches = allMatches.stream()
                .filter(m -> m.getRoundNumber() == 1)
                .sorted(Comparator.comparing(Match::getMatchNumberInBracket))
                .collect(Collectors.toList());

        if (sortedParticipants.size() > tournament.getMaxParticipants()) {
            sortedParticipants = sortedParticipants.subList(0, tournament.getMaxParticipants());
        }

        List<TournamentRegistration> seeds = new ArrayList<>();
        List<TournamentRegistration> others = new ArrayList<>();

        for (int i = 0; i < sortedParticipants.size(); i++) {
            TournamentRegistration reg = sortedParticipants.get(i);
            if (i < numberOfSeeds && i < sortedParticipants.size()) {
                reg.setSeedNumber(i + 1);
                seeds.add(reg);
            } else {
                others.add(reg);
            }
        }
        if (!seeds.isEmpty()) {
            tournamentRegistrationRepository.saveAll(seeds);
        }

        TournamentRegistration[] slots = new TournamentRegistration[tournament.getMaxParticipants()];

        if (seeds.size() >= 1) slots[0] = seeds.get(0);
        if (seeds.size() >= 2) slots[tournament.getMaxParticipants() - 1] = seeds.get(1);
        if (seeds.size() >= 3) {
            int slot3Index = tournament.getMaxParticipants() / 2;
            slots[slot3Index] = seeds.get(2);
        }
        if (seeds.size() >= 4) {
            int slot4Index = tournament.getMaxParticipants() / 2 - 1;
            if(slot4Index == 0 && slots[0] != null && seeds.size() > 1 && slots[0].equals(seeds.get(0))) slot4Index = 1; // Check if S1 is already there
            slots[slot4Index] = seeds.get(3);
        }

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
        unplacedParticipants.sort(Comparator.comparing(TournamentRegistration::getSeedNumber, Comparator.nullsLast(Comparator.naturalOrder())));

        // Перемешиваем только тех, кто действительно несеяный (seedNumber is null)
        // или тех сеяных, которые не были размещены по специальным правилам
        long placedByRuleCount = Arrays.stream(slots).filter(Objects::nonNull).count();
        long unplacedSeedsCount = unplacedParticipants.stream().filter(p -> p.getSeedNumber() != null).count();

        if (!unplacedParticipants.isEmpty()) {
            // Сеяные, которые не были размещены по правилам, не должны перемешиваться случайно,
            // они должны занимать следующие лучшие слоты или быть размещены по более сложным правилам.
            // Случайное перемешивание здесь применяется только к действительно несеяным.
            List<TournamentRegistration> trulyUnseeded = unplacedParticipants.stream()
                    .filter(p -> p.getSeedNumber() == null)
                    .collect(Collectors.toList());
            Collections.shuffle(trulyUnseeded);

            // Сборка обратно: сначала "неразмещенные сеяные", потом "перемешанные несеяные"
            List<TournamentRegistration> finalUnplacedOrder = unplacedParticipants.stream()
                    .filter(p -> p.getSeedNumber() != null)
                    .collect(Collectors.toList());
            finalUnplacedOrder.addAll(trulyUnseeded);
            unplacedParticipants = finalUnplacedOrder;
        }


        int unplacedIdx = 0;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null && unplacedIdx < unplacedParticipants.size()) {
                slots[i] = unplacedParticipants.get(unplacedIdx++);
            }
        }

        int currentSlotIndex = 0;
        for (Match match : firstRoundMatches) {
            if (currentSlotIndex < slots.length) {
                assignParticipantToMatch(match, slots[currentSlotIndex++], 1);
            }
            if (currentSlotIndex < slots.length) {
                assignParticipantToMatch(match, slots[currentSlotIndex++], 2);
            }

            if (match.getParticipant1() != null && match.getParticipant2() != null) {
                match.setStatus(MatchStatus.SCHEDULED);
            } else if (match.getParticipant1() != null || match.getParticipant2() != null) {
                TournamentRegistration byeWinner = match.getParticipant1() != null ? match.getParticipant1() : match.getParticipant2();
                match.setWinner(byeWinner);
                match.setScore("BYE"); // Можно использовать константу
                match.setStatus(MatchStatus.WALKOVER);
                match.setCompletedTime(LocalDateTime.now());
            }
        }
    }

    private void assignParticipantToMatch(Match match, TournamentRegistration registration, int slot) {
        if (registration == null) {
            return;
        }
        if (slot == 1) {
            match.setParticipant1(registration);
        } else {
            match.setParticipant2(registration);
        }
    }
}