package com.mycompany.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.mycompany.exception.ConflictException;
import com.mycompany.exception.NotFoundException;
import com.mycompany.models.Patient;
import com.mycompany.repo.PatientRepository;

@Service
public class PatientService {

    private final PatientRepository repo;

    public PatientService(PatientRepository repo) {
        this.repo = repo;
    }

    public Page<Patient> getAllPatients(int page, int size) {
        return repo.findAll(PageRequest.of(page, size));
    }

    public Patient getPatientById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + id));
    }

    public Patient createPatient(Patient patient) {
        if (repo.existsByEmail(patient.getEmail())) {
            throw new ConflictException("Patient already exists with email: " + patient.getEmail());
        }
        return repo.save(patient);
    }

    public Patient updatePatient(UUID id, Patient updated) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Patient not found: " + id);
        }
        updated.setId(id);
        return repo.save(updated);
    }

    public void deletePatient(UUID id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Patient not found: " + id);
        }
        repo.deleteById(id);
    }
}
