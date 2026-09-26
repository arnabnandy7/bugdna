export {
  BugDna,
  BugDnaAssertions,
  dependencyGraph,
  FailureDependencyGraph,
  FingerprintAssert,
  generate,
  generateFromSynthetic,
  type SyntheticFailureInput,
  type SyntheticFrame,
} from './bugdna.js';
export { categorize, FailureCategory } from './category.js';
export { FailureContext, type FailureContextInput } from './context.js';
export {
  BugDiff,
  detectLayer,
  diffErrors,
  diffFingerprints,
  type DiffInput,
  type FingerprintDiff,
} from './diff.js';
export {
  detectDrift,
  FingerprintDriftDetector,
  hasSignatureDrift,
  type DriftInput,
  type FingerprintDrift,
} from './drift.js';
export {
  buildFamilyEvidence,
  classifyFamilyFromEvidence,
  FailureFamily,
} from './family.js';
export {
  createFingerprint,
  FingerprintImpl,
  type Fingerprint,
  type FingerprintData,
} from './fingerprint.js';
export {
  clearKnowledgeBaseForTesting,
  FingerprintKnowledge,
  loadKnowledgeBase,
  lookup,
  parseKnowledgeBase,
  readKnowledgeBase,
} from './knowledge.js';
export { normalize } from './normalize.js';
export { FailurePriority, prioritize } from './priority.js';
export {
  DeploymentComparison,
  DeploymentSnapshot,
  RegressionDetector,
} from './regression.js';
export {
  equalScore,
  frameSimilarity,
  methodSimilarity,
  overlap,
  parseSignature,
  simpleClassName,
  type SignatureParts,
} from './shape.js';
export {
  BugSimilarity,
  compareFingerprints,
  type FingerprintShapeInput,
  type Similarity,
} from './similarity.js';
export {
  ConsumerFailureAggregate,
  ConsumerFailureTracker,
  FailureAggregate,
  FailureBurst,
  FailureFamilyAggregate,
  FailureOccurrence,
  FailureTracker,
  SkipReasonAnalyzer,
} from './tracker.js';
