#!/usr/bin/env python3
"""
Comprehensive test suite for the Health Portal API.

Usage:
    python test_api.py                        # tests localhost:8080/api/v1
    python test_api.py https://your-app.onrender.com/api/v1

Requires:
    pip install requests
"""

import sys
import uuid
import requests

# Force UTF-8 output on Windows so box-drawing and check/cross chars render
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

BASE_URL = sys.argv[1].rstrip("/") if len(sys.argv) > 1 else "http://localhost:8080/api/v1"

# ── ANSI colours ──────────────────────────────────────────────────────────────
GREEN  = "\033[92m"
RED    = "\033[91m"
YELLOW = "\033[93m"
CYAN   = "\033[96m"
BOLD   = "\033[1m"
DIM    = "\033[2m"
RESET  = "\033[0m"

passed = 0
failed = 0


# ── output helpers ────────────────────────────────────────────────────────────

def section(title: str):
    print(f"\n{BOLD}{CYAN}── {title} {'─' * max(0, 55 - len(title))}{RESET}")

def ok(name: str, detail: str = ""):
    global passed
    passed += 1
    suffix = f"  {DIM}{detail}{RESET}" if detail else ""
    print(f"  {GREEN}✓{RESET}  {name}{suffix}")

def fail(name: str, detail: str = ""):
    global failed
    failed += 1
    suffix = f"  {YELLOW}{detail}{RESET}" if detail else ""
    print(f"  {RED}✗{RESET}  {name}{suffix}")

def check(name: str, condition: bool, detail: str = ""):
    (ok if condition else fail)(name, detail)

def assert_status(name: str, r: requests.Response, expected: int):
    if r.status_code == expected:
        ok(name, f"HTTP {r.status_code}")
    else:
        body_preview = r.text[:120].replace("\n", " ")
        fail(name, f"expected {expected}, got {r.status_code} — {body_preview}")

def assert_links(name: str, data: dict):
    check(name, isinstance(data.get("_links"), dict),
          f"_links={'present' if '_links' in data else 'MISSING'}")

def assert_error_shape(name: str, r: requests.Response):
    """Verify the ErrorResponse schema: timestamp, status, error, message, path."""
    try:
        body = r.json()
        has_all = all(k in body for k in ("timestamp", "status", "error", "message", "path"))
        check(name, has_all, str({k: body.get(k) for k in ("status", "error", "message")}))
    except Exception:
        fail(name, "response is not JSON")


# ── HTTP helpers ──────────────────────────────────────────────────────────────

def _headers(token=None):
    h = {}
    if token:
        h["Authorization"] = f"Bearer {token}"
    return h

def GET(path, token=None, params=None):
    return requests.get(f"{BASE_URL}{path}", headers=_headers(token), params=params, timeout=10)

def POST(path, body, token=None):
    h = {**_headers(token), "Content-Type": "application/json"}
    return requests.post(f"{BASE_URL}{path}", json=body, headers=h, timeout=10)

def PUT(path, body, token=None):
    h = {**_headers(token), "Content-Type": "application/json"}
    return requests.put(f"{BASE_URL}{path}", json=body, headers=h, timeout=10)

def DELETE(path, token=None):
    return requests.delete(f"{BASE_URL}{path}", headers=_headers(token), timeout=10)


# ── seeded IDs (match data.sql) ───────────────────────────────────────────────

CLINIC_1    = "a1b2c3d4-0000-0000-0000-000000000001"
CLINIC_2    = "a1b2c3d4-0000-0000-0000-000000000002"
PATIENT_1   = "b2c3d4e5-0000-0000-0000-000000000001"   # Jane Doe
PATIENT_2   = "b2c3d4e5-0000-0000-0000-000000000002"   # John Smith
VAC_1       = "c3d4e5f6-0000-0000-0000-000000000001"   # Jane's FLU
APPT_1      = "d4e5f6a7-0000-0000-0000-000000000001"   # Jane's appointment
SLOT_DATE_1 = "2026-05-15"
SLOT_DATE_2 = "2026-05-20"

# unique email so the test patient doesn't conflict with seeded data
TEST_EMAIL = f"test.{uuid.uuid4().hex[:10]}@example.com"

PATIENT_BODY = {
    "firstName": "Test",
    "lastName":  "Patient",
    "dateOfBirth": "1995-06-15",
    "email":     TEST_EMAIL,
    "phone":     "410-555-9999",
    "address":   {"street": "789 Test Blvd", "city": "Baltimore", "state": "MD", "zip": "21210"},
}

# nextDueDate in the PAST → isDue() = true → triggers the schedule link
DUE_VACCINATION_BODY = {
    "vaccineType":      "MMR",
    "dateAdministered": "2024-03-01",
    "nextDueDate":      "2025-03-01",   # past date
    "provider":         "Test Clinic",
    "lotNumber":        "T1T2T3",
}

FUTURE_VACCINATION_BODY = {
    "vaccineType":      "HEPATITIS_B",
    "dateAdministered": "2026-01-15",
    "nextDueDate":      "2027-01-15",   # future → isDue() = false
    "provider":         "Test Clinic",
    "lotNumber":        "H9H8H7",
}

# unique dateTime to avoid conflicts with seeded appointments
TEST_APPT_DATETIME = "2026-09-05T10:00:00"

# ═════════════════════════════════════════════════════════════════════════════
print(f"\n{BOLD}Health Portal API — Full Test Suite{RESET}")
print(f"{DIM}Target: {BASE_URL}{RESET}")

# connectivity check
try:
    requests.get(BASE_URL, timeout=5)
except requests.exceptions.ConnectionError:
    print(f"\n{RED}{BOLD}ERROR: Cannot connect to {BASE_URL}{RESET}")
    print("Make sure the server is running before executing the tests.\n")
    sys.exit(1)

TOKEN        = None
patient_id   = None
due_vac_id   = None
fut_vac_id   = None
appointment_id = None


# ─────────────────────────────────────────────────────────────────────────────
section("AUTH  POST /auth/login")

r = POST("/auth/login", {"username": "admin", "password": "admin123"})
assert_status("Valid credentials → 200", r, 200)
if r.status_code == 200:
    body = r.json()
    check("Body has 'token'",     "token"     in body)
    check("Body has 'expiresIn'", "expiresIn" in body)
    TOKEN = body.get("token")
else:
    fail("Could not obtain token — aborting remaining tests")
    sys.exit(1)

r = POST("/auth/login", {"username": "staff", "password": "staff123"})
assert_status("Secondary user valid credentials → 200", r, 200)

r = POST("/auth/login", {"username": "wrong", "password": "bad"})
assert_status("Wrong credentials → 401", r, 401)
assert_error_shape("Error body has correct shape", r)

r = POST("/auth/login", {"username": "", "password": "admin123"})
assert_status("Empty username → 400", r, 400)

r = POST("/auth/login", {"username": "admin", "password": ""})
assert_status("Empty password → 400", r, 400)

r = POST("/auth/login", {})
assert_status("Empty body → 400", r, 400)


# ─────────────────────────────────────────────────────────────────────────────
section("SECURITY  JWT enforcement on protected endpoints")

r = GET("/patients")
assert_status("No token → 401", r, 401)

r = GET("/patients", token="this.is.not.valid")
assert_status("Malformed token → 401", r, 401)

r = GET("/patients", token="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlIn0.invalidsignature")
assert_status("Invalid signature → 401", r, 401)


# ─────────────────────────────────────────────────────────────────────────────
section("PATIENTS  GET /patients")

r = GET("/patients", token=TOKEN)
assert_status("List patients → 200", r, 200)
body = r.json()
check("Has 'content' array",    isinstance(body.get("content"), list))
check("Has 'page' metadata",    isinstance(body.get("page"), dict))
assert_links("Has '_links'", body)
page = body.get("page", {})
check("totalElements >= 2 (seeded data)", page.get("totalElements", 0) >= 2)

r = GET("/patients", token=TOKEN, params={"page": 0, "size": 1})
assert_status("Pagination size=1 → 200", r, 200)
body = r.json()
check("Page size respected (≤ 1 item)", len(body.get("content", [])) <= 1)
check("totalPages > 1 when size=1",     body.get("page", {}).get("totalPages", 0) > 1)

# Each patient in the list should have _links
r = GET("/patients", token=TOKEN)
patients = r.json().get("content", [])
if patients:
    check("Each patient has '_links'", all("_links" in p for p in patients))


# ─────────────────────────────────────────────────────────────────────────────
section("PATIENTS  POST /patients")

r = POST("/patients", PATIENT_BODY, token=TOKEN)
assert_status("Create patient → 201", r, 201)
check("Location header present", "Location" in r.headers)
body = r.json()
check("Body has 'id'", "id" in body)
assert_links("Body has '_links'", body)
patient_id = body.get("id")

r = POST("/patients", PATIENT_BODY, token=TOKEN)
assert_status("Duplicate email → 409", r, 409)
assert_error_shape("409 body has correct shape", r)

r = POST("/patients", {}, token=TOKEN)
assert_status("Empty body → 400", r, 400)

r = POST("/patients", {"firstName": "X", "lastName": "Y", "dateOfBirth": "2000-01-01"}, token=TOKEN)
assert_status("Missing email + address → 400", r, 400)
assert_error_shape("400 body has correct shape", r)

r = POST("/patients", {**PATIENT_BODY, "email": f"new{uuid.uuid4().hex[:6]}@x.com",
                        "address": {"street": "1 A St"}}, token=TOKEN)
assert_status("Incomplete address (missing city/state/zip) → 400", r, 400)


# ─────────────────────────────────────────────────────────────────────────────
section("PATIENTS  GET /patients/{patientId}")

r = GET(f"/patients/{patient_id}", token=TOKEN)
assert_status("Get test patient → 200", r, 200)
body  = r.json()
links = body.get("_links", {})
check("id matches",                     body.get("id") == patient_id)
check("firstName correct",              body.get("firstName") == "Test")
assert_links("Has '_links'",            body)
check("self link present",              "self"         in links)
check("update link present",            "update"       in links)
check("delete link present",            "delete"       in links)
check("vaccinations link present",      "vaccinations" in links)
check("appointments link has patientId query param",
      "appointments" in links and
      f"patientId={patient_id}" in links["appointments"].get("href", ""))
check("NO schedule link yet (no vaccinations)", "schedule" not in links)

r = GET(f"/patients/{PATIENT_1}", token=TOKEN)
assert_status("Get seeded patient (Jane) → 200", r, 200)
jane_links = r.json().get("_links", {})
check("Jane has no schedule link (all vaccinations future)",
      "schedule" not in jane_links)

r = GET(f"/patients/{uuid.uuid4()}", token=TOKEN)
assert_status("Unknown patientId → 404", r, 404)
assert_error_shape("404 body has correct shape", r)


# ─────────────────────────────────────────────────────────────────────────────
section("PATIENTS  PUT /patients/{patientId}")

updated_body = {**PATIENT_BODY, "firstName": "Updated"}
r = PUT(f"/patients/{patient_id}", updated_body, token=TOKEN)
assert_status("Update patient → 200", r, 200)
check("firstName updated to 'Updated'", r.json().get("firstName") == "Updated")
assert_links("Response has '_links'", r.json())

r = PUT(f"/patients/{patient_id}", {}, token=TOKEN)
assert_status("Update with empty body → 400", r, 400)

r = PUT(f"/patients/{uuid.uuid4()}", updated_body, token=TOKEN)
assert_status("Update unknown patient → 404", r, 404)


# ─────────────────────────────────────────────────────────────────────────────
section("VACCINATIONS  POST /patients/{patientId}/vaccinations")

# Create a PAST-due vaccination — this should make the schedule link appear
r = POST(f"/patients/{patient_id}/vaccinations", DUE_VACCINATION_BODY, token=TOKEN)
assert_status("Create due vaccination → 201", r, 201)
check("Location header present", "Location" in r.headers)
body = r.json()
check("Body has 'id'",                    "id"        in body)
check("patientId set correctly",          body.get("patientId") == patient_id)
check("vaccineType correct",              body.get("vaccineType") == "MMR")
check("isDue = True (past nextDueDate)",  body.get("isDue") == True)
assert_links("Body has '_links'", body)
due_vac_id = body.get("id")

# Create a FUTURE vaccination (isDue = false)
r = POST(f"/patients/{patient_id}/vaccinations", FUTURE_VACCINATION_BODY, token=TOKEN)
assert_status("Create future vaccination → 201", r, 201)
check("isDue = False (future nextDueDate)", r.json().get("isDue") == False)
fut_vac_id = r.json().get("id")

r = POST(f"/patients/{uuid.uuid4()}/vaccinations", DUE_VACCINATION_BODY, token=TOKEN)
assert_status("Vaccination for unknown patient → 404", r, 404)

r = POST(f"/patients/{patient_id}/vaccinations",
         {"vaccineType": "NOT_A_VACCINE", "dateAdministered": "2025-01-01"}, token=TOKEN)
assert_status("Invalid vaccineType → 400", r, 400)

r = POST(f"/patients/{patient_id}/vaccinations",
         {"provider": "Some Clinic"}, token=TOKEN)
assert_status("Missing vaccineType + dateAdministered → 400", r, 400)


# ─────────────────────────────────────────────────────────────────────────────
section("HATEOAS  schedule link appears / disappears based on isDue")

r = GET(f"/patients/{patient_id}", token=TOKEN)
assert_status("Get patient after adding due vaccination → 200", r, 200)
links = r.json().get("_links", {})
check("schedule link PRESENT after due vaccination added",
      "schedule" in links,
      f"links present: {list(links.keys())}")
if "schedule" in links:
    check("schedule method is POST",               links["schedule"].get("method") == "POST")
    check("schedule href points to /appointments", "/appointments" in links["schedule"].get("href", ""))

# Delete the due vaccination and verify the schedule link goes away
DELETE(f"/patients/{patient_id}/vaccinations/{due_vac_id}", token=TOKEN)
due_vac_id = None  # consumed by delete

r = GET(f"/patients/{patient_id}", token=TOKEN)
assert_status("Get patient after removing due vaccination → 200", r, 200)
links = r.json().get("_links", {})
check("schedule link ABSENT after due vaccination removed",
      "schedule" not in links,
      f"links present: {list(links.keys())}")


# ─────────────────────────────────────────────────────────────────────────────
section("VACCINATIONS  GET /patients/{patientId}/vaccinations")

r = GET(f"/patients/{patient_id}/vaccinations", token=TOKEN)
assert_status("List vaccinations → 200", r, 200)
body = r.json()
check("Has 'content' array",           isinstance(body.get("content"), list))
assert_links("Has '_links'",           body)
check("patient link in _links",        "patient" in body.get("_links", {}))
check("At least 1 vaccination (future one still exists)",
      len(body.get("content", [])) >= 1)

r = GET(f"/patients/{PATIENT_1}/vaccinations", token=TOKEN)
assert_status("Seeded patient vaccinations → 200", r, 200)
check("Jane has 2 seeded vaccinations",
      len(r.json().get("content", [])) == 2)

r = GET(f"/patients/{uuid.uuid4()}/vaccinations", token=TOKEN)
assert_status("Vaccinations for unknown patient → 404", r, 404)


# ─────────────────────────────────────────────────────────────────────────────
section("VACCINATIONS  GET /patients/{patientId}/vaccinations/{vaccinationId}")

r = GET(f"/patients/{patient_id}/vaccinations/{fut_vac_id}", token=TOKEN)
assert_status("Get vaccination → 200", r, 200)
body = r.json()
check("id matches",                         body.get("id") == fut_vac_id)
check("patientId matches",                  body.get("patientId") == patient_id)
check("isDue = False (future nextDueDate)", body.get("isDue") == False)
assert_links("Has '_links'", body)
vac_links = body.get("_links", {})
check("self link present",    "self"    in vac_links)
check("update link present",  "update"  in vac_links)
check("delete link present",  "delete"  in vac_links)
check("patient link present", "patient" in vac_links)

r = GET(f"/patients/{patient_id}/vaccinations/{uuid.uuid4()}", token=TOKEN)
assert_status("Unknown vaccinationId → 404", r, 404)

r = GET(f"/patients/{uuid.uuid4()}/vaccinations/{fut_vac_id}", token=TOKEN)
assert_status("Wrong patientId for vaccination → 404", r, 404)

r = GET(f"/patients/{uuid.uuid4()}/vaccinations/{uuid.uuid4()}", token=TOKEN)
assert_status("Unknown patient + unknown vaccination → 404", r, 404)


# ─────────────────────────────────────────────────────────────────────────────
section("VACCINATIONS  PUT /patients/{patientId}/vaccinations/{vaccinationId}")

r = PUT(f"/patients/{patient_id}/vaccinations/{fut_vac_id}",
        {**FUTURE_VACCINATION_BODY, "provider": "Updated Clinic"}, token=TOKEN)
assert_status("Update vaccination → 200", r, 200)
check("provider updated", r.json().get("provider") == "Updated Clinic")

r = PUT(f"/patients/{patient_id}/vaccinations/{uuid.uuid4()}",
        FUTURE_VACCINATION_BODY, token=TOKEN)
assert_status("Update unknown vaccination → 404", r, 404)


# ─────────────────────────────────────────────────────────────────────────────
section("APPOINTMENTS  POST /appointments")

appt_body = {
    "patientId": patient_id,
    "clinicId":  CLINIC_1,
    "dateTime":  TEST_APPT_DATETIME,
    "reason":    "Test appointment",
}
r = POST("/appointments", appt_body, token=TOKEN)
assert_status("Create appointment → 201", r, 201)
check("Location header present",             "Location" in r.headers)
body = r.json()
check("Body has 'id'",                       "id"        in body)
check("status defaults to SCHEDULED",        body.get("status") == "SCHEDULED")
check("patientId correct",                   body.get("patientId") == patient_id)
check("clinicId correct",                    body.get("clinicId")  == CLINIC_1)
assert_links("Body has '_links'", body)
appointment_id = body.get("id")

r = POST("/appointments", appt_body, token=TOKEN)
assert_status("Double-book same clinic+dateTime → 409", r, 409)
assert_error_shape("409 body has correct shape", r)

r = POST("/appointments", {**appt_body, "patientId": str(uuid.uuid4())}, token=TOKEN)
assert_status("Unknown patientId → 404", r, 404)

r = POST("/appointments", {**appt_body, "clinicId": str(uuid.uuid4()),
                            "dateTime": "2026-09-05T11:00:00"}, token=TOKEN)
assert_status("Unknown clinicId → 404", r, 404)

r = POST("/appointments", {"dateTime": TEST_APPT_DATETIME}, token=TOKEN)
assert_status("Missing patientId + clinicId → 400", r, 400)

r = POST("/appointments", {}, token=TOKEN)
assert_status("Empty body → 400", r, 400)


# ─────────────────────────────────────────────────────────────────────────────
section("APPOINTMENTS  GET /appointments")

r = GET("/appointments", token=TOKEN)
assert_status("List all appointments → 200", r, 200)
body = r.json()
check("Has 'content' array",  isinstance(body.get("content"), list))
check("Has 'page' metadata",  isinstance(body.get("page"), dict))
assert_links("Has '_links'",  body)
check("At least 3 appointments (2 seeded + 1 test)",
      body.get("page", {}).get("totalElements", 0) >= 3)

r = GET("/appointments", token=TOKEN, params={"patientId": patient_id})
assert_status("Filter by patientId → 200", r, 200)
content = r.json().get("content", [])
check("All results belong to test patient",
      all(a.get("patientId") == patient_id for a in content))

r = GET("/appointments", token=TOKEN, params={"clinicId": CLINIC_1})
assert_status("Filter by clinicId → 200", r, 200)
content = r.json().get("content", [])
check("All results belong to CLINIC_1",
      all(a.get("clinicId") == CLINIC_1 for a in content))

r = GET("/appointments", token=TOKEN, params={"status": "SCHEDULED"})
assert_status("Filter by status=SCHEDULED → 200", r, 200)
content = r.json().get("content", [])
check("All results have SCHEDULED status",
      all(a.get("status") == "SCHEDULED" for a in content) if content else True)

r = GET("/appointments", token=TOKEN, params={"patientId": patient_id, "status": "SCHEDULED"})
assert_status("Filter by patientId + status → 200", r, 200)

r = GET("/appointments", token=TOKEN, params={"page": 0, "size": 1})
assert_status("Pagination size=1 → 200", r, 200)
check("Page size respected", len(r.json().get("content", [])) <= 1)


# ─────────────────────────────────────────────────────────────────────────────
section("APPOINTMENTS  GET /appointments/{appointmentId}")

r = GET(f"/appointments/{appointment_id}", token=TOKEN)
assert_status("Get appointment → 200", r, 200)
body  = r.json()
links = body.get("_links", {})
check("id matches",           body.get("id") == appointment_id)
assert_links("Has '_links'",  body)
check("self link present",    "self"   in links)
check("update link present",  "update" in links)
check("cancel link present",  "cancel" in links)

r = GET(f"/appointments/{uuid.uuid4()}", token=TOKEN)
assert_status("Unknown appointmentId → 404", r, 404)
assert_error_shape("404 body has correct shape", r)


# ─────────────────────────────────────────────────────────────────────────────
section("APPOINTMENTS  GET /patients/{patientId}/appointments  (convenience)")

r = GET(f"/patients/{patient_id}/appointments", token=TOKEN)
assert_status("Convenience endpoint → 200", r, 200)
body  = r.json()
links = body.get("_links", {})
check("Has 'content' array",                    isinstance(body.get("content"), list))
assert_links("Has '_links'",                    body)
check("patient link points back to patient",    "patient" in links)
check("All items belong to test patient",
      all(a.get("patientId") == patient_id for a in body.get("content", [])))

r = GET(f"/patients/{patient_id}/appointments", token=TOKEN, params={"status": "SCHEDULED"})
assert_status("Convenience with status filter → 200", r, 200)

r = GET(f"/patients/{uuid.uuid4()}/appointments", token=TOKEN)
assert_status("Convenience for unknown patient → 404", r, 404)


# ─────────────────────────────────────────────────────────────────────────────
section("APPOINTMENTS  PUT /appointments/{appointmentId}")

reschedule_body = {**appt_body, "dateTime": "2026-10-01T14:00:00", "reason": "Rescheduled"}
r = PUT(f"/appointments/{appointment_id}", reschedule_body, token=TOKEN)
assert_status("Update appointment → 200", r, 200)
body = r.json()
check("reason updated",   body.get("reason")   == "Rescheduled")
check("dateTime updated", body.get("dateTime") == "2026-10-01T14:00:00")
assert_links("Response has '_links'", body)

r = PUT(f"/appointments/{uuid.uuid4()}", reschedule_body, token=TOKEN)
assert_status("Update unknown appointment → 404", r, 404)


# ─────────────────────────────────────────────────────────────────────────────
section("CLINICS  GET /clinics")

r = GET("/clinics", token=TOKEN)
assert_status("List clinics → 200", r, 200)
body = r.json()
check("Has 'content' array",      isinstance(body.get("content"), list))
check("Has 'page' metadata",      isinstance(body.get("page"), dict))
assert_links("Has '_links'",      body)
check("At least 2 seeded clinics", len(body.get("content", [])) >= 2)
if body.get("content"):
    check("Each clinic has '_links'", all("_links" in c for c in body["content"]))

r = GET("/clinics", token=TOKEN, params={"zip": "21287"})
assert_status("Filter by zip → 200", r, 200)
check("Zip filter returns ≥ 1 result", len(r.json().get("content", [])) >= 1)

r = GET("/clinics", token=TOKEN, params={"city": "Baltimore"})
assert_status("Filter by city → 200", r, 200)
check("City filter returns ≥ 1 result", len(r.json().get("content", [])) >= 1)

r = GET("/clinics", token=TOKEN, params={"zip": "00000"})
assert_status("Filter by nonexistent zip → 200 empty", r, 200)
check("No results for unknown zip", len(r.json().get("content", [])) == 0)


# ─────────────────────────────────────────────────────────────────────────────
section("CLINICS  GET /clinics/{clinicId}")

r = GET(f"/clinics/{CLINIC_1}", token=TOKEN)
assert_status("Get clinic → 200", r, 200)
body  = r.json()
links = body.get("_links", {})
check("id present",        "id"      in body)
check("name present",      "name"    in body)
check("address present",   "address" in body)
assert_links("Has '_links'", body)
check("self link present",  "self"   in links)
check("slots link present", "slots"  in links)

r = GET(f"/clinics/{uuid.uuid4()}", token=TOKEN)
assert_status("Unknown clinicId → 404", r, 404)


# ─────────────────────────────────────────────────────────────────────────────
section("CLINICS  GET /clinics/{clinicId}/slots")

r = GET(f"/clinics/{CLINIC_1}/slots", token=TOKEN, params={"date": SLOT_DATE_1})
assert_status("Get slots for date → 200", r, 200)
body  = r.json()
slots = body.get("content", [])
assert_links("Has '_links'", body)
check("At least 1 slot for seeded date",  len(slots) >= 1)
if slots:
    s = slots[0]
    check("Slot has 'id'",        "id"        in s)
    check("Slot has 'dateTime'",  "dateTime"  in s)
    check("Slot has 'available'", "available" in s)
    assert_links("Slot has '_links'", s)
    check("Slot _links has 'book'",   "book" in s.get("_links", {}))

check("Mix of available/unavailable slots",
      any(s.get("available") for s in slots) and any(not s.get("available") for s in slots))

r = GET(f"/clinics/{CLINIC_1}/slots", token=TOKEN)
assert_status("Missing required date param → 400", r, 400)

r = GET(f"/clinics/{CLINIC_1}/slots", token=TOKEN, params={"date": "2099-01-01"})
assert_status("Date with no slots → 200 empty", r, 200)
check("No slots for far-future date", r.json().get("content", []) == [])

r = GET(f"/clinics/{uuid.uuid4()}/slots", token=TOKEN, params={"date": SLOT_DATE_1})
assert_status("Slots for unknown clinic → 404", r, 404)


# ─────────────────────────────────────────────────────────────────────────────
section("ADDRESS VALIDATION  GET /address/validate")

r = GET("/address/validate", token=TOKEN, params={
    "street": "600 N Wolfe St", "city": "Baltimore", "state": "md", "zip": "21287",
})
assert_status("Valid address params → 200", r, 200)
body = r.json()
check("'valid' field present",               "valid"             in body)
check("'normalizedAddress' field present",   "normalizedAddress" in body)
check("'message' field present",             "message"           in body)
check("valid = True",                        body.get("valid") == True)
normalized = body.get("normalizedAddress") or {}
check("State normalized to uppercase 'MD'",
      normalized.get("state") == "MD")
check("Street preserved",
      normalized.get("street") is not None)

r = GET("/address/validate", token=TOKEN, params={"street": "123 Main St"})
assert_status("Missing city/state/zip → 400", r, 400)

r = GET("/address/validate", token=TOKEN)
assert_status("All params missing → 400", r, 400)


# ─────────────────────────────────────────────────────────────────────────────
section("SWAGGER UI  accessibility")

# Swagger UI is served outside the API context path — adjust URL accordingly
server_root = BASE_URL.split("/api/v1")[0]

r = requests.get(f"{server_root}/api/v1/swagger-ui/index.html", timeout=10)
check("Swagger UI reachable (HTTP 2xx)", 200 <= r.status_code < 300,
      f"HTTP {r.status_code}")

r = requests.get(f"{server_root}/api/v1/openapi.yaml", timeout=10)
check("openapi.yaml reachable (HTTP 2xx)", 200 <= r.status_code < 300,
      f"HTTP {r.status_code}")


# ─────────────────────────────────────────────────────────────────────────────
section("CLEANUP  DELETE created test data")

if fut_vac_id:
    r = DELETE(f"/patients/{patient_id}/vaccinations/{fut_vac_id}", token=TOKEN)
    assert_status("Delete vaccination → 204", r, 204)

    r = GET(f"/patients/{patient_id}/vaccinations/{fut_vac_id}", token=TOKEN)
    assert_status("Deleted vaccination → 404", r, 404)

    r = DELETE(f"/patients/{patient_id}/vaccinations/{fut_vac_id}", token=TOKEN)
    assert_status("Re-delete vaccination → 404", r, 404)

if appointment_id:
    r = DELETE(f"/appointments/{appointment_id}", token=TOKEN)
    assert_status("Cancel appointment → 204", r, 204)

    r = GET(f"/appointments/{appointment_id}", token=TOKEN)
    assert_status("Cancelled appointment → 404", r, 404)

    r = DELETE(f"/appointments/{appointment_id}", token=TOKEN)
    assert_status("Re-cancel appointment → 404", r, 404)

r = DELETE(f"/appointments/{uuid.uuid4()}", token=TOKEN)
assert_status("Cancel nonexistent appointment → 404", r, 404)

if patient_id:
    r = DELETE(f"/patients/{patient_id}", token=TOKEN)
    assert_status("Delete patient → 204", r, 204)

    r = GET(f"/patients/{patient_id}", token=TOKEN)
    assert_status("Deleted patient → 404", r, 404)

    r = DELETE(f"/patients/{patient_id}", token=TOKEN)
    assert_status("Re-delete patient → 404", r, 404)

r = DELETE(f"/patients/{uuid.uuid4()}", token=TOKEN)
assert_status("Delete nonexistent patient → 404", r, 404)


# ─────────────────────────────────────────────────────────────────────────────
total = passed + failed
bar   = "═" * 56
print(f"\n{BOLD}{bar}{RESET}")
print(f"{BOLD}  Results:  {GREEN}{passed} passed{RESET}{BOLD}  ·  {RED}{failed} failed{RESET}{BOLD}  ·  {total} total{RESET}")
print(f"{BOLD}{bar}{RESET}\n")

if failed == 0:
    print(f"  {GREEN}{BOLD}All tests passed.{RESET}\n")
else:
    print(f"  {YELLOW}Fix the {failed} failure(s) marked with ✗ above.{RESET}\n")

sys.exit(0 if failed == 0 else 1)
