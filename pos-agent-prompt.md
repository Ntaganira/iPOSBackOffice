<!--
  - Project   : iPOS - Point of Sale, Stock and Back-Office
  - Package   : project
  - File      : pos-agent-prompt.md
  - Date      : 2026. 10. 26.
  - User      : Heritier NTAGANIRA
  - Desc      : Prompt describing the iPOS agent integration.
-->

# Agent Prompt: Build iPOS, a Small-Shop POS (EBM Rwanda style)

You are a senior Java engineer working autonomously. Build **iPOS**, a complete, fully working Point of Sale (POS) and stock management web application for a small shop in Rwanda, with receipts that follow the Rwanda Revenue Authority (RRA) EBM (Electronic Billing Machine) receipt style. Work continuously until every requirement below is implemented, the app runs, and the tests pass. Do not stop to ask questions; when something is ambiguous, choose the most reasonable option, record it in `DECISIONS.md`, and keep going.

## 0. Product name and branding
- The product is called **iPOS**. Use this name consistently everywhere: Maven `artifactId` `ipos`, base package `rw.ipos` (or the existing template's package convention if it already has one), `spring.application.name=ipos`, README title, page `<title>` (e.g. "Sales | iPOS"), login page, sidebar/top bar brand, browser tab, PDF metadata, and the "Powered by iPOS" line on receipts and invoices.
- The shop's own name, logo and TIN still come from System Settings; "iPOS" is the software brand, not the shop's name.
- Create a simple text/SVG iPOS logo mark (no copyrighted artwork) for the login page and sidebar, and a favicon.
- Use `ipos` for the database name (`ipos`, and `ipos_test` for tests), the Docker Compose service/container names, and environment variable prefixes (e.g. `IPOS_DB_HOST`, `IPOS_DB_USER`, `IPOS_DB_PASSWORD`, `IPOS_ADMIN_PASSWORD`).

## 1. Tech stack (fixed)

- Java 21 (LTS), Spring Boot 3.x (latest 3.x release that supports Java 21), Maven. Set `<java.version>21</java.version>` in the `pom.xml`. Use modern Java 21 features where they make the code clearer (records for DTOs, switch expressions, pattern matching, sealed types, text blocks); do not use preview features. Virtual threads are optional and must stay disabled unless explicitly needed.
- Spring MVC + Thymeleaf (server-rendered), Spring Security, Spring Data JPA (Hibernate), Bean Validation
- PostgreSQL for every environment (dev, test and production). Do NOT use H2 or any embedded/in-memory database anywhere, not even for tests. Flyway for migrations.
- Frontend: Thymeleaf layouts and fragments, Bootstrap 5 (or the CSS of the existing template), small amounts of vanilla JS or htmx where needed
- QR code generation: ZXing. PDF/print: browser print CSS for 80mm thermal receipts, plus optional PDF via OpenPDF
- Tests: JUnit 5, Mockito, Spring Boot Test, MockMvc

## 2. Existing template

If a Spring Boot + Thymeleaf template/project already exists in the workspace, build on it. Keep its package structure, base layout, sidebar fragment, styling, and conventions. Do not rewrite it. If none exists, scaffold a clean project with a layout of: dark sidebar, top bar with the logged-in user, and a content area.

## 2a. UI/UX enhancement (user-friendly interface)
Enhance the existing template's UI so iPOS is clean, modern and easy to use for non-technical shop staff. Keep the template's identity (dark sidebar, accent colour, layout) and improve it, rather than redesigning from scratch. Before building screens, review the current layout, fragments and CSS, then apply these improvements consistently across **every** page.

**Layout and navigation**
- Collapsible sidebar with icons and labels, a clearly highlighted active item, grouped sections, and an icon-only collapsed mode that remembers the user's choice. On tablets and phones it becomes an off-canvas menu.
- Breadcrumbs and a clear page title with a short subtitle and a primary action button (e.g. "+ New Product") on every list page.
- Top bar with a global search, a quick "New Sale" shortcut, notifications (low stock, pending expenses, failed emails), and the user dropdown.
- A dashboard home with KPI cards, charts and quick-action tiles that links to the most common tasks.

**Forms and data entry**
- Logically grouped fields with clear labels, placeholders, helper text, required-field markers, and inline validation messages next to the field (not only at the top). Sensible defaults, autofocus on the first field, Enter to submit, and masks/formatting for money, phone and TIN fields.
- Searchable dropdowns (type-ahead) for products, customers, suppliers and categories instead of long plain selects.
- Prevent mistakes: confirm destructive actions in a modal with clear wording, disable buttons while submitting, warn about unsaved changes, and offer Undo where practical.

**Lists and tables**
- Consistent data tables with search, filters, sortable columns, pagination, page-size selector, sticky header, row hover, and responsive behaviour (horizontal scroll or card view on small screens).
- Status shown as coloured badges (e.g. PAID, PENDING, LOW STOCK, VOID), row action menus (View, Edit, Print, Email), bulk actions where useful, and CSV/PDF export buttons.
- Helpful empty states (icon, short explanation, and a button to create the first record) and skeleton/loading states instead of blank screens.

**Feedback and polish**
- Toast notifications for success, warning and error; spinners on slow actions; clear, human-readable error messages (never stack traces); friendly 403/404/500 pages with a way back.
- Consistent design system: one spacing scale, a defined colour palette with sufficient contrast, one icon set (Bootstrap Icons or similar), consistent buttons (primary, secondary, danger), card and modal styles, and readable typography with right-aligned, thousands-separated numbers and consistent date/currency formats.
- Light theme by default with an optional dark mode toggle that remembers the choice.
- Keyboard-friendly (visible focus, logical tab order, shortcuts on the POS screen such as F2 search, F4 checkout, Esc to close) and accessible (labels, ARIA attributes, alt text, colour not the only indicator).
- Responsive from a phone to a desktop; touch-friendly targets (minimum 44px) on the POS screen.
- Performance: keep pages light, lazy-load images, avoid layout shifts, and serve static assets with caching.
- Optional: English plus Kinyarwanda/French labels through Spring message bundles (`messages_en.properties`, `messages_rw.properties`, `messages_fr.properties`) and a language switcher, with English as the default.
- Provide first-run help: a short onboarding checklist on the dashboard for a new shop (add shop details, add products, add a user, make the first sale) and tooltips or a "?" help hint on complex fields.

**Process**
- Create one shared set of Thymeleaf fragments (layout, header, sidebar, page header, form field, table, pagination, modal, toast, empty state) and reuse them everywhere instead of copying markup.
- Keep custom CSS in a single `ipos.css` with CSS variables for the palette, so the look can be changed in one place.
- After finishing each module, review its screens on a desktop and a tablet width and fix any inconsistency before moving on.

## 3. Navigation (sidebar menu)

1. Sales Management (Invoicing)
2. Product Management
3. Customer Management
4. Purchase Management
5. Import Management
6. Expense Management
   - Expenses
   - Expense Categories
7. Stock Management
   - Opening/Closing
   - Stock Status
   - Stock In History
   - Stock Out History
   - Stock Reception
8. Settings
   - User Management (RBAC: users, roles, permissions)
   - System Settings
   - Email Log
   - Audit Logs

The top bar has a user dropdown (avatar, name, role) with: My Profile, Change Password, Logout.

Menu items must be shown or hidden based on the logged-in user's permissions.

## 4. Functional requirements

### 4.1 Sales Management (Invoicing / POS)
- **Single-screen POS, modal-driven.** The cashier must be able to complete an entire sale without leaving the POS page. No full-page navigation for adding a customer, adding a product, choosing payment, or printing. Use Bootstrap modals (or offcanvas panels) loaded via fetch/htmx, with the cart state preserved while any modal is open.
- Fast POS screen: search by name/code/barcode, add to cart, change quantity, per-line discount, remove line. Layout: product search and grid/list on the left, cart on the right, totals and a large Checkout button at the bottom of the cart. Keep keyboard focus on the search/barcode field after every action.
- **Quick Add Customer modal:** opened from a "+" next to the customer selector. Fields: name, phone, TIN (optional), email, address, type. Validates inline, saves via AJAX, and on success closes the modal and selects the new customer on the current sale. A searchable customer dropdown (type-ahead by name/phone/TIN) with "Walk-in customer" as the default.
- **Quick Add Product modal:** opened from a "+ New product" button (and offered automatically when a scanned barcode or search finds nothing). Fields: name, code, barcode (prefilled if scanned), category, unit, buying price, selling price, tax category, opening quantity. Saves via AJAX, then adds the new product straight into the cart. Requires `PRODUCT_CREATE`; hide the button otherwise.
- **Checkout modal:** opened from the Checkout button. Shows the order summary (items, discount, tax breakdown by category, total), customer, payment method selector (Cash, Mobile Money, Card, Bank, Credit) with split payment rows, amount received, change due (calculated live) and balance for credit sales, plus optional notes. Quick-cash buttons (exact, 1,000, 5,000, 10,000 ...). Validates that payments cover the total (or the credit limit is respected), disables the Confirm button while submitting to prevent double sales, and shows errors inside the modal.
- **After confirming**, the checkout modal is replaced by the print preview modal (see 4.2a) in the same page; "New Sale" closes it, clears the cart and refocuses the search field. No redirect to another page.
- Other POS actions are also modals or panels: line discount/edit, hold (park) sale and recall held sales, apply sale-level discount, cart notes, and "Recent sales" with quick reprint/refund. Confirmations use a modal, not browser `confirm()` or `alert()`.
- Modals must be closable with Esc, trap focus, work with the keyboard (Enter to confirm, Tab order), and be usable on a tablet. Use loading spinners and toast notifications for success/errors instead of page reloads.
- Prefer the same modal-based pattern for simple create/edit forms across the app (categories, units, suppliers, expense categories, roles), using a full page only for complex screens (reports, purchases, imports, stock periods).
- Payment methods: Cash, Mobile Money, Card, Bank, Credit (with amount paid and change due). Support split payment.
- On checkout: validate stock, create the sale, deduct stock, write stock-out movements, generate the receipt, all in a single transaction. The server re-validates everything (stock, prices, totals, credit limit); never trust totals sent from the browser.
- Refunds / credit notes: a refund is linked to the original sale, restores stock, and has its own receipt marked "REFUND".
- Sales list with filters (date range, cashier, customer, payment method, status), view details, reprint receipt (marked "COPY").
- Sales are never hard-deleted; use status (COMPLETED, REFUNDED, VOID) and an audit trail.

### 4.2 EBM-style receipt
Follow the RRA EBM layout as closely as possible without connecting to RRA. Each receipt shows:
- Shop name, address, phone, shop TIN (from System Settings)
- Client name and TIN (when provided)
- Receipt type: NORMAL SALE / COPY / REFUND, and a sequential receipt number per shop (`NS-000001` style, with separate counters for refunds)
- Date and time
- Item lines: name, quantity, unit price, line total, with tax category letter (A/B/C/D)
- Totals: total, total per tax category (taxable amount and tax amount), total tax, amount paid, change
- Payment method(s)
- Footer block: receipt counter and internal number, a locally generated signature/hash of the receipt data, "Powered by iPOS" text, and a QR code encoding the receipt number, date, total and shop TIN
- Tax categories and rates must be configurable in System Settings, with defaults A = exempt, B = 18% VAT, C = 0% (export), D = non-VAT. Prices are stored VAT-inclusive; compute tax as `gross * rate / (100 + rate)`. Put a note in `DECISIONS.md` that categories and rates should be verified against current RRA EBM documentation.
- Print layout: 80mm thermal paper CSS, with an auto-print option.
- Design the code so that a future `EbmService` interface (e.g. a VSDC/SDC integration) can be plugged in. Implement only a `LocalEbmService` now. Do not call any external RRA service.

### 4.2a Invoice / receipt print preview
- After checkout, and from the sales list and sale details, open a polished **print preview** (modal or full-page) that shows exactly what will be printed, before printing.
- Two formats with a toggle in the preview: **80mm thermal receipt** (EBM style, see 4.2) and **A4 invoice** (professional layout: logo, shop details and TIN, client details and TIN, invoice number and date, items table with tax category, subtotal, discounts, tax breakdown per category, total, amount paid, balance due, payment methods, notes/terms, QR code, signature/hash block, footer text). Default format is configurable in System Settings.
- Preview toolbar: Print, Download PDF, zoom in/out/fit, format toggle, copy/share link where relevant, Close, and "New Sale" after checkout. Keyboard shortcuts (Ctrl+P to print, Esc to close).
- Clear status marks that appear on the document: NORMAL SALE, COPY (reprints), REFUND, VOID, PAID / PARTIALLY PAID / UNPAID.
- Clean print CSS: `@media print` hides the app chrome, uses correct page sizes (`@page` 80mm width with auto height, and A4 with margins), avoids splitting item rows across pages, repeats table headers on multi-page invoices, and prints background colours and the QR code correctly. The preview must look identical to the printed output.
- Looks good on screen: paper-like page on a neutral background with a soft shadow, consistent typography, readable number alignment (right-aligned amounts, thousands separators), and a responsive layout on tablet.
- The same preview is used for refunds/credit notes, purchase documents, and the customer statement.
- Reprints are audit-logged and always marked "COPY".

### 4.2b Email invoices (customers and suppliers)
- Email is **configurable** in Settings > System Settings > Email: SMTP host, port, username, password, security (None / STARTTLS / SSL), "from" name and address, reply-to, optional BCC copy to the shop, default subject and body templates, and an enable/disable switch. The SMTP password is stored encrypted (never plain text, never shown again after saving, masked in audit logs). Include a **Send test email** button that reports success or the exact error in a modal.
- **Send from the print preview and the sales list:** an "Email" button opens a modal with the recipient (prefilled from the customer's email, editable), CC, subject, message (from the template with placeholders such as `{customerName}`, `{invoiceNumber}`, `{total}`, `{shopName}`, `{dueDate}`), the attachment choice (A4 invoice PDF by default, optionally the 80mm receipt PDF), and a Send button. Walk-in customers without an email can still be emailed by typing an address, with an option to save it to the customer record.
- **Suppliers:** the same mechanism sends purchase orders and purchase documents to suppliers (prefilled from the supplier's email), and statements to customers or suppliers from their statement pages.
- **Attachments:** generate the PDF server-side so it looks identical to the print preview (same A4 layout, QR code and status marks). Name files clearly (e.g. `Invoice-NS-000123.pdf`). Reprints sent by email carry the "COPY" mark.
- **Optional auto-send:** a setting to automatically email the invoice after checkout when the customer has an email address (off by default), and a "Send" checkbox in the checkout modal that overrides it for that sale.
- **Reliability:** send asynchronously (a background executor or a small outbox table with retries) so checkout is never blocked or failed by an email problem. Show a toast when sending starts, succeeds or fails.
- **Email log:** every email is recorded (date, user, recipient, subject, document type and number, status SENT / FAILED / PENDING, error message, retry count) and viewable in a searchable list with a Resend action. Permission `EMAIL_SEND` to send, `EMAIL_LOG_VIEW` to see the log, `EMAIL_SETTINGS_MANAGE` to configure SMTP. Sending and settings changes are audit-logged (before -> after, with secrets masked).
- **Validation and safety:** validate addresses, limit attachment size, rate-limit sends per user, escape all template placeholders to prevent HTML/header injection, and never expose SMTP credentials in logs or the UI.
- Use `spring-boot-starter-mail` and Thymeleaf HTML email templates (with a plain-text fallback), branded with the shop name and "Powered by iPOS". For development, add a Mailpit container to `docker-compose.yml` (SMTP on 1025, web UI on 8025) so emails can be tested without a real mail server.

### 4.3 Product Management
- Fields: code, barcode, name, category, unit of measure, buying price, selling price, tax category, reorder level, active flag, optional image.
- CRUD, search, pagination, categories management, CSV import/export of products.

### 4.4 Customer Management
- Fields: name, TIN (optional), phone, email, address, type (individual/company), credit limit, balance.
- CRUD, search, customer statement (sales and payments), credit sales tracking and payment recording.

### 4.5 Purchase Management
- Suppliers CRUD (name, TIN, phone, address).
- Purchase orders / purchase invoices with line items, supplier, date, payment status.
- Receiving a purchase increases stock and writes stock-in movements; it updates the product buying price (configurable).
- Supplier payments and balances.

### 4.6 Import Management
- Record imported goods: reference/declaration number, supplier, origin country, currency and exchange rate, customs duty, taxes and freight/other costs, landed cost allocated across the items.
- Receiving an import increases stock and writes stock-in movements at landed cost.

### 4.6a Expense Management
- **Expense categories:** CRUD (e.g. Rent, Salaries, Electricity, Water, Transport, Airtime/Internet, Maintenance, Taxes/Licences, Marketing, Miscellaneous), with active flag. Seed sensible defaults.
- **Expenses:** date, category, description, amount, payment method (Cash, Mobile Money, Bank, Card), reference/receipt number, optional payee/supplier, optional attachment (image/PDF of the receipt, max 5 MB, validated), and notes. Support an optional tax/VAT amount when the expense has a VAT invoice.
- Recurring expenses: optionally mark an expense as recurring (monthly/weekly) and generate the next entries automatically via a scheduled job, with the user able to review or cancel them.
- Approval flow (configurable in System Settings): expenses above a set amount stay PENDING until a MANAGER/ADMIN approves or rejects them with a reason. Only APPROVED and PAID expenses count in reports.
- List with filters (date range, category, payment method, status, user), search, pagination, totals for the filtered result, edit while PENDING, and void (never hard-delete) with a reason.
- Cash drawer link: cash expenses paid from the till are recorded in the daily cash summary (opening cash + cash sales - cash expenses - cash refunds = expected cash), so the cashier can reconcile at closing.
- Reports: expenses by category and by period, daily/monthly expense summary, expense vs. sales chart, and CSV export.
- Permissions: `EXPENSE_VIEW`, `EXPENSE_CREATE`, `EXPENSE_EDIT`, `EXPENSE_APPROVE`, `EXPENSE_VOID`, `EXPENSE_CATEGORY_MANAGE`. Seed them to ADMIN and MANAGER; CASHIER gets `EXPENSE_CREATE` and `EXPENSE_VIEW` (own entries only).
- All changes are audit-logged with before -> after values (see 4.8b).

### 4.7 Stock Management
- **Opening/Closing:** open a stock period with opening balances; close the period with a snapshot of closing quantities and values; closed periods are read-only.
- **Stock Status:** current quantity and value per product, low-stock and out-of-stock highlighting, filters, export.
- **Stock In History / Stock Out History:** every movement with date, product, quantity, source (purchase, import, sale, refund, adjustment, reception), reference, and user.
- **Stock Reception:** receive goods against purchases/imports or as manual receptions; supports partial receipt.
- All stock changes go through a single `StockMovement` ledger. Current stock is derived from, or reconciled with, this ledger. Negative stock is blocked unless allowed in settings.

### 4.8 Settings
- **User Management (RBAC):** users, roles, permissions. Permissions are fine-grained (`PRODUCT_VIEW`, `PRODUCT_CREATE`, `SALE_REFUND`, and so on). Roles are collections of permissions. Seed roles: ADMIN, MANAGER, CASHIER, STOREKEEPER. Passwords are BCrypt-hashed. Forced password change on first login. Account lock/disable.
- **System Settings:** shop details (name, TIN, address, phone, logo), currency, receipt footer text, tax categories and rates, receipt numbering, stock options (allow negative stock), printing options, and email/SMTP settings with templates (see 4.2b).
- **Email Log** page under Settings (see 4.2b).

### 4.8a User Profile (available to every logged-in user, no extra permission)
- **My Profile** page: view and edit full name, email, phone. Username and role are read-only.
- **Avatar:** upload, preview, replace and remove a profile picture. Accept only JPG/PNG/WEBP, max 2 MB, validate the real content type (not just the extension), resize/crop to a square thumbnail, and store it outside the web root (or in the database) under a generated filename. Show the avatar in the top bar and user lists, with an initials placeholder when none is set.
- **Change Password:** requires the current password, new password and confirmation. Enforce a password policy (min 8 characters, mix of letters and digits, must differ from the current password), BCrypt-hash it, and invalidate other active sessions after the change. Admins can also reset another user's password, which forces a change at next login.
- The profile page shows the user's last login time and their recent activity (from the audit log).
- Every profile change, avatar change and password change is audit-logged (never log the password or its hash).

### 4.8b Audit Logs (Settings > Audit Logs, permission `AUDIT_VIEW`)
- Record every create, update and delete of business and security data, with **before and after values**: for updates, store the old and new value of each changed field (e.g. `sellingPrice: 1500 -> 1800`), for creates store the new values, and for deletes/voids store the old values.
- Each entry captures: timestamp, user (id and username), role, action (CREATE, UPDATE, DELETE, VOID, REFUND, LOGIN, LOGOUT, LOGIN_FAILED, PASSWORD_CHANGE, PASSWORD_RESET, AVATAR_CHANGE, ROLE_CHANGE, STOCK_ADJUSTMENT, SETTINGS_CHANGE), entity type, entity id and a readable label, changed fields as JSON (`{field: {before, after}}`), IP address, and user agent.
- Implement it generically (for example a JPA entity listener or Hibernate interceptor / AOP on the service layer) so new entities are covered automatically. Exclude sensitive fields: passwords, hashes and tokens are masked as `***` and never stored.
- Audit entries are append-only: no edit or delete from the UI or the API.
- The audit log screen has filters (date range, user, action, entity type, entity id), search, pagination, a detail view that shows a clear before -> after table with changed fields highlighted, and CSV export.
- Each key record (product, customer, sale, user) has a "History" tab or button showing its own audit trail.

### 4.9 Reports
- Daily sales summary (by cashier, by payment method), sales by product, tax report (per category, per period), stock valuation, low-stock report, purchases report, profit report (gross profit = sales minus cost of goods; net profit = gross profit minus approved expenses), expenses report (see 4.6a).
- Dashboard: today's sales, today's expenses, number of receipts, low-stock count, top products, simple charts (Chart.js).
- Export to CSV; PDF where practical.

## 5. Non-functional requirements
- Layered architecture: controller, service, repository, domain, DTO; no business logic in controllers or templates.
- Money uses `BigDecimal` (scale 2, `RoundingMode.HALF_UP`), never `double`.
- All writes to stock and sales are transactional. Use optimistic locking on stock/product rows to prevent overselling.
- Audit fields (createdAt, createdBy, updatedAt, updatedBy) on all entities, plus the full before -> after audit log described in 4.8b.
- CSRF protection on, server-side validation with clear error messages, safe handling of file uploads.
- Flyway migrations for the full schema plus seed data (roles, permissions, admin user, default tax categories, default settings). Default admin credentials come from environment variables, never hard-coded in production profiles.
- Spring profiles: `dev` and `prod`, both using PostgreSQL, with the connection (host, port, database, username, password) read from environment variables with sensible local defaults (e.g. `localhost:5432`). Tests run against a real PostgreSQL: use Testcontainers (`postgres` image) when Docker is available, otherwise a dedicated `ipos_test` database configured through environment variables. Never add the H2 dependency.
- Provide a `docker-compose.yml` that starts a local PostgreSQL (with a volume) for development, and document in the README how to create the database and run the app without Docker.
- Responsive UI that works on a tablet and a desktop. The POS screen must be usable with a barcode scanner (keyboard wedge) and fast with keyboard input.
- Consistent error pages (403, 404, 500) and flash messages.

## 6. Suggested build order
1. Project setup, UI/UX foundation (design tokens, shared fragments, layout, sidebar, modals, toasts), security, RBAC, seed data, generic audit logging (before -> after)
2. System Settings, users, roles, user profile (avatar, password change), Audit Logs screen
3. Products, categories, customers, suppliers
4. Stock ledger and Stock Management screens
5. Purchases and Imports, with stock-in; Expense Management (categories, expenses, approval, attachments)
6. POS sales, payments, EBM-style receipt, print preview (80mm and A4), printing, and email invoices (SMTP settings, PDF, email log)
7. Refunds and credit sales
8. Reports and dashboard
9. Tests, polish, documentation

## 7. Working rules
- Work in small increments. After each module, build and run the tests, and fix failures before moving on.
- Never leave stubs, TODOs, or placeholder screens. Every menu item must lead to a working page.
- Never fake data in the UI. Everything shown comes from the database.
- Keep a `PROGRESS.md` checklist of all requirements above and tick items off as they are done and verified.
- Write unit tests for the services (tax calculation, stock movements, receipt numbering, refunds, RBAC checks) and MockMvc tests for the main flows (login, create product, make a sale, refund, purchase receive).
- Provide seed demo data (about 20 products, 3 customers, 2 suppliers) behind a `demo` profile.

## 8. Definition of done (do not finish until all are true)
- `mvn clean verify` passes with no failing tests.
- The app starts with `mvn spring-boot:run` (dev profile) against PostgreSQL, Flyway migrations apply cleanly on an empty database, and you can log in with the seeded admin. There is no H2 dependency or reference anywhere in the project.
- On the POS page, a cashier can add a new customer, add a new product, check out and print, all through modals without leaving or reloading the page; the cart survives every modal, the new customer/product is selected/added automatically, and "New Sale" resets the screen.
- With SMTP configured (Mailpit in dev), the "Send test email" button works, and an invoice can be emailed to a customer and a purchase order to a supplier as a PDF that matches the print preview; the email appears in the Email Log as SENT, a failed send shows FAILED with the error and can be resent, and checkout is never blocked by an email failure.
- The UI follows section 2a on every page: same layout and components, working collapsible sidebar, breadcrumbs, inline validation, toasts, empty states, responsive tables, and usable on both desktop and tablet widths, with no unstyled or broken pages.
- A full flow works end to end: create product, receive purchase (stock increases), make a sale (stock decreases, receipt prints with correct tax breakdown and QR code), refund the sale (stock restored, refund receipt), view the stock history and the daily report.
- A CASHIER cannot see or open Settings or purchase pages, and gets a 403 if the URL is typed directly.
- After a sale, the print preview opens, the user can switch between the 80mm receipt and the A4 invoice, print or download a PDF, and the printed output matches the preview; a reprint is marked "COPY" and audit-logged.
- An expense can be recorded with a receipt attachment, approved by a manager, and shows up in the expenses report, the net profit report and the daily cash summary; a cash expense reduces expected cash.
- Editing a product price produces an audit entry showing the old and new price, the user, and the time, and it is visible in Audit Logs and on the product's History tab.
- A user can upload an avatar, see it in the top bar, change their password (wrong current password is rejected), and both actions appear in the audit log without any password data.
- Every requirement in section 4 is ticked in `PROGRESS.md`.
- `README.md` explains setup, profiles, default login, and how to run and test; `DECISIONS.md` lists assumptions.

When finished, give a short summary: what was built, how to run it, default credentials, and any known limitations.
