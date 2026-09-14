# Frontend Build Prompt — Glasses Shop (Request → Appointment → Eye Exam → Prescription)

Use this prompt to instruct an AI coding agent (e.g. opencode) to build the
customer-facing + staff-facing frontend for the Glasses Shop backend.

---

## System prompt (copy this into opencode)

Build the frontend for an Optical Shop management system that talks to the
existing Spring Boot backend at the endpoints described below. Follow the
business logic exactly.

You are building a user-facing web frontend for an Optometry/Glasses Shop.
The frontend must implement the full request workflow:

```
Customer
 → submits Eye Exam Request (type = "exam")
 → Request status = PENDING
 → Staff approves
 → Appointment created (request_id = Request.id, status = PENDING_SCHEDULING)
 → Appointment scheduled + optometrist assigned
 → Customer attends the eye exam
 → Optometrist records the customer's Prescription
 → Prescription is linked to the Customer
```

### Business rules (MUST be respected)

1. Submitting an eye-exam request NEVER requires a prescription and NEVER
   creates one. Payload is only `type`, optional `notes`, optional
   `customerId` (staff/admin only). NO `prescriptionId` field exists in the
   request API.
2. The order of the flow is **Request → Appointment → Eye Exam → Prescription**.
   A new eye-exam request is valid even if the customer has no prescription.
3. Approving an `exam` request sets the Request to `APPROVED` and the backend
   auto-creates an Appointment with status `PENDING_SCHEDULING`.
   Do NOT call the appointment creation API yourself for this — approval does it.
4. Approving a `product` request sets it to `APPROVED` only. NO appointment is
   created. Product purchases are handled by the Order workflow.
5. Rejecting a request sets it to `REJECTED`. No appointment, no prescription.
6. A prescription is only created AFTER the eye exam, by an optometrist, via
   `POST /api/prescriptions`.
7. `updatedAt` on a request is only present in responses after a real
   modification. Do not treat `updatedAt` as required.

### Backend API contract

Base URL is configurable (`VITE_API_BASE_URL` or a constant at the top of the
code). All endpoints below are JSON.

#### Auth

| Method | Endpoint | Auth | Request BODY | Response |
|---|---|---|---|---|
| POST | `/api/auth/login` | public | `{"identifier": "email-or-phone", "password": "..."}` | `{access_token, token_type, expires_at, user}` |
| POST | `/api/customers/register` | public | `{"name", "phone", "email", "address?", "date_of_birth?", "password"}` | `{access_token, token_type, expires_at, user}` |
| GET | `/api/auth/me` | Bearer | — | `user` |

The token is a JWT. Store it in `localStorage` and send it as:
`Authorization: Bearer <access_token>` for every authenticated call.

#### Requests (customer request workflow)

| Method | Endpoint | Auth (JWT role) |
|---|---|---|
| POST | `/api/requests` | CUSTOMER or ADMIN |
| POST | `/api/requests/{id}/approve` | STAFF or ADMIN |
| POST | `/api/requests/{id}/reject` | STAFF or ADMIN |
| GET | `/api/requests` | any authenticated |
| GET | `/api/requests/{id}` | any authenticated |
| GET | `/api/requests/customer/{customerId}` | any authenticated |
| GET | `/api/requests/status/{status}` | any authenticated |
| DELETE | `/api/requests/{id}` | any authenticated |

POST `/api/requests` body:

```json
{
  "type": "exam",
  "customerId": 5,
  "notes": "I would like to have my vision checked."
}
```

- `type` required: `exam` or `product` (case-insensitive). Else HTTP 400.
- `notes` optional (max 1000).
- `customerId` OPTIONAL. Only accepted when the token role is STAFF or ADMIN
  (submit on behalf of a customer). A CUSTOMER token sending `customerId`
  gets HTTP 403. When omitted, the backend uses the customer linked to the
  token's user.

Response (201 on create), `RequestResponse`:

```json
{
  "id": 2,
  "customer_id": 2,
  "customer_name": "Sokha Chann",
  "type": "exam",
  "status": "PENDING",
  "notes": "...",
  "createdAt": "2026-09-15T00:34:19.619002",
  "updatedAt": null
}
```

`status` is one of: `PENDING`, `APPROVED`, `REJECTED`.

#### Prescriptions (created only AFTER the exam)

| Method | Endpoint | Auth |
|---|---|---|
| POST | `/api/prescriptions` | any authenticated |
| GET | `/api/prescriptions` | any authenticated |
| GET | `/api/prescriptions/{id}` | any authenticated |
| GET | `/api/prescriptions/customer/{customerId}` | any authenticated |
| GET | `/api/prescriptions/user/{userId}` | any authenticated |
| PUT | `/api/prescriptions/{id}` | any authenticated |
| DELETE | `/api/prescriptions/{id}` | any authenticated |

POST `/api/prescriptions` body:

```json
{
  "customer_id": 5,
  "user_id": 3,
  "od_sphere": -1.5,
  "od_cylinder": -0.25,
  "od_axis": 180,
  "os_sphere": -1.0,
  "os_cylinder": -0.5,
  "os_axis": 175,
  "near_addition": 1.0,
  "pupillary_distance": 62.0,
  "notes": "Anti-reflective coating recommended.",
  "prescription_date": "2026-09-15"
}
```

- `customer_id` required (the customer the prescription belongs to).
- `user_id` optional (the optometrist who performed the exam).
- Axis values must be 0–180 (else 400).

#### Appointments

| Method | Endpoint | Auth |
|---|---|---|
| POST | `/api/appointments` | any authenticated |
| GET | `/api/appointments` | any authenticated |
| GET | `/api/appointments/{id}` | any authenticated |
| PUT | `/api/appointments/{id}` | any authenticated |
| DELETE | `/api/appointments/{id}` | any authenticated |

POST `/api/appointments` body:

```json
{
  "customer_id": 5,
  "optometrist_id": 3,
  "scheduled_at": "2026-09-20T09:30:00",
  "status": "SCHEDULED",
  "notes": "Eye exam follow-up"
}
```

Do NOT create an appointment when approving a request — the backend does that.
Use `POST /api/appointments` only for scheduling/updating an existing approved
request's appointment (set `scheduled_at` and `optometrist_id`).

#### Supporting APIs (for staff/admin screens)

| Method | Endpoint | Notes |
|---|---|---|
| GET | `/api/customers?search=&page=&size=` | search/paginate customers |
| GET | `/api/customers/{id}` | customer detail |
| POST | `/api/customers` | create customer |
| GET | `/api/user` | list users (staff/optometrists) |
| GET | `/api/products?search=` | public product catalog |
| GET | `/api/inventories/low-stock` | low-stock list |
| GET | `/api/orders?status=` | order list |

### Frontend UI requirements

Views to build (role-aware):

1. **Auth** — login (email or phone) + customer registration. Runs first, gates
   everything else.
2. **Customer portal** (`/`) — after login/registration a customer can:
   - Submit an eye exam request (`type=exam`) with optional notes.
   - Submit a product request (`type=product`) with optional notes.
   - See their own requests and their status (PENDING/APPROVED/REJECTED).
3. **Staff/admin portal** (`/staff`) — staff and admin can:
   - See all incoming requests.
   - Approve or reject a PENDING request.
   - On APPROVED exam requests, schedule the auto-created appointment
     (pick `scheduled_at` and an optometrist).
   - See the customer record for a request (pre-filled in the request list).
4. **Optometrist screen** (`/optometrist`) — optometrists can:
   - See scheduled appointments.
   - After the exam, create/record the customer's prescription
     (form with customer, OD/OS sphere/cylinder/axis, near addition, PD, notes).
   - Link the prescription to the customer via `POST /api/prescriptions`.
5. **Redirect logic** — admin/staff are UNLIKELY to have a linked Customer
   profile, so the submit form must let them pass `customerId` (chosen from the
   customer list) when submitting on behalf of a customer.

### Technical constraints

- Plain HTML/CSS/vanilla JS, no build step (the existing backend serves static
  files from `src/main/resources/static`). Reuse the visual style of the
  existing `request-form.html`.
- Keep the JWT in `localStorage` and always send `Authorization: Bearer ...`.
- Gracefully handle HTTP error bodies (`{"error": "..."}` or field-error maps)
  and show messages to the user.
- Role comes back inside the JWT claim `role` and in the login/register
  `user.role` object — use it to gate which views are visible.
- Do NOT invent endpoints. Use only the contract above.

### Acceptance test scenarios

- A brand-new customer with no prescription can register, log in, and submit an
  `exam` request successfully (it must not ask for a prescription).
- Staff sees the request as PENDING, approves it, and the request becomes
  APPROVED with a PENDING_SCHEDULING appointment auto-created.
- Staff rejects another request and it becomes REJECTED with no appointment.
- The customer sees the status updates on their list page.
- After the appointment, the optometrist records a new prescription and it
  becomes available on the customer's profile.
- A product request never creates an appointment.