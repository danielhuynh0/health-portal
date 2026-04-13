package com.mycompany.school_service_spring;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mycompany.controller.PatientController;
import com.mycompany.exception.ConflictException;
import com.mycompany.exception.NotFoundException;
import com.mycompany.models.Address;
import com.mycompany.models.Patient;
import com.mycompany.security.JwtUtil;
import com.mycompany.service.PatientService;
import com.mycompany.service.VaccinationService;

/**
 * Controller-layer tests for PatientController.
 *
 * Verifies HTTP status codes, the HATEOAS schedule link requirement, the
 * error response body shape, and the Location header on 201 responses.
 * Uses @WebMvcTest so only the web layer is loaded — services are mocked.
 */
@WebMvcTest(PatientController.class)
@WithMockUser
class PatientControllerTest {

    @Autowired MockMvc mockMvc;

    // ObjectMapper is not auto-registered in the Spring Boot 4 @WebMvcTest slice
    private final ObjectMapper objectMapper = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    @MockitoBean PatientService patientService;
    @MockitoBean VaccinationService vaccinationService;
    @MockitoBean JwtUtil jwtUtil; // required by JwtRequestFilter which is loaded by @WebMvcTest

    private static final UUID PATIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    // GET /patients/{patientId}

    @Test
    void getPatient_returns200WithLinks() throws Exception {
        when(patientService.getPatientById(PATIENT_ID)).thenReturn(patient());
        when(vaccinationService.hasAnyDueVaccination(PATIENT_ID)).thenReturn(false);

        mockMvc.perform(get("/patients/{id}", PATIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.vaccinations").exists())
                .andExpect(jsonPath("$._links.appointments").exists());
    }

    @Test
    void getPatient_includesScheduleLink_whenVaccinationIsDue() throws Exception {
        // This is the core HATEOAS requirement: a due vaccination must trigger the schedule link.
        when(patientService.getPatientById(PATIENT_ID)).thenReturn(patient());
        when(vaccinationService.hasAnyDueVaccination(PATIENT_ID)).thenReturn(true);

        mockMvc.perform(get("/patients/{id}", PATIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.schedule").exists());
    }

    @Test
    void getPatient_excludesScheduleLink_whenNoVaccinationIsDue() throws Exception {
        when(patientService.getPatientById(PATIENT_ID)).thenReturn(patient());
        when(vaccinationService.hasAnyDueVaccination(PATIENT_ID)).thenReturn(false);

        mockMvc.perform(get("/patients/{id}", PATIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.schedule").doesNotExist());
    }

    @Test
    void getPatient_returns404_whenNotFound() throws Exception {
        when(patientService.getPatientById(PATIENT_ID)).thenThrow(new NotFoundException("Patient not found"));

        mockMvc.perform(get("/patients/{id}", PATIENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    // POST /patients

    @Test
    void createPatient_returns201WithLocationHeader() throws Exception {
        Patient saved = patient();
        when(patientService.createPatient(any())).thenReturn(saved);
        when(vaccinationService.hasAnyDueVaccination(any())).thenReturn(false);

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientBody())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/patients/" + PATIENT_ID))
                .andExpect(jsonPath("$.id").value(PATIENT_ID.toString()));
    }

    @Test
    void createPatient_returns409_onDuplicateEmail() throws Exception {
        when(patientService.createPatient(any())).thenThrow(new ConflictException("Email already registered"));

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientBody())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createPatient_returns400_onMissingRequiredFields() throws Exception {
        String emptyBody = "{}";

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createPatient_returns400_onInvalidEmail() throws Exception {
        Patient body = patientBody();
        body.setEmail("not-an-email");

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // PUT /patients/{patientId}

    @Test
    void updatePatient_returns200() throws Exception {
        Patient updated = patient();
        updated.setFirstName("Updated");
        when(patientService.updatePatient(eq(PATIENT_ID), any())).thenReturn(updated);
        when(vaccinationService.hasAnyDueVaccination(PATIENT_ID)).thenReturn(false);

        mockMvc.perform(put("/patients/{id}", PATIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    void updatePatient_returns404_whenNotFound() throws Exception {
        when(patientService.updatePatient(eq(PATIENT_ID), any())).thenThrow(new NotFoundException("Patient not found"));

        mockMvc.perform(put("/patients/{id}", PATIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientBody())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // DELETE /patients/{patientId}

    @Test
    void deletePatient_returns204() throws Exception {
        mockMvc.perform(delete("/patients/{id}", PATIENT_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePatient_returns404_whenNotFound() throws Exception {
        doThrow(new NotFoundException("Patient not found")).when(patientService).deletePatient(PATIENT_ID);

        mockMvc.perform(delete("/patients/{id}", PATIENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // Helpers

    private Patient patient() {
        Patient p = new Patient();
        p.setId(PATIENT_ID);
        p.setFirstName("Jane");
        p.setLastName("Doe");
        p.setDateOfBirth("1990-01-01");
        p.setEmail("jane@example.com");
        p.setAddress(new Address("123 Main St", "Baltimore", "MD", "21201"));
        return p;
    }

    /** A valid patient body for POST/PUT requests. */
    private Patient patientBody() {
        Patient p = new Patient();
        p.setFirstName("Jane");
        p.setLastName("Doe");
        p.setDateOfBirth("1990-01-01");
        p.setEmail("jane@example.com");
        p.setAddress(new Address("123 Main St", "Baltimore", "MD", "21201"));
        return p;
    }
}
