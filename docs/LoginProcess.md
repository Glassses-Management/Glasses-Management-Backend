                    User logs in
                         │
                         ▼
                 Check email/password
                         │
                         ▼
                 Login successful
                         │
                         ▼
                 generateToken(user)
                         │
             ┌───────────┴───────────┐
             ▼                       ▼
        Current time             Expiration
             │                       │
             └───────────┬───────────┘
                         ▼
                     JWT Header
                         │
                         ▼
                     JWT Claims
                  ┌──────┼──────┐
                  │      │      │
                email   uid    role
                         │
                     branch_id
                         │
                       iat
                         │
                       exp
                         ▼
                  Sign with secret
                         │
                         ▼
                      JWT Token
                         │
                         ▼
                    Send to client


                FRONTEND
                    │
                    │
                    │ email + password
                    ▼
              login(request)
                    │
                    ▼
          Find user by email
                    │
             ┌──────┴──────┐
             │             │
          Not found       Found
             │             │
             ▼             ▼
          Reject       Check password
                           │
                    ┌──────┴──────┐
                    │             │
                 Wrong          Correct
                    │             │
                    ▼             ▼
                 Reject       Generate JWT
                                  │
                                  ▼
                           Create AuthResponse
                                  │
                   ┌──────────────┼──────────────┐
                   │              │              │
                 Token         Expiration       User
                   │              │              │
                   └──────────────┼──────────────┘
                                  │
                                  ▼
                              FRONTEND