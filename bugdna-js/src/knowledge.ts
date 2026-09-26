import fs from 'node:fs';

export class FingerprintKnowledge {
  readonly id: string;
  readonly fields: Readonly<Record<string, string>>;

  constructor(id: string, fields: Record<string, string>) {
    if (!id || id.trim().length === 0) {
      throw new Error('id must not be blank');
    }
    this.id = id;
    this.fields = Object.freeze({ ...fields });
    Object.freeze(this);
  }

  getId(): string {
    return this.id;
  }

  getTitle(): string | null {
    return this.fields['title'] ?? null;
  }

  getOwner(): string | null {
    return this.fields['owner'] ?? null;
  }

  getRunbook(): string | null {
    return this.fields['runbook'] ?? null;
  }

  get(name: string): string | null {
    return this.fields[name] ?? null;
  }

  getFields(): Readonly<Record<string, string>> {
    return this.fields;
  }
}

function stripComment(line: string): string {
  let inSingleQuotes = false;
  let inDoubleQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (ch === "'" && !inDoubleQuotes) {
      inSingleQuotes = !inSingleQuotes;
    } else if (ch === '"' && !inSingleQuotes) {
      inDoubleQuotes = !inDoubleQuotes;
    } else if (ch === '#' && !inSingleQuotes && !inDoubleQuotes) {
      return line.substring(0, i);
    }
  }
  return line;
}

function unquote(value: string): string {
  if (value.length >= 2) {
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      return value.substring(1, value.length - 1);
    }
  }
  return value;
}

export function parseKnowledgeBase(yamlContent: string): Map<string, FingerprintKnowledge> {
  if (yamlContent === null || yamlContent === undefined) {
    throw new Error('yamlContent must not be null');
  }
  const lines = yamlContent.split(/\r?\n/);
  const entries = new Map<string, FingerprintKnowledge>();
  let currentId: string | null = null;
  let currentFields: Record<string, string> = {};

  const flushCurrent = (): void => {
    if (currentId !== null) {
      entries.set(currentId, new FingerprintKnowledge(currentId, currentFields));
    }
  };

  for (let i = 0; i < lines.length; i++) {
    const lineNumber = i + 1;
    const rawLine = stripComment(lines[i]);
    if (rawLine.trim().length === 0) {
      continue;
    }

    const indented = rawLine.startsWith(' ') || rawLine.startsWith('\t');
    const trimmed = rawLine.trim();

    if (!indented) {
      if (!trimmed.endsWith(':') || trimmed.length === 1) {
        throw new Error(
          `Invalid knowledge base entry at line ${lineNumber}: ${lines[i]}`
        );
      }
      flushCurrent();
      currentId = unquote(trimmed.substring(0, trimmed.length - 1).trim());
      currentFields = {};
    } else {
      if (currentId === null) {
        throw new Error(
          `Field declared before fingerprint ID at line ${lineNumber}`
        );
      }
      const colonIdx = trimmed.indexOf(':');
      if (colonIdx <= 0) {
        throw new Error(
          `Invalid knowledge base field at line ${lineNumber}: ${lines[i]}`
        );
      }
      const key = trimmed.substring(0, colonIdx).trim();
      const val = unquote(trimmed.substring(colonIdx + 1).trim());
      currentFields[key] = val;
    }
  }

  flushCurrent();
  return entries;
}

const DEFAULT_KNOWLEDGE_FILES = [
  'bugdna.yml',
  'bugdna.yaml',
  'bugdna-fingerprints.yml',
  'bugdna-fingerprints.yaml',
];

let loadedKnowledgeBase: Map<string, FingerprintKnowledge> | null = null;

export function readKnowledgeBase(filePath: string): Map<string, FingerprintKnowledge> {
  if (!filePath) throw new Error('path must not be null');
  const content = fs.readFileSync(filePath, 'utf8');
  return parseKnowledgeBase(content);
}

export function loadKnowledgeBase(
  source: string | Map<string, FingerprintKnowledge> | Record<string, FingerprintKnowledge>
): void {
  if (!source) throw new Error('source must not be null');
  if (typeof source === 'string') {
    loadedKnowledgeBase = readKnowledgeBase(source);
  } else if (source instanceof Map) {
    loadedKnowledgeBase = new Map(source);
  } else {
    loadedKnowledgeBase = new Map(Object.entries(source));
  }
}

export function clearKnowledgeBaseForTesting(): void {
  loadedKnowledgeBase = null;
}

function discoverDefaultKnowledgeBase(): Map<string, FingerprintKnowledge> {
  const configuredPath = process.env['BUGDNA_KNOWLEDGE_PATH'];
  if (configuredPath && configuredPath.trim().length > 0) {
    return readKnowledgeBase(configuredPath.trim());
  }
  for (const fileName of DEFAULT_KNOWLEDGE_FILES) {
    if (fs.existsSync(fileName) && fs.statSync(fileName).isFile()) {
      return readKnowledgeBase(fileName);
    }
  }
  return new Map();
}

export function lookup(id: string): FingerprintKnowledge | null {
  if (!id) throw new Error('id must not be null');
  if (loadedKnowledgeBase === null) {
    loadedKnowledgeBase = discoverDefaultKnowledgeBase();
  }
  return loadedKnowledgeBase.get(id) ?? null;
}
