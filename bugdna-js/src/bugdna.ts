import crypto from 'node:crypto';
import path from 'node:path';
import { categorize, FailureCategory } from './category.js';
import { FailureContext, FailureContextInput } from './context.js';
import {
  buildFamilyEvidence,
  classifyFamilyFromEvidence,
  FailureFamily,
} from './family.js';
import { createFingerprint, Fingerprint } from './fingerprint.js';
import {
  clearKnowledgeBaseForTesting,
  FingerprintKnowledge,
  loadKnowledgeBase,
  lookup,
  readKnowledgeBase,
} from './knowledge.js';
import { normalize } from './normalize.js';
import { FailurePriority, prioritize } from './priority.js';
import { parseSignature, simpleClassName } from './shape.js';

const ID_PREFIX = 'BUGDNA-';
const HASH_LENGTH = 16;
const MAX_FINGERPRINT_FRAMES = 5;

export interface SyntheticFrame {
  readonly class: string;
  readonly method: string;
}

export interface SyntheticFailureInput {
  readonly rootCause: string;
  readonly frames?: readonly (SyntheticFrame | string)[];
  readonly causeChain?: readonly string[];
  readonly messages?: readonly string[];
}

export class FailureDependencyGraph {
  readonly fingerprints: readonly Fingerprint[];

  constructor(fingerprints: readonly Fingerprint[]) {
    if (!fingerprints) {
      throw new Error('fingerprints must not be null');
    }
    if (fingerprints.length === 0) {
      throw new Error('fingerprints must not be empty');
    }
    this.fingerprints = Object.freeze([...fingerprints]);
    Object.freeze(this);
  }

  get root(): Fingerprint {
    return this.fingerprints[0];
  }

  get dependencies(): readonly Fingerprint[] {
    return Object.freeze(this.fingerprints.slice(1));
  }

  get depth(): number {
    return this.fingerprints.length;
  }

  getRoot(): Fingerprint {
    return this.root;
  }

  getFingerprints(): readonly Fingerprint[] {
    return this.fingerprints;
  }

  getDependencies(): readonly Fingerprint[] {
    return this.dependencies;
  }

  getDepth(): number {
    return this.depth;
  }

  report(): string {
    const nl = '\n';
    let out = '';
    for (let i = 0; i < this.fingerprints.length; i++) {
      if (i > 0) {
        let indent = ' ';
        for (let d = 1; d < i; d++) {
          indent += '     ';
        }
        out += nl + indent + '└─ ';
      }
      out += this.fingerprints[i].id;
    }
    return out;
  }

  toString(): string {
    return `FailureDependencyGraph{depth=${this.fingerprints.length}, root='${this.getRoot().id}'}`;
  }
}

export class FingerprintAssert {
  private readonly fingerprint: Fingerprint;

  constructor(fingerprint: Fingerprint) {
    if (!fingerprint) {
      throw new Error('Expected fingerprint to be non-null, but was null');
    }
    this.fingerprint = fingerprint;
  }

  hasCategory(expected: FailureCategory): this {
    if (!expected) throw new Error('expected category must not be null');
    if (this.fingerprint.category !== expected) {
      throw new Error(
        `Expected category <${expected}> but was <${this.fingerprint.category}> for fingerprint <${this.fingerprint.id}>`
      );
    }
    return this;
  }

  hasFamily(expected: FailureFamily): this {
    if (!expected) throw new Error('expected family must not be null');
    if (this.fingerprint.family !== expected) {
      throw new Error(
        `Expected family <${expected}> but was <${this.fingerprint.family}> for fingerprint <${this.fingerprint.id}>`
      );
    }
    return this;
  }

  hasRootCause(expected: string | (new (...args: never[]) => Error)): this {
    if (!expected) throw new Error('expected rootCause must not be null');
    const expectedName = typeof expected === 'string' ? expected : expected.name;
    if (this.fingerprint.rootCause !== expectedName) {
      throw new Error(
        `Expected rootCause <${expectedName}> but was <${this.fingerprint.rootCause}> for fingerprint <${this.fingerprint.id}>`
      );
    }
    return this;
  }

  hasId(expected: string): this {
    if (!expected) throw new Error('expected id must not be null');
    if (this.fingerprint.id !== expected) {
      throw new Error(
        `Expected id <${expected}> but was <${this.fingerprint.id}>`
      );
    }
    return this;
  }

  hasSignature(expected: string): this {
    if (!expected) throw new Error('expected signature must not be null');
    if (this.fingerprint.signature !== expected) {
      throw new Error(
        `Expected signature <${expected}> but was <${this.fingerprint.signature}> for fingerprint <${this.fingerprint.id}>`
      );
    }
    return this;
  }

  hasQualifiedSignature(expected: string): this {
    if (!expected) throw new Error('expected qualifiedSignature must not be null');
    if (this.fingerprint.qualifiedSignature !== expected) {
      throw new Error(
        `Expected qualifiedSignature <${expected}> but was <${this.fingerprint.qualifiedSignature}> for fingerprint <${this.fingerprint.id}>`
      );
    }
    return this;
  }

  hasStabilityScore(expected: number): this {
    if (this.fingerprint.stabilityScore !== expected) {
      throw new Error(
        `Expected stabilityScore <${expected}> but was <${this.fingerprint.stabilityScore}> for fingerprint <${this.fingerprint.id}>`
      );
    }
    return this;
  }

  actual(): Fingerprint {
    return this.fingerprint;
  }
}

export const BugDnaAssertions = {
  assertThat(fingerprint: Fingerprint): FingerprintAssert {
    return new FingerprintAssert(fingerprint);
  },
} as const;

function shortHash(value: string): string {
  return crypto
    .createHash('sha256')
    .update(value, 'utf8')
    .digest('hex')
    .substring(0, HASH_LENGTH)
    .toUpperCase();
}

function createFailureChain(frames: readonly string[]): string[] {
  const chain: string[] = [];
  for (let i = frames.length - 1; i >= 0; i--) {
    const parts = parseSignature(frames[i]);
    const simpleName = simpleClassName(parts.className);
    if (chain.length === 0 || chain[chain.length - 1] !== simpleName) {
      chain.push(simpleName);
    }
  }
  return chain;
}

function calculateStabilityScore(stackDepth: number): number {
  if (stackDepth === 0) {
    return 70;
  }
  const normalizedFrameCount = Math.min(stackDepth, MAX_FINGERPRINT_FRAMES);
  const score = 86 + normalizedFrameCount * 4;
  return Math.min(score, 98);
}

function createExplanation(
  rootCause: string,
  qualifiedSignature: string,
  frameCount: number,
  stabilityScore: number,
  causeChain: readonly string[],
  priority: FailurePriority,
  context: FailureContext
): string {
  let explanation =
    `${rootCause} originated at ${qualifiedSignature} and was grouped using ` +
    `${frameCount} normalized stack frame${frameCount !== 1 ? 's' : ''}. ` +
    `Fingerprint stability confidence is ${stabilityScore}%.`;

  if (causeChain.length > 1) {
    explanation += ` Cause chain: ${causeChain.join(' -> ')}.`;
  }

  if (priority === FailurePriority.UNKNOWN) {
    explanation += ' Priority is unknown because no impact context was supplied.';
  } else {
    explanation +=
      ` Priority ${priority} is based on ${context.occurrences} occurrence(s), ` +
      `${context.affectedUsers} affected user(s), and fatal=${context.fatal}.`;
  }

  return explanation;
}

function resolveErrorTypeName(error: Error): string {
  if (error.name && error.name !== 'Error') {
    return error.name;
  }
  if (
    error.constructor &&
    error.constructor.name &&
    error.constructor.name !== 'Error'
  ) {
    return error.constructor.name;
  }
  return error.name || 'Error';
}

function fileStemFromLocation(location: string): string {
  const cleaned = location
    .replace(/^file:\/\//, '')
    .replace(/:\d+:\d+$/, '')
    .replace(/:\d+$/, '');
  const base = path.basename(cleaned);
  const ext = path.extname(base);
  const stem = ext ? base.slice(0, -ext.length) : base;
  return stem || 'Anonymous';
}

function parseV8StackFrames(stack: string | undefined): string[] {
  if (!stack || stack.trim().length === 0) {
    return [];
  }
  const lines = stack.split(/\r?\n/);
  const frames: string[] = [];

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line.startsWith('at ')) {
      continue;
    }

    const body = line.slice(3).trim().replace(/^async\s+/, '');

    // Direct Class#method format
    if (/^[A-Za-z0-9_$.]+#[A-Za-z0-9_$<>]+$/.test(body)) {
      frames.push(body);
      continue;
    }

    // Constructor: "new ClassName (location)" or "new ClassName"
    if (body.startsWith('new ')) {
      const rest = body.slice(4).trim();
      const parenIdx = rest.indexOf('(');
      const className = (parenIdx >= 0 ? rest.slice(0, parenIdx) : rest).trim();
      if (className) {
        frames.push(`${className}#<init>`);
        continue;
      }
    }

    // Named target with location: "ClassName.method (file:line:col)" or "funcName (file:line:col)"
    const parenOpen = body.indexOf('(');
    const parenClose = body.lastIndexOf(')');
    if (parenOpen > 0 && parenClose > parenOpen) {
      const target = body.slice(0, parenOpen).trim().replace(/\s+\[as\s+[^\]]+\]$/, '');
      const location = body.slice(parenOpen + 1, parenClose).trim();

      if (target.includes('#')) {
        frames.push(target);
        continue;
      }

      const lastDot = target.lastIndexOf('.');
      if (lastDot > 0) {
        const rawClass = target.slice(0, lastDot);
        const methodName = target.slice(lastDot + 1);
        const className =
          rawClass === 'Object' || rawClass === 'Module'
            ? fileStemFromLocation(location)
            : rawClass;
        frames.push(`${className}#${methodName}`);
      } else {
        const className = fileStemFromLocation(location);
        frames.push(`${className}#${target}`);
      }
      continue;
    }

    // Location-only frame: "file:line:col"
    const stem = fileStemFromLocation(body);
    frames.push(`${stem}#<anonymous>`);
  }

  return frames;
}

function findRootCause(failure: Error): Error {
  const visited = new Set<Error>();
  let current: Error = failure;

  while (
    current.cause instanceof Error &&
    !visited.has(current)
  ) {
    visited.add(current);
    if (visited.has(current.cause)) {
      break;
    }
    current = current.cause;
  }

  return current;
}

function collectCauseChainAndMessages(failure: Error): {
  causeChain: string[];
  messages: string[];
} {
  const visited = new Set<Error>();
  const causeChain: string[] = [];
  const messages: string[] = [];
  let current: Error | undefined = failure;

  while (current instanceof Error && !visited.has(current)) {
    visited.add(current);
    causeChain.push(resolveErrorTypeName(current));
    if (current.message) {
      messages.push(current.message);
    }
    current = current.cause instanceof Error ? current.cause : undefined;
  }

  return { causeChain, messages };
}

export function generateFromSynthetic(
  input: SyntheticFailureInput,
  context?: FailureContext | FailureContextInput | null
): Fingerprint {
  if (!input || !input.rootCause) {
    throw new Error('rootCause must not be null');
  }
  const ctx = FailureContext.from(context);
  const rootCauseName = input.rootCause;
  const rawInputFrames = input.frames ?? [];
  const parsedFrames: string[] = rawInputFrames.map((f) =>
    typeof f === 'string' ? f : `${f.class}#${f.method}`
  );
  const stackDepth = parsedFrames.length;

  let signature: string;
  let qualifiedSignature: string;
  let frames: string[];

  if (stackDepth === 0) {
    signature = simpleClassName(rootCauseName);
    qualifiedSignature = rootCauseName;
    frames = [rootCauseName];
  } else {
    const origin = parseSignature(parsedFrames[0]);
    signature = `${simpleClassName(origin.className)}#${origin.methodName}`;
    qualifiedSignature = `${origin.className}#${origin.methodName}`;
    frames = parsedFrames.slice(0, MAX_FINGERPRINT_FRAMES);
  }

  const failureChain = createFailureChain(frames);
  const causeChain =
    input.causeChain && input.causeChain.length > 0
      ? [...input.causeChain]
      : [rootCauseName];
  const canonicalValue = `${rootCauseName}|${frames.join('|')}`;
  const stabilityScore = calculateStabilityScore(stackDepth);
  const priority = prioritize(ctx);
  const category = categorize(rootCauseName);
  const evidence = buildFamilyEvidence(
    rootCauseName,
    causeChain,
    input.messages ?? [],
    frames
  );
  const family = classifyFamilyFromEvidence(category, evidence);
  const explanation = createExplanation(
    rootCauseName,
    qualifiedSignature,
    frames.length,
    stabilityScore,
    causeChain,
    priority,
    ctx
  );

  return createFingerprint({
    id: ID_PREFIX + shortHash(canonicalValue),
    rootCause: rootCauseName,
    signature,
    qualifiedSignature,
    frames,
    failureChain,
    causeChain,
    explanation,
    stabilityScore,
    priority,
    category,
    family,
  });
}

/**
 * Generates a deterministic fingerprint from a JavaScript/TypeScript Error.
 */
export function generate(
  failure: Error,
  context?: FailureContext | FailureContextInput | null
): Fingerprint {
  if (!failure) {
    throw new Error('failure must not be null');
  }
  if (context === null) {
    throw new Error('context must not be null');
  }
  const ctx = FailureContext.from(context);
  const rootCause = findRootCause(failure);
  return createFingerprintFromErrors(failure, rootCause, ctx);
}

function createFingerprintFromErrors(
  failure: Error,
  rootCause: Error,
  ctx: FailureContext
): Fingerprint {
  const rootCauseName = resolveErrorTypeName(rootCause);
  const rawFrames = parseV8StackFrames(rootCause.stack);
  const { causeChain, messages } = collectCauseChainAndMessages(failure);

  return generateFromSynthetic(
    {
      rootCause: rootCauseName,
      frames: rawFrames,
      causeChain,
      messages,
    },
    ctx
  );
}

/**
 * Generates a causal dependency graph from an Error and its cause chain.
 */
export function dependencyGraph(failure: Error): FailureDependencyGraph {
  if (!failure) {
    throw new Error('failure must not be null');
  }
  const visited = new Set<Error>();
  const fingerprints: Fingerprint[] = [];
  let current: Error | undefined = failure;

  while (current instanceof Error && !visited.has(current)) {
    visited.add(current);
    fingerprints.push(
      createFingerprintFromErrors(current, current, FailureContext.unknown())
    );
    current = current.cause instanceof Error ? current.cause : undefined;
  }

  return new FailureDependencyGraph(fingerprints);
}

export const BugDna = {
  generate,
  generateFromSynthetic,
  dependencyGraph,
  normalize,
  lookup,
  loadKnowledgeBase,
  readKnowledgeBase,
  clearKnowledgeBaseForTesting,
} as const;
