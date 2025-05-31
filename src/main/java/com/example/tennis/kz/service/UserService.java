package com.example.tennis.kz.service;

import com.example.tennis.kz.exception.BadRequestException; // Наш кастомный BadRequestException
import com.example.tennis.kz.model.*;
import com.example.tennis.kz.model.response.UserSearchResultDto;
import com.example.tennis.kz.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
// import org.hibernate.NonUniqueObjectException; // Будет заменен
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
// import org.springframework.data.domain.PageRequest; // Не используется напрямую здесь
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с email: " + username + " не найден."));
    }

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Пользователь не аутентифицирован или аутентификационные данные отсутствуют.");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            // Если principal - это строка "anonymousUser", то это тоже проблема аутентификации
            // Spring Security обычно возвращает 401/403 до этого момента.
            throw new IllegalStateException("Аутентифицированный principal не является экземпляром класса User.");
        }
        return (User) principal;
    }

    public User updateUser(User user, Gender gender, String firstName, String lastName, String phone, Float rating, Integer age) {
        if (user == null) {
            throw new BadRequestException("Объект User для обновления не может быть null.");
        }
        UserInfo userInfo = user.getUserInfo();
        if (userInfo == null) {
            // Это указывает на неконсистентное состояние объекта User
            throw new IllegalStateException("UserInfo для пользователя ID " + user.getId() + " не может быть null при обновлении.");
        }

        // Дополнительные валидации для передаваемых параметров, если необходимо
        if (rating != null && rating < 0) {
            throw new BadRequestException("Рейтинг не может быть отрицательным.");
        }
        if (age != null && age <= 0) {
            throw new BadRequestException("Возраст должен быть положительным числом.");
        }
        // Валидация формата телефона и т.д.

        if (gender != null) userInfo.setGender(gender);
        if (firstName != null) {
            if(firstName.trim().isEmpty() && user.getUserInfo().getFirstName() != null) throw new BadRequestException("Имя не может быть очищено пустой строкой, если оно уже было установлено.");
            userInfo.setFirstName(firstName);
        }
        if (lastName != null) {
            if(lastName.trim().isEmpty() && user.getUserInfo().getLastName() != null) throw new BadRequestException("Фамилия не может быть очищена пустой строкой, если она уже была установлена.");
            userInfo.setLastName(lastName);
        }
        if (phone != null) userInfo.setPhone(phone);
        if (rating != null) userInfo.setRating(rating);
        if (age != null) userInfo.setAge(age);

        // userRepository.save(user) сохранит и User и UserInfo благодаря CascadeType.ALL
        return userRepository.save(user);
    }

    public ResponseEntity<?> confirmEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new BadRequestException("Токен подтверждения не может быть пустым.");
        }
        ConfirmationToken confirmationToken = confirmationTokenRepository.findConfirmationTokenByConfirmationToken(token)
                .orElseThrow(() -> new NoSuchElementException("Токен подтверждения: " + token + " не найден или истек."));

        User user = confirmationToken.getUser();
        if (user == null) {
            // Это нештатная ситуация, если токен есть, а пользователя нет
            throw new IllegalStateException("Не найден пользователь для токена подтверждения: " + token);
        }

        if (user.isEnabled()) {
            return ResponseEntity.ok("Email уже подтвержден.");
        }
        user.setEnabled(true);
        userRepository.save(user);
        return ResponseEntity.ok("Email успешно подтвержден.");
    }

    @Transactional // Важно для консистентности (сохранение + отправка токена)
    public User saveUser(User user) {
        if (user == null) {
            throw new BadRequestException("Объект User для сохранения не может быть null.");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email пользователя не может быть пустым.");
        }
        // Добавить другие проверки на обязательные поля user и user.getUserInfo()

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BadRequestException("Пользователь с email: " + user.getEmail() + " уже существует.");
        }

        // Убедимся, что UserInfo создается, если его нет (хотя по вашей логике он передается в user)
        if (user.getUserInfo() == null) {
            // Если UserInfo обязателен, то это ошибка
            throw new BadRequestException("UserInfo не может быть null для нового пользователя.");
            // Или создать пустой UserInfo: user.setUserInfo(new UserInfo());
        }

        userRepository.save(user);
        // Отправка токена подтверждения должна происходить только при успешном сохранении
        confirmationTokenService.sendConfirmationToken(user);
        return user;
    }

    public User findUserById(Long id) {
        if (id == null) {
            throw new BadRequestException("ID пользователя не может быть null.");
        }
        return userRepository.findById(id).orElseThrow(()
                -> new NoSuchElementException(String.format("Пользователь с ID '%d' не найден.", id)));
    }

    public User findUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email для поиска не может быть пустым.");
        }
        return userRepository.findUserByEmail(email).orElseThrow(()
                // Корректный форматтер для строки
                -> new NoSuchElementException(String.format("Пользователь с email '%s' не найден.", email)));
    }

    public User updateUserRole(User user, Role role) {
        if (user == null) {
            throw new BadRequestException("Объект User для обновления роли не может быть null.");
        }
        if (role == null) {
            throw new BadRequestException("Новая роль не может быть null.");
        }
        user.setRole(role);
        return userRepository.save(user);
    }

    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            // В зависимости от контракта метода:
            // return false; // если невалидный email означает "не существует"
            throw new BadRequestException("Email для проверки существования не может быть пустым.");
        }
        return userRepository.existsByEmail(email);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<TournamentRegistration> findAllRegistrations(User user) {
        if (user == null || user.getId() == null) {
            throw new BadRequestException("Пользователь или ID пользователя не может быть null для поиска регистраций.");
        }
        return registrationRepository.findByPartnerIdAndStatus(user.getId(), RegistrationStatus.PENDING_PARTNER);
    }

    public List<Tournament> findAllTournaments(User user) {
        if (user == null || user.getId() == null) {
            throw new BadRequestException("Пользователь или ID пользователя не может быть null для поиска турниров.");
        }
        List<RegistrationStatus> allowed = List.of(RegistrationStatus.REGISTERED, RegistrationStatus.PENDING_PARTNER);
        List<TournamentRegistration> registrations =
                registrationRepository.findByUserIdOrPartnerIdAndStatusIn(
                        user.getId(),
                        user.getId(),
                        allowed
                );
        Set<Tournament> tournaments = new HashSet<>();
        for (TournamentRegistration reg : registrations) {
            if (reg.getTournament() == null) {
                // Это указывает на возможную проблему с данными или логикой
                throw new IllegalStateException("Регистрация ID " + reg.getId() + " ссылается на null турнир.");
            }
            tournaments.add(reg.getTournament());
        }
        return new ArrayList<>(tournaments);
    }

    public Page<User> findAllUsers(Pageable pageable) {
        if (pageable == null) {
            throw new BadRequestException("Pageable не может быть null.");
        }
        return userRepository.findAll(pageable);
    }

    public Page<UserSearchResultDto> searchUsersByName(String nameQuery, Pageable pageable) {
        if (pageable == null) {
            throw new BadRequestException("Pageable не может быть null.");
        }
        // nameQuery может быть null или пустым, это обрабатывается (возвращается пустая страница)
        if (nameQuery == null || nameQuery.isBlank()) {
            return Page.empty(pageable);
        }
        Page<User> userPage = userRepository.searchByFirstNameOrLastNameStartsWith(nameQuery.trim(), pageable);
        List<UserSearchResultDto> dtoList = userPage.getContent().stream()
                .map(user -> {
                    UserInfo info = user.getUserInfo();
                    return UserSearchResultDto.builder()
                            .id(user.getId())
                            .firstName(info != null ? info.getFirstName() : null)
                            .lastName(info != null ? info.getLastName() : null)
                            .rating(info != null ? info.getRating() : null)
                            .build();
                })
                .collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, userPage.getTotalElements());
    }

    @Transactional // Убедимся, что @Transactional на месте
    public void deleteUsers() {

        confirmationTokenRepository.deleteAll(); // Удаляем ConfirmationToken
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    public Page<UserInfo> getPaginatedUserInfo(Pageable pageable) {
        if (pageable == null) {
            throw new BadRequestException("Pageable не может быть null.");
        }
        return userInfoRepository.findAllByPointsIsNotNull(pageable);
    }
}