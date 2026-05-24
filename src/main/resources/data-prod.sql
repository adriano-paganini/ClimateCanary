-- noinspection SqlNoDataSourceInspectionForFile

-- =========================================
-- 1. Allow NULL temporarily for bootstrap
-- =========================================
ALTER TABLE USERX ALTER COLUMN CREATE_USER_ID DROP NOT NULL;

-- =========================================
-- 2. Insert USERS (idempotent)
-- =========================================

INSERT INTO USERX (
    ENABLED,
    FIRST_NAME,
    LAST_NAME,
    PASSWORD,
    USERNAME,
    CREATE_USER_ID,
    CREATE_DATE
)
SELECT
    TRUE,
    'Admin',
    'Istrator',
    '{bcrypt}$2b$12$gimw81jnsxtcHMBRe6LVe.NZCPf3G2ugUyKqcYwJRlJuN6ubTXkNS',
    'admin',
    NULL,
    '2024-01-01 00:00:00'
    WHERE NOT EXISTS (
    SELECT 1
    FROM USERX
    WHERE USERNAME = 'admin'
);

INSERT INTO USERX (
    ENABLED,
    FIRST_NAME,
    LAST_NAME,
    PASSWORD,
    USERNAME,
    CREATE_USER_ID,
    CREATE_DATE
)
SELECT
    TRUE,
    'Susi',
    'Kaufgern',
    '{bcrypt}$2b$12$gimw81jnsxtcHMBRe6LVe.NZCPf3G2ugUyKqcYwJRlJuN6ubTXkNS',
    'user1',
    NULL,
    '2024-01-01 00:00:00'
    WHERE NOT EXISTS (
    SELECT 1
    FROM USERX
    WHERE USERNAME = 'user1'
);

-- =========================================
-- 3. Insert USER ROLES (idempotent)
-- =========================================

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
SELECT u.ID, 'SYSTEM_ADMIN'
FROM USERX u
WHERE u.USERNAME = 'admin'
  AND NOT EXISTS (
    SELECT 1
    FROM USERX_USERX_ROLE r
    WHERE r.USERX_ID = u.ID
      AND r.ROLES = 'SYSTEM_ADMIN'
);

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
SELECT u.ID, 'MANAGEMENT'
FROM USERX u
WHERE u.USERNAME = 'user1'
  AND NOT EXISTS (
    SELECT 1
    FROM USERX_USERX_ROLE r
    WHERE r.USERX_ID = u.ID
      AND r.ROLES = 'MANAGEMENT'
);

-- =========================================
-- 4. Fix CREATE_USER_ID
-- =========================================

UPDATE USERX
SET CREATE_USER_ID = (
    SELECT ID
    FROM USERX
    WHERE USERNAME = 'admin'
)
WHERE USERNAME = 'user1';

UPDATE USERX
SET CREATE_USER_ID = (
    SELECT ID
    FROM USERX
    WHERE USERNAME = 'user1'
)
WHERE USERNAME = 'admin';

-- =========================================
-- 5. Re-enable NOT NULL constraint
-- =========================================

ALTER TABLE USERX ALTER COLUMN CREATE_USER_ID SET NOT NULL;