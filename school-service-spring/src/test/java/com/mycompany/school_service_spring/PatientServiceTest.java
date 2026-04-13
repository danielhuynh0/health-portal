package com.mycompany.school_service_spring;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mycompany.exception.ConflictException;
import com.mycompany.exception.NotFoundException;
import com.mycompany.models.Patient;
import com.mycompany.repo.PatientRepository;
import com.mycompany.service.PatientService;

/**
 * Tests for PatientService.
 *
 * Covers the core CRUD contract: duplicate email detection, and correct
 * 404 behavior when operating on a patient that does not exist.
 */
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock PatientRepository patientRepo;

    @InjectMocks PatientService service;

    @Test
    void createPatient_throwsConflict_whenEmailAlreadyRegistered() {
        Patient patient = patient("jane.doe@example.com");
        when(patientRepo.existsByEmail("jane.doe@example.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.createPatient(patient),
                "Registering a patient with a duplicate email must throw a 409 Conflict");
        verify(patientRepo, never()).save(any());
    }

    @Test
    void createPatient_savesPatient_whenEmailIsUnique() {
        Patient patient = patient("new.patient@example.com");
        when(patientRepo.existsByEmail("new.patient@example.com")).thenReturn(false);
        when(patientRepo.save(patient)).thenReturn(patient);

        service.createPatient(patient);

        verify(patientRepo).save(patient);
    }

    @Test
    void getPatientById_throwsNotFound_whenPatientDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(patientRepo.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.getPatientById(unknownId),
                "Fetching a non-existent patient must throw a 404 Not Found");
    }

    @Test
    void updatePatient_throwsNotFound_whenPatientDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(patientRepo.existsById(unknownId)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> service.updatePatient(unknownId, patient("x@example.com")),
                "Updating a non-existent patient must throw a 404 Not Found");
        verify(patientRepo, never()).save(any());
    }

    @Test
    void deletePatient_throwsNotFound_whenPatientDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(patientRepo.existsById(unknownId)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> service.deletePatient(unknownId),
                "Deleting a non-existent patient must throw a 404 Not Found");
        verify(patientRepo, never()).deleteById(any());
    }

    private Patient patient(String email) {
        Patient p = new Patient();
        p.setFirstName("Test");
        p.setLastName("Patient");
        p.setEmail(email);
        return p;
    }
}
