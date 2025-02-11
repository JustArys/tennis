package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.Coach;
import com.example.tennis.kz.model.request.CoachRequest;
import com.example.tennis.kz.service.CoachService;
import com.example.tennis.kz.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> findAllCoaches(@RequestParam(defaultValue = "0") int page){
        return ResponseEntity.ok(coachService.getAllCoaches(page));
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
    public ResponseEntity<?> updateCoach(@PathVariable Long id, @RequestBody CoachRequest coach) {
        return ResponseEntity.ok(coachService.updateCoach(id, coach));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCoach(@PathVariable Long id) {
        coachService.deleteCoach(id);
        return ResponseEntity.ok().build();
    }
}
