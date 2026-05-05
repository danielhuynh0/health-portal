package com.mycompany.repo;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mycompany.models.Appointment;
import com.mycompany.models.AppointmentStatus;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    Page<Appointment> findByPatientId(UUID patientId, Pageable pageable);
    Page<Appointment> findByClinicId(UUID clinicId, Pageable pageable);
    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);
    Page<Appointment> findByPatientIdAndStatus(UUID patientId, AppointmentStatus status, Pageable pageable);
    boolean existsByClinicIdAndDateTime(UUID clinicId, String dateTime);
}
