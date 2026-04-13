package com.mycompany.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mycompany.models.Vaccination;

@Repository
public interface VaccinationRepository extends JpaRepository<Vaccination, UUID> {
    List<Vaccination> findByPatientId(UUID patientId);
}
