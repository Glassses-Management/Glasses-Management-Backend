PLAN.MD
Optical Shop Management System - Backend Roadmap

STATUS: CRUD complete for all modules (Branch, User, Customer, Product,
Inventory, Appointment, Prescription, Order, OrderItem).
Current task: Security and JWT authentication.

CURRENT TASK: AUTHENTICATION AND AUTHORIZATION (IN PROGRESS)

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

BACKLOG (FUTURE WORK AFTER SECURITY)

Task 2. Business logic
- Placing an Order decrements Inventory atomically
- Prevent double booking of Appointments
- Order status lifecycle transitions

Task 3. Pagination and filtering
- Pageable list endpoints for Customers, Orders, Products
- Search filters by branch, status, date range, customer

Task 4. Tests
- Unit tests for services
- MockMvc tests for controllers

Task 5. Deployment readiness
- Dockerfile and docker compose
- Spring profiles dev and prod
- Flyway database migrations instead of ddl-auto=update

