package com.mycompany.controller;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycompany.models.Link;
import com.mycompany.models.Vaccination;
import com.mycompany.models.VaccinationListResponse;
import com.mycompany.service.VaccinationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/patients/{patientId}/vaccinations")
public class VaccinationController {

    private static final Logger log = LoggerFactory.getLogger(VaccinationController.class);

    private final VaccinationService service;

    public VaccinationController(VaccinationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<VaccinationListResponse> getVaccinations(@PathVariable UUID patientId) {
        log.info("GET /patients/{}/vaccinations", patientId);
        List<Vaccination> vaccinations = service.getVaccinationsForPatient(patientId);
        vaccinations.forEach(v -> v.setLinks(vaccinationLinks(patientId, v.getId())));

        Map<String, Link> links = new LinkedHashMap<>();
        links.put("self", new Link("/api/v1/patients/" + patientId + "/vaccinations", "self", "GET"));
        links.put("patient", new Link("/api/v1/patients/" + patientId, "patient", "GET"));

        return ResponseEntity.ok(new VaccinationListResponse(vaccinations, links));
    }

    @GetMapping("/{vaccinationId}")
    public ResponseEntity<Vaccination> getVaccination(@PathVariable UUID patientId,
                                                       @PathVariable UUID vaccinationId) {
        log.info("GET /patients/{}/vaccinations/{}", patientId, vaccinationId);
        Vaccination v = service.getVaccination(patientId, vaccinationId);
        v.setLinks(vaccinationLinks(patientId, vaccinationId));
        return ResponseEntity.ok(v);
    }

    @PostMapping
    public ResponseEntity<Vaccination> createVaccination(@PathVariable UUID patientId,
                                                          @Valid @RequestBody Vaccination vaccination) {
        log.info("POST /patients/{}/vaccinations  type={}", patientId, vaccination.getVaccineType());
        Vaccination created = service.createVaccination(patientId, vaccination);
        created.setLinks(vaccinationLinks(patientId, created.getId()));
        URI location = URI.create("/api/v1/patients/" + patientId + "/vaccinations/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{vaccinationId}")
    public ResponseEntity<Vaccination> updateVaccination(@PathVariable UUID patientId,
                                                          @PathVariable UUID vaccinationId,
                                                          @Valid @RequestBody Vaccination vaccination) {
        log.info("PUT /patients/{}/vaccinations/{}", patientId, vaccinationId);
        Vaccination updated = service.updateVaccination(patientId, vaccinationId, vaccination);
        updated.setLinks(vaccinationLinks(patientId, vaccinationId));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{vaccinationId}")
    public ResponseEntity<Void> deleteVaccination(@PathVariable UUID patientId,
                                                   @PathVariable UUID vaccinationId) {
        log.info("DELETE /patients/{}/vaccinations/{}", patientId, vaccinationId);
        service.deleteVaccination(patientId, vaccinationId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Link> vaccinationLinks(UUID patientId, UUID vaccinationId) {
        String base = "/api/v1/patients/" + patientId + "/vaccinations/" + vaccinationId;
        Map<String, Link> links = new LinkedHashMap<>();
        links.put("self", new Link(base, "self", "GET"));
        links.put("update", new Link(base, "update", "PUT"));
        links.put("delete", new Link(base, "delete", "DELETE"));
        links.put("patient", new Link("/api/v1/patients/" + patientId, "patient", "GET"));
        return links;
    }
}
