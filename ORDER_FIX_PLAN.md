# Order Logic Fix Plan — Critical Bugs #1-3

## Fix #1: Price Calculation Inconsistency + Lens Fields

**Problem**: `OrderService` ignores `len_price` in total. `OrderRequest.ItemRequest` has no lens fields.

**Files to change** (6 files):

1. **`OrderRequest.java`** — Add `len_type`, `len_coating`, `len_index`, `len_price` fields to `ItemRequest`
2. **`OrderService.java:79-121`** — In the item loop, copy lens fields to `OrderItem`, include `lenPrice` in subtotal: `subtotal = (unitPrice + lenPrice) * quantity`
3. **`OrderItem.java`** — Change `len_price`, `unit_price`, `total_price` from `Float` to `BigDecimal`
4. **`Order.java`** — Change `total` from `Float` to `BigDecimal`
5. **`OrderResponse.java`** — Change `total` to `BigDecimal`
6. **`OrderItemResponse.java`** — Change `len_price`, `unit_price`, `total_price` to `BigDecimal`

## Fix #2: Restore Inventory on Order Delete

**Problem**: `OrderService.delete()` doesn't restore inventory.

**File to change** (1 file):

7. **`OrderService.java:185-188`** — Before deleting, load all `OrderItem`s for the order, look up each `Inventory` by `product_id + branch_id`, and increment quantity back. Use `inventoryRepository.save()`.

## Fix #3: Add Inventory Check on Standalone OrderItem Create

**Problem**: `OrderItemService.create()` has no inventory validation.

**Files to change** (1 file):

8. **`OrderItemService.java`** — Inject `InventoryRepository` and `BranchRepository` (or get branch from the Order). In `buildOrderItem()`, look up inventory by `product_id + branch_id`, validate stock, decrement inventory.

## Supporting: BigDecimal Migration

**Files already listed above** +:

9. **`OrderItemRequest.java`** — Change `len_price` from `Float` to `BigDecimal`
10. **`OrderItemService.java`** — Update all price math to use `BigDecimal` arithmetic
11. **`OrderItemMapper.java`** — No changes needed (Lombok handles getter/setter mapping)

## Summary of files to modify

| # | File | Change |
|---|------|--------|
| 1 | `OrderRequest.java` | Add lens fields to `ItemRequest` |
| 2 | `OrderService.java` | Fix price formula, add lens field mapping, restore inventory on delete |
| 3 | `OrderItemService.java` | Add inventory check, inject repositories, BigDecimal math |
| 4 | `Order.java` | `Float` -> `BigDecimal` for `total` |
| 5 | `OrderItem.java` | `Float` -> `BigDecimal` for `len_price`, `unit_price`, `total_price` |
| 6 | `OrderResponse.java` | `Float` -> `BigDecimal` for `total` |
| 7 | `OrderItemResponse.java` | `Float` -> `BigDecimal` for `len_price`, `unit_price`, `total_price` |
| 8 | `OrderItemRequest.java` | `Float` -> `BigDecimal` for `len_price` |

## Note

`BigDecimal` maps to MySQL `DECIMAL` by default. If existing columns are `FLOAT`, they need an `ALTER TABLE` to change to `DECIMAL(19,2)`.
