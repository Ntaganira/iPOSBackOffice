# iPOS design decisions

This file records assumptions and choices made while building iPOS. Tax behaviour and receipt content must be checked against current RRA EBM guidance before production use.

## Application and data

- The existing template package `com.ntaganira.heritier.ipos.backoffice` is kept instead of moving to `rw.ipos`; the prompt allows the existing package convention.
- PostgreSQL is the only database in every profile, including tests. Amounts use `NUMERIC(14,2)`, quantities use `NUMERIC(14,3)`, and Java `BigDecimal` is used throughout.
- Money is rounded half-up to two decimals at each persisted amount and tax calculation boundary.
- Selling prices are VAT-inclusive. VAT is extracted as `gross * rate / (100 + rate)`, as required by the prompt.
- Tax category codes A, B, C and D are seeded with rates 0, 18, 0 and 0 respectively. Category D means non-VAT. The seeded rates and labels must be verified against the current RRA EBM documentation and can be changed in **Settings > Tax**.
- Receipt numbers use a database-backed sequence with separate prefixes for sales and refunds (`NS-` and `RF-` by default), producing values such as `NS-000001`. The sequence continues across restarts.
- The EBM signature is a local, deterministic signature for receipt presentation. No RRA EBM device or API is called.
- Audit entries store before/after snapshots as JSON. Passwords and SMTP secrets are never written to the audit log.
- Flyway owns the schema. Hibernate runs in `validate` mode and never creates or drops tables.

## Sales, money and stock

- Sale totals are recalculated server-side from the product catalogue; browser-supplied totals are ignored.
- A sale can have multiple payments (split payment). Non-cash payments cannot exceed the amount due; cash can be tendered above the total and produces change.
- Credit sales require a customer and are rejected when they exceed that customer's available credit limit. Payments reduce the customer balance.
- Refunds are new linked sales with type `REFUND`; the original sale becomes `REFUNDED`. Stock is restored, and the original receipt can still be reprinted. Sales are never hard-deleted; voiding is represented by status.
- A refund does not record a separate refund-payment ledger. The daily cash summary attributes a refund to cash in proportion to the original sale's cash payment (the full original cash portion, capped only by its original amount).
- Receiving a purchase adds stock and writes `PURCHASE` movements, and updates the product buying price to the received unit cost. Receiving an import writes `IMPORT` movements at the calculated landed unit cost.
- Stock value on reports is quantity multiplied by the current buying price. There is no weighted-average or FIFO costing ledger.
- Stock periods store closing quantity and value snapshots. A closed period is read-only and guards against later mutation of historical valuations.
- Negative stock is blocked unless **Allow negative stock** is enabled in shop settings.

## Imports

- An import is created as a `DRAFT` with foreign-currency line costs. The landed total is the foreign goods value converted at the exchange rate, plus customs duty, import taxes, freight and other costs.
- Additional costs are allocated across lines in proportion to each line's foreign value. The landed unit cost is rounded to two decimals, and the line total is the rounded unit cost times quantity. The sum of rounded line totals is stored as the batch landed total.
- Receiving is allowed only for a draft import with at least one line. It applies all lines in one transaction, sets `RECEIVED`, and records the received timestamp.
- Draft imports can be edited or cancelled. Received or cancelled imports are immutable.

## Expenses

- The approval threshold is read from shop settings. New expenses at or below the threshold are created as `PAID`; larger expenses start as `PENDING` until a manager approves or rejects them.
- Only `APPROVED` and `PAID` expenses count in profit and cash reports. Rejected and voided expenses are excluded.
- Cash expenses include both `APPROVED` and `PAID` statuses because the prompt treats approval as spend approval. The daily cash summary labels this explicitly.
- Recurring expenses generate a new `PAID` expense on their next occurrence through a nightly scheduled job, then advance the template date. Generated entries are reviewed and voided if needed.
- Attachments are validated by size and content type and stored outside the classpath. They are served through `/files/**`; files are not guessed from user input.

## Cash drawer

- The prompt asks for a daily cash summary but not a shift module. `Reports > Daily cash drawer` calculates opening cash (entered for the day) + cash sale payments - refunds paid in cash - approved/paid cash expenses = expected cash.
- Opening cash is not persisted as a shift record; it is a report input, so a manager can reconcile any day without opening a shift.
- Cash sales are derived from `sale_payments` rows with method `CASH` on completed sales. This includes split payments' cash portion.

## Security and users

- Passwords use BCrypt strength 12. The first administrator password comes from `IPOS_ADMIN_PASSWORD`.
- Permissions are seeded per role. Controllers use method-level `@PreAuthorize` with permission codes, not hard-coded role checks.
- Access-denied responses redirect to `/403`, matching the application's error-page style.
- `ADMIN` has every permission and is the only role that can manage users, roles and system/SMTP settings. `MANAGER` handles day-to-day operations, approvals and imports but cannot change users, roles or SMTP settings. `CASHIER` is restricted to sales, customers, POS and permitted expenses; `STOREKEEPER` handles stock, products, purchases and imports; `VIEWER` is read-only.
- The role permission matrix is read-only in the UI; role grants are controlled by migrations so permission changes are reviewable and auditable.
- CSRF is enforced for every state-changing request. The POS and admin JavaScript read the CSRF meta tags from the layout.

## Files, email and reports

- Uploaded files are stored under `IPOS_UPLOAD_DIR`; `/files/**` is public so receipt images can be opened from emails or links. File names are generated by the application.
- SMTP passwords are encrypted with the application encryption key. Email logs store recipients and error messages, never credentials.
- Report CSV exports use UTF-8 with a BOM so Excel opens accents correctly.
- French and Kinyarwanda message bundles are present for navigation and the shared shell. Some module-specific page text is English-only; this is a translation gap rather than a functional restriction.

## Testing and operations

- Testcontainers is pinned to 1.21.4 because the version managed by Spring Boot 3.4.1 does not negotiate the Docker 29 API on current Docker Desktop.
- The test profile uses a disposable PostgreSQL container, Flyway migrations, a seeded demo catalogue, and MockMvc page checks. The import flow has an integration test that verifies landed cost allocation, stock increase and the `IMPORT` movement.
- Docker Compose maps PostgreSQL to host port `5437` to avoid clashing with a locally installed PostgreSQL on 5432.
- The development profile enables demo data by default; production disables it.
