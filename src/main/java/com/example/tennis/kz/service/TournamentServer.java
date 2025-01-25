package com.example.tennis.kz.service;

import com.example.tennis.kz.model.Tournament;
import com.example.tennis.kz.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TournamentServer {
    private final TournamentRepository tournamentRepository;


}
