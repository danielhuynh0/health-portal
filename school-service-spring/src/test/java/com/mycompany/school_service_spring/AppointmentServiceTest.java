package com.mycompany.school_service_spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mycompany.exception.ConflictException;
import com.mycompany.exception.NotFoundException;
import com.mycompany.models.Appointment;
import com.mycompany.models.Slot;
import com.mycompany.repo.AppointmentRepository;
import com.mycompany.repo.ClinicRepository;
import com.mycompany.repo.PatientRepository;
import com.mycompany.repo.SlotRepository;
import com.mycompany.service.AppointmentService;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock AppointmentRepository appointmentRepo;
    @Mock PatientRepository patientRepo;
    @Mock ClinicRepository clinicRepo;
    @Mock SlotRepository slotRepo;

    @InjectMocks AppointmentService service;

    private UUID patientId;
    private UUID clinicId;
    private UUID slotId;
    private Slot availableSlot;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        clinicId  = UUID.randomUUID();
        slotId    = UUID.randomUUID();

        availableSlot = new Slot();
        availableSlot.setId(slotId);
        availableSlot.setClinicId(clinicId);
        availableSlot.setDateTime("2026-06-01T09:00:00");
        availableSlot.setAvailable(true);

        appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setClinicId(clinicId);
        appointment.setSlotId(slotId);
        appointment.setDateTime("2026-06-01T09:00:00");
    }

    @Test
    void createAppointment_marksSlotUnavailable() {
        when(patientRepo.existsById(patientId)).thenReturn(true);
        when(clinicRepo.existsById(clinicId)).thenReturn(true);
        when(slotRepo.findById(slotId)).thenReturn(Optional.of(availableSlot));
        when(appointmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createAppointment(appointment);

        ArgumentCaptor<Slot> slotCaptor = ArgumentCaptor.forClass(Slot.class);
        verify(slotRepo).save(slotCaptor.capture());
        assertFalse(slotCaptor.getValue().isAvailable(), "Slot should be marked unavailable after booking");
    }

    @Test
    void createAppointment_throwsConflict_whenSlotUnavailable() {
        availableSlot.setAvailable(false);

        when(patientRepo.existsById(patientId)).thenReturn(true);
        when(clinicRepo.existsById(clinicId)).thenReturn(true);
        when(slotRepo.findById(slotId)).thenReturn(Optional.of(availableSlot));

        assertThrows(ConflictException.class, () -> service.createAppointment(appointment));
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void createAppointment_throwsNotFound_whenSlotMissing() {
        when(patientRepo.existsById(patientId)).thenReturn(true);
        when(clinicRepo.existsById(clinicId)).thenReturn(true);
        when(slotRepo.findById(slotId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createAppointment(appointment));
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void deleteAppointment_freesSlot() {
        UUID appointmentId = UUID.randomUUID();
        appointment.setId(appointmentId);

        when(appointmentRepo.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(slotRepo.findById(slotId)).thenReturn(Optional.of(availableSlot));
        availableSlot.setAvailable(false);

        service.deleteAppointment(appointmentId);

        ArgumentCaptor<Slot> slotCaptor = ArgumentCaptor.forClass(Slot.class);
        verify(slotRepo).save(slotCaptor.capture());
        assertTrue(slotCaptor.getValue().isAvailable(), "Slot should be freed when appointment is cancelled");
    }

    @Test
    void deleteAppointment_throwsNotFound_whenMissing() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepo.findById(appointmentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.deleteAppointment(appointmentId));
    }
}
