-- Drop the not null constraint (you can use ALTER TABLE depending on your database)
ALTER TABLE USERX ALTER COLUMN CREATE_USER_ID DROP NOT NULL;

-- Insert users into USERX table
INSERT INTO USERX (ENABLED, FIRST_NAME, LAST_NAME, PASSWORD, USERNAME, CREATE_USER_ID, CREATE_DATE)
VALUES (TRUE, 'Admin', 'Istrator', '{bcrypt}$2b$12$gimw81jnsxtcHMBRe6LVe.NZCPf3G2ugUyKqcYwJRlJuN6ubTXkNS', 'admin', NULL, '2024-01-01 00:00:00');

INSERT INTO USERX (ENABLED, FIRST_NAME, LAST_NAME, PASSWORD, USERNAME, CREATE_USER_ID, CREATE_DATE)
VALUES (TRUE, 'Susi', 'Kaufgern', '{bcrypt}$2b$12$gimw81jnsxtcHMBRe6LVe.NZCPf3G2ugUyKqcYwJRlJuN6ubTXkNS', 'user1', NULL, '2024-01-01 00:00:00');

INSERT INTO USERX (ENABLED, FIRST_NAME, LAST_NAME, PASSWORD, USERNAME, CREATE_USER_ID, CREATE_DATE)
VALUES (TRUE, 'Max', 'Mustermann', '{bcrypt}$2b$12$gimw81jnsxtcHMBRe6LVe.NZCPf3G2ugUyKqcYwJRlJuN6ubTXkNS', 'user2', NULL, '2024-01-01 00:00:00');

INSERT INTO USERX (ENABLED, FIRST_NAME, LAST_NAME, PASSWORD, USERNAME, CREATE_USER_ID, CREATE_DATE)
VALUES (TRUE, 'Elvis', 'The King', '{bcrypt}$2b$12$gimw81jnsxtcHMBRe6LVe.NZCPf3G2ugUyKqcYwJRlJuN6ubTXkNS', 'elvis', NULL, '2024-01-01 00:00:00');

-- Insert roles into USERX_USERX_ROLE table by looking up the corresponding user ID
INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'admin'), 'SYSTEM_ADMIN');

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'admin'), 'EMPLOYEE');

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'user1'), 'MANAGEMENT');

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'user1'), 'EMPLOYEE');

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'user2'), 'EMPLOYEE');

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'elvis'), 'SYSTEM_ADMIN');

INSERT INTO USERX_USERX_ROLE (USERX_ID, ROLES)
VALUES ((SELECT ID FROM USERX WHERE USERNAME = 'elvis'), 'EMPLOYEE');

-- Update CREATE_USER_ID fields after the initial insert
UPDATE USERX SET CREATE_USER_ID = (SELECT ID FROM USERX WHERE USERNAME = 'admin') WHERE USERNAME = 'admin';
UPDATE USERX SET CREATE_USER_ID = (SELECT ID FROM USERX WHERE USERNAME = 'admin') WHERE USERNAME = 'user1';
UPDATE USERX SET CREATE_USER_ID = (SELECT ID FROM USERX WHERE USERNAME = 'admin') WHERE USERNAME = 'user2';
UPDATE USERX SET CREATE_USER_ID = (SELECT ID FROM USERX WHERE USERNAME = 'elvis') WHERE USERNAME = 'elvis';

-- Add the not null constraint back
ALTER TABLE USERX ALTER COLUMN CREATE_USER_ID SET NOT NULL;

-- ---------------------------------------------------------------------------
-- Extended test data based on SWE Postman create/patch payloads
-- Inserted in dependency order and using final patched state where applicable.
-- ---------------------------------------------------------------------------

-- Address (create + patch)
INSERT INTO ADDRESSES (COUNTRY, ZIP_CODE, CITY, STREET, HOUSE_NUMBER, EXTRA)
VALUES ('Italy', '4999', 'Something', 'N/A', '81/A', 'Primary Address');

-- Building (create + patch)
INSERT INTO BUILDINGS (NAME, ADDRESS_ID)
VALUES (
           'Still OLD IT',
           (SELECT ID FROM ADDRESSES
            WHERE COUNTRY = 'Italy'
              AND ZIP_CODE = '4999'
              AND CITY = 'Something'
              AND STREET = 'N/A'
              AND HOUSE_NUMBER = '81/A'
              AND EXTRA = 'Primary Address')
       );

-- Department (create + patch), departmentLeadId/userxId = 1 -> admin
INSERT INTO DEPARTMENTS (NAME, USERX_ID)
VALUES (
           'Research (2)',
           (SELECT ID FROM USERX WHERE USERNAME = 'admin')
       );

INSERT INTO DEPARTMENTS (NAME, USERX_ID)
VALUES (
           'Sales',
           (SELECT ID FROM USERX WHERE USERNAME = 'elvis')
       );

-- Room (create only; the "Update room" request in Postman actually targets /building/1)
INSERT INTO ROOMS (ACTIVE, PRIVACY_MODE, BUILDING_ID, DEPARTMENT_ID, NAME, ROOM_TYPE)
VALUES (
           TRUE,
           1,
           (SELECT ID FROM BUILDINGS WHERE NAME = 'Still OLD IT'),
           (SELECT ID FROM DEPARTMENTS WHERE NAME = 'Research (2)'),
           'Room 1',
           'OFFICE'
       );

-- EmployeeProfile (create; patch body is empty)
INSERT INTO EMPLOYEEPROFILE (USERX_ID, DEPARTMENT_ID, ROOM_ID)
VALUES (
           (SELECT ID FROM USERX WHERE USERNAME = 'admin'),
           (SELECT ID FROM DEPARTMENTS WHERE NAME = 'Research (2)'),
           (SELECT ID FROM ROOMS WHERE NAME = 'Room 1')
       );

-- Absence (create + patch)
INSERT INTO ABSENCES (START_DATE, END_DATE, USERX_ID, ABSENCE_STATUS, ABSENCE_TYPE)
VALUES (
           TIMESTAMP '2026-04-22 19:11:35.668',
           TIMESTAMP '2026-04-22 19:11:35.668',
           (SELECT ID FROM USERX WHERE USERNAME = 'admin'),
           'PLANNED',
           'HOLIDAY'
       );

INSERT INTO ABSENCES (START_DATE, END_DATE, USERX_ID, ABSENCE_STATUS, ABSENCE_TYPE)
VALUES (
           TIMESTAMP '2026-04-10 09:00:00',
           TIMESTAMP '2026-04-12 18:00:00',
           (SELECT ID FROM USERX WHERE USERNAME = 'user1'),
           'PLANNED',
           'SICKNESS'
       );

INSERT INTO ABSENCES (START_DATE, END_DATE, USERX_ID, ABSENCE_STATUS, ABSENCE_TYPE)
VALUES (
           TIMESTAMP '2026-04-10 09:00:00',
           TIMESTAMP '2026-04-12 18:00:00',
           (SELECT ID FROM USERX WHERE USERNAME = 'elvis'),
           'PLANNED',
           'SICKNESS'
       );

-- user1 in Research (2)
INSERT INTO ABSENCES (START_DATE, END_DATE, USERX_ID, ABSENCE_STATUS, ABSENCE_TYPE)
VALUES (
           TIMESTAMP '2026-04-05 08:00:00',
           TIMESTAMP '2026-04-06 17:00:00',
           (SELECT ID FROM USERX WHERE USERNAME = 'admin'),
           'PLANNED',
           'SICKNESS'
       );

-- evlis in Management
INSERT INTO ABSENCES (START_DATE, END_DATE, USERX_ID, ABSENCE_STATUS, ABSENCE_TYPE)
VALUES (
           TIMESTAMP '2026-04-08 09:00:00',
           TIMESTAMP '2026-04-09 18:00:00',
           (SELECT ID FROM USERX WHERE USERNAME = 'elvis'),
           'PLANNED',
           'PARENTAL_LEAVE'
       );


-- ClimateHint (create + patch)
-- Metric is stored as ordinal tinyint in this table. Based on the Metric enum
-- ordering used elsewhere (IAQ, HUMIDITY, PRESSURE, TEMPERATURE), HUMIDITY = 1.
INSERT INTO CLIMATEHINTS (METRIC, HINT_TEXT)
VALUES (1, 'Use a reverse-water-sprayer');

-- Supporting second hint because the threshold payload references climateHintIds [1, 2]
INSERT INTO CLIMATEHINTS (METRIC, HINT_TEXT)
VALUES (3, 'Open windows when temperature is high');

-- Threshold (create + patch)
-- Postman uses thresholdType = MAX, but the database enum is LOWER/UPPER, so MAX -> UPPER.
INSERT INTO THRESHOLDS (ENABLED, BOUND_VALUE, ROOM_ID, METRIC, THRESHOLD_TYPE)
VALUES (
           TRUE,
           60,
           (SELECT ID FROM ROOMS WHERE NAME = 'Room 1'),
           'HUMIDITY',
           'UPPER'
       );

-- Threshold to ClimateHint links
INSERT INTO CLIMATEHINT_THRESHOLD (CLIMATEHINT_ID, THRESHOLD_ID)
VALUES (
           (SELECT ID FROM CLIMATEHINTS WHERE HINT_TEXT = 'Use a reverse-water-sprayer'),
           (SELECT ID FROM THRESHOLDS WHERE ROOM_ID = (SELECT ID FROM ROOMS WHERE NAME = 'Room 1') AND METRIC = 'HUMIDITY' AND BOUND_VALUE = 60)
       );

INSERT INTO CLIMATEHINT_THRESHOLD (CLIMATEHINT_ID, THRESHOLD_ID)
VALUES (
           (SELECT ID FROM CLIMATEHINTS WHERE HINT_TEXT = 'Open windows when temperature is high'),
           (SELECT ID FROM THRESHOLDS WHERE ROOM_ID = (SELECT ID FROM ROOMS WHERE NAME = 'Room 1') AND METRIC = 'HUMIDITY' AND BOUND_VALUE = 60)
       );

-- ThresholdViolation (create + patch)
INSERT INTO THRESHOLDVIOLATIONS (
    END_TIME,
    ROOM_ID,
    START_TIME,
    THRESHOLD_ID,
    "value",
    METRIC,
    VIOLATION_STATUS
) VALUES (
             TIMESTAMP '2026-04-13 11:00:00',
             (SELECT ID FROM ROOMS WHERE NAME = 'Room 1'),
             TIMESTAMP '2026-04-13 10:00:00',
             (SELECT ID FROM THRESHOLDS
              WHERE ROOM_ID = (SELECT ID FROM ROOMS WHERE NAME = 'Room 1')
                AND METRIC = 'HUMIDITY'
                AND BOUND_VALUE = 60),
             28,
             'TEMPERATURE',
             'RESOLVED'
         );


-- Common-areas room (same building + department as Room 1)
INSERT INTO ROOMS (ACTIVE, PRIVACY_MODE, BUILDING_ID, DEPARTMENT_ID, NAME, ROOM_TYPE)
VALUES (
           TRUE,
           FALSE,
           (SELECT ID FROM BUILDINGS WHERE NAME = 'Still OLD IT'),
           (SELECT ID FROM DEPARTMENTS WHERE NAME = 'Research (2)'),
           'Common Area 1',
           'COMMON_AREAS'
       );

-- EmployeeProfiles for user1 and user2
--    Assumes users with USERNAME = 'user1' and 'user2' exist in USERX.
INSERT INTO EMPLOYEEPROFILE (USERX_ID, DEPARTMENT_ID, ROOM_ID)
VALUES (
           (SELECT ID FROM USERX WHERE USERNAME = 'user1'),
           (SELECT ID FROM DEPARTMENTS WHERE NAME = 'Research (2)'),
           (SELECT ID FROM ROOMS WHERE NAME = 'Room 1')
       );

INSERT INTO EMPLOYEEPROFILE (USERX_ID, DEPARTMENT_ID, ROOM_ID)
VALUES (
           (SELECT ID FROM USERX WHERE USERNAME = 'user2'),
           (SELECT ID FROM DEPARTMENTS WHERE NAME = 'Research (2)'),
           (SELECT ID FROM ROOMS WHERE NAME = 'Common Area 1')
       );

INSERT INTO EMPLOYEEPROFILE (USERX_ID, DEPARTMENT_ID, ROOM_ID)
VALUES (
           (SELECT ID FROM USERX WHERE USERNAME = 'elvis'),
           (SELECT ID FROM DEPARTMENTS WHERE NAME = 'Management'),
           (SELECT ID FROM ROOMS WHERE NAME = 'Common Area 2')
       );

-- RaspberryPi + SensorStation needed as FK for Measurements
INSERT INTO RASPBERRYPIS (HOST_NAME, IP_ADDRESS, DEVICE_STATUS, ROOM_ID)
VALUES (
           'rpi-room1',
           '192.168.1.10',
           'ONLINE',
           (SELECT ID FROM ROOMS WHERE NAME = 'Room 1')
       );

INSERT INTO SENSORSTATIONS (NAME, DEVICE_STATUS, MEASUREMENTS_PER_SEC, RASPBERRY_PI_ID, ROOM_ID)
VALUES (
           'Station A',
           'ONLINE',
           1.0,
           (SELECT ID FROM RASPBERRYPIS WHERE HOST_NAME = 'rpi-room1'),
           (SELECT ID FROM ROOMS WHERE NAME = 'Room 1')
       );

-- Measurements for Room 1 (gives Room Cards actual data to display)
INSERT INTO MEASUREMENT (TIMESTAMP, MEASUREMENT, METRIC, ROOM_ID, SENSORSTATION_ID)
VALUES (
           TIMESTAMP '2026-04-23 08:00:00',
           65,
           'HUMIDITY',
           (SELECT ID FROM ROOMS WHERE NAME = 'Room 1'),
           (SELECT ID FROM SENSORSTATIONS WHERE NAME = 'Station A')
       );

INSERT INTO MEASUREMENT (TIMESTAMP, MEASUREMENT, METRIC, ROOM_ID, SENSORSTATION_ID)
VALUES (
           TIMESTAMP '2026-04-23 08:00:00',
           22,
           'TEMPERATURE',
           (SELECT ID FROM ROOMS WHERE NAME = 'Room 1'),
           (SELECT ID FROM SENSORSTATIONS WHERE NAME = 'Station A')
       );

-- Active ThresholdViolation (triggers the warning badge on Room Cards)
--    Re-uses the existing HUMIDITY threshold on Room 1.
INSERT INTO THRESHOLDVIOLATIONS (
    END_TIME,
    ROOM_ID,
    START_TIME,
    THRESHOLD_ID,
    "value",
    METRIC,
    VIOLATION_STATUS
) VALUES (
             NULL,
             (SELECT ID FROM ROOMS WHERE NAME = 'Room 1'),
             TIMESTAMP '2026-04-23 07:45:00',
             (SELECT ID FROM THRESHOLDS
              WHERE ROOM_ID = (SELECT ID FROM ROOMS WHERE NAME = 'Room 1')
                AND METRIC = 'HUMIDITY'
                AND BOUND_VALUE = 60),
             65,
             'HUMIDITY',
             'ACTIVE'
         );

INSERT INTO MEASUREMENT (TIMESTAMP, MEASUREMENT, METRIC, ROOM_ID, SENSORSTATION_ID)
VALUES (
           TIMESTAMP '2026-04-23 08:00:00',
           400,
           'IAQ',
           (SELECT ID FROM ROOMS WHERE NAME = 'Room 1'),
           (SELECT ID FROM SENSORSTATIONS WHERE NAME = 'Station A')
       );

INSERT INTO RASPBERRYPIS (HOST_NAME, IP_ADDRESS, DEVICE_STATUS, ROOM_ID)
VALUES (
           'rpi-common1',
           '192.168.1.11',
           'ONLINE',
           (SELECT ID FROM ROOMS WHERE NAME = 'Common Area 1')
       );

INSERT INTO SENSORSTATIONS (NAME, DEVICE_STATUS, MEASUREMENTS_PER_SEC, RASPBERRY_PI_ID, ROOM_ID)
VALUES (
           'Station B',
           'ONLINE',
           1.0,
           (SELECT ID FROM RASPBERRYPIS WHERE HOST_NAME = 'rpi-common1'),
           (SELECT ID FROM ROOMS WHERE NAME = 'Common Area 1')
       );

INSERT INTO MEASUREMENT (TIMESTAMP, MEASUREMENT, METRIC, ROOM_ID, SENSORSTATION_ID)
VALUES (
           TIMESTAMP '2026-04-23 08:00:00',
           21,
           'TEMPERATURE',
           (SELECT ID FROM ROOMS WHERE NAME = 'Common Area 1'),
           (SELECT ID FROM SENSORSTATIONS WHERE NAME = 'Station B')
       );