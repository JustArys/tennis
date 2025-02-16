package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.TournamentRegistration;
import com.example.tennis.kz.model.request.RegistrationRequest;
import com.example.tennis.kz.service.TournamentRegistrationService;
import com.example.tennis.kz.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/registration")
public class TournamentRegistrationController {

    private final TournamentRegistrationService registrationService;
    private final UserService userService;

    @PostMapping
    public TournamentRegistration createRegistration(@RequestBody RegistrationRequest request) {
        return registrationService.register(request);
    }

    @PutMapping("/{registrationId}/confirm")
    public TournamentRegistration confirmRegistration(@PathVariable Long registrationId,
                                                      @RequestParam Long partnerId) {
        return registrationService.confirmPartner(registrationId, partnerId);
    }

    @PutMapping("/{registrationId}/reject")
    public TournamentRegistration rejectRegistration(@PathVariable Long registrationId,
                                                     @RequestParam Long partnerId) {
        return registrationService.rejectPartner(registrationId, partnerId);
    }
    @PutMapping("/withdraw")
    public ResponseEntity<?> withdrawFromTournament(@RequestParam Long tournamentId){
        return ResponseEntity.ok(registrationService.withdrawFromTournament(tournamentId, userService.getAuthenticatedUser().getId()));
    }

}
