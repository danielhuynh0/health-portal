package com.mycompany.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.mycompany.exception.NotFoundException;
import com.mycompany.models.Vaccination;
import com.mycompany.repo.PatientRepository;
import com.mycompany.repo.VaccinationRepository;

@Service
public class VaccinationService {

    private final VaccinationRepository repo;
    private final PatientRepository patientRepo;

    public VaccinationService(VaccinationRepository repo, PatientRepository patientRepo) {
        this.repo = repo;
        this.patientRepo = patientRepo;
    }

    public List<Vaccination> getVaccinationsForPatient(UUID patientId) {
        requirePatient(patientId);
        return repo.findByPatientId(patientId);
    }

    public Vaccination getVaccination(UUID patientId, UUID vaccinationId) {
        requirePatient(patientId);
        Vaccination v = repo.findById(vaccinationId)
                .orElseThrow(() -> new NotFoundException("Vaccination not found: " + vaccinationId));
        if (!v.getPatientId().equals(patientId)) {
            throw new NotFoundException("Vaccination not found for patient: " + vaccinationId);
        }
        return v;
    }

    public Vaccination createVaccination(UUID patientId, Vaccination vaccination) {
        requirePatient(patientId);
        vaccination.setPatientId(patientId);
        return repo.save(vaccination);
    }

    public Vaccination updateVaccination(UUID patientId, UUID vaccinationId, Vaccination updated) {
        Vaccination existing = getVaccination(patientId, vaccinationId);
        updated.setId(existing.getId());
        updated.setPatientId(patientId);
        return repo.save(updated);
    }

    public void deleteVaccination(UUID patientId, UUID vaccinationId) {
        getVaccination(patientId, vaccinationId);
        repo.deleteById(vaccinationId);
    }

    public boolean hasAnyDueVaccination(UUID patientId) {
        return repo.findByPatientId(patientId).stream().anyMatch(Vaccination::isDue);
    }

    private void requirePatient(UUID patientId) {
        if (!patientRepo.existsById(patientId)) {
            throw new NotFoundException("Patient not found: " + patientId);
        }
    }
}
