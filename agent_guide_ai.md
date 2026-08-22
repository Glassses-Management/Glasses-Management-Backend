**# Optical Shop Management System — Business Workflow**

\> Simple business-level flowcharts showing what the system does from the

\> **\*\*customer's point of view\*\***, plus a staff/admin view and a small mapping of

\> business steps to database tables.

\>

\> Shapes: rounded = Start/End · rectangle = process · diamond = decision ·

\> parallelogram = input/output

**---**

**## 1. Main Customer Business Workflow**

\`\`\`mermaid

flowchart TD

    %% ================= STYLES =================

    classDef startEnd fill:#D6EAF8,stroke:#21618C,stroke-width:2px,color:#000

    classDef process fill:#D6EAF8,stroke:#2874A6,stroke-width:1.5px,color:#000

    classDef io fill:#EAF4FB,stroke:#2874A6,stroke-width:1.5px,color:#000

    classDef decision fill:#FFFFFF,stroke:#1F618D,stroke-width:1.5px,color:#000

    classDef warn fill:#FCF3CF,stroke:#B7950B,stroke-width:1.5px,color:#000

    %% ================== ENTRY =================

    START([START]):::startEnd --> SCAN[/Scan QR Code/]:::io

    SCAN --> ACC{Already Have\<br>an Account?}:::decision

    ACC -->|YES| LOGIN[Login]:::process

    ACC -->|NO| REG[Register]:::process

    LOGIN --> ENTER[Enter Optical Shop System]:::process

    REG --> ENTER

    %% =============== CHOOSE SERVICE ===========

    ENTER --> SVC{Choose Service}:::decision

    SVC -->|Buy Products| BROWSE[Browse Products]:::process

    SVC -->|Book Eye Exam| BOOKAPPT[Book Eye Exam]:::process

    SVC -.-> SIDE[View Prescriptions / Orders]:::process

    %% ================= SHOPPING ===============

    BROWSE --> SEL[Select Product]:::process

    SEL --> AVAIL{Product\<br>Available?}:::decision

    AVAIL -->|NO| OOS[/Out of Stock/]:::io

    OOS --> AGAIN[Choose Another Product]:::process

    AGAIN --> BROWSE

    AVAIL -->|YES| CART[Add to Cart]:::process

    CART --> QTY[/Enter Quantity/]:::io

    %% ============= PRESCRIPTION CHECK =========

    QTY --> RXQ{Prescription\<br>Required?}:::decision

    RXQ -->|NO| PLACE[Place Order]:::process

    RXQ -->|YES| RXCHK[Check Prescription]:::process

    RXCHK --> VALID{Valid\<br>Prescription?}:::decision

    VALID -->|YES| PLACE

    VALID -->|NO| BOOKAPPT

    %% ============== EYE EXAMINATION ===========

    BOOKAPPT --> BRN[Select Branch]:::process

    BRN --> OPT[Select Optometrist]:::process

    OPT --> DTTM[/Select Date and Time/]:::io

    DTTM --> CONF[Appointment Confirmed]:::process

    CONF --> EXAM[Eye Examination]:::process

    EXAM --> CRX[Create Prescription]:::process

    CRX --> SRX[Prescription Saved]:::process

    SRX --> BACK[Continue to Order]:::process

    BACK --> PLACE

    %% ============= ORDER AND PAYMENT ==========

    PLACE --> REVIEW[Review Order]:::process

    REVIEW --> PAY[/Make Payment/]:::io

    PAY --> PAYSUC{Payment\<br>Successful?}:::decision

    PAYSUC -->|NO| RETRY[Try Payment Again]:::process

    RETRY --> PAY

    PAYSUC -->|YES| ORDOK[Order Confirmed]:::process

    %% ================ INVENTORY ===============

    ORDOK --> INV[Update Inventory]:::process

    INV --> REDUCE[Reduce Product Stock]:::process

    REDUCE --> LOWQ{Stock Below\<br>Reorder Level?}:::decision

    LOWQ -->|YES| ALERT[Low Stock Alert]:::warn

    ALERT --> RESTOCK[Staff Restocks Product]:::process

    RESTOCK --> INV

    LOWQ -->|NO| PROC[Order Processing]:::process

    %% ============= ORDER COMPLETION ===========

    PROC --> PREP[Prepare Product]:::process

    PREP --> RECV[Customer Receives Product]:::process

    RECV --> DONE[Order Completed]:::process

    DONE --> ENDE([END]):::startEnd

\`\`\`

**---**

**## 2. Staff / Admin Workflow**

\`\`\`mermaid

flowchart TD

    classDef startEnd fill:#D6EAF8,stroke:#21618C,stroke-width:2px,color:#000

    classDef process fill:#D6EAF8,stroke:#2874A6,stroke-width:1.5px,color:#000

    classDef decision fill:#FFFFFF,stroke:#1F618D,stroke-width:1.5px,color:#000

    SS([START]):::startEnd --> SLOG[Staff Login]:::process --> DASH[Admin Dashboard]:::process

    DASH --> M1[Manage Customers]:::process

    DASH --> M2[Manage Products]:::process

    DASH --> M3[Manage Inventory]:::process

    DASH --> M4[Manage Appointments]:::process

    DASH --> M5[Manage Prescriptions]:::process

    DASH --> M6[Manage Orders]:::process

    DASH --> M7[Manage Branches]:::process

    DASH --> M8[Manage Users]:::process

    %% ----- Inventory detail -----

    M3 --> C1[Check Stock]:::process

    C1 --> L1{Low Stock?}:::decision

    L1 -->|YES| R1[Restock Product]:::process

    L1 -->|NO| U1[Update Stock]:::process

    R1 --> U1

    %% ---- Appointments detail ----

    M4 --> V4[View Appointment]:::process

    V4 --> A4{Action?}:::decision

    A4 -->|Confirm| U4[Update Appointment Status]:::process

    A4 -->|Cancel| U4

    A4 -->|Complete| U4

    %% ------ Orders detail -------

    M6 --> V6[View Order]:::process

    V6 --> P6[Process Order]:::process

    P6 --> ST6[Update Order Status]:::process

    ST6 --> CO6[Complete Order]:::process

    %% ---------- Finish ----------

    M1 --> SE([END]):::startEnd

    M2 --> SE

    M5 --> SE

    M7 --> SE

    M8 --> SE

    U1 --> SE

    U4 --> SE

    CO6 --> SE

\`\`\`

**---**

**## 3. Database Mapping (kept separate from the main flow)**

\`\`\`mermaid

flowchart LR

    classDef step fill:#D6EAF8,stroke:#2874A6,stroke-width:1.5px,color:#000

    classDef table fill:#FFFFFF,stroke:#117A65,stroke-width:1.5px,color:#000

    REG[Customer Registration / Login]:::step --> T1[(customers)]:::table

    PROD[Browse and Select Products]:::step --> T2[(products)]:::table

    STOCK[Stock Check]:::step --> T3[(inventory)]:::table

    APPT[Appointment Booking]:::step --> T4[(appointments)]:::table

    EXAMR[Eye Examination Result]:::step --> T5[(prescriptions)]:::table

    ORDER[Place Order]:::step --> T6[(orders)]:::table

    PAYMENT[Payment]:::step --> T6

    ITEMS[Order Line Items]:::step --> T7[(order\_items)]:::table

    STAFFB[Staff and Branch Management]:::step --> T8[(users)]:::table

    STAFFB --> T9[(branches)]:::table

\`\`\`

\> **\*\*Implementation note:\*\*** the backend currently defines these tables as

\> \`customer\` and \`branch\` (singular) in \`Customer.java\` and \`Branch.java\`.

\> The mapping above uses the database diagram naming — align one or the other

\> before implementing.

**---**

**## 4. Short Business Explanation**

This system digitizes an optical shop.

\- **\*\*Entry:\*\*** a customer scans a QR code, registers or logs in, then enters the system.

\- **\*\*Shopping:\*\*** they browse products; the system checks stock in real time — out-of-stock items loop back to browsing.

\- **\*\*Prescription:\*\*** if a product requires a prescription and none is valid on file, the customer books an appointment, gets an eye examination, a prescription is created, and they return seamlessly to checkout.

\- **\*\*Order & payment:\*\*** the customer places and reviews an order, pays, and retries on failure.

\- **\*\*Inventory:\*\*** successful payment triggers automatic inventory updates — stock is reduced, and if it drops below the reorder level, a low-stock alert prompts staff restocking.

\- **\*\*Completion:\*\*** the order is processed, prepared, handed to the customer, and completed.

On the **\*\*staff side\*\***, an admin dashboard manages customers, products, inventory,

appointments, prescriptions, orders, branches, and users — with dedicated flows for

restocking low stock, updating appointment statuses, and processing orders through

to completion.

**---**

*\*Render any Mermaid block at [mermaid.live]\(*[https://mermaid.live](https://mermaid.live)*), Notion, or GitHub.\**

what is this 