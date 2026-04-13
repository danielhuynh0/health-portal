package com.mycompany.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.mycompany.exception.ConflictException;
import com.mycompany.exception.NotFoundException;
import com.mycompany.models.Appointment;
import com.mycompany.models.AppointmentStatus;
import com.mycompany.models.Slot;
import com.mycompany.repo.AppointmentRepository;
import com.mycompany.repo.ClinicRepository;
import com.mycompany.repo.PatientRepository;
import com.mycompany.repo.SlotRepository;

@Service
public class AppointmentService {

    private final AppointmentRepository repo;
    private final PatientRepository patientRepo;
    private final ClinicRepository clinicRepo;
    private final SlotRepository slotRepo;

    public AppointmentService(AppointmentRepository repo, PatientRepository patientRepo,
                              ClinicRepository clinicRepo, SlotRepository slotRepo) {
        this.repo = repo;
        this.patientRepo = patientRepo;
        this.clinicRepo = clinicRepo;
        this.slotRepo = slotRepo;
    }

    public Page<Appointment> getAppointments(UUID patientId, UUID clinicId,
                                             AppointmentStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (patientId != null && status != null) {
            return repo.findByPatientIdAndStatus(patientId, status, pageable);
        }
        if (patientId != null) {
            return repo.findByPatientId(patientId, pageable);
        }
        if (clinicId != null) {
            return repo.findByClinicId(clinicId, pageable);
        }
        if (status != null) {
            return repo.findByStatus(status, pageable);
        }
        return repo.findAll(pageable);
    }

    public Page<Appointment> getAppointmentsForPatient(UUID patientId, AppointmentStatus status,
                                                       int page, int size) {
        if (!patientRepo.existsById(patientId)) {
            throw new NotFoundException("Patient not found: " + patientId);
        }
        return getAppointments(patientId, null, status, page, size);
    }

    public Appointment getAppointmentById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + id));
    }

    public Appointment createAppointment(Appointment appointment) {
        if (!patientRepo.existsById(appointment.getPatientId())) {
            throw new NotFoundException("Patient not found: " + appointment.getPatientId());
        }
        if (!clinicRepo.existsById(appointment.getClinicId())) {
            throw new NotFoundException("Clinic not found: " + appointment.getClinicId());
        }
        Slot slot = slotRepo.findById(appointment.getSlotId())
                .orElseThrow(() -> new NotFoundException("Slot not found: " + appointment.getSlotId()));
        if (!slot.isAvailable()) {
            throw new ConflictException("Slot is no longer available: " + appointment.getSlotId());
        }
        Appointment saved = repo.save(appointment);
        slot.setAvailable(false);
        slotRepo.save(slot);
        return saved;
    }

    public Appointment updateAppointment(UUID id, Appointment updated) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Appointment not found: " + id);
        }
        updated.setId(id);
        return repo.save(updated);
    }

    public void deleteAppointment(UUID id) {
        Appointment appointment = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + id));
        repo.delete(appointment);
        slotRepo.findById(appointment.getSlotId()).ifPresent(slot -> {
            slot.setAvailable(true);
            slotRepo.save(slot);
        });
    }
}
