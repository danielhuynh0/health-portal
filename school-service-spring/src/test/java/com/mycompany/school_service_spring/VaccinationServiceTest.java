package com.mycompany.school_service_spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mycompany.exception.NotFoundException;
import com.mycompany.models.Vaccination;
import com.mycompany.models.VaccineType;
import com.mycompany.repo.PatientRepository;
import com.mycompany.repo.VaccinationRepository;
import com.mycompany.service.VaccinationService;

@ExtendWith(MockitoExtension.class)
class VaccinationServiceTest {

    @Mock VaccinationRepository vaccinationRepo;
    @Mock PatientRepository patientRepo;

    @InjectMocks VaccinationService service;

    @Test
    void hasAnyDueVaccination_returnsTrue_whenOneVaccinationIsOverdue() {
        UUID patientId = UUID.randomUUID();
        Vaccination overdue = vaccination(VaccineType.FLU, LocalDate.now().minusDays(30).toString());
        when(vaccinationRepo.findByPatientId(patientId)).thenReturn(List.of(overdue));

        assertTrue(service.hasAnyDueVaccination(patientId));
    }

    @Test
    void hasAnyDueVaccination_returnsTrue_whenOnlyOneDueAmongMany() {
        UUID patientId = UUID.randomUUID();
        Vaccination future = vaccination(VaccineType.COVID_19, LocalDate.now().plusYears(1).toString());
        Vaccination overdue = vaccination(VaccineType.MMR, LocalDate.now().minusDays(1).toString());
        Vaccination future2 = vaccination(VaccineType.HEPATITIS_B, LocalDate.now().plusMonths(6).toString());
        when(vaccinationRepo.findByPatientId(patientId)).thenReturn(List.of(future, overdue, future2));

        assertTrue(service.hasAnyDueVaccination(patientId));
    }

    @Test
    void hasAnyDueVaccination_returnsFalse_whenAllVaccinationsAreFuture() {
        UUID patientId = UUID.randomUUID();
        Vaccination v1 = vaccination(VaccineType.FLU,        LocalDate.now().plusMonths(3).toString());
        Vaccination v2 = vaccination(VaccineType.COVID_19,   LocalDate.now().plusYears(1).toString());
        when(vaccinationRepo.findByPatientId(patientId)).thenReturn(List.of(v1, v2));

        assertFalse(service.hasAnyDueVaccination(patientId));
    }

    @Test
    void hasAnyDueVaccination_returnsFalse_whenPatientHasNoVaccinations() {
        UUID patientId = UUID.randomUUID();
        when(vaccinationRepo.findByPatientId(patientId)).thenReturn(List.of());

        assertFalse(service.hasAnyDueVaccination(patientId));
    }

    @Test
    void getVaccination_throwsNotFound_whenVaccinationBelongsToDifferentPatient() {
        UUID ownerPatientId    = UUID.randomUUID();
        UUID requestingPatient = UUID.randomUUID();
        UUID vaccinationId     = UUID.randomUUID();

        when(patientRepo.existsById(requestingPatient)).thenReturn(true);

        Vaccination v = vaccination(VaccineType.FLU, LocalDate.now().plusYears(1).toString());
        v.setId(vaccinationId);
        v.setPatientId(ownerPatientId);   // belongs to a different patient
        when(vaccinationRepo.findById(vaccinationId)).thenReturn(Optional.of(v));

        assertThrows(NotFoundException.class,
                () -> service.getVaccination(requestingPatient, vaccinationId));
    }

    @Test
    void getVaccination_throwsNotFound_whenPatientDoesNotExist() {
        UUID unknownPatient = UUID.randomUUID();
        when(patientRepo.existsById(unknownPatient)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> service.getVaccination(unknownPatient, UUID.randomUUID()));
    }

    @Test
    void createVaccination_throwsNotFound_whenPatientDoesNotExist() {
        UUID unknownPatient = UUID.randomUUID();
        when(patientRepo.existsById(unknownPatient)).thenReturn(false);

        Vaccination v = vaccination(VaccineType.MMR, LocalDate.now().plusYears(1).toString());
        assertThrows(NotFoundException.class,
                () -> service.createVaccination(unknownPatient, v));
    }

    @Test
    void updateVaccination_throwsNotFound_whenVaccinationBelongsToDifferentPatient() {
        UUID ownerPatientId = UUID.randomUUID();
        UUID requestingPatient = UUID.randomUUID();
        UUID vaccinationId = UUID.randomUUID();

        when(patientRepo.existsById(requestingPatient)).thenReturn(true);

        Vaccination v = vaccination(VaccineType.FLU, LocalDate.now().plusYears(1).toString());
        v.setId(vaccinationId);
        v.setPatientId(ownerPatientId);
        when(vaccinationRepo.findById(vaccinationId)).thenReturn(Optional.of(v));

        Vaccination update = vaccination(VaccineType.FLU, LocalDate.now().plusYears(1).toString());
        assertThrows(NotFoundException.class,
                () -> service.updateVaccination(requestingPatient, vaccinationId, update));
    }

    @Test
    void deleteVaccination_throwsNotFound_whenVaccinationDoesNotExist() {
        UUID patientId = UUID.randomUUID();
        UUID vaccinationId = UUID.randomUUID();

        when(patientRepo.existsById(patientId)).thenReturn(true);
        when(vaccinationRepo.findById(vaccinationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.deleteVaccination(patientId, vaccinationId));
    }

    private Vaccination vaccination(VaccineType type, String nextDueDate) {
        Vaccination v = new Vaccination();
        v.setVaccineType(type);
        v.setDateAdministered("2025-01-01");
        v.setNextDueDate(nextDueDate);
        return v;
    }
}
