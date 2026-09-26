import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import {
  categorize,
  classifyFamilyFromEvidence,
  compareFingerprints,
  createFingerprint,
  detectDrift,
  diffFingerprints,
  FailureCategory,
  FailureContext,
  FailurePriority,
  FailureTracker,
  generateFromSynthetic,
  prioritize,
} from '../src/index.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const FIXTURES_DIR = path.resolve(__dirname, '../../specification/fixtures');

function loadFixture<T>(fileName: string): T[] {
  const filePath = path.join(FIXTURES_DIR, fileName);
  const raw = fs.readFileSync(filePath, 'utf8');
  return JSON.parse(raw) as T[];
}

function makeDummyFingerprint(id: string) {
  return createFingerprint({
    id,
    rootCause: 'java.lang.RuntimeException',
    signature: 'Example#run',
    qualifiedSignature: 'com.example.Example#run',
    frames: ['com.example.Example#run'],
    failureChain: ['Example'],
    causeChain: ['java.lang.RuntimeException'],
    explanation: 'synthetic',
    stabilityScore: 90,
    priority: FailurePriority.UNKNOWN,
    category: FailureCategory.UNKNOWN,
    family: 'UNKNOWN',
  });
}

describe('Cross-Language Specification Compatibility Fixtures', () => {
  describe('identity.json', () => {
    interface IdentityFixture {
      description: string;
      input: {
        rootCause: string;
        frames: { class: string; method: string }[];
        causeChain?: string[];
      };
      expected: {
        id?: string;
        stabilityScore?: number;
        signature?: string;
        qualifiedSignature?: string;
        failureChain?: string[];
        frameCount?: number;
      };
    }

    const cases = loadFixture<IdentityFixture>('identity.json');
    for (const tc of cases) {
      it(tc.description, () => {
        const fp = generateFromSynthetic(tc.input);
        if (tc.expected.id !== undefined) {
          expect(fp.id).toBe(tc.expected.id);
        }
        if (tc.expected.stabilityScore !== undefined) {
          expect(fp.stabilityScore).toBe(tc.expected.stabilityScore);
        }
        if (tc.expected.signature !== undefined) {
          expect(fp.signature).toBe(tc.expected.signature);
        }
        if (tc.expected.qualifiedSignature !== undefined) {
          expect(fp.qualifiedSignature).toBe(tc.expected.qualifiedSignature);
        }
        if (tc.expected.failureChain !== undefined) {
          expect([...fp.failureChain]).toEqual(tc.expected.failureChain);
        }
        if (tc.expected.frameCount !== undefined) {
          expect(fp.frames.length).toBe(tc.expected.frameCount);
        }
      });
    }
  });

  describe('category.json', () => {
    interface CategoryFixture {
      description: string;
      input: { rootCause: string };
      expected: { category: FailureCategory };
    }

    const cases = loadFixture<CategoryFixture>('category.json');
    for (const tc of cases) {
      it(tc.description, () => {
        expect(categorize(tc.input.rootCause)).toBe(tc.expected.category);
      });
    }
  });

  describe('family.json', () => {
    interface FamilyFixture {
      description: string;
      input: {
        rootCause: string;
        category: FailureCategory;
        evidence: string;
      };
      expected: { family: string };
    }

    const cases = loadFixture<FamilyFixture>('family.json');
    for (const tc of cases) {
      it(tc.description, () => {
        const family = classifyFamilyFromEvidence(
          tc.input.category,
          tc.input.evidence
        );
        expect(family).toBe(tc.expected.family);
      });
    }
  });

  describe('priority.json', () => {
    interface PriorityFixture {
      description: string;
      input: {
        occurrences: number;
        affectedUsers: number;
        fatal: boolean;
      };
      expected: { priority: FailurePriority };
    }

    const cases = loadFixture<PriorityFixture>('priority.json');
    for (const tc of cases) {
      it(tc.description, () => {
        const ctx = FailureContext.from(tc.input);
        expect(prioritize(ctx)).toBe(tc.expected.priority);
      });
    }
  });

  describe('similarity.json', () => {
    interface SimilarityFixture {
      description: string;
      input: {
        first: {
          id: string;
          rootCause: string;
          qualifiedSignature: string;
          frames: string[];
          causeChain: string[];
        };
        second: {
          id: string;
          rootCause: string;
          qualifiedSignature: string;
          frames: string[];
          causeChain: string[];
        };
      };
      expected: {
        percentage?: number;
        isLikelyRelated: boolean;
      };
    }

    const cases = loadFixture<SimilarityFixture>('similarity.json');
    for (const tc of cases) {
      it(tc.description, () => {
        const sim = compareFingerprints(tc.input.first, tc.input.second);
        if (tc.expected.percentage !== undefined) {
          expect(sim.percentage).toBe(tc.expected.percentage);
        }
        expect(sim.isLikelyRelated).toBe(tc.expected.isLikelyRelated);
      });
    }
  });

  describe('diff.json', () => {
    interface DiffFixture {
      description: string;
      input: {
        old: {
          id: string;
          rootCause: string;
          qualifiedSignature: string;
          frames: string[];
        };
        new: {
          id: string;
          rootCause: string;
          qualifiedSignature: string;
          frames: string[];
        };
      };
      expected: { summary: string };
    }

    const cases = loadFixture<DiffFixture>('diff.json');
    for (const tc of cases) {
      it(tc.description, () => {
        const diff = diffFingerprints(tc.input.old, tc.input.new);
        expect(diff.summary).toBe(tc.expected.summary);
      });
    }
  });

  describe('drift.json', () => {
    interface DriftFixture {
      description: string;
      input: {
        old: {
          id: string;
          qualifiedSignature: string;
          frames: string[];
        };
        new: {
          id: string;
          qualifiedSignature: string;
          frames: string[];
        };
      };
      expected: {
        signatureDriftPercentage?: number;
        isPossibleCodePathChange: boolean;
      };
    }

    const cases = loadFixture<DriftFixture>('drift.json');
    for (const tc of cases) {
      it(tc.description, () => {
        const drift = detectDrift(tc.input.old, tc.input.new);
        if (tc.expected.signatureDriftPercentage !== undefined) {
          expect(drift.signatureDriftPercentage).toBe(
            tc.expected.signatureDriftPercentage
          );
        }
        expect(drift.isPossibleCodePathChange).toBe(
          tc.expected.isPossibleCodePathChange
        );
      });
    }
  });

  describe('timeline.json', () => {
    interface TimelineFixture {
      description: string;
      input: {
        events: { id: string; epochSecond: number }[];
        timelineLimit: number;
      };
      expected: {
        size?: number;
        order: string[];
      };
    }

    const cases = loadFixture<TimelineFixture>('timeline.json');
    for (const tc of cases) {
      it(tc.description, () => {
        const tracker = new FailureTracker(tc.input.timelineLimit);
        for (const ev of tc.input.events) {
          tracker.captureFingerprint(makeDummyFingerprint(ev.id), ev.epochSecond);
        }
        const timeline = tracker.timeline();
        if (tc.expected.size !== undefined) {
          expect(timeline.length).toBe(tc.expected.size);
        }
        expect(timeline.map((e) => e.id)).toEqual(tc.expected.order);
      });
    }
  });

  describe('burst.json', () => {
    interface BurstFixture {
      description: string;
      input: {
        events: { id: string; epochSecond: number }[];
        maximumIdleGapSeconds: number;
        minimumPeakRatePerMinute: number;
      };
      expected: {
        burstCount: number;
        bursts: {
          id: string;
          occurrences: number;
          peakRatePerMinute?: number;
        }[];
      };
    }

    const cases = loadFixture<BurstFixture>('burst.json');
    for (const tc of cases) {
      it(tc.description, () => {
        const tracker = new FailureTracker();
        for (const ev of tc.input.events) {
          tracker.captureFingerprint(makeDummyFingerprint(ev.id), ev.epochSecond);
        }
        const bursts = tracker.bursts(
          tc.input.minimumPeakRatePerMinute,
          tc.input.maximumIdleGapSeconds
        );
        expect(bursts.length).toBe(tc.expected.burstCount);
        for (let i = 0; i < tc.expected.bursts.length; i++) {
          const exp = tc.expected.bursts[i];
          expect(bursts[i].id).toBe(exp.id);
          expect(bursts[i].occurrences).toBe(exp.occurrences);
          if (exp.peakRatePerMinute !== undefined) {
            expect(bursts[i].peakRatePerMinute).toBe(exp.peakRatePerMinute);
          }
        }
      });
    }
  });
});
