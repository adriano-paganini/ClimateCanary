import { Configuration } from "../generated-skeleton-api";
import { BASE_PATH } from "../generated-skeleton-api/base";

// Global axios interceptor in config.ts already attaches the Bearer token to every request.
export const apiConfig = new Configuration({ basePath: BASE_PATH });
