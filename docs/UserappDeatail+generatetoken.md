LOGIN
  │
  │ email + password
  ▼
AppUserDetailsService
  │
  │ find user
  ▼
Database
  │
  ▼
UserDetails
  │
  │ password verification
  ▼
Authentication successful
  │
  ▼
generateToken(user)
  │
  ▼
JWT
  │
  ▼
Frontend