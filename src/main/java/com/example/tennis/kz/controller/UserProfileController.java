package com.example.tennis.kz.controller;


import com.example.tennis.kz.model.response.MatchHistoryDTO;
import com.example.tennis.kz.model.response.UserStatsDTO;
import com.example.tennis.kz.service.UserActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1") // Общий префикс для этих эндпоинтов
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints for accessing user-specific match history and statistics")
public class UserProfileController {

    private final UserActivityService userActivityService;

    @GetMapping("/profile/matches")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get match history for the authenticated user",
            description = "Returns a paginated list of matches played by the currently logged-in user.")
    public ResponseEntity<Page<MatchHistoryDTO>> getCurrentUserMatchHistory(
            @Parameter(description = "Page number (1-indexed)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size) {

        // Корректная сортировка: сначала по времени завершения (новые вверху, NULLS LAST),
        // затем по времени начала (новые вверху, NULLS LAST), затем по ID для стабильности.
        Sort sort = Sort.by(
                Sort.Order.desc("completedTime").nullsLast(),
                Sort.Order.desc("scheduledTime").nullsLast(),
                Sort.Order.desc("id") // Для уникальности порядка, если время совпадает
        );
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<MatchHistoryDTO> matchHistory = userActivityService.getCurrentUserMatchHistory(pageable);
        return ResponseEntity.ok(matchHistory);
    }

    @GetMapping("/profile/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get statistics for the authenticated user",
            description = "Returns various gameplay statistics for the currently logged-in user.")
    public ResponseEntity<UserStatsDTO> getCurrentUserStats() {
        UserStatsDTO stats = userActivityService.getCurrentUserStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users/{userId}/matches")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Get match history for a specific user",
            description = "Returns a paginated list of matches for a given user ID. Requires ADMIN role or for the userId to match the authenticated user.")
    public ResponseEntity<Page<MatchHistoryDTO>> getUserMatchHistoryById(
            @Parameter(description = "ID of the user") @PathVariable Long userId,
            @Parameter(description = "Page number (1-indexed)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size) {

        Sort sort = Sort.by(
                Sort.Order.desc("completedTime").nullsLast(),
                Sort.Order.desc("scheduledTime").nullsLast(),
                Sort.Order.desc("id")
        );
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<MatchHistoryDTO> matchHistory = userActivityService.getUserMatchHistory(userId, pageable);
        return ResponseEntity.ok(matchHistory);
    }

    @GetMapping("/users/{userId}/stats")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Get statistics for a specific user",
            description = "Returns gameplay statistics for a given user ID. Requires ADMIN role or for the userId to match the authenticated user.")
    public ResponseEntity<UserStatsDTO> getUserStatsById(
            @Parameter(description = "ID of the user") @PathVariable Long userId) {
        UserStatsDTO stats = userActivityService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }
}