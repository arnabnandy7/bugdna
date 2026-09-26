import { generate } from './bugdna.js';
import { FailureFamily } from './family.js';
import { Fingerprint } from './fingerprint.js';

const DEFAULT_TOP_FAILURE_LIMIT = 10;
const DEFAULT_TOP_FAMILY_LIMIT = 10;
const DEFAULT_TIMELINE_LIMIT = 10_000;
const DEFAULT_BURST_MAX_IDLE_GAP_SECONDS = 60;

export class FailureOccurrence {
  readonly occurredAt: Date;
  readonly epochSecond: number;
  readonly fingerprint: Fingerprint;
  readonly id: string;

  constructor(occurredAt: Date | number, fingerprint: Fingerprint) {
    if (occurredAt === null || occurredAt === undefined) {
      throw new Error('occurredAt must not be null');
    }
    if (!fingerprint) {
      throw new Error('fingerprint must not be null');
    }
    if (typeof occurredAt === 'number') {
      this.epochSecond = Math.floor(occurredAt);
      this.occurredAt = new Date(this.epochSecond * 1000);
    } else {
      this.occurredAt = new Date(occurredAt.getTime());
      this.epochSecond = Math.floor(this.occurredAt.getTime() / 1000);
    }
    this.fingerprint = fingerprint;
    this.id = fingerprint.id;
    Object.freeze(this);
  }

  getOccurredAt(): Date {
    return this.occurredAt;
  }

  getFingerprint(): Fingerprint {
    return this.fingerprint;
  }

  getId(): string {
    return this.id;
  }
}

export class FailureBurst {
  readonly fingerprint: Fingerprint;
  readonly id: string;
  readonly firstSeen: Date;
  readonly lastSeen: Date;
  readonly peakRatePerMinute: number;
  readonly occurrences: number;
  readonly durationSeconds: number;

  constructor(
    fingerprint: Fingerprint,
    firstSeen: Date,
    lastSeen: Date,
    peakRatePerMinute: number,
    occurrences: number
  ) {
    if (!fingerprint) throw new Error('fingerprint must not be null');
    if (!firstSeen) throw new Error('firstSeen must not be null');
    if (!lastSeen) throw new Error('lastSeen must not be null');
    if (lastSeen.getTime() < firstSeen.getTime()) {
      throw new Error('lastSeen must not be before firstSeen');
    }
    if (peakRatePerMinute < 1) {
      throw new Error('peakRatePerMinute must be at least 1');
    }
    if (occurrences < 1) {
      throw new Error('occurrences must be at least 1');
    }

    this.fingerprint = fingerprint;
    this.id = fingerprint.id;
    this.firstSeen = new Date(firstSeen.getTime());
    this.lastSeen = new Date(lastSeen.getTime());
    this.peakRatePerMinute = peakRatePerMinute;
    this.occurrences = occurrences;
    this.durationSeconds = Math.floor(
      (this.lastSeen.getTime() - this.firstSeen.getTime()) / 1000
    );
    Object.freeze(this);
  }

  getFingerprint(): Fingerprint {
    return this.fingerprint;
  }

  getId(): string {
    return this.id;
  }

  getFirstSeen(): Date {
    return this.firstSeen;
  }

  getLastSeen(): Date {
    return this.lastSeen;
  }

  getPeakRatePerMinute(): number {
    return this.peakRatePerMinute;
  }

  getOccurrences(): number {
    return this.occurrences;
  }

  getDurationSeconds(): number {
    return this.durationSeconds;
  }

  report(): string {
    const nl = '\n';
    const durationStr =
      this.durationSeconds % 60 === 0
        ? `${this.durationSeconds / 60} min`
        : `${this.durationSeconds} sec`;
    return (
      `${this.id} burst detected` +
      nl +
      nl +
      `First Seen: ${formatUtcTime(this.firstSeen)}` +
      nl +
      `Peak Rate: ${this.peakRatePerMinute}/min` +
      nl +
      `Duration: ${durationStr}`
    );
  }
}

export class FailureAggregate {
  readonly fingerprint: Fingerprint;
  readonly id: string;
  readonly occurrences: number;

  constructor(fingerprint: Fingerprint, occurrences: number) {
    if (!fingerprint) throw new Error('fingerprint must not be null');
    if (occurrences < 1) throw new Error('occurrences must be at least 1');
    this.fingerprint = fingerprint;
    this.id = fingerprint.id;
    this.occurrences = occurrences;
    Object.freeze(this);
  }

  getFingerprint(): Fingerprint {
    return this.fingerprint;
  }

  getId(): string {
    return this.id;
  }

  getOccurrences(): number {
    return this.occurrences;
  }
}

export class FailureFamilyAggregate {
  readonly family: FailureFamily;
  readonly failures: readonly FailureAggregate[];
  readonly uniqueFailures: number;
  readonly occurrences: number;

  constructor(
    family: FailureFamily,
    failures: readonly FailureAggregate[],
    occurrences: number
  ) {
    if (!family) throw new Error('family must not be null');
    if (!failures || failures.length === 0) {
      throw new Error('failures must not be empty');
    }
    if (occurrences < 1) throw new Error('occurrences must be at least 1');
    this.family = family;
    this.failures = Object.freeze([...failures]);
    this.uniqueFailures = this.failures.length;
    this.occurrences = occurrences;
    Object.freeze(this);
  }

  getFamily(): FailureFamily {
    return this.family;
  }

  getFailures(): readonly FailureAggregate[] {
    return this.failures;
  }

  getUniqueFailures(): number {
    return this.uniqueFailures;
  }

  getOccurrences(): number {
    return this.occurrences;
  }
}

function formatUtcTime(date: Date): string {
  const hours = String(date.getUTCHours()).padStart(2, '0');
  const mins = String(date.getUTCMinutes()).padStart(2, '0');
  return `${hours}:${mins}`;
}

class BurstAccumulator {
  readonly fingerprint: Fingerprint;
  private readonly minuteCounts = new Map<number, number>();
  private firstSeen: Date | null = null;
  private lastSeen: Date | null = null;
  private lastEpochSecond = 0;
  private occurrences = 0;
  private peakRatePerMinute = 0;

  constructor(fingerprint: Fingerprint) {
    this.fingerprint = fingerprint;
  }

  add(occurrence: FailureOccurrence): void {
    const dt = occurrence.occurredAt;
    if (this.firstSeen === null || dt.getTime() < this.firstSeen.getTime()) {
      this.firstSeen = dt;
    }
    if (this.lastSeen === null || dt.getTime() > this.lastSeen.getTime()) {
      this.lastSeen = dt;
      this.lastEpochSecond = occurrence.epochSecond;
    }
    this.occurrences++;
    const minute = Math.floor(occurrence.epochSecond / 60);
    const count = (this.minuteCounts.get(minute) ?? 0) + 1;
    this.minuteCounts.set(minute, count);
    if (count > this.peakRatePerMinute) {
      this.peakRatePerMinute = count;
    }
  }

  gapBeforeSeconds(epochSecond: number): number {
    return epochSecond - this.lastEpochSecond;
  }

  snapshot(): FailureBurst {
    return new FailureBurst(
      this.fingerprint,
      this.firstSeen!,
      this.lastSeen!,
      this.peakRatePerMinute,
      this.occurrences
    );
  }
}

export class FailureTracker {
  private readonly trackedFailures = new Map<
    string,
    { fingerprint: Fingerprint; count: number }
  >();
  private totalCount = 0;
  private readonly timelineEvents: FailureOccurrence[] = [];
  private readonly timelineLimitValue: number;

  constructor(timelineLimit = DEFAULT_TIMELINE_LIMIT) {
    if (timelineLimit < 1) {
      throw new Error('timelineLimit must be at least 1');
    }
    this.timelineLimitValue = timelineLimit;
  }

  capture(failure: Error | Fingerprint, occurredAt?: Date | number): Fingerprint {
    if (!failure) {
      throw new Error('failure must not be null');
    }
    const timestamp = occurredAt ?? new Date();
    if (failure instanceof Error) {
      const fp = generate(failure);
      this.captureFingerprint(fp, timestamp);
      return fp;
    }
    this.captureFingerprint(failure, timestamp);
    return failure;
  }

  captureFingerprint(fingerprint: Fingerprint, occurredAt: Date | number = new Date()): void {
    if (!fingerprint) {
      throw new Error('fingerprint must not be null');
    }
    if (occurredAt === null || occurredAt === undefined) {
      throw new Error('occurredAt must not be null');
    }
    const existing = this.trackedFailures.get(fingerprint.id);
    if (existing) {
      existing.count++;
    } else {
      this.trackedFailures.set(fingerprint.id, { fingerprint, count: 1 });
    }
    this.totalCount++;
    this.timelineEvents.push(new FailureOccurrence(occurredAt, fingerprint));
    while (this.timelineEvents.length > this.timelineLimitValue) {
      this.timelineEvents.shift();
    }
  }

  timeline(): readonly FailureOccurrence[] {
    const snapshot = [...this.timelineEvents];
    snapshot.sort((a, b) => {
      const timeDiff = a.occurredAt.getTime() - b.occurredAt.getTime();
      if (timeDiff !== 0) return timeDiff;
      return a.id.localeCompare(b.id);
    });
    return Object.freeze(snapshot);
  }

  timelineReport(): string {
    const nl = '\n';
    return this.timeline()
      .map((occ) => `${formatUtcTime(occ.occurredAt)} ${occ.id}`)
      .join(nl);
  }

  bursts(
    minimumPeakRatePerMinute: number,
    maximumIdleGapSeconds = DEFAULT_BURST_MAX_IDLE_GAP_SECONDS
  ): readonly FailureBurst[] {
    if (minimumPeakRatePerMinute < 1) {
      throw new Error('minimumPeakRatePerMinute must be at least 1');
    }
    if (maximumIdleGapSeconds <= 0) {
      throw new Error('maximumIdleGap must be positive');
    }

    const accumulators = new Map<string, BurstAccumulator>();
    const result: FailureBurst[] = [];

    for (const occurrence of this.timeline()) {
      let acc = accumulators.get(occurrence.id);
      if (
        acc !== undefined &&
        acc.gapBeforeSeconds(occurrence.epochSecond) > maximumIdleGapSeconds
      ) {
        const snap = acc.snapshot();
        if (snap.peakRatePerMinute >= minimumPeakRatePerMinute) {
          result.push(snap);
        }
        acc = undefined;
      }
      if (acc === undefined) {
        acc = new BurstAccumulator(occurrence.fingerprint);
        accumulators.set(occurrence.id, acc);
      }
      acc.add(occurrence);
    }

    for (const acc of accumulators.values()) {
      const snap = acc.snapshot();
      if (snap.peakRatePerMinute >= minimumPeakRatePerMinute) {
        result.push(snap);
      }
    }

    result.sort((a, b) => {
      if (b.peakRatePerMinute !== a.peakRatePerMinute) {
        return b.peakRatePerMinute - a.peakRatePerMinute;
      }
      return a.id.localeCompare(b.id);
    });

    return Object.freeze(result);
  }

  burstReport(minimumPeakRatePerMinute: number): string {
    const nl = '\n';
    return this.bursts(minimumPeakRatePerMinute)
      .map((b) => b.report())
      .join(nl + nl);
  }

  getTimelineLimit(): number {
    return this.timelineLimitValue;
  }

  failures(): readonly FailureAggregate[] {
    const snapshot: FailureAggregate[] = [];
    for (const entry of this.trackedFailures.values()) {
      snapshot.push(new FailureAggregate(entry.fingerprint, entry.count));
    }
    snapshot.sort((a, b) => {
      if (b.occurrences !== a.occurrences) {
        return b.occurrences - a.occurrences;
      }
      return a.id.localeCompare(b.id);
    });
    return Object.freeze(snapshot);
  }

  topFailures(limit = DEFAULT_TOP_FAILURE_LIMIT): readonly FailureAggregate[] {
    if (limit < 1) {
      throw new Error('limit must be at least 1');
    }
    return Object.freeze(this.failures().slice(0, limit));
  }

  families(): readonly FailureFamilyAggregate[] {
    const grouped = new Map<FailureFamily, FailureAggregate[]>();
    const counts = new Map<FailureFamily, number>();

    for (const failure of this.failures()) {
      const family = failure.fingerprint.family;
      const list = grouped.get(family) ?? [];
      list.push(failure);
      grouped.set(family, list);
      counts.set(family, (counts.get(family) ?? 0) + failure.occurrences);
    }

    const snapshot: FailureFamilyAggregate[] = [];
    for (const [family, list] of grouped.entries()) {
      snapshot.push(
        new FailureFamilyAggregate(family, list, counts.get(family) ?? 0)
      );
    }

    snapshot.sort((a, b) => {
      if (b.occurrences !== a.occurrences) {
        return b.occurrences - a.occurrences;
      }
      return a.family.localeCompare(b.family);
    });

    return Object.freeze(snapshot);
  }

  topFamilies(limit = DEFAULT_TOP_FAMILY_LIMIT): readonly FailureFamilyAggregate[] {
    if (limit < 1) {
      throw new Error('limit must be at least 1');
    }
    return Object.freeze(this.families().slice(0, limit));
  }

  getTotalOccurrences(): number {
    return this.totalCount;
  }

  getUniqueFailures(): number {
    return this.trackedFailures.size;
  }

  getUniqueFamilies(): number {
    return this.families().length;
  }

  report(): string {
    const nl = '\n';
    const grouped = this.failures();
    let out = `${grouped.length} unique failure signature${grouped.length === 1 ? '' : 's'}`;
    for (const failure of grouped) {
      out += nl + nl + failure.id + nl + `Count: ${failure.occurrences}`;
    }
    return out;
  }

  topFailureReport(limit = DEFAULT_TOP_FAILURE_LIMIT): string {
    const nl = '\n';
    let out = `Top ${limit} Failure Signatures`;
    for (const failure of this.topFailures(limit)) {
      out += nl + failure.id + nl + `Count: ${failure.occurrences}`;
    }
    return out;
  }

  familyReport(): string {
    return this.formatFamilyReport('Root Cause Families', this.families());
  }

  topFamilyReport(limit = DEFAULT_TOP_FAMILY_LIMIT): string {
    if (limit < 1) {
      throw new Error('limit must be at least 1');
    }
    return this.formatFamilyReport(
      `Top ${limit} Root Cause Families`,
      this.topFamilies(limit)
    );
  }

  clear(): void {
    this.trackedFailures.clear();
    this.totalCount = 0;
    this.timelineEvents.length = 0;
  }

  private formatFamilyReport(
    heading: string,
    families: readonly FailureFamilyAggregate[]
  ): string {
    const nl = '\n';
    let out = heading;
    for (const fam of families) {
      out +=
        nl +
        nl +
        `Family: ${fam.family}` +
        nl +
        `Occurrences: ${fam.occurrences}` +
        nl +
        `Unique Failures: ${fam.uniqueFailures}`;
      for (const failure of fam.failures) {
        out += nl + `${failure.id} (${failure.occurrences})`;
      }
    }
    return out;
  }
}

export class SkipReasonAnalyzer {
  private readonly tracker: FailureTracker;

  constructor(tracker = new FailureTracker()) {
    if (!tracker) throw new Error('tracker must not be null');
    this.tracker = tracker;
  }

  record(failure: Error | Fingerprint): Fingerprint {
    if (!failure) throw new Error('failure must not be null');
    return this.tracker.capture(failure);
  }

  getMostCommonFailure(): FailureAggregate | null {
    const top = this.tracker.topFailures(1);
    return top.length === 0 ? null : top[0];
  }

  report(): string {
    const nl = '\n';
    const mostCommon = this.getMostCommonFailure();
    if (!mostCommon) {
      return 'No skipped records captured';
    }
    return (
      `Most common skip reason: ${mostCommon.id} (${mostCommon.occurrences})` +
      nl +
      nl +
      this.tracker.topFailureReport()
    );
  }

  clear(): void {
    this.tracker.clear();
  }
}

export class ConsumerFailureAggregate {
  readonly topic: string;
  readonly partition: number;
  readonly offset: number;
  readonly fingerprint: Fingerprint;
  readonly id: string;
  readonly occurrences: number;

  constructor(
    topic: string,
    partition: number,
    offset: number,
    fingerprint: Fingerprint,
    occurrences: number
  ) {
    this.topic = topic;
    this.partition = partition;
    this.offset = offset;
    this.fingerprint = fingerprint;
    this.id = fingerprint.id;
    this.occurrences = occurrences;
    Object.freeze(this);
  }

  getTopic(): string {
    return this.topic;
  }

  getPartition(): number {
    return this.partition;
  }

  getOffset(): number {
    return this.offset;
  }

  getFingerprint(): Fingerprint {
    return this.fingerprint;
  }

  getId(): string {
    return this.id;
  }

  getOccurrences(): number {
    return this.occurrences;
  }
}

export class ConsumerFailureTracker {
  private readonly entries = new Map<
    string,
    {
      topic: string;
      partition: number;
      offset: number;
      fingerprint: Fingerprint;
      occurrences: number;
    }
  >();

  capture(
    topic: string,
    partition: number,
    offset: number,
    failure: Error | Fingerprint
  ): Fingerprint {
    if (!topic || topic.trim().length === 0) {
      throw new Error('topic must not be blank');
    }
    if (partition < 0) {
      throw new Error('partition must not be negative');
    }
    if (offset < 0) {
      throw new Error('offset must not be negative');
    }
    if (!failure) {
      throw new Error('failure must not be null');
    }
    const fp = failure instanceof Error ? generate(failure) : failure;
    const key = `${topic}::${fp.id}`;
    const existing = this.entries.get(key);
    if (existing) {
      existing.partition = partition;
      existing.offset = offset;
      existing.occurrences++;
    } else {
      this.entries.set(key, {
        topic,
        partition,
        offset,
        fingerprint: fp,
        occurrences: 1,
      });
    }
    return fp;
  }

  failures(): readonly ConsumerFailureAggregate[] {
    const result: ConsumerFailureAggregate[] = [];
    for (const e of this.entries.values()) {
      result.push(
        new ConsumerFailureAggregate(
          e.topic,
          e.partition,
          e.offset,
          e.fingerprint,
          e.occurrences
        )
      );
    }
    result.sort((a, b) => {
      if (b.occurrences !== a.occurrences) return b.occurrences - a.occurrences;
      const idCmp = a.id.localeCompare(b.id);
      if (idCmp !== 0) return idCmp;
      return a.topic.localeCompare(b.topic);
    });
    return Object.freeze(result);
  }

  report(): string {
    const nl = '\n';
    const list = this.failures();
    if (list.length === 0) {
      return 'No consumer failures captured';
    }
    return list
      .map(
        (item) =>
          `Topic: ${item.topic}` +
          nl +
          `Partition: ${item.partition}` +
          nl +
          `Offset: ${item.offset}` +
          nl +
          `Fingerprint: ${item.id}` +
          nl +
          `Occurrences: ${item.occurrences}`
      )
      .join(nl + nl);
  }

  clear(): void {
    this.entries.clear();
  }
}
