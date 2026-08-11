# ClimateCanary Portfolio Summary

## Short Description

ClimateCanary is an indoor climate-monitoring system that links Arduino sensor stations, Raspberry Pi gateways, a Spring Boot backend, and a React frontend to detect threshold violations and present room data under explicit privacy constraints.

## Medium Description

ClimateCanary is a full-stack indoor environmental monitoring project built around Arduino Nano 33 BLE sensor stations, Raspberry Pi room gateways, a Spring Boot backend, and a React frontend. Sensor stations capture temperature, humidity, pressure, and air-quality data and stream it over a custom BLE protocol. The Raspberry Pi gateway authenticates devices, buffers measurements in SQLite, evaluates configurable thresholds locally, pushes warning state back to the sensor station, and synchronizes data with the backend. The web application provides role-specific dashboards for employees, department leads, management, and administrators, while occupancy-based privacy mode suppresses measurement persistence and upload when office-room conditions are not privacy-compliant.

## My Role

- Designed and implemented the Arduino control logic for the sensor station.
- Took primary responsibility for backend development and system integration.
- Integrated the embedded hardware and Raspberry Pi gateway with the web platform.
- Implemented or refined backend support for Pi configuration, threshold synchronization, violation handling, and occupancy/privacy updates.
- Contributed to the React frontend where hardware and backend workflows had to surface correctly in the UI.

## Technical Highlights

- Custom BLE onboarding flow with persistent Raspberry Pi trust binding on the Arduino.
- Raspberry Pi gateway that buffers unsent measurements in SQLite and retries backend upload.
- Gateway-side threshold evaluation with sliding windows, reminder cooldowns, and explicit resolution flow.
- Bidirectional warning propagation: backend-visible violations and on-device LCD/LED feedback.
- Occupancy-based privacy mode that suppresses persistence and upload while preserving local warning behavior.
- JWT-protected backend with role-specific dashboards in the React frontend.

## Architecture Summary

Each room has one Raspberry Pi gateway and one or more Arduino sensor stations. The station reads environmental data from a BME sensor and transmits it over BLE. The Pi authenticates the station, reconstructs timestamps, stores unsent data locally, evaluates threshold violations, and reports measurements and alerts to the Spring Boot backend over HTTP. The backend persists the system state and serves a React frontend with different views for employees, department leads, management, and administrators.

## Key Technologies

- Arduino Nano 33 BLE
- BME68x sensor stack
- Bluetooth Low Energy
- Raspberry Pi
- Python, FastAPI, `bleak`, SQLite
- Java, Spring Boot, Spring Security, JWT
- React, TypeScript, Vite
- Docker

## Interesting Engineering Challenges

- Designing a repeatable first-time setup flow for identical BLE sensor stations without manual per-device firmware changes.
- Preserving data during temporary backend outages by buffering on the Raspberry Pi and retrying uploads safely.
- Distinguishing transient threshold spikes from sustained violations before opening user-visible alerts.
- Propagating warning state back to the physical sensor station, not just into the web application.
- Enforcing occupancy-based privacy rules in the runtime data pipeline rather than only in frontend visibility logic.

## Suggested Portfolio Case-Study Structure

1. Problem and context: indoor climate monitoring with privacy constraints in office rooms.
2. System architecture: Arduino station, BLE, Raspberry Pi gateway, backend, frontend.
3. My contribution: Arduino logic, backend ownership, and integration work.
4. Engineering challenges: BLE onboarding, gateway buffering, threshold logic, privacy mode.
5. Implementation evidence: diagrams, code links, screenshots, and selected workflows.
6. Reflection: team context, trade-offs, and what I would improve next.
