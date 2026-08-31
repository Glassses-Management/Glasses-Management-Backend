Plan: Enable login/register to QR code to submit request flow
Status: Done (implemented). Verified with ./mvnw compile -q and ./mvnw test.

Context

This repo is a backend-only Spring Boot 4 / Java 21 REST API. There is no frontend code. The user complaint "login/register is not let move on to the qr code" has two root causes:

1. Registration returns no JWT. The endpoint POST /api/customers/register returns only a CustomerResponse, so a newly-registered customer has no auth token and cannot call any authenticated endpoint (the QR/request step).
2. The QR step and request submission do not exist yet. There is no QR generation endpoint, no request-form page, and no customer request endpoint.

Decisions (confirmed with user):
- Frontend will be built in this repo as a single self-contained HTML page served by the backend, no separate framework.
- After auth, the user scans a QR code to submit a glasses request.
- The QR encodes a generic request-form URL. Branch was removed, so no branch/store id is encoded.
- Submitting a request creates a new Order with status PENDING_REVIEW via a new POST /api/requests endpoint.

Implementation steps

Step 1. Fix register to return a JWT (the actual blocker)
- File service/CustomerService.java, method registerCustomer (around line 52): inject JwtService, generate a token for the saved User, and return AuthResponse instead of CustomerResponse.
- File controller/CustomerController.java, method register (around line 43): change return type to AuthResponse.
- Build UserResponse from the saved User, reusing the existing AuthResponse and UserResponse types. This mirrors AuthService.login so a registered customer is immediately authenticated and can proceed to the QR/request step.

Step 2. QR code generation backend
- File pom.xml: add com.google.zxing:core and com.google.zxing:javase, for example version 3.5.3.
- New file service/QrCodeService.java: method byte[] generateQrPng(String text, int size) using QRCodeWriter and MatrixToImageWriter to produce PNG bytes.
- New file controller/QrCodeController.java: GET /api/qr/request returns image/png. It encodes a generic URL made of app.base-url plus /request-form. Add app.base-url to application.properties with default http://localhost:8080.
- File security/SecurityConfig.java (around line 75): permit GET /api/qr/**.

Step 3. Request-form HTML page (built in this repo)
- New file src/main/resources/static/request-form.html: a vanilla-JS page with no framework that does the following:
  1. Reads token from localStorage. If absent, shows a Login form (POST /api/auth/login) and a Register form (POST /api/customers/register).
  2. On success, register now returns a token, so store it and reveal the glasses request form.
  3. The request form has type (exam/product), notes, and optional prescription id, then submits to POST /api/requests with Authorization Bearer and shows a confirmation.
- New file controller/PageController.java: GetMapping /request-form returns the HTML with MediaType.TEXT_HTML using ClassPathResource. Permit it in SecurityConfig.

Step 4. Customer request submission endpoint
- New file controller/RequestController.java: POST /api/requests, authenticated.
  - Resolve the current Customer from the JWT uid claim using AuthenticationPrincipal Jwt.
  - Create an Order with status PENDING_REVIEW, the customer, order_date set to today, created_at set to now, and optional prescription from a new CustomerRequestRequest DTO.
  - Return OrderResponse.
- New file dto/request/CustomerRequestRequest.java: fields type, notes, prescriptionId.
- Service layer: add OrderService.submitCustomerRequest(...) (or a small RequestService) that builds and saves the Order.

Step 5. SecurityConfig updates
Permit GET /api/qr/**, GET /request-form, and static resources if needed. Keep /api/customers/register and /api/auth/login public.

Step 6. Verification
- Run ./mvnw compile -q
- Start the app and register a customer, confirm the response now includes access_token.
- Call GET /api/qr/request and confirm it returns a scannable PNG.
- Open /request-form, register or login, submit, and confirm POST /api/requests creates an Order with PENDING_REVIEW.

Open questions / options (resolved during build)
- /api/requests is restricted to hasRole CUSTOMER only (the QR flow is customer-facing).
- The request form has type (exam/product) + notes + optional prescription ID; type is validated server-side to "exam" or "product".
- request_type (VARCHAR 20) and notes (TEXT) were added to the orders table via Flyway V5 so the submitted request reaches the staff dashboard.
