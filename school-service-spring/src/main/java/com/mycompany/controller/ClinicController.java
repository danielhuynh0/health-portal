package com.mycompany.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mycompany.models.Clinic;
import com.mycompany.models.ClinicListResponse;
import com.mycompany.models.Link;
import com.mycompany.models.PageMetadata;
import com.mycompany.models.Slot;
import com.mycompany.models.SlotListResponse;
import com.mycompany.service.ClinicService;
import com.mycompany.service.GeoapifyPlacesService;

@RestController
@RequestMapping("/clinics")
public class ClinicController {

    private static final Logger log = LoggerFactory.getLogger(ClinicController.class);

    private final ClinicService service;
    private final GeoapifyPlacesService placesService;

    public ClinicController(ClinicService service, GeoapifyPlacesService placesService) {
        this.service = service;
        this.placesService = placesService;
    }

    @GetMapping
    public ResponseEntity<ClinicListResponse> getClinics(
            @RequestParam(required = false) String zip,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /clinics  zip={} city={}", zip, city);
        Map<String, Link> links = new LinkedHashMap<>();
        links.put("self", new Link("/api/v1/clinics?page=" + page + "&size=" + size, "self", "GET"));

        // When a location filter is provided, delegate to Geoapify Places for
        // real-world clinic discovery; otherwise return internal seeded clinics.
        if (zip != null || city != null) {
            List<Clinic> external = placesService.searchNearby(zip, city, size, page * size);
            external.forEach(c -> c.setLinks(clinicLinks(c.getId())));
            PageMetadata meta = new PageMetadata(page, size, external.size(), 1);
            return ResponseEntity.ok(new ClinicListResponse(external, meta, links));
        }

        Page<Clinic> result = service.getClinics(null, null, page, size);
        result.getContent().forEach(c -> c.setLinks(clinicLinks(c.getId())));
        PageMetadata meta = new PageMetadata(result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
        return ResponseEntity.ok(new ClinicListResponse(result.getContent(), meta, links));
    }

    @GetMapping("/{clinicId}")
    public ResponseEntity<Clinic> getClinic(@PathVariable UUID clinicId) {
        log.info("GET /clinics/{}", clinicId);
        Clinic clinic = service.getClinicById(clinicId);
        clinic.setLinks(clinicLinks(clinicId));
        return ResponseEntity.ok(clinic);
    }

    @GetMapping("/{clinicId}/slots")
    public ResponseEntity<SlotListResponse> getSlots(
            @PathVariable UUID clinicId,
            @RequestParam String date) {

        log.info("GET /clinics/{}/slots  date={}", clinicId, date);
        List<Slot> slots = service.getSlotsForClinic(clinicId, date);
        slots.forEach(s -> s.setLinks(slotLinks(clinicId, s.getId())));

        Map<String, Link> links = new LinkedHashMap<>();
        links.put("self", new Link("/api/v1/clinics/" + clinicId + "/slots?date=" + date, "self", "GET"));
        links.put("clinic", new Link("/api/v1/clinics/" + clinicId, "clinic", "GET"));

        return ResponseEntity.ok(new SlotListResponse(slots, links));
    }

    private Map<String, Link> clinicLinks(UUID id) {
        String base = "/api/v1/clinics/" + id;
        Map<String, Link> links = new LinkedHashMap<>();
        links.put("self", new Link(base, "self", "GET"));
        links.put("slots", new Link(base + "/slots", "slots", "GET"));
        return links;
    }

    private Map<String, Link> slotLinks(UUID clinicId, UUID slotId) {
        Map<String, Link> links = new LinkedHashMap<>();
        links.put("self", new Link("/api/v1/clinics/" + clinicId + "/slots", "self", "GET"));
        links.put("clinic", new Link("/api/v1/clinics/" + clinicId, "clinic", "GET"));
        links.put("book", new Link("/api/v1/appointments", "book", "POST"));
        return links;
    }
}
