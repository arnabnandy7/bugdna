import { FailureCategory } from './category.js';
import { FailureFamily } from './family.js';
import { FailurePriority } from './priority.js';
import { simpleClassName } from './shape.js';

export interface FingerprintData {
  readonly id: string;
  readonly rootCause: string;
  readonly signature: string;
  readonly qualifiedSignature: string;
  readonly frames: readonly string[];
  readonly failureChain: readonly string[];
  readonly causeChain: readonly string[];
  readonly explanation: string;
  readonly stabilityScore: number;
  readonly priority: FailurePriority;
  readonly category: FailureCategory;
  readonly family: FailureFamily;
}

export interface Fingerprint extends FingerprintData {
  getId(): string;
  getRootCause(): string;
  getSignature(): string;
  getQualifiedSignature(): string;
  getFrames(): readonly string[];
  getFailureChain(): readonly string[];
  getCauseChain(): readonly string[];
  getExplanation(): string;
  getStabilityScore(): number;
  getPriority(): FailurePriority;
  getCategory(): FailureCategory;
  getFamily(): FailureFamily;
  explain(): string;
  equals(other: unknown): boolean;
  toJSON(): Record<string, unknown>;
}

export class FingerprintImpl implements Fingerprint {
  readonly id: string;
  readonly rootCause: string;
  readonly signature: string;
  readonly qualifiedSignature: string;
  readonly frames: readonly string[];
  readonly failureChain: readonly string[];
  readonly causeChain: readonly string[];
  readonly explanation: string;
  readonly stabilityScore: number;
  readonly priority: FailurePriority;
  readonly category: FailureCategory;
  readonly family: FailureFamily;

  constructor(data: FingerprintData) {
    if (!data.id) throw new Error('id must not be null');
    if (!data.rootCause) throw new Error('rootCause must not be null');
    if (data.signature === null || data.signature === undefined) {
      throw new Error('signature must not be null');
    }
    if (data.qualifiedSignature === null || data.qualifiedSignature === undefined) {
      throw new Error('qualifiedSignature must not be null');
    }
    if (!data.frames) throw new Error('frames must not be null');
    if (!data.failureChain) throw new Error('failureChain must not be null');
    if (!data.causeChain) throw new Error('causeChain must not be null');
    if (data.explanation === null || data.explanation === undefined) {
      throw new Error('explanation must not be null');
    }
    if (data.stabilityScore < 0 || data.stabilityScore > 100) {
      throw new Error('stabilityScore must be between 0 and 100');
    }
    if (!data.priority) throw new Error('priority must not be null');
    if (!data.category) throw new Error('category must not be null');
    if (!data.family) throw new Error('family must not be null');

    this.id = data.id;
    this.rootCause = data.rootCause;
    this.signature = data.signature;
    this.qualifiedSignature = data.qualifiedSignature;
    this.frames = Object.freeze([...data.frames]);
    this.failureChain = Object.freeze([...data.failureChain]);
    this.causeChain = Object.freeze([...data.causeChain]);
    this.explanation = data.explanation;
    this.stabilityScore = data.stabilityScore;
    this.priority = data.priority;
    this.category = data.category;
    this.family = data.family;
    Object.freeze(this);
  }

  getId(): string {
    return this.id;
  }

  getRootCause(): string {
    return this.rootCause;
  }

  getSignature(): string {
    return this.signature;
  }

  getQualifiedSignature(): string {
    return this.qualifiedSignature;
  }

  getFrames(): readonly string[] {
    return this.frames;
  }

  getFailureChain(): readonly string[] {
    return this.failureChain;
  }

  getCauseChain(): readonly string[] {
    return this.causeChain;
  }

  getExplanation(): string {
    return this.explanation;
  }

  getStabilityScore(): number {
    return this.stabilityScore;
  }

  getPriority(): FailurePriority {
    return this.priority;
  }

  getCategory(): FailureCategory {
    return this.category;
  }

  getFamily(): FailureFamily {
    return this.family;
  }

  explain(): string {
    const nl = '\n';
    return (
      this.id +
      nl +
      nl +
      'Root Cause:' +
      nl +
      simpleClassName(this.rootCause) +
      nl +
      nl +
      'Origin:' +
      nl +
      this.signature +
      nl +
      nl +
      'Confidence:' +
      nl +
      this.stabilityScore +
      '%' +
      nl +
      nl +
      'Failure Chain:' +
      nl +
      this.failureChain.join(' -> ')
    );
  }

  equals(other: unknown): boolean {
    if (this === other) return true;
    if (!other || typeof other !== 'object') return false;
    return 'id' in other && (other as { id: unknown }).id === this.id;
  }

  toJSON(): Record<string, unknown> {
    return {
      id: this.id,
      rootCause: this.rootCause,
      signature: this.signature,
      qualifiedSignature: this.qualifiedSignature,
      frames: [...this.frames],
      failureChain: [...this.failureChain],
      causeChain: [...this.causeChain],
      stabilityScore: this.stabilityScore,
      priority: this.priority,
      category: this.category,
      family: this.family,
    };
  }

  toString(): string {
    return (
      `Fingerprint{id='${this.id}', rootCause='${this.rootCause}', ` +
      `signature='${this.signature}', stabilityScore=${this.stabilityScore}, ` +
      `priority=${this.priority}, category=${this.category}, family=${this.family}}`
    );
  }
}

export function createFingerprint(data: FingerprintData): Fingerprint {
  return new FingerprintImpl(data);
}
