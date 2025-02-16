package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.Category;
import com.example.tennis.kz.model.City;
import com.example.tennis.kz.model.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/enum")
public class EnumController {
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
}
