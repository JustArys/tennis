package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.Gender;
import com.example.tennis.kz.model.Role;
import com.example.tennis.kz.model.User;
import com.example.tennis.kz.model.UserInfo;
import com.example.tennis.kz.model.response.CustomPageResponse;
import com.example.tennis.kz.model.response.UserSearchResultDto;
import com.example.tennis.kz.service.TournamentRegistrationService;
import com.example.tennis.kz.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TournamentRegistrationService registrationService;


    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestParam(required = false) Gender gender,
    @RequestParam(required = false) String firstName,
    @RequestParam(required = false) String lastName,
    @RequestParam(required = false) String phone,
    @RequestParam(required = false) Float rating,
    @RequestParam(required = false) Integer age) {
        var user = userService.updateUser(userService.getAuthenticatedUser(), gender, firstName, lastName, phone, rating, age);
        return ResponseEntity.ok(user);
    }
    @GetMapping("/authenticated")
    public ResponseEntity<?> getAuthenticatedUserId() {
        return ResponseEntity.ok(userService.getAuthenticatedUser());
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findUserById(id));
    }

    @RequestMapping(value = "/confirmemail", method = {RequestMethod.GET, RequestMethod.POST})
    private ResponseEntity<?> confirmEmail(@RequestParam("token") String token) {
        return userService.confirmEmail(token);
    }

    @PutMapping("/role")
    public ResponseEntity<?> updateRole(@RequestBody Role role) {
        return ResponseEntity.ok(userService.updateUserRole(userService.getAuthenticatedUser(), role));
    }

    @GetMapping("/invintations")
    public ResponseEntity<?> getInvitations() {
        return ResponseEntity.ok(userService.findAllRegistrations(userService.getAuthenticatedUser()));
    }

    @GetMapping("/tournament")
    public ResponseEntity<?> getTournament() {
        return ResponseEntity.ok(userService.findAllTournaments(userService.getAuthenticatedUser()));
    }

    @GetMapping("/all")
    private ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    @GetMapping("/page")
    private ResponseEntity<?> getAllUsersPagination(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page-1, size, Sort.by(Sort.Order.desc("createdAt")));

        Page<User> users = userService.findAllUsers(pageable);
        return ResponseEntity.ok(new CustomPageResponse<>(users.getNumber() + 1, users.getSize(), users.getTotalElements(), users.getContent()));
    }
    @DeleteMapping("/all")
    public ResponseEntity<String> deleteAllUsers() {
        try {
            userService.deleteUsers();
            return ResponseEntity.ok("All users and related data have been successfully deleted.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete users: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam(name = "name") String nameQuery,
            @RequestParam(defaultValue = "1") int page, // Номер страницы (начиная с 0)
            @RequestParam(defaultValue = "10") int size) { // Количество результатов на странице

        if (nameQuery.isBlank()) {
            return ResponseEntity.badRequest().body("Параметр 'name' не может быть пустым.");
        }

        // Ограничиваем размер страницы для безопасности и производительности
        int finalSize = Math.min(size, 50); // Например, не больше 50 результатов за раз

        Pageable pageable = PageRequest.of(page-1, finalSize, Sort.by("userInfo.firstName").ascending().and(Sort.by("userInfo.lastName").ascending()));

        Page<UserSearchResultDto> results = userService.searchUsersByName(nameQuery, pageable);


        // Или используем твой CustomPageResponse, если он есть
        CustomPageResponse<UserSearchResultDto> response = new CustomPageResponse<>(
                results.getNumber() + 1, // +1 если у тебя нумерация с 1
                results.getSize(),
                results.getTotalElements(),
                results.getContent()
        );
        return ResponseEntity.ok(response);
    }
}
