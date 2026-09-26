/**
 * Impact metadata used to compute dynamic failure priority.
 */
export interface FailureContext {
  readonly occurrences: number;
  readonly affectedUsers: number;
  readonly fatal: boolean;
  hasImpactData(): boolean;
}

class FailureContextImpl implements FailureContext {
  readonly occurrences: number;
  readonly affectedUsers: number;
  readonly fatal: boolean;

  constructor(occurrences: number, affectedUsers: number, fatal: boolean) {
    this.occurrences = occurrences;
    this.affectedUsers = affectedUsers;
    this.fatal = Boolean(fatal);
    Object.freeze(this);
  }

  hasImpactData(): boolean {
    return this.occurrences >= 0 && this.affectedUsers >= 0;
  }
}

const UNKNOWN_CONTEXT = new FailureContextImpl(-1, -1, false);

export interface FailureContextInput {
  occurrences?: number;
  affectedUsers?: number;
  fatal?: boolean;
}

export const FailureContext = {
  unknown(): FailureContext {
    return UNKNOWN_CONTEXT;
  },

  of(occurrences: number, affectedUsers: number, fatal = false): FailureContext {
    if (occurrences < 0) {
      throw new Error('occurrences must not be negative');
    }
    if (affectedUsers < 0) {
      throw new Error('affectedUsers must not be negative');
    }
    return new FailureContextImpl(occurrences, affectedUsers, fatal);
  },

  from(input?: FailureContext | FailureContextInput | null): FailureContext {
    if (!input) {
      return UNKNOWN_CONTEXT;
    }
    if (typeof (input as FailureContext).hasImpactData === 'function') {
      return input as FailureContext;
    }
    const occurrences = input.occurrences ?? -1;
    const affectedUsers = input.affectedUsers ?? -1;
    const fatal = Boolean(input.fatal);
    return new FailureContextImpl(occurrences, affectedUsers, fatal);
  },
} as const;
