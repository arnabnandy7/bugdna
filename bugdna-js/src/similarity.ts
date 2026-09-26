import { Fingerprint } from './fingerprint.js';
import {
  equalScore,
  frameSimilarity,
  methodSimilarity,
  overlap,
  parseSignature,
} from './shape.js';

const SAME_ID_SCORE = 100;
const ROOT_CAUSE_WEIGHT = 35;
const ORIGIN_CLASS_WEIGHT = 25;
const METHOD_WEIGHT = 20;
const FRAME_WEIGHT = 15;
const CAUSE_CHAIN_WEIGHT = 5;
const LIKELY_RELATED_THRESHOLD = 80;

export interface Similarity {
  readonly percentage: number;
  readonly isLikelyRelated: boolean;
  readonly explanation: string;
  getPercentage(): number;
  getExplanation(): string;
}

class SimilarityImpl implements Similarity {
  readonly percentage: number;
  readonly isLikelyRelated: boolean;
  readonly explanation: string;

  constructor(percentage: number, explanation: string) {
    if (percentage < 0 || percentage > 100) {
      throw new Error('percentage must be between 0 and 100');
    }
    if (!explanation) {
      throw new Error('explanation must not be null');
    }
    this.percentage = percentage;
    this.isLikelyRelated = percentage >= LIKELY_RELATED_THRESHOLD;
    this.explanation = explanation;
    Object.freeze(this);
  }

  getPercentage(): number {
    return this.percentage;
  }

  getExplanation(): string {
    return this.explanation;
  }
}

export interface FingerprintShapeInput {
  readonly id: string;
  readonly rootCause: string;
  readonly qualifiedSignature: string;
  readonly frames: readonly string[];
  readonly causeChain: readonly string[];
}

export function compareFingerprints(
  first: Fingerprint | FingerprintShapeInput,
  second: Fingerprint | FingerprintShapeInput
): Similarity {
  if (!first) throw new Error('first must not be null');
  if (!second) throw new Error('second must not be null');

  if (first.id === second.id) {
    return new SimilarityImpl(
      SAME_ID_SCORE,
      'Fingerprints have the same id and represent the same failure group.'
    );
  }

  const firstSignature = parseSignature(first.qualifiedSignature);
  const secondSignature = parseSignature(second.qualifiedSignature);

  const rootScore = equalScore(first.rootCause, second.rootCause, ROOT_CAUSE_WEIGHT);
  const classScore = equalScore(
    firstSignature.className,
    secondSignature.className,
    ORIGIN_CLASS_WEIGHT
  );
  const methodScore = Math.round(
    METHOD_WEIGHT * methodSimilarity(firstSignature.methodName, secondSignature.methodName)
  );
  const frameScore = Math.round(
    FRAME_WEIGHT * frameSimilarity(first.frames, second.frames)
  );
  const causeScore = Math.round(
    CAUSE_CHAIN_WEIGHT * overlap(first.causeChain, second.causeChain)
  );
  const total = rootScore + classScore + methodScore + frameScore + causeScore;

  const explanation =
    `Similarity ${total}% between ${first.id} and ${second.id} from ` +
    `rootCause=${rootScore}, originClass=${classScore}, method=${methodScore}, ` +
    `frames=${frameScore}, causeChain=${causeScore}.`;

  return new SimilarityImpl(total, explanation);
}

export const BugSimilarity = {
  compare: compareFingerprints,
} as const;
