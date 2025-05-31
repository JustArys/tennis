package com.example.tennis.kz.service;

import com.example.tennis.kz.exception.BadRequestException; // Импорт
import com.example.tennis.kz.model.City;
import com.example.tennis.kz.model.Partner;
import com.example.tennis.kz.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // Убедимся, что Pageable импортирован
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PartnerService {
    private final PartnerRepository partnerRepository;

    public List<Partner> getAllPartners(Boolean enabled) {
        // enabled может быть null, если это допустимо по логике.
        // Если enabled всегда должен быть true/false, можно добавить проверку.
        return partnerRepository.findByEnabled(enabled);
    }

    public Partner getPartnerById(Long id) {
        if (id == null) {
            throw new BadRequestException("ID партнера не может быть null.");
        }
        return partnerRepository.findById(id).orElseThrow(()
                -> new NoSuchElementException(String.format("Партнер с ID '%d' не найден.", id)));
    }

    public List<Partner> getAllPartners() {
        return partnerRepository.findAll();
    }

    public Page<Partner> getPartners(Pageable pageable, Boolean enabled) {
        if (pageable == null) {
            throw new BadRequestException("Pageable не может быть null.");
        }
        // enabled может быть null, если это допустимо.
        return partnerRepository.findAllByEnabled(enabled, pageable);
    }

    public Partner enablePartner(Long id) {
        Partner partner = getPartnerById(id); // getPartnerById уже содержит проверки
        partner.setEnabled(true);
        return partnerRepository.save(partner);
    }

    public Partner createPartner(Partner partner) {
        if (partner == null) {
            throw new BadRequestException("Объект партнера не может быть null.");
        }
        if (partner.getFirstName() == null || partner.getFirstName().trim().isEmpty()) {
            throw new BadRequestException("Имя партнера не может быть пустым.");
        }
        if (partner.getLastName() == null || partner.getLastName().trim().isEmpty()) {
            throw new BadRequestException("Фамилия партнера не может быть пустой.");
        }
        if (partner.getPhone() == null || partner.getPhone().trim().isEmpty()) {
            throw new BadRequestException("Телефон партнера не может быть пустым.");
        }
        if (partner.getCity() == null) {
            throw new BadRequestException("Город для партнера должен быть указан.");
        }
        if (partner.getRating() != null && partner.getRating() < 0) {
            throw new BadRequestException("Рейтинг не может быть отрицательным.");
        }

        var newPartner = Partner.builder()
                .enabled(false) // По умолчанию false
                .phone(partner.getPhone())
                .firstName(partner.getFirstName())
                .lastName(partner.getLastName())
                .rating(partner.getRating())
                .city(partner.getCity())
                .stadium(partner.getStadium())
                .description(partner.getDescription())
                .createdAt(LocalDateTime.now())
                // updatedAt будет null при создании
                .build();
        return partnerRepository.save(newPartner);
    }

    public Partner updatePartnerParams(
            Long id,
            String phone,
            String firstName,
            String lastName,
            Float rating,
            City city,
            String stadium,
            String description) {

        Partner partner = getPartnerById(id); // getPartnerById уже содержит проверки для id и существования

        if (phone != null) {
            if (phone.trim().isEmpty()) throw new BadRequestException("Телефон не может быть пустой строкой при обновлении.");
            partner.setPhone(phone);
        }
        if (firstName != null) {
            if (firstName.trim().isEmpty()) throw new BadRequestException("Имя не может быть пустой строкой при обновлении.");
            partner.setFirstName(firstName);
        }
        if (lastName != null) {
            if (lastName.trim().isEmpty()) throw new BadRequestException("Фамилия не может быть пустой строкой при обновлении.");
            partner.setLastName(lastName);
        }
        if (rating != null) {
            if (rating < 0) throw new BadRequestException("Рейтинг не может быть отрицательным.");
            partner.setRating(rating);
        }
        if (city != null) {
            partner.setCity(city);
        }
        if (stadium != null) {
            // if (stadium.trim().isEmpty()) throw new BadRequestException("Стадион не может быть пустой строкой при обновлении, если указан.");
            partner.setStadium(stadium); // Пустая строка может быть валидна для очистки поля
        }
        if (description != null) {
            // if (description.trim().isEmpty()) throw new BadRequestException("Описание не может быть пустой строкой при обновлении, если указано.");
            partner.setDescription(description); // Пустая строка может быть валидна для очистки поля
        }
        partner.setUpdatedAt(LocalDateTime.now());

        return partnerRepository.save(partner);
    }

    public void deletePartner(Long id) {
        if (id == null) {
            throw new BadRequestException("ID партнера для удаления не может быть null.");
        }
        if (!partnerRepository.existsById(id)) {
            throw new NoSuchElementException(String.format("Партнер с ID '%d' не найден, удаление невозможно.", id));
        }
        partnerRepository.deleteById(id);
    }
}