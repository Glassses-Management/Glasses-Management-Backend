Submit product request
   (customer_id, product interest, notes)
            │
            ▼
   Find prescription by customer_id
            │
     ┌──────┴──────┐
     │             │
  Not found     Found
     │             │
     ▼             ▼
  Reject      Check expiry_date
  (route to                │
   exam request)     ┌─────┴─────┐
                     │           │
                   Expired      Valid
                      │           │
                      ▼           ▼
                   Reject   Attach prescription_id
                             (route to        to request
                              exam request        │
                              for renewal)        ▼
                                          status: PENDING_REVIEW
                                                    │
                                                    ▼
                                        Create RequestResponse
                                                    │
                                        ┌───────────┼───────────┐
                                        │           │           │
                                    request_id   status   prescription_id
                                        │           │           │
                                        └───────────┼───────────┘
                                                    │
                                                    ▼
                                      Sent to staff dashboard   ──► see Section 3