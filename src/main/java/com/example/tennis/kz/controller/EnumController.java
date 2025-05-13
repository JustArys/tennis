package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.*;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/enum")
public class EnumController {
    private final ResourceLoader resourceLoader;

    public EnumController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("/city")
    public ResponseEntity<?> getAllCities() {
        List<City> cities = Arrays.asList(City.values());
        return ResponseEntity.ok(cities);
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getAllRoles() {
        return ResponseEntity.ok(Arrays.asList(Role.values()));
    }

    @GetMapping("/category")
    public ResponseEntity<?> getAllCategories() {
        return ResponseEntity.ok(Arrays.asList(Category.values()));
    }

    @GetMapping("/language")
    public ResponseEntity<?> getAllLanguages() {
        return ResponseEntity.ok(Arrays.asList(Language.values()));
    }

    @GetMapping("/gender")
    public ResponseEntity<?> getAllGenders() {
        return ResponseEntity.ok(Arrays.asList(Gender.values()));
    }

    @GetMapping("/service")
    public ResponseEntity<?> getAllServices() {
        return ResponseEntity.ok(Arrays.asList(CoachService.GET_ALL_SERVICES));
    }

    @GetMapping("/tier")
    public ResponseEntity<?> getAllTiers() {
        return ResponseEntity.ok(Arrays.asList(TournamentTier.values()));
    }

}
