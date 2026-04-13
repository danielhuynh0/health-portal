package com.mycompany.school_service_spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.mycompany.models.Vaccination;
import com.mycompany.models.VaccineType;

class VaccinationIsDueTest {

    private Vaccination vaccination(String nextDueDate) {
        Vaccination v = new Vaccination();
        v.setVaccineType(VaccineType.FLU);
        v.setDateAdministered("2025-01-01");
        v.setNextDueDate(nextDueDate);
        return v;
    }

    @Test
    void isDue_returnsFalse_whenNextDueDateIsAbsent() {
        Vaccination v = new Vaccination();
        v.setVaccineType(VaccineType.FLU);
        v.setDateAdministered("2025-01-01");
        assertFalse(v.isDue());
    }

    @Test
    void isDue_returnsFalse_whenNextDueDateIsOneYearFromNow() {
        String future = LocalDate.now().plusYears(1).toString();
        assertFalse(vaccination(future).isDue());
    }

    @Test
    void isDue_returnsFalse_whenNextDueDateIsTomorrow() {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        assertFalse(vaccination(tomorrow).isDue());
    }

    @Test
    void isDue_returnsTrue_whenNextDueDateIsToday() {
        String today = LocalDate.now().toString();
        assertTrue(vaccination(today).isDue());
    }

    @Test
    void isDue_returnsTrue_whenNextDueDateWasYesterday() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        assertTrue(vaccination(yesterday).isDue());
    }

    @Test
    void isDue_returnsTrue_whenNextDueDateWasOneYearAgo() {
        String lastYear = LocalDate.now().minusYears(1).toString();
        assertTrue(vaccination(lastYear).isDue());
    }

    @Test
    void vaccineTypes_exactlyFourSupported() {
        VaccineType[] types = VaccineType.values();
        assertTrue(types.length == 4);
        assertTrue(containsAll(types, VaccineType.COVID_19, VaccineType.FLU,
                VaccineType.MMR, VaccineType.HEPATITIS_B));
    }

    private boolean containsAll(VaccineType[] actual, VaccineType... expected) {
        java.util.Set<VaccineType> set = java.util.EnumSet.copyOf(java.util.Arrays.asList(actual));
        return set.containsAll(java.util.Arrays.asList(expected));
    }
}
