package com.mycompany.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.mycompany.exception.NotFoundException;
import com.mycompany.models.Clinic;
import com.mycompany.models.Slot;
import com.mycompany.repo.ClinicRepository;
import com.mycompany.repo.SlotRepository;

@Service
public class ClinicService {

    private final ClinicRepository repo;
    private final SlotRepository slotRepo;

    public ClinicService(ClinicRepository repo, SlotRepository slotRepo) {
        this.repo = repo;
        this.slotRepo = slotRepo;
    }

    public Page<Clinic> getClinics(String zip, String city, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (zip != null && city != null) {
            return repo.findByAddressZipAndAddressCity(zip, city, pageable);
        }
        if (zip != null) {
            return repo.findByAddressZip(zip, pageable);
        }
        if (city != null) {
            return repo.findByAddressCity(city, pageable);
        }
        return repo.findAll(pageable);
    }

    public Clinic getClinicById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Clinic not found: " + id));
    }

    public List<Slot> getSlotsForClinic(UUID clinicId, String date) {
        if (!repo.existsById(clinicId)) {
            throw new NotFoundException("Clinic not found: " + clinicId);
        }
        return slotRepo.findByClinicIdAndDateTimeStartingWith(clinicId, date);
    }
}
