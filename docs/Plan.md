PLAN.MD
Optical Shop Management System - Backend Roadmap

STATUS: CRUD complete for all modules except Branch.
Branch module was removed. There is no Branch entity and no branch_id column
on any table. Any text in this file that says "filter by branch" is no longer
valid. Update those references when you read this file.

Completed work:
- CRUD for User, Customer, Product, Inventory, Appointment, Prescription, Order, OrderItem
- Security and JWT authentication (auth endpoints, token, role checks, error handling)

Current task: PAGINATION, FILTERING AND SEARCH (backend)
This was moved ahead of the QR code task. See the section at the bottom of
this file for the full plan.

Step 1. Dependencies (pom.xml)
- Add spring-boot-starter-security
- Add spring-boot-starter-oauth2-resource-server

Step 2. Configuration
- application.properties plus .env file gets these variables:
  JWT_SECRET (base64 encoded, at least 32 bytes)
  JWT_EXPIRATION_MINUTES (for example 60)
  ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_BRANCH_ID (used to seed first admin)

Step 3. Role enum
- New file: model/Role.java with values ADMIN, STAFF, OPTOMETRIST
- User.java changes role field from String to:
  Enumerated(EnumType.STRING) private Role role
- Database column stays VARCHAR so existing schema still works

Step 4. New security package
Files to create:
- SecurityConfig.java
  Stateless sessions, CSRF off, permit auth and swagger paths,
  all other endpoints require authentication.
  Beans: PasswordEncoder (BCrypt) and JwtDecoder.
- JwtService.java
  Issues tokens using NimbusJwtEncoder.
  Token claims: email (subject), user id, role, branch id.
- AppUserDetailsService.java
  Loads users by email using UserRepository.findByEmail.

Step 5. Auth endpoints
- New DTOs: LoginRequest (email and password), AuthResponse
  (accessToken, tokenType Bearer, expiresIn, user info without password)
- New controller: AuthController
  POST /api/auth/login returns token
  GET /api/auth/me returns current user from token

Step 6. Fix password handling
- UserService.create and UserService.update now hash passwords with BCrypt
- Password never returned in any response

Step 7. Authorization rules
- UserController: only ADMIN can create, bulk create, update, delete
- GET endpoints available to any authenticated user
- Other modules require login but no specific role yet

Step 8. Error handling (GlobalExceptionHandler)
- Bad credentials returns 401
- Missing role returns 403
- Unauthenticated requests get JSON response, not default HTML

Step 9. Bootstrap admin account
- ApplicationRunner creates one admin at startup if no user exists

IMPORTANT NOTE ABOUT OLD DATA
Existing rows in the database have plaintext passwords and text roles.
They will NOT work after this change. Recommended: delete dev data
and reseed, or manually set one admin password hash.

VERIFICATION STEPS
1. Run: ./mvnw compile -q
2. Start app, login as seeded admin
3. Call GET /api/user with token: expect 200
4. Call GET /api/user without token: expect 401
5. Swagger Authorize button works with the token

CURRENT TASK: QR CODE CUSTOMER SELF-REGISTRATION (NOT STARTED - deferred, do pagination first)

Overview
- Customer scans a QR code at the shop
- QR opens a registration page in the browser (no frontend yet, backend serves HTML)
- Customer fills in full profile and submits
- Backend creates the customer record, auto-assigned to the branch from the QR code

Flow
  QR Code (PNG) → Customer scans → Opens browser → Registration form → Submit → Customer created

Step 1. Add QR code library dependency
- pom.xml: add com.google.zxing (core + javase) for QR code generation

Step 2. QR code generation endpoint
- GET /api/qr/branch/{branchId} → returns QR code as PNG image
- QR encodes URL: http://localhost:8080/register?branch={branchId}
- PermitAll (no auth needed to scan)

Step 3. Serve registration HTML page
- GET /register?branch={branchId} → returns simple HTML page with form
- Fields: name, phone, email, address, date of birth
- Form submits via JavaScript fetch to POST /api/customers/register
- PermitAll

Step 4. Public registration API endpoint
- POST /api/customers/register → creates customer record
- PermitAll in SecurityConfig (no JWT needed)
- Validates unique phone and email (reuse existing logic in CustomerService)
- Auto-assigns branch from query param

Step 5. Update SecurityConfig
- PermitAll for:
  GET /api/qr/branch/**
  GET /register
  POST /api/customers/register

New files to create:
- service/QrCodeService.java (QR generation logic using ZXing)
- controller/QrCodeController.java (GET /api/qr/branch/{branchId})
- resources/templates/register.html (registration form page)
- dto/request/CustomerRegisterRequest.java (name, phone, email, address, dateOfBirth)

Modified files:
- SecurityConfig.java (add permitAll rules)
- CustomerController.java (add public registration endpoint)
- pom.xml (add ZXing dependency)

BACKLOG (FUTURE WORK AFTER SECURITY)
Task 2. Business logic
- Placing an Order decrements Inventory atomically
- Prevent double booking of Appointments
- Order status lifecycle transitions

Task 3. Pagination, Filtering and Search (NOW IN PROGRESS)

Goal
  Add pageable list endpoints for Customers, Orders and Products. Each returns a
  Spring Page object with the content list plus page metadata (total count, page
  number, page size). Replace the old endpoints that returned the full list.

Where the work happens
  Filtering and search are done on the backend, not in the frontend. The
  frontend sends query parameters. The backend runs them as database queries and
  returns only the matching page. The frontend only renders the result.

Filtering versus searching
  Filtering means exact or range checks on known fields (for example status
  equals a value, or a date is between two dates).
  Searching means free text matching across text columns (for example name
  contains a word).
  Both are built the same way on the backend but they are different kinds of
  checks.

Filters for each resource (only fields that exist in the data model)

Customers
  - search by name, phone, email (text match, not case sensitive)
  - filter by created date range (from date, to date)

Orders
  - filter by status (for example PENDING_REVIEW, CONFIRMED, IN_PROGRESS,
    READY_FOR_PICKUP, COMPLETED)
  - filter by customer id
  - filter by order date range (from date, to date)

Products
  - filter by category
  - search by brand, model
  - free text search across sku, model, brand

Pagination
  - page number, page size and sort field are sent by the frontend
  - backend returns one page at a time

Branch filtering
  - Removed. There is no Branch entity, so do not add branch filters.

Why this is needed
  - The frontend plan requires search and filter on all tables.
  - Staff need to find products and filter orders by status during daily work.
  - Returning the full list breaks the UI tables and the dashboard recent orders widget.

When to build
  - Now, before the frontend CRUD and polish phases, so the API contract is stable.

Frontend contract note
  - The frontend types currently show list endpoints returning a plain array.
  - After this change the response becomes a Page object. Update the frontend
    types to wrap the list when that work starts.

Implementation notes (backend only, no code written yet)
  - Repositories extend JpaSpecificationExecutor.
  - Services build a Specification from the optional filters and call findAll
    with a Pageable, then map results to the response DTO.
  - Controllers accept the filter query params and a Pageable, and return a Page.
  - Keep get by id and create, update, delete endpoints unchanged.
  - Remove the old order filter branching that used customerId and status.
  - Verify with ./mvnw compile -q after changes.

Task 4. Tests
- Unit tests for services
- MockMvc tests for controllers

Task 5. Deployment readiness
- Dockerfile and docker compose
- Spring profiles dev and prod
- Flyway database migrations instead of ddl-auto=update

