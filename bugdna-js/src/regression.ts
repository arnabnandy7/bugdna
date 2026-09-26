import { detectDrift, FingerprintDrift, hasSignatureDrift } from './drift.js';
import { Fingerprint } from './fingerprint.js';

export class DeploymentSnapshot {
  readonly version: string;
  readonly fingerprints: readonly Fingerprint[];

  constructor(version: string, fingerprints: Iterable<Fingerprint>) {
    if (!version || version.trim().length === 0) {
      throw new Error('version must not be blank');
    }
    if (!fingerprints) {
      throw new Error('fingerprints must not be null');
    }
    const byId = new Map<string, Fingerprint>();
    for (const fp of fingerprints) {
      if (!fp) throw new Error('fingerprint must not be null');
      if (!byId.has(fp.id)) {
        byId.set(fp.id, fp);
      }
    }
    const sorted = Array.from(byId.values()).sort((a, b) => a.id.localeCompare(b.id));
    this.version = version;
    this.fingerprints = Object.freeze(sorted);
    Object.freeze(this);
  }

  getVersion(): string {
    return this.version;
  }

  getFingerprints(): readonly Fingerprint[] {
    return this.fingerprints;
  }
}

export class DeploymentComparison {
  readonly oldVersion: string;
  readonly newVersion: string;
  readonly newFingerprints: readonly Fingerprint[];
  readonly resolvedFingerprints: readonly Fingerprint[];
  readonly recurringFingerprints: readonly Fingerprint[];
  readonly fingerprintDrifts: readonly FingerprintDrift<Fingerprint>[];

  constructor(
    oldVersion: string,
    newVersion: string,
    newFingerprints: readonly Fingerprint[],
    resolvedFingerprints: readonly Fingerprint[],
    recurringFingerprints: readonly Fingerprint[],
    fingerprintDrifts: readonly FingerprintDrift<Fingerprint>[] = []
  ) {
    this.oldVersion = oldVersion;
    this.newVersion = newVersion;
    this.newFingerprints = Object.freeze([...newFingerprints]);
    this.resolvedFingerprints = Object.freeze([...resolvedFingerprints]);
    this.recurringFingerprints = Object.freeze([...recurringFingerprints]);
    this.fingerprintDrifts = Object.freeze([...fingerprintDrifts]);
    Object.freeze(this);
  }

  getOldVersion(): string {
    return this.oldVersion;
  }

  getNewVersion(): string {
    return this.newVersion;
  }

  getNewFingerprints(): readonly Fingerprint[] {
    return this.newFingerprints;
  }

  getResolvedFingerprints(): readonly Fingerprint[] {
    return this.resolvedFingerprints;
  }

  getRecurringFingerprints(): readonly Fingerprint[] {
    return this.recurringFingerprints;
  }

  getFingerprintDrifts(): readonly FingerprintDrift<Fingerprint>[] {
    return this.fingerprintDrifts;
  }

  getNewFingerprintCount(): number {
    return this.newFingerprints.length;
  }

  getResolvedFingerprintCount(): number {
    return this.resolvedFingerprints.length;
  }

  getRecurringFingerprintCount(): number {
    return this.recurringFingerprints.length;
  }

  getFingerprintDriftCount(): number {
    return this.fingerprintDrifts.length;
  }

  report(): string {
    const nl = '\n';
    let out =
      `Version ${this.oldVersion} -> Version ${this.newVersion}` +
      nl +
      nl +
      `New fingerprints: ${this.getNewFingerprintCount()}` +
      nl +
      `Resolved fingerprints: ${this.getResolvedFingerprintCount()}` +
      nl +
      `Recurring fingerprints: ${this.getRecurringFingerprintCount()}`;

    if (this.fingerprintDrifts.length > 0) {
      out += nl + `Fingerprint drifts: ${this.getFingerprintDriftCount()}`;
      for (const drift of this.fingerprintDrifts) {
        out += nl + nl + drift.report();
      }
    }
    return out;
  }
}

export const RegressionDetector = {
  compare(
    oldDeployment: DeploymentSnapshot,
    newDeployment: DeploymentSnapshot
  ): DeploymentComparison {
    if (!oldDeployment) throw new Error('oldDeployment must not be null');
    if (!newDeployment) throw new Error('newDeployment must not be null');

    const oldMap = new Map<string, Fingerprint>();
    for (const fp of oldDeployment.fingerprints) {
      oldMap.set(fp.id, fp);
    }

    const newMap = new Map<string, Fingerprint>();
    for (const fp of newDeployment.fingerprints) {
      newMap.set(fp.id, fp);
    }

    const added: Fingerprint[] = [];
    const recurring: Fingerprint[] = [];
    const drifts: FingerprintDrift<Fingerprint>[] = [];

    for (const [id, newFp] of newMap.entries()) {
      const oldFp = oldMap.get(id);
      if (oldFp) {
        recurring.push(newFp);
        if (hasSignatureDrift(oldFp, newFp)) {
          drifts.push(detectDrift(oldFp, newFp));
        }
      } else {
        added.push(newFp);
      }
    }

    const resolved: Fingerprint[] = [];
    for (const [id, oldFp] of oldMap.entries()) {
      if (!newMap.has(id)) {
        resolved.push(oldFp);
      }
    }

    return new DeploymentComparison(
      oldDeployment.version,
      newDeployment.version,
      added,
      resolved,
      recurring,
      drifts
    );
  },
} as const;
