package com.mycompany.school_service_spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.mycompany.models.Vaccination;
import com.mycompany.models.VaccineType;

/**
 * Tests for the isDue() flag on Vaccination.
 *
 * isDue() is what drives the HATEOAS schedule link on a patient response — if any
 * vaccination is due, the patient record must include a link to the scheduling service.
 * Getting the boundary conditions wrong here would silently break that requirement.
 */
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
        assertFalse(v.isDue(), "A vaccination with no nextDueDate should never be considered due");
    }

    @Test
    void isDue_returnsFalse_whenNextDueDateIsOneYearFromNow() {
        String future = LocalDate.now().plusYears(1).toString();
        assertFalse(vaccination(future).isDue(),
                "A vaccination due one year from now should not be considered due");
    }

    @Test
    void isDue_returnsFalse_whenNextDueDateIsTomorrow() {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        assertFalse(vaccination(tomorrow).isDue(),
                "A vaccination due tomorrow should not be considered due today");
    }

    @Test
    void isDue_returnsTrue_whenNextDueDateIsToday() {
        // Today is the boundary — a vaccination due today should prompt the patient to act.
        String today = LocalDate.now().toString();
        assertTrue(vaccination(today).isDue(),
                "A vaccination due today should be considered due");
    }

    @Test
    void isDue_returnsTrue_whenNextDueDateWasYesterday() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        assertTrue(vaccination(yesterday).isDue(),
                "An overdue vaccination should be considered due");
    }

    @Test
    void isDue_returnsTrue_whenNextDueDateWasOneYearAgo() {
        String lastYear = LocalDate.now().minusYears(1).toString();
        assertTrue(vaccination(lastYear).isDue(),
                "A vaccination overdue by one year should be considered due");
    }

    @Test
    void vaccineTypes_exactlyFourSupported() {
        VaccineType[] types = VaccineType.values();
        assertTrue(types.length == 4,
                "System must support exactly 4 vaccine types per the project specification");
        assertTrue(containsAll(types, VaccineType.COVID_19, VaccineType.FLU,
                VaccineType.MMR, VaccineType.HEPATITIS_B),
                "System must support COVID_19, FLU, MMR, and HEPATITIS_B");
    }

    private boolean containsAll(VaccineType[] actual, VaccineType... expected) {
        java.util.Set<VaccineType> set = java.util.EnumSet.copyOf(java.util.Arrays.asList(actual));
        return set.containsAll(java.util.Arrays.asList(expected));
    }
}
