package com.example.tennis.kz.repository;


import com.example.tennis.kz.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
}
