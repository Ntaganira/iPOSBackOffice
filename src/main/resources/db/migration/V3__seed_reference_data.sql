-- Reference data for iPOS.

INSERT INTO roles (code, name, description, system_role) VALUES
    ('ADMIN', 'Administrator', 'Full access to every module and setting.', TRUE),
    ('MANAGER', 'Manager', 'Runs the shop, approves expenses and views reports.', TRUE),
    ('CASHIER', 'Cashier', 'Sells at the till and records expenses.', TRUE),
    ('STOREKEEPER', 'Storekeeper', 'Manages products, purchases, imports and stock.', TRUE),
    ('VIEWER', 'Viewer', 'Read-only access to sales, stock and reports.', TRUE);

INSERT INTO permissions (code, name, category, sort_order) VALUES
    ('POS_USE', 'Use the point of sale', 'Sales', 10),
    ('SALE_VIEW', 'View sales', 'Sales', 20),
    ('SALE_CREATE', 'Create sales', 'Sales', 30),
    ('SALE_REFUND', 'Refund sales', 'Sales', 40),
    ('SALE_VOID', 'Void sales', 'Sales', 50),
    ('PRODUCT_VIEW', 'View products', 'Products', 100),
    ('PRODUCT_CREATE', 'Create products', 'Products', 110),
    ('PRODUCT_EDIT', 'Edit products', 'Products', 120),
    ('PRODUCT_DELETE', 'Deactivate products', 'Products', 130),
    ('CATEGORY_MANAGE', 'Manage categories', 'Products', 140),
    ('UNIT_MANAGE', 'Manage units of measure', 'Products', 150),
    ('CUSTOMER_VIEW', 'View customers', 'Customers', 200),
    ('CUSTOMER_CREATE', 'Create customers', 'Customers', 210),
    ('CUSTOMER_EDIT', 'Edit customers', 'Customers', 220),
    ('SUPPLIER_VIEW', 'View suppliers', 'Purchasing', 300),
    ('SUPPLIER_MANAGE', 'Manage suppliers', 'Purchasing', 310),
    ('PURCHASE_VIEW', 'View purchases', 'Purchasing', 320),
    ('PURCHASE_MANAGE', 'Create and edit purchases', 'Purchasing', 330),
    ('PURCHASE_RECEIVE', 'Receive purchases', 'Purchasing', 340),
    ('IMPORT_VIEW', 'View imports', 'Purchasing', 350),
    ('IMPORT_MANAGE', 'Create and edit imports', 'Purchasing', 360),
    ('IMPORT_RECEIVE', 'Receive imports', 'Purchasing', 370),
    ('EXPENSE_VIEW', 'View expenses', 'Expenses', 400),
    ('EXPENSE_CREATE', 'Create expenses', 'Expenses', 410),
    ('EXPENSE_EDIT', 'Edit expenses', 'Expenses', 420),
    ('EXPENSE_APPROVE', 'Approve or reject expenses', 'Expenses', 430),
    ('EXPENSE_VOID', 'Void expenses', 'Expenses', 440),
    ('EXPENSE_CATEGORY_MANAGE', 'Manage expense categories', 'Expenses', 450),
    ('STOCK_VIEW', 'View stock', 'Stock', 500),
    ('STOCK_ADJUST', 'Adjust stock', 'Stock', 510),
    ('STOCK_PERIOD_MANAGE', 'Open and close stock periods', 'Stock', 520),
    ('STOCK_RECEPTION', 'Record stock receptions', 'Stock', 530),
    ('REPORT_VIEW', 'View reports', 'Reports', 600),
    ('USER_VIEW', 'View users', 'Administration', 700),
    ('USER_MANAGE', 'Manage users', 'Administration', 710),
    ('ROLE_MANAGE', 'Manage roles and permissions', 'Administration', 720),
    ('SETTINGS_VIEW', 'View system settings', 'Administration', 730),
    ('SETTINGS_MANAGE', 'Change system settings', 'Administration', 740),
    ('EMAIL_SEND', 'Send documents by email', 'Administration', 750),
    ('EMAIL_LOG_VIEW', 'View the email log', 'Administration', 760),
    ('EMAIL_SETTINGS_MANAGE', 'Configure email/SMTP settings', 'Administration', 770),
    ('AUDIT_VIEW', 'View audit logs', 'Administration', 780),
    ('DEVICE_MANAGE', 'Manage terminals', 'Terminal sync', 800),
    ('SYNC_VIEW', 'View terminal sync inbox', 'Terminal sync', 810);

-- ADMIN gets everything.
INSERT INTO role_permissions (role_code, permission_code)
SELECT 'ADMIN', code FROM permissions;

-- MANAGER runs day-to-day operations but cannot change users, roles or SMTP.
INSERT INTO role_permissions (role_code, permission_code)
SELECT 'MANAGER', code FROM permissions
WHERE code NOT IN ('USER_MANAGE', 'ROLE_MANAGE', 'SETTINGS_MANAGE', 'EMAIL_SETTINGS_MANAGE');

INSERT INTO role_permissions (role_code, permission_code) VALUES
    ('CASHIER', 'POS_USE'),
    ('CASHIER', 'SALE_VIEW'),
    ('CASHIER', 'SALE_CREATE'),
    ('CASHIER', 'PRODUCT_VIEW'),
    ('CASHIER', 'CUSTOMER_VIEW'),
    ('CASHIER', 'CUSTOMER_CREATE'),
    ('CASHIER', 'CUSTOMER_EDIT'),
    ('CASHIER', 'EXPENSE_VIEW'),
    ('CASHIER', 'EXPENSE_CREATE'),
    ('CASHIER', 'STOCK_VIEW');

INSERT INTO role_permissions (role_code, permission_code) VALUES
    ('STOREKEEPER', 'PRODUCT_VIEW'),
    ('STOREKEEPER', 'PRODUCT_CREATE'),
    ('STOREKEEPER', 'PRODUCT_EDIT'),
    ('STOREKEEPER', 'CATEGORY_MANAGE'),
    ('STOREKEEPER', 'UNIT_MANAGE'),
    ('STOREKEEPER', 'SUPPLIER_VIEW'),
    ('STOREKEEPER', 'SUPPLIER_MANAGE'),
    ('STOREKEEPER', 'PURCHASE_VIEW'),
    ('STOREKEEPER', 'PURCHASE_MANAGE'),
    ('STOREKEEPER', 'PURCHASE_RECEIVE'),
    ('STOREKEEPER', 'IMPORT_VIEW'),
    ('STOREKEEPER', 'IMPORT_MANAGE'),
    ('STOREKEEPER', 'IMPORT_RECEIVE'),
    ('STOREKEEPER', 'STOCK_VIEW'),
    ('STOREKEEPER', 'STOCK_ADJUST'),
    ('STOREKEEPER', 'STOCK_PERIOD_MANAGE'),
    ('STOREKEEPER', 'STOCK_RECEPTION');

INSERT INTO role_permissions (role_code, permission_code)
SELECT 'VIEWER', code FROM permissions WHERE code LIKE '%_VIEW';

INSERT INTO tax_categories (code, name, rate, exempt, active, sort_order) VALUES
    ('A', 'Exempt', 0.00, TRUE, TRUE, 10),
    ('B', 'VAT 18%', 18.00, FALSE, TRUE, 20),
    ('C', 'Zero-rated (export)', 0.00, FALSE, TRUE, 30),
    ('D', 'Non-VAT', 0.00, FALSE, TRUE, 40);

INSERT INTO units (name, code) VALUES
    ('Piece', 'PCS'),
    ('Kilogram', 'KG'),
    ('Litre', 'L'),
    ('Box', 'BOX'),
    ('Pack', 'PACK'),
    ('Metre', 'M');

INSERT INTO categories (name) VALUES
    ('General'),
    ('Beverages'),
    ('Food'),
    ('Household'),
    ('Electronics'),
    ('Stationery');

INSERT INTO expense_categories (name) VALUES
    ('Rent'),
    ('Salaries'),
    ('Electricity'),
    ('Water'),
    ('Transport'),
    ('Airtime/Internet'),
    ('Maintenance'),
    ('Taxes/Licences'),
    ('Marketing'),
    ('Miscellaneous');

INSERT INTO shop_settings (
    id, shop_name, shop_tin, address, phone, email, currency, receipt_footer,
    default_print_format, receipt_prefix, refund_prefix, allow_negative_stock,
    auto_print, expense_approval_threshold, email_enabled, smtp_host, smtp_port,
    smtp_security, email_from_name, email_from_address,
    email_subject_template, email_body_template
) VALUES (
    1, 'iPOS Demo Shop', '123456789', 'KN 4 Ave, Kigali, Rwanda', '+250 788 000 000',
    'shop@example.rw', 'RWF', 'Thank you for your business!', 'RECEIPT', 'NS-', 'RF-',
    FALSE, TRUE, 100000.00, FALSE, 'localhost', 1025, 'NONE', 'iPOS Demo Shop',
    'no-reply@ipos.local', 'Invoice {invoiceNumber} from {shopName}',
    'Dear {customerName},

Please find attached invoice {invoiceNumber} for a total of {total}.

Thank you for shopping with {shopName}.

This document was generated by iPOS.'
);

INSERT INTO document_sequences (name, value) VALUES
    ('NS-', 0),
    ('RF-', 0),
    ('PO-', 0),
    ('IMP-', 0);
