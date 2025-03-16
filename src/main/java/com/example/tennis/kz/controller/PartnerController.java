package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.City;
import com.example.tennis.kz.model.Partner;
import com.example.tennis.kz.service.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/partner")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @GetMapping
    public ResponseEntity<?> findAllPartner(@RequestParam Boolean enabled) {
        return ResponseEntity.ok(partnerService.getAllPartners(enabled));
    }

    @GetMapping("/all")
    public ResponseEntity<?> findAllPartner() {
        return ResponseEntity.ok(partnerService.getAllPartners());
    }

    @GetMapping("/page")
    public ResponseEntity<?> findAllPartner(@RequestParam(defaultValue = "0") int page){
        return ResponseEntity.ok(partnerService.getPartners(page));
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> findPartner(@PathVariable Long id) {
        return ResponseEntity.ok(partnerService.getPartnerById(id));
    }

    @PostMapping
    public ResponseEntity<?> addPartner(@RequestBody Partner partner) {
        return ResponseEntity.ok(partnerService.createPartner(partner));
    }

    @PatchMapping("enable/{id}")
    public ResponseEntity<?> enablePartner(@PathVariable Long id) {
        return ResponseEntity.ok(partnerService.enablePartner(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePartner(
            @PathVariable Long id,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) Float rating,
            @RequestParam(required = false) City city,
            @RequestParam(required = false) String stadium,
            @RequestParam(required = false) String description) {

        Partner updatedPartner = partnerService.updatePartnerParams(
                id, phone, firstName, lastName, rating, city, stadium, description
        );
        return ResponseEntity.ok(updatedPartner);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePartner(@PathVariable Long id) {
        partnerService.deletePartner(id);
        return ResponseEntity.ok().build();
    }
}
