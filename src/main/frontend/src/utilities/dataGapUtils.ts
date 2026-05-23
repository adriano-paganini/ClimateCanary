import { MeasurementDTO } from "../generated-skeleton-api";

//true if gap of more than maxGapms within interval not covered my any meas.
export function hasDataGap(
  measurements: MeasurementDTO[],
  from: Date,
  to: Date,
  maxGapMs = 2 * 60 * 60 * 1000,
): boolean {
  if (measurements.length === 0) return true;

  const sorted = [...measurements]
    .filter((m) => m.timestamp !== undefined)
    .map((m) => new Date(m.timestamp!).getTime())
    .sort((a, b) => a - b);

  if (sorted[0] - from.getTime() > maxGapMs) return true;
  if (to.getTime() - sorted[sorted.length - 1] > maxGapMs) return true;

  for (let i = 0; i < sorted.length - 1; i++) {
    if (sorted[i + 1] - sorted[i] > maxGapMs) return true;
  }

  return false;
}

//returns index ranges where consecutive trend points are further apart than thresholdMultiplier * median interval
export function findGapRanges(
  points: { timestamp: string }[],
  thresholdMultiplier = 2,
): Array<{ startIdx: number; endIdx: number }> {
  if (points.length < 2) return [];

  const intervals = points
    .slice(0, -1)
    .map(
      (p, i) =>
        new Date(points[i + 1].timestamp).getTime() -
        new Date(p.timestamp).getTime(),
    );

  const sorted = [...intervals].sort((a, b) => a - b);
  const median = sorted[Math.floor(sorted.length / 2)];
  const threshold = median * thresholdMultiplier;

  return intervals
    .map((interval, i) =>
      interval > threshold ? { startIdx: i, endIdx: i + 1 } : null,
    )
    .filter((g): g is { startIdx: number; endIdx: number } => g !== null);
}
