import { Fingerprint } from './fingerprint.js';
import { frameSimilarity, methodSimilarity, parseSignature } from './shape.js';

const ORIGIN_CLASS_WEIGHT = 27;
const METHOD_WEIGHT = 27;
const FRAME_WEIGHT = 46;
const CODE_PATH_CHANGE_THRESHOLD = 50;

export interface DriftInput {
  readonly id: string;
  readonly qualifiedSignature: string;
  readonly frames: readonly string[];
}

export interface FingerprintDrift<T extends DriftInput = Fingerprint> {
  readonly id: string;
  readonly oldFingerprint: T;
  readonly newFingerprint: T;
  readonly signatureDriftPercentage: number;
  readonly isPossibleCodePathChange: boolean;
  getId(): string;
  getOldFingerprint(): T;
  getNewFingerprint(): T;
  getSignatureDriftPercentage(): number;
  report(): string;
}

class FingerprintDriftImpl<T extends DriftInput> implements FingerprintDrift<T> {
  readonly id: string;
  readonly oldFingerprint: T;
  readonly newFingerprint: T;
  readonly signatureDriftPercentage: number;
  readonly isPossibleCodePathChange: boolean;

  constructor(oldFingerprint: T, newFingerprint: T, signatureDriftPercentage: number) {
    if (!oldFingerprint) throw new Error('oldFingerprint must not be null');
    if (!newFingerprint) throw new Error('newFingerprint must not be null');
    if (oldFingerprint.id !== newFingerprint.id) {
      throw new Error('drift requires matching fingerprint ids');
    }
    if (signatureDriftPercentage < 0 || signatureDriftPercentage > 100) {
      throw new Error('signatureDriftPercentage must be between 0 and 100');
    }
    this.id = oldFingerprint.id;
    this.oldFingerprint = oldFingerprint;
    this.newFingerprint = newFingerprint;
    this.signatureDriftPercentage = signatureDriftPercentage;
    this.isPossibleCodePathChange = signatureDriftPercentage >= CODE_PATH_CHANGE_THRESHOLD;
    Object.freeze(this);
  }

  getId(): string {
    return this.id;
  }

  getOldFingerprint(): T {
    return this.oldFingerprint;
  }

  getNewFingerprint(): T {
    return this.newFingerprint;
  }

  getSignatureDriftPercentage(): number {
    return this.signatureDriftPercentage;
  }

  report(): string {
    const nl = '\n';
    const message = this.isPossibleCodePathChange
      ? 'Possible code path change detected'
      : 'Minor signature shape change detected';
    return `${this.id}${nl}Signature Drift: ${this.signatureDriftPercentage}%${nl}${message}`;
  }
}

export function detectDrift<T extends DriftInput>(
  oldFingerprint: T,
  newFingerprint: T
): FingerprintDrift<T> {
  if (!oldFingerprint) throw new Error('oldFingerprint must not be null');
  if (!newFingerprint) throw new Error('newFingerprint must not be null');
  if (oldFingerprint.id !== newFingerprint.id) {
    throw new Error('fingerprint IDs must match');
  }

  const oldSignature = parseSignature(oldFingerprint.qualifiedSignature);
  const newSignature = parseSignature(newFingerprint.qualifiedSignature);

  const classScore =
    oldSignature.className === newSignature.className ? ORIGIN_CLASS_WEIGHT : 0;
  const methodScore = Math.round(
    METHOD_WEIGHT * methodSimilarity(oldSignature.methodName, newSignature.methodName)
  );
  const frameScore = Math.round(
    FRAME_WEIGHT * frameSimilarity(oldFingerprint.frames, newFingerprint.frames)
  );
  const similarity = classScore + methodScore + frameScore;

  return new FingerprintDriftImpl(oldFingerprint, newFingerprint, 100 - similarity);
}

export function hasSignatureDrift<T extends DriftInput>(
  oldFingerprint: T,
  newFingerprint: T
): boolean {
  return detectDrift(oldFingerprint, newFingerprint).signatureDriftPercentage > 0;
}

export const FingerprintDriftDetector = {
  detect: detectDrift,
  hasSignatureDrift,
} as const;
