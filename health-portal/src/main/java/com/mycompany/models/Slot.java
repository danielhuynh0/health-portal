package com.mycompany.models;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "slots")
@Getter @Setter
@JsonPropertyOrder({"id", "clinicId", "dateTime", "available", "_links"})
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private UUID clinicId;

    @NotBlank
    private String dateTime;

    private boolean available = true;

    @Transient
    @JsonProperty("_links")
    private Map<String, Link> links;

    public Slot() {}
}
