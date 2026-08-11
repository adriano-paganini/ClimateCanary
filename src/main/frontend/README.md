# ClimateCanary Frontend

React and TypeScript frontend for the ClimateCanary web application.

## Purpose

The frontend consumes the Spring Boot API and presents role-specific views for:

- employees,
- department leads,
- management,
- building administrators,
- system administrators.

It is not a standalone app. It depends on the backend for authentication, room data, measurements, violations, and administration workflows.

## Local Development

1. Copy `.env_example` or `.env.example` to `.env`.
2. Set `REACT_APP_BACKEND_SERVER_URL` to the backend URL, usually `http://localhost:8080`.
3. Install dependencies and start Vite:

```bash
npm install
npm start
```

## Structure

- `src/views/` contains route-level screens such as employee, department, and management dashboards.
- `src/components/` contains reusable UI building blocks.
- `src/services/` contains API-facing service wrappers.
- `src/generated-skeleton-api/` is generated from the backend OpenAPI specification during the Maven build.

## Notes

- `npm start` runs the Vite dev server.
- `npm run build` creates the production bundle used by the backend Docker build.
- Authentication tokens are attached through the Axios setup in `src/config/config.ts`.
