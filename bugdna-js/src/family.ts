import { FailureCategory } from './category.js';
import { normalize } from './normalize.js';

export type FailureFamily =
  | 'DATABASE_CONNECTIVITY'
  | 'DATABASE_OPERATION'
  | 'NETWORK_CONNECTIVITY'
  | 'VALIDATION'
  | 'SECURITY'
  | 'SERIALIZATION'
  | 'CONFIGURATION'
  | 'BUSINESS'
  | 'UNKNOWN';

export const FailureFamily = {
  DATABASE_CONNECTIVITY: 'DATABASE_CONNECTIVITY' as FailureFamily,
  DATABASE_OPERATION: 'DATABASE_OPERATION' as FailureFamily,
  NETWORK_CONNECTIVITY: 'NETWORK_CONNECTIVITY' as FailureFamily,
  VALIDATION: 'VALIDATION' as FailureFamily,
  SECURITY: 'SECURITY' as FailureFamily,
  SERIALIZATION: 'SERIALIZATION' as FailureFamily,
  CONFIGURATION: 'CONFIGURATION' as FailureFamily,
  BUSINESS: 'BUSINESS' as FailureFamily,
  UNKNOWN: 'UNKNOWN' as FailureFamily,
} as const;

const DATABASE_CONTEXT_PATTERNS = [
  'java.sql.',
  '.sql',
  'database',
  'jdbc',
  'datasource',
  'hikari',
  'connectionpool',
  'poolbase',
  'postgres',
  'mysql',
  'mariadb',
  'oracle',
  'sequelize',
  'prisma',
  'typeorm',
  'knex',
];

const CONNECTIVITY_PATTERNS = [
  'connectexception',
  'socketexception',
  'sockettimeoutexception',
  'sqltransientconnectionexception',
  'sqlnontransientconnectionexception',
  'sqlrecoverableexception',
  'sqltimeoutexception',
  'jdbcconnectionexception',
  'connection refused',
  'connection reset',
  'connection timed out',
  'socket timeout',
  'communications link failure',
  'unable to acquire connection',
  'could not open connection',
  'pool exhausted',
  'connection pool',
  'timeout',
  'econnrefused',
  'econnreset',
  'etimedout',
];

function matchesAny(value: string, patterns: readonly string[]): boolean {
  for (const pattern of patterns) {
    if (value.includes(pattern)) {
      return true;
    }
  }
  return false;
}

export function classifyFamilyFromEvidence(
  category: FailureCategory,
  evidence: string
): FailureFamily {
  const lowerEvidence = evidence.toLowerCase();
  const connectivity = matchesAny(lowerEvidence, CONNECTIVITY_PATTERNS);
  const databaseContext =
    category === FailureCategory.DATABASE ||
    matchesAny(lowerEvidence, DATABASE_CONTEXT_PATTERNS);

  if (connectivity && databaseContext) {
    return FailureFamily.DATABASE_CONNECTIVITY;
  }
  if (category === FailureCategory.DATABASE) {
    return FailureFamily.DATABASE_OPERATION;
  }
  if (category === FailureCategory.NETWORK) {
    return FailureFamily.NETWORK_CONNECTIVITY;
  }
  if (category === FailureCategory.VALIDATION) {
    return FailureFamily.VALIDATION;
  }
  if (category === FailureCategory.SECURITY) {
    return FailureFamily.SECURITY;
  }
  if (category === FailureCategory.SERIALIZATION) {
    return FailureFamily.SERIALIZATION;
  }
  if (category === FailureCategory.CONFIGURATION) {
    return FailureFamily.CONFIGURATION;
  }
  if (category === FailureCategory.BUSINESS) {
    return FailureFamily.BUSINESS;
  }
  return FailureFamily.UNKNOWN;
}

export function buildFamilyEvidence(
  rootCauseName: string,
  causeChainNames: readonly string[],
  messages: readonly string[],
  frames: readonly string[]
): string {
  const parts: string[] = [rootCauseName];
  for (const cause of causeChainNames) {
    parts.push(cause);
  }
  for (const msg of messages) {
    if (msg) {
      parts.push(normalize(msg));
    }
  }
  for (const frame of frames) {
    parts.push(frame);
  }
  return parts.join(' ').toLowerCase();
}
