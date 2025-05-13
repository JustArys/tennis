package com.example.tennis.kz.repository;

import com.example.tennis.kz.model.RegistrationStatus;
import com.example.tennis.kz.model.TournamentRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, Long> {
    List<TournamentRegistration> findByTournamentIdAndStatus(Long tournamentId, RegistrationStatus status);
    List<TournamentRegistration> findByPartnerIdAndStatus(Long partnerId, RegistrationStatus status);
    List<TournamentRegistration> findByUserIdOrPartnerIdAndStatusIn(
            Long userId,
            Long partnerId,
            Collection<RegistrationStatus> statuses
    );
    @Query("SELECT r FROM TournamentRegistration r " +
            "WHERE r.tournament.id = :tId " +
            "  AND (r.user.id = :uId OR r.partner.id = :uId)")
    Optional<TournamentRegistration> findByTournamentIdAndUserOrPartner(
            @Param("tId") Long tournamentId,
            @Param("uId") Long userId
    );

    List<TournamentRegistration> findByTournamentId(Long tournamentId);

    boolean existsByTournamentId(Long id);
}
