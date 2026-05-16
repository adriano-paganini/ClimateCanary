-- noinspection SqlNoDataSourceInspectionForFile

-- =========================================
-- 1. Allow NULL temporarily for bootstrap
-- =========================================
ALTER TABLE USERX ALTER COLUMN CREATE_USER_ID DROP NOT NULL;

-- =========================================
-- 2. Insert USERS (idempotent)
--    Requires UNIQUE constraint on USERNAME
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
VALUES (
           TRUE,
           'Admin',
           'Istrator',
           '{bcrypt}$2b$12$gimw81jnsxtcHMBRe6LVe.NZCPf3G2ugUyKqcYwJRlJuN6ubTXkNS',
           'admin',
           NULL,
           '2024-01-01 00:00:00'
       )
ON CONFLICT (USERNAME) DO NOTHING;

INSERT INTO USERX (
    ENABLED,
    FIRST_NAME,
    LAST_NAME,
    PASSWORD,
    USERNAME,
    CREATE_USER_ID,
    CREATE_DATE
)
VALUES (
           TRUE,
           'Susi',
           'Kaufgern',
           '{bcrypt}$2b$12$gimw81jnsxtcHMBRe6LVe.NZCPf3G2ugUyKqcYwJRlJuN6ubTXkNS',
           'user1',
           NULL,
           '2024-01-01 00:00:00'
       )
ON CONFLICT (USERNAME) DO NOTHING;

INSERT INTO USERX (
    ENABLED,
    FIRST_NAME,
    LAST_NAME,
    PASSWORD,
    USERNAME,
    CREATE_USER_ID,
    CREATE_DATE
)
VALUES (
           TRUE,
           'Max',
           'Mustermann',
           '{bcrypt}$2b$12$gimw81jnsxtcHMBRe6LVe.NZCPf3G2ugUyKqcYwJRlJuN6ubTXkNS',
           'user2',
           NULL,
           '2024-01-01 00:00:00'
       )
ON CONFLICT (USERNAME) DO NOTHING;

INSERT INTO USERX (
    ENABLED,
    FIRST_NAME,
    LAST_NAME,
    PASSWORD,
    USERNAME,
    CREATE_USER_ID,
    CREATE_DATE
)
VALUES (
           TRUE,
           'Elvis',
           'The King',
           '{bcrypt}$2b$12$gimw81jnsxtcHMBRe6LVe.NZCPf3G2ugUyKqcYwJRlJuN6ubTXkNS',
           'elvis',
           NULL,
           '2024-01-01 00:00:00'
       )
ON CONFLICT (USERNAME) DO NOTHING;

-- =========================================
-- 3. Insert USER ROLES (idempotent)
--    Requires UNIQUE(USERX_ID, ROLES)
-- =========================================

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'admin'), 'SYSTEM_ADMIN')
ON CONFLICT DO NOTHING;

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'admin'), 'EMPLOYEE')
ON CONFLICT DO NOTHING;

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'user1'), 'MANAGEMENT')
ON CONFLICT DO NOTHING;

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'user1'), 'EMPLOYEE')
ON CONFLICT DO NOTHING;

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'user2'), 'EMPLOYEE')
ON CONFLICT DO NOTHING;

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'elvis'), 'SYSTEM_ADMIN')
ON CONFLICT DO NOTHING;

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'elvis'), 'EMPLOYEE')
ON CONFLICT DO NOTHING;

-- =========================================
-- 4. Fix CREATE_USER_ID (safe update)
-- =========================================

UPDATE USERX
SET CREATE_USER_ID = (SELECT ID FROM USERX WHERE USERNAME = 'admin')
WHERE USERNAME IN ('admin', 'user1', 'user2');

UPDATE USERX
SET CREATE_USER_ID = (SELECT ID FROM USERX WHERE USERNAME = 'elvis')
WHERE USERNAME = 'elvis';

-- =========================================
-- 5. Re-enable NOT NULL constraint
-- =========================================

ALTER TABLE USERX ALTER COLUMN CREATE_USER_ID SET NOT NULL;