package com.mycompany.models;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vaccinations")
@Getter @Setter
@JsonPropertyOrder({"id", "patientId", "vaccineType", "dateAdministered", "nextDueDate", "isDue", "provider", "lotNumber", "_links"})
public class Vaccination {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @NotNull
    private VaccineType vaccineType;

    @NotBlank
    private String dateAdministered;

    private String nextDueDate;
    private String provider;
    private String lotNumber;

    @Transient
    @JsonProperty(value = "isDue", access = JsonProperty.Access.READ_ONLY)
    public boolean isDue() {
        if (nextDueDate == null) return false;
        return !LocalDate.parse(nextDueDate).isAfter(LocalDate.now());
    }

    @Transient
    @JsonProperty("_links")
    private Map<String, Link> links;

    public Vaccination() {}
}
