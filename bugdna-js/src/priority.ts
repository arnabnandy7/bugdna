import { FailureContext, FailureContextInput } from './context.js';

export type FailurePriority =
  | 'UNKNOWN'
  | 'LOW'
  | 'MEDIUM'
  | 'HIGH'
  | 'CRITICAL';

export const FailurePriority = {
  UNKNOWN: 'UNKNOWN' as FailurePriority,
  LOW: 'LOW' as FailurePriority,
  MEDIUM: 'MEDIUM' as FailurePriority,
  HIGH: 'HIGH' as FailurePriority,
  CRITICAL: 'CRITICAL' as FailurePriority,
} as const;

export function prioritize(context?: FailureContext | FailureContextInput | null): FailurePriority {
  const ctx = FailureContext.from(context);
  if (!ctx.hasImpactData()) {
    return FailurePriority.UNKNOWN;
  }
  if (ctx.fatal || ctx.affectedUsers >= 100 || ctx.occurrences >= 1000) {
    return FailurePriority.CRITICAL;
  }
  if (ctx.affectedUsers >= 10 || ctx.occurrences >= 100) {
    return FailurePriority.HIGH;
  }
  if (ctx.affectedUsers > 0 || ctx.occurrences >= 10) {
    return FailurePriority.MEDIUM;
  }
  return FailurePriority.LOW;
}
