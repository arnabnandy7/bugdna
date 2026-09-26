import { generate } from './bugdna.js';
import { Fingerprint } from './fingerprint.js';
import { parseSignature, simpleClassName } from './shape.js';

export interface FingerprintDiff {
  readonly summary: string;
  readonly oldValue: string;
  readonly newValue: string;
  readonly explanation: string;
  getSummary(): string;
  getOldValue(): string;
  getNewValue(): string;
  getExplanation(): string;
  explain(): string;
}

class FingerprintDiffImpl implements FingerprintDiff {
  readonly summary: string;
  readonly oldValue: string;
  readonly newValue: string;
  readonly explanation: string;

  constructor(summary: string, oldValue: string, newValue: string, explanation: string) {
    if (summary === null || summary === undefined) throw new Error('summary must not be null');
    if (oldValue === null || oldValue === undefined) throw new Error('oldValue must not be null');
    if (newValue === null || newValue === undefined) throw new Error('newValue must not be null');
    if (explanation === null || explanation === undefined) {
      throw new Error('explanation must not be null');
    }
    this.summary = summary;
    this.oldValue = oldValue;
    this.newValue = newValue;
    this.explanation = explanation;
    Object.freeze(this);
  }

  getSummary(): string {
    return this.summary;
  }

  getOldValue(): string {
    return this.oldValue;
  }

  getNewValue(): string {
    return this.newValue;
  }

  getExplanation(): string {
    return this.explanation;
  }

  explain(): string {
    const nl = '\n';
    return (
      this.summary +
      nl +
      nl +
      'Old:' +
      nl +
      this.oldValue +
      nl +
      nl +
      'New:' +
      nl +
      this.newValue
    );
  }

  toString(): string {
    return (
      `FingerprintDiff{summary='${this.summary}', oldValue='${this.oldValue}', ` +
      `newValue='${this.newValue}', explanation='${this.explanation}'}`
    );
  }
}

export interface DiffInput {
  readonly id: string;
  readonly rootCause: string;
  readonly qualifiedSignature: string;
  readonly signature?: string;
  readonly frames: readonly string[];
}

export function detectLayer(className: string): string {
  if (className.endsWith('Controller')) return 'Controller';
  if (className.endsWith('Service')) return 'Service';
  if (className.endsWith('Repository')) return 'Repository';
  if (className.endsWith('Gateway')) return 'Gateway';
  if (className.endsWith('Client')) return 'Client';
  if (className.endsWith('Handler')) return 'Handler';
  if (className.endsWith('Validator')) return 'Validator';
  if (className.endsWith('Configuration') || className.endsWith('Config')) {
    return 'Configuration';
  }
  if (className.endsWith('Mapper')) return 'Mapper';
  if (className.endsWith('Codec')) return 'Codec';
  return 'Application';
}

function resolveSignature(fp: Fingerprint | DiffInput): string {
  if (fp.signature) return fp.signature;
  const origin = parseSignature(fp.qualifiedSignature);
  const simple = simpleClassName(origin.className);
  return origin.methodName ? `${simple}#${origin.methodName}` : simple;
}

export function diffFingerprints(
  oldFingerprint: Fingerprint | DiffInput,
  newFingerprint: Fingerprint | DiffInput
): FingerprintDiff {
  if (!oldFingerprint) throw new Error('oldFingerprint must not be null');
  if (!newFingerprint) throw new Error('newFingerprint must not be null');

  if (oldFingerprint.id === newFingerprint.id) {
    return new FingerprintDiffImpl(
      'No Fingerprint Change',
      oldFingerprint.id,
      newFingerprint.id,
      'Both failures resolve to the same fingerprint.'
    );
  }

  const oldOrigin = parseSignature(oldFingerprint.qualifiedSignature);
  const newOrigin = parseSignature(newFingerprint.qualifiedSignature);
  const oldClassName = simpleClassName(oldOrigin.className);
  const newClassName = simpleClassName(newOrigin.className);
  const oldLayer = detectLayer(oldClassName);
  const newLayer = detectLayer(newClassName);

  if (oldClassName !== newClassName && oldLayer === newLayer) {
    return new FingerprintDiffImpl(
      `${oldLayer} Layer Changed`,
      oldClassName,
      newClassName,
      `Origin moved within the ${oldLayer} layer.`
    );
  }

  if (oldLayer !== newLayer) {
    return new FingerprintDiffImpl(
      'Layer Changed',
      oldLayer,
      newLayer,
      `Origin moved from the ${oldLayer} layer to the ${newLayer} layer.`
    );
  }

  if (oldOrigin.methodName !== newOrigin.methodName) {
    return new FingerprintDiffImpl(
      'Method Changed',
      oldOrigin.methodName,
      newOrigin.methodName,
      `Origin method changed in ${oldClassName}.`
    );
  }

  if (oldFingerprint.rootCause !== newFingerprint.rootCause) {
    return new FingerprintDiffImpl(
      'Root Cause Changed',
      simpleClassName(oldFingerprint.rootCause),
      simpleClassName(newFingerprint.rootCause),
      'Root-cause exception type changed.'
    );
  }

  return new FingerprintDiffImpl(
    'Call Path Changed',
    resolveSignature(oldFingerprint),
    resolveSignature(newFingerprint),
    'Fingerprint changed because the normalized call path changed.'
  );
}

export function diffErrors(oldError: Error, newError: Error): FingerprintDiff {
  if (!oldError) throw new Error('oldError must not be null');
  if (!newError) throw new Error('newError must not be null');
  return diffFingerprints(generate(oldError), generate(newError));
}

export const BugDiff = {
  compare(
    oldItem: Error | Fingerprint | DiffInput,
    newItem: Error | Fingerprint | DiffInput
  ): FingerprintDiff {
    if (oldItem instanceof Error && newItem instanceof Error) {
      return diffErrors(oldItem, newItem);
    }
    return diffFingerprints(
      oldItem as Fingerprint | DiffInput,
      newItem as Fingerprint | DiffInput
    );
  },
} as const;
