package com.mycompany.school_service_spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mycompany.exception.NotFoundException;
import com.mycompany.models.Clinic;
import com.mycompany.models.Slot;
import com.mycompany.repo.ClinicRepository;
import com.mycompany.repo.SlotRepository;
import com.mycompany.service.ClinicService;

/**
 * Tests for ClinicService — clinic lookup and slot retrieval.
 */
@ExtendWith(MockitoExtension.class)
class ClinicServiceTest {

    @Mock ClinicRepository clinicRepo;
    @Mock SlotRepository slotRepo;

    @InjectMocks ClinicService service;

    @Test
    void getClinicById_throwsNotFound_whenClinicDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(clinicRepo.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getClinicById(unknownId));
    }

    @Test
    void getClinicById_returnsClinic_whenFound() {
        UUID clinicId = UUID.randomUUID();
        Clinic clinic = new Clinic();
        clinic.setId(clinicId);
        when(clinicRepo.findById(clinicId)).thenReturn(Optional.of(clinic));

        Clinic result = service.getClinicById(clinicId);

        assertEquals(clinicId, result.getId());
    }

    @Test
    void getSlotsForClinic_throwsNotFound_whenClinicDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(clinicRepo.existsById(unknownId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getSlotsForClinic(unknownId, "2026-06-01"));
    }

    @Test
    void getSlotsForClinic_returnsSlots_forKnownClinic() {
        UUID clinicId = UUID.randomUUID();
        Slot slot = new Slot();
        slot.setClinicId(clinicId);
        slot.setDateTime("2026-06-01T09:00:00");

        when(clinicRepo.existsById(clinicId)).thenReturn(true);
        when(slotRepo.findByClinicIdAndDateTimeStartingWith(clinicId, "2026-06-01"))
                .thenReturn(List.of(slot));

        List<Slot> results = service.getSlotsForClinic(clinicId, "2026-06-01");

        assertEquals(1, results.size());
        assertEquals(clinicId, results.get(0).getClinicId());
    }

    @Test
    void getSlotsForClinic_returnsEmptyList_whenNoSlotsExistForDate() {
        UUID clinicId = UUID.randomUUID();
        when(clinicRepo.existsById(clinicId)).thenReturn(true);
        when(slotRepo.findByClinicIdAndDateTimeStartingWith(clinicId, "2099-01-01"))
                .thenReturn(List.of());

        List<Slot> results = service.getSlotsForClinic(clinicId, "2099-01-01");

        assertEquals(0, results.size());
    }
}
