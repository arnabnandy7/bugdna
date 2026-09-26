import { afterEach, describe, expect, it } from 'vitest';
import {
  BugDiff,
  BugDna,
  BugDnaAssertions,
  BugSimilarity,
  ConsumerFailureTracker,
  DeploymentSnapshot,
  diffErrors,
  FailureCategory,
  FailureContext,
  FailureFamily,
  FailurePriority,
  FailureTracker,
  FingerprintDriftDetector,
  FingerprintKnowledge,
  generate,
  normalize,
  parseKnowledgeBase,
  RegressionDetector,
  SkipReasonAnalyzer,
} from '../src/index.js';

function createErrorAt(
  error: Error,
  className: string,
  methodName: string,
  line: number
): Error {
  error.stack = `${error.name}: ${error.message}\n    at ${className}.${methodName} (/app/src/${className}.ts:${line}:12)`;
  return error;
}

function createErrorWithFrames(
  error: Error,
  frames: { className: string; methodName: string; line: number }[]
): Error {
  const stackLines = frames.map(
    (f) => `    at ${f.className}.${f.methodName} (/app/src/${f.className}.ts:${f.line}:10)`
  );
  error.stack = [`${error.name}: ${error.message}`, ...stackLines].join('\n');
  return error;
}

describe('BugDNA TypeScript Native Implementation', () => {
  afterEach(() => {
    BugDna.clearKnowledgeBaseForTesting();
  });

  it('produces identical fingerprints across different line numbers and messages', () => {
    const err1 = createErrorAt(
      new TypeError('user 123 was undefined'),
      'UserService',
      'getUser',
      57
    );
    const err2 = createErrorAt(
      new TypeError('email alice@example.com was null'),
      'UserService',
      'getUser',
      92
    );

    const fp1 = generate(err1);
    const fp2 = generate(err2);

    expect(fp1.id).toBe(fp2.id);
    expect(fp1.equals(fp2)).toBe(true);
    expect(fp1.signature).toBe('UserService#getUser');
    expect(fp1.stabilityScore).toBe(90);
  });

  it('produces different fingerprints for different methods', () => {
    const err1 = createErrorAt(new Error('fail'), 'UserService', 'getUser', 10);
    const err2 = createErrorAt(new Error('fail'), 'UserService', 'saveUser', 10);

    expect(generate(err1).id).not.toBe(generate(err2).id);
  });

  it('extracts deepest cause and handles cyclic Error.cause chains', () => {
    const root = createErrorAt(
      new RangeError('out of bounds'),
      'OrderRepository',
      'findById',
      42
    );
    const wrapper = createErrorAt(
      new Error('service failure', { cause: root }),
      'OrderService',
      'getOrder',
      18
    );

    const fp = generate(wrapper);
    expect(fp.rootCause).toBe('RangeError');
    expect(fp.signature).toBe('OrderRepository#findById');
    expect(fp.causeChain).toEqual(['Error', 'RangeError']);
    expect(fp.category).toBe(FailureCategory.VALIDATION);

    // Cyclic cause chain
    const cycleA = createErrorAt(new Error('a'), 'ServiceA', 'run', 1);
    const cycleB = createErrorAt(new TypeError('b'), 'ServiceB', 'run', 2);
    (cycleA as { cause?: unknown }).cause = cycleB;
    (cycleB as { cause?: unknown }).cause = cycleA;

    const cycleFp = generate(cycleA);
    expect(cycleFp.rootCause).toBe('TypeError');
    expect(cycleFp.causeChain).toEqual(['Error', 'TypeError']);
  });

  it('handles errors without stack traces gracefully', () => {
    const err = new Error('no stack');
    err.stack = '';

    const fp = generate(err);
    expect(fp.stabilityScore).toBe(70);
    expect(fp.signature).toBe('Error');
    expect(fp.frames).toEqual(['Error']);
  });

  it('builds causal dependency graphs and formats reports', () => {
    const dbErr = createErrorAt(new Error('db'), 'AccountRepository', 'find', 30);
    const svcErr = createErrorAt(
      new Error('svc', { cause: dbErr }),
      'AccountService',
      'load',
      20
    );
    const ctrlErr = createErrorAt(
      new Error('ctrl', { cause: svcErr }),
      'AccountController',
      'handle',
      10
    );

    const graph = BugDna.dependencyGraph(ctrlErr);
    expect(graph.getDepth()).toBe(3);
    expect(graph.getRoot().signature).toBe('AccountController#handle');
    expect(graph.getDependencies().length).toBe(2);
    expect(graph.report()).toContain('└─ ');
  });

  it('normalizes PII tokens in text', () => {
    expect(
      normalize('Account 123456 for john.doe@example.com failed on order 98765')
    ).toBe('Account {NUMBER} for {EMAIL} failed on order {NUMBER}');
  });

  it('computes similarity and structural diffs between errors', () => {
    const err1 = createErrorWithFrames(new Error('db fail'), [
      { className: 'OrderRepository', methodName: 'findById', line: 10 },
      { className: 'OrderService', methodName: 'getOrder', line: 20 },
    ]);
    const err2 = createErrorWithFrames(new Error('db fail'), [
      { className: 'OrderRepository', methodName: 'findByCustomer', line: 15 },
      { className: 'OrderService', methodName: 'getOrder', line: 20 },
    ]);

    const fp1 = generate(err1);
    const fp2 = generate(err2);

    const sim = BugSimilarity.compare(fp1, fp2);
    expect(sim.isLikelyRelated).toBe(true);
    expect(sim.percentage).toBeGreaterThanOrEqual(80);

    const diff = diffErrors(err1, err2);
    expect(diff.summary).toBe('Method Changed');
    expect(diff.oldValue).toBe('findById');
    expect(diff.newValue).toBe('findByCustomer');
    expect(BugDiff.compare(fp1, fp2).explain()).toContain('Method Changed');
  });

  it('tracks failures, families, timelines, and bursts in FailureTracker', () => {
    const tracker = new FailureTracker();
    const err = createErrorAt(
      new Error('Connection refused to postgres'),
      'UserRepository',
      'query',
      11
    );
    err.name = 'DatabaseConnectionException';

    tracker.capture(err, new Date('2026-01-01T00:00:00.100Z'));
    tracker.capture(err, new Date('2026-01-01T00:00:10.000Z'));
    tracker.capture(err, new Date('2026-01-01T00:01:10.900Z'));
    tracker.capture(err, new Date('2026-01-01T00:01:20.000Z'));

    // Mutating returned Date must not affect retained timeline
    const originalTime = tracker.timeline()[0].occurredAt.getTime();
    tracker.timeline()[0].occurredAt.setTime(0);
    expect(tracker.timeline()[0].occurredAt.getTime()).toBe(originalTime);

    expect(tracker.getTotalOccurrences()).toBe(4);
    expect(tracker.getUniqueFailures()).toBe(1);
    expect(tracker.getUniqueFamilies()).toBe(1);
    expect(tracker.families()[0].family).toBe(FailureFamily.DATABASE_CONNECTIVITY);
    // 00:00:10.000Z to 00:01:10.900Z is 60.9s (> 60s default idle gap), so 2 separate bursts of 2/min
    expect(tracker.bursts(2).length).toBe(2);
    expect(tracker.bursts(2)[0].peakRatePerMinute).toBe(2);
    expect(tracker.report()).toContain('1 unique failure signature');
  });

  it('compares deployment snapshots and detects regressions and drift', () => {
    const fpOld = generate(
      createErrorAt(new Error('old'), 'UserService', 'login', 10)
    );
    const fpRecurring = generate(
      createErrorAt(new Error('rec'), 'OrderService', 'checkout', 20)
    );
    const fpNew = generate(
      createErrorAt(new Error('new'), 'PaymentGateway', 'charge', 30)
    );

    const oldSnap = new DeploymentSnapshot('1.0.0', [fpOld, fpRecurring]);
    const newSnap = new DeploymentSnapshot('1.1.0', [fpRecurring, fpNew]);

    const comparison = RegressionDetector.compare(oldSnap, newSnap);
    expect(comparison.getNewFingerprintCount()).toBe(1);
    expect(comparison.getResolvedFingerprintCount()).toBe(1);
    expect(comparison.getRecurringFingerprintCount()).toBe(1);
    expect(comparison.report()).toContain('Version 1.0.0 -> Version 1.1.0');

    const drift = FingerprintDriftDetector.detect(fpRecurring, fpRecurring);
    expect(drift.signatureDriftPercentage).toBe(0);
  });

  it('supports SkipReasonAnalyzer, ConsumerFailureTracker, Assertions, and KnowledgeBase', () => {
    const fp = generate(
      createErrorAt(
        new RangeError('bad input'),
        'InputValidator',
        'validate',
        12
      ),
      FailureContext.of(15, 2, false)
    );

    BugDnaAssertions.assertThat(fp)
      .hasCategory(FailureCategory.VALIDATION)
      .hasFamily(FailureFamily.VALIDATION)
      .hasRootCause('RangeError')
      .hasSignature('InputValidator#validate')
      .hasStabilityScore(90);

    expect(fp.priority).toBe(FailurePriority.MEDIUM);

    const skipAnalyzer = new SkipReasonAnalyzer();
    skipAnalyzer.record(fp);
    expect(skipAnalyzer.getMostCommonFailure()?.id).toBe(fp.id);
    expect(skipAnalyzer.report()).toContain(fp.id);

    const consumerTracker = new ConsumerFailureTracker();
    consumerTracker.capture('orders-topic', 2, 104, fp);
    expect(consumerTracker.failures()[0].topic).toBe('orders-topic');
    expect(consumerTracker.report()).toContain('orders-topic');

    const kb = parseKnowledgeBase(
      `${fp.id}:\n  title: Bad Input Error\n  owner: Core Team\n  runbook: docs/validation.md\n`
    );
    BugDna.loadKnowledgeBase(kb);
    const info: FingerprintKnowledge | null = BugDna.lookup(fp.id);
    expect(info?.getTitle()).toBe('Bad Input Error');
    expect(info?.getOwner()).toBe('Core Team');
  });
});
