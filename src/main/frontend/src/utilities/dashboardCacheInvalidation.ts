type CacheClearHandler = () => void;

const handlers = new Set<CacheClearHandler>();

export function registerDashboardCacheClearHandler(handler: CacheClearHandler): () => void {
    handlers.add(handler);
    return () => handlers.delete(handler);
}

export function invalidateDashboardCaches(): void {
    handlers.forEach(handler => handler());
}
