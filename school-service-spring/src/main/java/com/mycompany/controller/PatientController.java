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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mycompany.models.Link;
import com.mycompany.models.PageMetadata;
import com.mycompany.models.Patient;
import com.mycompany.models.PatientListResponse;
import com.mycompany.service.PatientService;
import com.mycompany.service.VaccinationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private static final Logger log = LoggerFactory.getLogger(PatientController.class);

    private final PatientService service;
    private final VaccinationService vaccinationService;

    public PatientController(PatientService service, VaccinationService vaccinationService) {
        this.service = service;
        this.vaccinationService = vaccinationService;
    }

    @GetMapping
    public ResponseEntity<PatientListResponse> getAllPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /patients  page={} size={}", page, size);

        Page<Patient> result = service.getAllPatients(page, size);
        result.getContent().forEach(p -> {
            boolean hasDue = vaccinationService.hasAnyDueVaccination(p.getId());
            p.setLinks(patientLinks(p.getId(), hasDue));
        });

        Map<String, Link> links = new LinkedHashMap<>();
        links.put("self", new Link("/api/v1/patients?page=" + page + "&size=" + size, "self", "GET"));

        PageMetadata meta = new PageMetadata(result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());

        return ResponseEntity.ok(new PatientListResponse(result.getContent(), meta, links));
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<Patient> getPatient(@PathVariable UUID patientId) {
        log.info("GET /patients/{}", patientId);
        Patient patient = service.getPatientById(patientId);
        boolean hasDue = vaccinationService.hasAnyDueVaccination(patientId);
        patient.setLinks(patientLinks(patientId, hasDue));
        return ResponseEntity.ok(patient);
    }

    @PostMapping
    public ResponseEntity<Patient> createPatient(@Valid @RequestBody Patient patient) {
        log.info("POST /patients  email={}", patient.getEmail());
        Patient created = service.createPatient(patient);
        created.setLinks(patientLinks(created.getId(), false));
        URI location = URI.create("/api/v1/patients/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<Patient> updatePatient(@PathVariable UUID patientId,
                                                  @Valid @RequestBody Patient patient) {
        log.info("PUT /patients/{}", patientId);
        Patient updated = service.updatePatient(patientId, patient);
        boolean hasDue = vaccinationService.hasAnyDueVaccination(patientId);
        updated.setLinks(patientLinks(patientId, hasDue));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{patientId}")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID patientId) {
        log.info("DELETE /patients/{}", patientId);
        service.deletePatient(patientId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Link> patientLinks(UUID id, boolean hasDueVaccination) {
        String base = "/api/v1/patients/" + id;
        Map<String, Link> links = new LinkedHashMap<>();
        links.put("self", new Link(base, "self", "GET"));
        links.put("update", new Link(base, "update", "PUT"));
        links.put("delete", new Link(base, "delete", "DELETE"));
        links.put("vaccinations", new Link(base + "/vaccinations", "vaccinations", "GET"));
        links.put("appointments", new Link("/api/v1/appointments?patientId=" + id, "appointments", "GET"));
        if (hasDueVaccination) {
            links.put("schedule", new Link("/api/v1/appointments", "schedule", "POST"));
        }
        return links;
    }
}
