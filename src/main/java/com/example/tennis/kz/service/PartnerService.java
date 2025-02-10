package com.example.tennis.kz.service;

import com.example.tennis.kz.model.Partner;
import com.example.tennis.kz.repository.PartnerRepository;
import jakarta.mail.Part;
import lombok.RequiredArgsConstructor;
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

    public Partner updatePartner(Long id, Partner partner) {
        return partnerRepository.findById(id).map(partnerNew ->
        {
            partnerNew.setPhone(partner.getPhone());
            partnerNew.setFirstName(partner.getFirstName());
            partnerNew.setLastName(partner.getLastName());
            partnerNew.setRating(partner.getRating());
            partnerNew.setCity(partner.getCity());
            partnerNew.setStadium(partner.getStadium());
            partnerNew.setDescription(partner.getDescription());
            partnerNew.setEnabled(partner.getEnabled());
            return partnerRepository.save(partnerNew);
        }).orElseThrow(() -> new NoSuchElementException(String.format("No Partner found with '%d'", id)));
    }

    public void deletePartner(Long id) {
        partnerRepository.deleteById(id);
    }
}
