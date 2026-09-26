from ._bugdna import (
    BugDna,
    BugDnaAssertions,
    FailureDependencyGraph,
    FingerprintAssert,
    dependency_graph,
    generate,
    generate_from_synthetic,
)
from ._category import FailureCategory, categorize
from ._context import FailureContext
from ._diff import (
    BugDiff,
    FingerprintDiff,
    detect_layer,
    diff_exceptions,
    diff_fingerprints,
)
from ._drift import (
    FingerprintDrift,
    FingerprintDriftDetector,
    detect_drift,
    has_signature_drift,
)
from ._family import (
    FailureFamily,
    build_family_evidence,
    classify_family_from_evidence,
)
from ._fingerprint import Fingerprint
from ._knowledge import (
    FingerprintKnowledge,
    clear_knowledge_base_for_testing,
    load_knowledge_base,
    lookup,
    parse_knowledge_base,
    read_knowledge_base,
)
from ._normalize import normalize
from ._priority import FailurePriority, prioritize
from ._regression import (
    DeploymentComparison,
    DeploymentSnapshot,
    RegressionDetector,
)
from ._shape import (
    SignatureParts,
    equal_score,
    frame_similarity,
    method_similarity,
    overlap,
    parse_signature,
    simple_class_name,
)
from ._similarity import (
    BugSimilarity,
    Similarity,
    compare_fingerprints,
)
from ._tracker import (
    ConsumerFailureAggregate,
    ConsumerFailureTracker,
    FailureAggregate,
    FailureBurst,
    FailureFamilyAggregate,
    FailureOccurrence,
    FailureTracker,
    SkipReasonAnalyzer,
)

__all__ = [
    "BugDiff",
    "BugDna",
    "BugDnaAssertions",
    "BugSimilarity",
    "ConsumerFailureAggregate",
    "ConsumerFailureTracker",
    "DeploymentComparison",
    "DeploymentSnapshot",
    "FailureAggregate",
    "FailureBurst",
    "FailureCategory",
    "FailureContext",
    "FailureDependencyGraph",
    "FailureFamily",
    "FailureFamilyAggregate",
    "FailureOccurrence",
    "FailurePriority",
    "FailureTracker",
    "Fingerprint",
    "FingerprintAssert",
    "FingerprintDiff",
    "FingerprintDrift",
    "FingerprintDriftDetector",
    "FingerprintKnowledge",
    "RegressionDetector",
    "SignatureParts",
    "Similarity",
    "SkipReasonAnalyzer",
    "build_family_evidence",
    "categorize",
    "classify_family_from_evidence",
    "clear_knowledge_base_for_testing",
    "compare_fingerprints",
    "dependency_graph",
    "detect_drift",
    "detect_layer",
    "diff_exceptions",
    "diff_fingerprints",
    "equal_score",
    "frame_similarity",
    "generate",
    "generate_from_synthetic",
    "has_signature_drift",
    "load_knowledge_base",
    "lookup",
    "method_similarity",
    "normalize",
    "overlap",
    "parse_knowledge_base",
    "parse_signature",
    "prioritize",
    "read_knowledge_base",
    "simple_class_name",
]
