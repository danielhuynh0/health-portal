# Design Decisions

This document records architectural and API design choices made for the Patient Appointment and Vaccination Portal, along with the reasoning behind each decision.

---

## 1. Appointment Resource Structure

### Decision: Top-level `/appointments` with query-param filtering

Appointments live at a flat top-level path rather than nested under a patient or clinic:

```
GET  /appointments
POST /appointments
GET  /appointments/{appointmentId}
PUT  /appointments/{appointmentId}
DELETE /appointments/{appointmentId}
```

### Rationale

An appointment has two natural parents — a **patient** and a **clinic**. Nesting it under either one creates an artificial hierarchy:

- `POST /patients/{patientId}/appointments` implies the patient owns the appointment, but the clinic's availability is an equal constraint.
- `POST /clinics/{clinicId}/appointments` has the same problem in reverse.

Modeling appointments as a top-level resource keeps the URI structure honest: an appointment is a relationship between a patient and a clinic, not a child of either.

### Filtering by Patient

To retrieve all appointments for a specific patient, the caller uses a query parameter:

```
GET /appointments?patientId={patientId}
```

Additional filters are also supported:

```
GET /appointments?patientId={patientId}&status=SCHEDULED
GET /appointments?clinicId={clinicId}&status=SCHEDULED
```

### HATEOAS Navigation

Because the filtering relationship is not obvious from the URI alone, the patient resource response includes a `_links` entry that pre-builds the filtered URL for the client:

```json
{
  "id": "abc-123",
  "firstName": "Jane",
  "_links": {
    "self":        { "href": "/api/v1/patients/abc-123",                          "rel": "self",         "method": "GET"    },
    "update":      { "href": "/api/v1/patients/abc-123",                          "rel": "update",       "method": "PUT"    },
    "delete":      { "href": "/api/v1/patients/abc-123",                          "rel": "delete",       "method": "DELETE" },
    "vaccinations":{ "href": "/api/v1/patients/abc-123/vaccinations",             "rel": "vaccinations", "method": "GET"    },
    "appointments":{ "href": "/api/v1/appointments?patientId=abc-123",            "rel": "appointments", "method": "GET"    },
    "schedule":    { "href": "/api/v1/appointments",                              "rel": "schedule",     "method": "POST"   }
  }
}
```

The client never needs to construct the query string manually — it follows the `appointments` link from the patient response.

### Convenience Sub-Resource

A read-only `GET /patients/{patientId}/appointments` endpoint is included for discoverability. It is equivalent to `GET /appointments?patientId={patientId}` and supports the same `status` filter. The canonical booking endpoint remains `POST /appointments` — no write operations exist under the patient sub-path.

---

## 2. Vaccinations vs. Appointments — Nesting Asymmetry

Vaccinations **are** nested under patients (`/patients/{patientId}/vaccinations`) while appointments are not. This is intentional:

- A vaccination record has exactly one owner (the patient). Nesting is appropriate.
- An appointment involves a patient and a clinic with equal weight. Nesting under either would misrepresent the relationship.

---

## 3. Data Access Security (Future Work)

### Current State

All endpoints are secured with JWT bearer authentication, meaning any authenticated user can call any endpoint. In particular:

- `GET /appointments` returns all appointments across all patients
- `GET /patients` returns all patient profiles

This is acceptable for the MVP while the focus is on the REST interface contract, but it represents a meaningful gap for any real-world deployment.

### The Problem

Healthcare data is sensitive. A patient should only be able to see their own records. A clinic staff member should only see appointments at their own clinic. As-is, any valid token grants read access to the entire dataset.

### Proposed Future Approach: Role-Based Access Control (RBAC)

The natural model for this system is three roles:

| Role | Access |
|---|---|
| `ROLE_PATIENT` | Own patient profile, own appointments, own vaccinations |
| `ROLE_ADMIN` | Full access to all resources |
| `ROLE_PROVIDER` | Appointments and slots scoped to their clinic |

Key endpoint-level changes this would drive:

- `GET /appointments` — restricted to `ROLE_ADMIN` and `ROLE_PROVIDER`. Patients would use `GET /patients/{patientId}/appointments` exclusively.
- `GET /patients` — restricted to `ROLE_ADMIN`. Patients access only their own record via `GET /patients/{patientId}`.
- `DELETE /patients/{patientId}` — `ROLE_ADMIN` only.
- Patient self-registration (`POST /patients`) would remain unauthenticated or use a separate registration flow.

### Why Deferred

Spring Security supports this via `@PreAuthorize("hasRole('ADMIN')")` on individual controller methods, which maps cleanly to the existing JWT setup. The interface contract would also be updated with an `x-roles-required` extension per operation so the spec remains the source of truth. This work is deferred because it requires agreeing on the user management model (how roles are assigned and stored) before it can be implemented meaningfully.

