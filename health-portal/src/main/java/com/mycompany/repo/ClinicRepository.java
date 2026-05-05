package com.mycompany.repo;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mycompany.models.Clinic;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, UUID> {
    Page<Clinic> findByAddressZip(String zip, Pageable pageable);
    Page<Clinic> findByAddressCity(String city, Pageable pageable);
    Page<Clinic> findByAddressZipAndAddressCity(String zip, String city, Pageable pageable);
}
