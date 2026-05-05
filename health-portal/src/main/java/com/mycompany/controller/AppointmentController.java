package com.mycompany.controller;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mycompany.models.Appointment;
import com.mycompany.models.AppointmentListResponse;
import com.mycompany.models.AppointmentStatus;
import com.mycompany.models.Link;
import com.mycompany.models.PageMetadata;
import com.mycompany.service.AppointmentService;

import jakarta.validation.Valid;

@RestController
public class AppointmentController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    // GET /appointments
    @GetMapping("/appointments")
    public ResponseEntity<AppointmentListResponse> getAppointments(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID clinicId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /appointments  patientId={} clinicId={} status={}", patientId, clinicId, status);
        Page<Appointment> result = service.getAppointments(patientId, clinicId, status, page, size);
        result.getContent().forEach(a -> a.setLinks(appointmentLinks(a.getId())));

        Map<String, Link> links = new LinkedHashMap<>();
        links.put("self", new Link("/api/v1/appointments?page=" + page + "&size=" + size, "self", "GET"));

        PageMetadata meta = new PageMetadata(result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());

        return ResponseEntity.ok(new AppointmentListResponse(result.getContent(), meta, links));
    }

    // GET /patients/{patientId}/appointments
    @GetMapping("/patients/{patientId}/appointments")
    public ResponseEntity<AppointmentListResponse> getAppointmentsForPatient(
            @PathVariable UUID patientId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /patients/{}/appointments  status={}", patientId, status);
        Page<Appointment> result = service.getAppointmentsForPatient(patientId, status, page, size);
        result.getContent().forEach(a -> a.setLinks(appointmentLinks(a.getId())));

        Map<String, Link> links = new LinkedHashMap<>();
        links.put("self", new Link("/api/v1/patients/" + patientId + "/appointments", "self", "GET"));
        links.put("patient", new Link("/api/v1/patients/" + patientId, "patient", "GET"));

        PageMetadata meta = new PageMetadata(result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());

        return ResponseEntity.ok(new AppointmentListResponse(result.getContent(), meta, links));
    }

    // GET /appointments/{appointmentId}
    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<Appointment> getAppointment(@PathVariable UUID appointmentId) {
        log.info("GET /appointments/{}", appointmentId);
        Appointment a = service.getAppointmentById(appointmentId);
        a.setLinks(appointmentLinks(appointmentId));
        return ResponseEntity.ok(a);
    }

    // POST /appointments
    @PostMapping("/appointments")
    public ResponseEntity<Appointment> createAppointment(@Valid @RequestBody Appointment appointment) {
        log.info("POST /appointments  patientId={} clinicId={}", appointment.getPatientId(), appointment.getClinicId());
        Appointment created = service.createAppointment(appointment);
        created.setLinks(appointmentLinks(created.getId()));
        URI location = URI.create("/api/v1/appointments/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    // PUT /appointments/{appointmentId}
    @PutMapping("/appointments/{appointmentId}")
    public ResponseEntity<Appointment> updateAppointment(@PathVariable UUID appointmentId,
            @Valid @RequestBody Appointment appointment) {
        log.info("PUT /appointments/{}", appointmentId);
        Appointment updated = service.updateAppointment(appointmentId, appointment);
        updated.setLinks(appointmentLinks(appointmentId));
        return ResponseEntity.ok(updated);
    }

    // DELETE /appointments/{appointmentId}
    @DeleteMapping("/appointments/{appointmentId}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable UUID appointmentId) {
        log.info("DELETE /appointments/{}", appointmentId);
        service.deleteAppointment(appointmentId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Link> appointmentLinks(UUID id) {
        String base = "/api/v1/appointments/" + id;
        Map<String, Link> links = new LinkedHashMap<>();
        links.put("self", new Link(base, "self", "GET"));
        links.put("update", new Link(base, "update", "PUT"));
        links.put("cancel", new Link(base, "cancel", "DELETE"));
        return links;
    }
}
