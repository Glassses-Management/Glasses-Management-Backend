Sent to staff dashboard
        │
        ▼
Staff opens pending request
        │
        ▼
Find product(s) matching request
        │
   ┌────┴────┐
   │         │
Not found   Found
   │         │
   ▼         ▼
Reject   Check Inventory
(no matching     │
 product)   ┌────┴────┐
            │         │
       Out of stock  In stock
            │         │
            ▼         ▼
         Reject   Assign product_id,
                   quantity, lens spec  s
                        │
                        ▼
                 Calculate total_price
                        │
                        ▼
                 Create Order + OrderItems
                        │
                        ▼
                 Deduct from Inventory
                        │
                        ▼
                 status: CONFIRMED
                        │
                        ▼
              Lens fitting / lab work
                        │
                        ▼
                 status: IN_PROGRESS
                        │
                        ▼
              Notify customer: ready
                        │
                        ▼
              status: READY_FOR_PICKUP
                        │
                        ▼
           Customer picks up + pays
                        │
                        ▼
              Create Payment record
                        │
                        ▼
                 status: COMPLETED
                        │
                        ▼
                Create OrderResponse
                        │
          ┌─────────────┼─────────────┐
          │             │             │
      order_id       status      total_price
          │             │             │
          └─────────────┼─────────────┘
                        ▼