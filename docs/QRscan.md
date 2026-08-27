CUSTOMER (Mobile)
        │
        │ scan QR code
        ▼
   Open request form
        │
        ▼
  Has account? (login or register)
        │
   ┌────┴────┐
   │         │
  No        Yes
   │         │
   ▼         ▼
Register   Login (email + password)
   │         │
   ▼         ▼
Create user    Find user by email
   │              │
   │        ┌─────┴─────┐
   │        │           │
   │     Not found    Found
   │        │           │
   │        ▼           ▼
   │     Reject    Check password
   │                    │
   │             ┌──────┴──────┐
   │             │             │
   │          Wrong         Correct
   │             │             │
   │             ▼             ▼
   │          Reject      Generate JWT
   │                            │
   └────────────┬───────────────┘
                │
                ▼
      Customer authenticated
                │
                ▼
      Fill request form
   (exam request OR product request)
                │
                ▼
   Does customer have valid prescription?
                │
        ┌───────┴───────┐
        │               │
       No              Yes
        │               │
        ▼               ▼
  Submit exam      Submit product
    request           request
        │               │
        ▼               ▼
  status: PENDING_REVIEW
        │
        ▼
  Create RequestResponse
        │
   ┌────┴────┐
   │         │
request_id  status
   │         │
   └────┬────┘
        │
        ▼
  Sent to staff dashboard