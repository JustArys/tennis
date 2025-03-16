package com.example.tennis.kz.service;

import com.example.tennis.kz.model.City;
import com.example.tennis.kz.model.Partner;
import com.example.tennis.kz.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PartnerService {
    private final PartnerRepository partnerRepository;

    public List<Partner> getAllPartners(Boolean enabled) {
        return partnerRepository.findByEnabled(enabled);
    }

    public Partner getPartnerById(Long id) {
        return partnerRepository.findById(id).orElseThrow(()
                -> new NoSuchElementException(String.format("No Partner found with '%d'", id)));
    }

    public List<Partner> getAllPartners() {
        return partnerRepository.findAll();
    }

    public Page<Partner> getPartners(int page) {
        Pageable pageable = PageRequest.of(page, 10);
        return partnerRepository.findAll(pageable);
    }
    public Partner enablePartner(Long id) {
        Partner partner = getPartnerById(id);
        partner.setEnabled(true);
        return partnerRepository.save(partner);
    }

    public Partner createPartner(Partner partner) {
        var newPartner = Partner.builder()
                .enabled(false)
                .phone(partner.getPhone())
                .firstName(partner.getFirstName())
                .lastName(partner.getLastName())
                .rating(partner.getRating())
                .city(partner.getCity())
                .stadium(partner.getStadium())
                .description(partner.getDescription())
                .createdAt(LocalDateTime.now())
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

        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Partner not found with id " + id));

        if (phone != null) {
            partner.setPhone(phone);
        }
        if (firstName != null) {
            partner.setFirstName(firstName);
        }
        if (lastName != null) {
            partner.setLastName(lastName);
        }
        if (rating != null) {
            partner.setRating(rating);
        }
        if (city != null) {
            partner.setCity(city);
        }
        if (stadium != null) {
            partner.setStadium(stadium);
        }
        if (description != null) {
            partner.setDescription(description);
        }
        // Update the updatedAt field to the current time
        partner.setUpdatedAt(LocalDateTime.now());

        return partnerRepository.save(partner);
    }
    public void deletePartner(Long id) {
        partnerRepository.deleteById(id);
    }
}
