package com.mycompany.models;

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
@Table(name = "appointments")
@Getter @Setter
@JsonPropertyOrder({"id", "slotId", "patientId", "clinicId", "dateTime", "reason", "status", "_links"})
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private UUID slotId;

    @NotNull
    @Column(nullable = false)
    private UUID patientId;

    @NotNull
    @Column(nullable = false)
    private UUID clinicId;

    @NotBlank
    private String dateTime;

    private String reason;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Transient
    @JsonProperty("_links")
    private Map<String, Link> links;

    public Appointment() {}
}
