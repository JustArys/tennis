package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.City;
import com.example.tennis.kz.model.Coach;
import com.example.tennis.kz.model.Language;
import com.example.tennis.kz.model.request.CoachRequest;
import com.example.tennis.kz.model.response.CustomPageResponse;
import com.example.tennis.kz.service.CoachService;
import com.example.tennis.kz.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/coach")
@RequiredArgsConstructor
public class CoachController {

    private final CoachService coachService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> findAllCoaches(@RequestParam Boolean enabled) {
        return ResponseEntity.ok(coachService.getAllCoaches(enabled));
    }

    @GetMapping("/all")
    public ResponseEntity<?> findAllCoaches() {
        return ResponseEntity.ok(coachService.getAllCoaches());
    }

    @GetMapping("/page")
    public ResponseEntity<?> findAllCoaches(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam Boolean enabled) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));

        Page<Coach> coaches = coachService.getAllCoaches(pageable, enabled);
        return ResponseEntity.ok(new CustomPageResponse<Coach>(coaches.getNumber() + 1, coaches.getSize(), coaches.getTotalElements(), coaches.getContent()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findCoachById(@PathVariable Long id) {
        return ResponseEntity.ok(coachService.getCoachById(id));
    }

    @PatchMapping("enable/{id}")
    public ResponseEntity<?> enableCoach(@PathVariable Long id ) {
        return ResponseEntity.ok(coachService.enableCoach(id));
    }

    @PostMapping
    public ResponseEntity<?> addCoach(@RequestBody CoachRequest coach) {
        return ResponseEntity.ok(coachService.addCoach(userService.getAuthenticatedUser(), coach));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCoach(
            @PathVariable Long id,
            @RequestParam(required = false) City city,
            @RequestParam(required = false) Set<Language> language,
            @RequestParam(required = false) Float cost,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer experience,
            @RequestParam(required = false) String stadium) {

        Coach updatedCoach = coachService.updateCoachParams(
                id, city, language, cost, service, description, experience, stadium
        );
        return ResponseEntity.ok(updatedCoach);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCoach(@PathVariable Long id) {
        coachService.deleteCoach(id);
        return ResponseEntity.ok().build();
    }
}
