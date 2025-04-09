package com.example.tennis.kz.service;

import com.example.tennis.kz.model.*;
import com.example.tennis.kz.model.response.UserSearchResultDto;
import com.example.tennis.kz.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.NonUniqueObjectException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final ConfirmationTokenService confirmationTokenService;
    private final UserInfoRepository userInfoRepository;
    private final TournamentRegistrationRepository registrationRepository;

    public UserDetailsService userDetailsService() {
        return username -> userRepository.findUserByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public User updateUser(User user, Gender gender, String firstName, String lastName, String phone, Float rating, Integer age) {
        UserInfo userInfo = user.getUserInfo();

        if (gender != null) {
            userInfo.setGender(gender);
        }
        if (firstName != null) {
            userInfo.setFirstName(firstName);
        }
        if (lastName != null) {
            userInfo.setLastName(lastName);
        }
        if (phone != null) {
            userInfo.setPhone(phone);
        }
        if (rating != null) {
            userInfo.setRating(rating);
        }
        if (age != null) {
            userInfo.setAge(age);
        }

        userRepository.save(user);
        return user;
    }

    public ResponseEntity<?> confirmEmail(String token) {
        var confirmationToken = confirmationTokenRepository.findConfirmationTokenByConfirmationToken(token);
        User user = confirmationToken.orElseThrow().getUser();
        if (user.isEnabled()) return ResponseEntity.ok("email already verified");
        user.setEnabled(true);
        userRepository.save(user);
        return ResponseEntity.ok("email successfully verified");
    }

    public User saveUser(User user) {
        if (userRepository.existsByEmail(user.getEmail()))
            throw new NonUniqueObjectException("", null, user.getEmail());
        userRepository.save(user);
        confirmationTokenService.sendConfirmationToken(user);
        return user;
    }

    public User findUserById(Long id) {
        return userRepository.findById(id).orElseThrow(()
                -> new NoSuchElementException(String.format("User with id '%d' not found", id)));
    }

    public User findUserByEmail(String email) {
        return userRepository.findUserByEmail(email).orElseThrow(()
                -> new NoSuchElementException(String.format("User with email '%d' not found", email)));
    }

    public User updateUserRole(User user, Role role){
        user.setRole(role);
        return userRepository.save(user);
    }
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<TournamentRegistration> findAllRegistrations(User user) {
        return registrationRepository.findByPartnerIdAndStatus(user.getId(), RegistrationStatus.PENDING_PARTNER);
    }

    public List<Tournament> findAllTournaments(User user) {
        // Скажем, хотим видеть только зарегистрированные и ожидающие подтверждения
        List<RegistrationStatus> allowed = List.of(RegistrationStatus.REGISTERED, RegistrationStatus.PENDING_PARTNER);

        // Запрос к БД: выдаст только нужные записи
        List<TournamentRegistration> registrations =
                registrationRepository.findByUserIdOrPartnerIdAndStatusIn(
                        user.getId(),
                        user.getId(),
                        allowed
                );

        // Собираем уникальные турниры в Set
        Set<Tournament> tournaments = new HashSet<>();
        for (TournamentRegistration reg : registrations) {
            // Если в БД уже фильтруются статусы, дальше можно не проверять
            tournaments.add(reg.getTournament());
        }
        return new ArrayList<>(tournaments);
    }

    public Page<User> findAllUsers(Pageable page) {
        return userRepository.findAll(page);
    }

    public Page<UserSearchResultDto> searchUsersByName(String nameQuery, Pageable pageable) {
        if (nameQuery == null || nameQuery.isBlank()) {
            // Если запрос пустой, возвращаем пустую страницу
            return Page.empty(pageable);
        }
        // Вызываем метод репозитория
        Page<User> userPage = userRepository.searchByFirstNameOrLastNameStartsWith(nameQuery.trim(), pageable);

        // Конвертируем Page<User> в Page<UserSearchResultDto>
        List<UserSearchResultDto> dtoList = userPage.getContent().stream()
                .map(user -> {
                    UserInfo info = user.getUserInfo(); // Получаем UserInfo
                    return UserSearchResultDto.builder()
                            .id(user.getId())
                            .firstName(info != null ? info.getFirstName() : null) // Проверяем info на null
                            .lastName(info != null ? info.getLastName() : null)
                            .rating(info != null ? info.getRating() : null)
                            .build();
                })
                .collect(Collectors.toList());

        // Создаем и возвращаем страницу с DTO
        return new PageImpl<>(dtoList, pageable, userPage.getTotalElements());
    }

    @Transactional
    public void deleteUsers() {
        confirmationTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        userInfoRepository.deleteAll();
    }
}