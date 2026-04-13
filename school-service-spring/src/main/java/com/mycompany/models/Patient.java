package com.mycompany.models;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "patients")
@Getter @Setter
@JsonPropertyOrder({"id", "firstName", "lastName", "dateOfBirth", "email", "phone", "address", "_links"})
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String dateOfBirth;

    @Email
    @NotBlank
    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    @Embedded
    @Valid
    @NotNull
    private Address address;

    @Transient
    @JsonProperty("_links")
    private Map<String, Link> links;

    public Patient() {}
}
