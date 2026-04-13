package com.mycompany.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mycompany.models.Slot;

@Repository
public interface SlotRepository extends JpaRepository<Slot, UUID> {
    List<Slot> findByClinicIdAndDateTimeStartingWith(UUID clinicId, String date);
}
