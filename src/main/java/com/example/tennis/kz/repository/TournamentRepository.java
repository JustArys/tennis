package com.example.tennis.kz.repository;


import com.example.tennis.kz.model.Category;
import com.example.tennis.kz.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findByCategories(Set<Category> categories);
}
