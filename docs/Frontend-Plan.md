# Glasses Management Frontend Build Plan

## Architecture Overview

There will be three separate projects in the same parent folder.

D:\Glasses-Management-Backend\Glasses-Mangement-Backend is the existing Spring Boot API running on port 8080.

D:\Glasses-Management-Backend\glasses-web is the React web frontend using Tailwind CSS and Vite.

D:\Glasses-Management-Backend\glasses-mobile is the React Native mobile app using Expo and React Native Paper.

Both frontend projects consume the same REST API endpoints at /api. Both use JWT for authentication by calling POST /api/auth/login, storing the token, and sending it in the Authorization header as Bearer token. The two frontend projects are completely separate with no shared code between them.

---

## Technology Stack

Web Frontend

- Framework: React 18 with TypeScript
- Build Tool: Vite
- UI Library: Tailwind CSS with lucide-react icons
- Routing: React Router v6
- HTTP Client: Axios
- State Management: React Context using AuthContext
- Token Storage: localStorage
- Package Manager: npm

Mobile App

- Framework: React Native with Expo and TypeScript
- Build Tool: Expo CLI
- UI Library: React Native Paper with Vector Icons
- Routing: Expo Router
- HTTP Client: Axios
- State Management: React Context using AuthContext
- Token Storage: AsyncStorage
- Package Manager: npm

---

## Phase 1: Web Frontend (glasses-web)

### Step 1 - Project Setup

Scaffold the project using npm create vite@latest glasses-web with the react-ts template.

Install the following dependencies.

- tailwindcss, postcss, autoprefixer for styling
- react-router-dom for routing
- axios for HTTP requests
- lucide-react for icons

Configure Tailwind by creating tailwind.config.js and postcss.config.js and adding the Tailwind directives to index.css.

Set up the following folder structure inside the src directory.

- api - contains the axios instance and all endpoint functions
- components - contains reusable UI components like Sidebar, DataTable, Modal
- pages - contains the route pages
- hooks - contains custom hooks like useAuth and useApi
- types - contains TypeScript interfaces matching the backend DTOs
- context - contains AuthContext for JWT state
- utils - contains helper functions like formatCurrency and formatDate

### Step 2 - Auth and Layout

Create an AuthContext that stores the JWT in localStorage and provides login, logout, and getUser methods.

Set up an Axios interceptor that attaches the Authorization Bearer token header to all requests and handles 401 errors by redirecting to the login page.

Build a Login page that calls POST /api/auth/login, stores the returned token, and redirects to the dashboard.

Create a Protected route wrapper that redirects unauthenticated users to the login page.

Build a Sidebar layout with navigation links to the following pages.

- Dashboard
- Customers
- Products
- Inventory
- Orders
- Appointments
- Prescriptions
- Branches
- Users (admin only)

### Step 3 - Dashboard

Build a Dashboard page with summary cards showing total customers, total orders, low-stock count, and upcoming appointments.

Include a recent orders table showing the last 5 orders.

Optionally include charts using recharts for order trends.

### Step 4 - CRUD Pages

All CRUD pages follow the same pattern. There is a list table, a create/edit modal, and a delete confirmation dialog.

Customers page uses the /api/customers endpoint. Key fields are name, phone, email, address, and date of birth.

Products page uses the /api/products endpoint. Key fields are SKU, model, brand, color, material, size, cost price, and sale price.

Inventory page uses the /api/inventories endpoint. Key fields are product, branch, quantity, and reorder threshold.

Orders page uses the /api/orders endpoint. Key fields are customer, branch, product, status, and payment method.

Order Items page uses the /api/order-items endpoint. Key fields are order, product, lens type, coating, and price.

Appointments page uses the /api/appointments endpoint. Key fields are customer, branch, optometrist, scheduled time, and status.

Prescriptions page uses the /api/prescriptions endpoint. Key fields are customer, OD and OS values, pupillary distance, and addition.

Branches page uses the /api/branches endpoint. Key fields are name, address, phone, and manager.

Users page uses the /api/user endpoint and is admin only. Key fields are name, email, role, and branch.

### Step 5 - Polish

Make the sidebar responsive so it collapses on mobile.

Add loading states and error toasts for all API calls.

Add search and filter functionality on all tables.

Add form validation for required fields, phone format, and email format.

---

## Phase 2: Mobile App (glasses-mobile)

### Step 1 - Project Setup

Scaffold the project using npx create-expo-app glasses-mobile with the blank-typescript template.

Install the following dependencies.

- react-native-paper for UI components
- react-native-vector-icons for icons
- @react-native-async-storage/async-storage for token storage
- axios for HTTP requests
- expo-router for navigation
- react-native-safe-area-context and react-native-screens for navigation support

Configure the Paper theme by wrapping the app in a PaperProvider component.

Set up the following folder structure inside the src directory.

- api - contains the axios instance and all endpoint functions
- components - contains reusable components like Card, List, FAB
- screens - contains screen components
- navigation - contains tab and stack navigators
- types - contains TypeScript interfaces
- context - contains AuthContext
- utils - contains helper functions

### Step 2 - Auth and Navigation

Create an AuthContext that stores the JWT in AsyncStorage.

Set up an Axios interceptor that attaches the Authorization Bearer token header and handles 401 errors by redirecting to the login screen.

Build a Login screen with Paper TextInput and Button components that calls POST /api/auth/login.

Set up bottom tab navigation with the following tabs.

- Dashboard
- Customers
- Products
- Orders
- More (includes Appointments, Inventory, Prescriptions, etc.)

### Step 3 - Dashboard

Build a Dashboard screen with summary cards using Paper Card components.

Add quick action buttons for creating a new order and booking an appointment.

### Step 4 - CRUD Screens

Each module follows the same pattern. There is a ListScreen with a FlatList and a FAB button to add new items, a DetailScreen to view item details, and a Create or Edit screen for forms.

Customers screen uses the /api/customers endpoint.

Products screen uses the /api/products endpoint.

Inventory screen uses the /api/inventories endpoint.

Orders screen uses the /api/orders endpoint.

Appointments screen uses the /api/appointments endpoint.

Prescriptions screen uses the /api/prescriptions endpoint.

### Step 5 - Polish

Add pull-to-refresh on all list screens.

Add loading skeletons or spinners during data fetching.

Add error handling with a retry option.

Add a bottom sheet for quick actions.

---

## Build Order

1. Web project setup with Tailwind and folder structure (Small effort)
2. Web authentication including login page, context, and interceptor (Small effort)
3. Web layout with sidebar and routing (Medium effort)
4. Web dashboard page (Small effort)
5. Web all CRUD pages for all 9 modules (Large effort)
6. Web polish including responsive design, validation, and error handling (Medium effort)
7. Mobile project setup with Paper and navigation (Small effort)
8. Mobile authentication including login screen and context (Small effort)
9. Mobile dashboard (Small effort)
10. Mobile all CRUD screens for 6 main modules (Large effort)
11. Mobile polish (Medium effort)

---

## API Reference

Base URL is http://localhost:8080/api

### Authentication

- POST /auth/login - Public - Login and receive a JWT token
- GET /auth/me - Requires Bearer token - Get the current authenticated user

### Users

- POST /user - Requires ADMIN role - Create a new user
- GET /user - Requires Bearer token - List all users
- GET /user/{id} - Requires Bearer token - Get a user by ID
- PUT /user/{id} - Requires ADMIN role - Update a user
- DELETE /user/{id} - Requires ADMIN role - Delete a user

### Branches

- POST /branches - Requires Bearer token - Create a new branch
- GET /branches - Requires Bearer token - List all branches
- GET /branches/{id} - Requires Bearer token - Get a branch by ID
- PUT /branches/{id} - Requires Bearer token - Update a branch
- DELETE /branches/{id} - Requires Bearer token - Delete a branch

### Customers

- POST /customers - Requires Bearer token - Create a new customer
- GET /customers - Requires Bearer token - List all customers
- GET /customers/{id} - Requires Bearer token - Get a customer by ID
- PUT /customers/{id} - Requires Bearer token - Update a customer
- DELETE /customers/{id} - Requires Bearer token - Delete a customer

### Products

- POST /products - Requires Bearer token - Create a new product
- GET /products - Requires Bearer token - List all products
- GET /products/{id} - Requires Bearer token - Get a product by ID
- PUT /products/{id} - Requires Bearer token - Update a product
- DELETE /products/{id} - Requires Bearer token - Delete a product

### Inventory

- POST /inventories - Requires Bearer token - Create an inventory record
- GET /inventories - Requires Bearer token - List all inventory
- GET /inventories/low-stock - Requires Bearer token - Get low-stock items
- GET /inventories/branch/{branchId} - Requires Bearer token - Get inventory by branch
- GET /inventories/{id} - Requires Bearer token - Get inventory by ID
- PUT /inventories/{id} - Requires Bearer token - Update inventory
- DELETE /inventories/{id} - Requires Bearer token - Delete inventory

### Appointments

- POST /appointments - Requires Bearer token - Book an appointment
- GET /appointments - Requires Bearer token - List all appointments
- GET /appointments/{id} - Requires Bearer token - Get an appointment by ID
- PUT /appointments/{id} - Requires Bearer token - Update an appointment
- DELETE /appointments/{id} - Requires Bearer token - Delete an appointment

### Prescriptions

- POST /prescriptions - Requires Bearer token - Create a prescription
- GET /prescriptions - Requires Bearer token - List all prescriptions
- GET /prescriptions/{id} - Requires Bearer token - Get a prescription by ID
- GET /prescriptions/customer/{customerId} - Requires Bearer token - Get prescriptions by customer
- GET /prescriptions/user/{userId} - Requires Bearer token - Get prescriptions by user
- PUT /prescriptions/{id} - Requires Bearer token - Update a prescription
- DELETE /prescriptions/{id} - Requires Bearer token - Delete a prescription

### Orders

- POST /orders - Requires Bearer token - Create a new order
- GET /orders - Requires Bearer token - List all orders, filterable by customerId, branchId, and status
- GET /orders/{id} - Requires Bearer token - Get an order by ID
- PUT /orders/{id} - Requires Bearer token - Update an order
- DELETE /orders/{id} - Requires Bearer token - Delete an order

### Order Items

- POST /order-items - Requires Bearer token - Create a new order item
- GET /order-items - Requires Bearer token - List all order items, filterable by orderId and productId
- GET /order-items/{id} - Requires Bearer token - Get an order item by ID
- PUT /order-items/{id} - Requires Bearer token - Update an order item
- DELETE /order-items/{id} - Requires Bearer token - Delete an order item

---

## TypeScript Types

These types are used by both the web and mobile projects.

LoginRequest - email (string), password (string)

LoginResponse - token (string), plus other fields from the JWT payload

User - uid (number), name (string), email (string), role (string: ADMIN, STAFF, or OPTOMETRIST), branch_id (number or null)

Branch - id (number), name (string), address (string), phone (string), manager_id (number)

Customer - id (number), name (string), phone (string), email (string), address (string), date_of_birth (string)

Product - id (number), sku (string), model (string), brand (string), color (string), material (string), size (string), cost_price (number), sale_price (number), supplier (string), category (string)

Inventory - id (number), product_id (number), branch_id (number), quantity (number), reorder_threshold (number), last_restock_date (string)

Appointment - id (number), customer_id (number), branch_id (number), optometrist_id (number), scheduled_time (string), status (string), notes (string)

Prescription - id (number), customer_id (number), user_id (number), od_sphere (number), od_cylinder (number), od_axis (number), os_sphere (number), os_cylinder (number), os_axis (number), near_addition (number), pupillary_distance (number)

Order - id (number), customer_id (number), branch_id (number), product_id (number), prescription_id (number or null), quantity (number), unit_price (number), total_price (number), status (string), payment_method (string), order_date (string)

OrderItem - id (number), order_id (number), product_id (number), lens_type (string), coating (string), lens_index (number), price (number)
