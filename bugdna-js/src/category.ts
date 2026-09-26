export type FailureCategory =
  | 'DATABASE'
  | 'NETWORK'
  | 'VALIDATION'
  | 'SECURITY'
  | 'SERIALIZATION'
  | 'CONFIGURATION'
  | 'BUSINESS'
  | 'UNKNOWN';

export const FailureCategory = {
  DATABASE: 'DATABASE' as FailureCategory,
  NETWORK: 'NETWORK' as FailureCategory,
  VALIDATION: 'VALIDATION' as FailureCategory,
  SECURITY: 'SECURITY' as FailureCategory,
  SERIALIZATION: 'SERIALIZATION' as FailureCategory,
  CONFIGURATION: 'CONFIGURATION' as FailureCategory,
  BUSINESS: 'BUSINESS' as FailureCategory,
  UNKNOWN: 'UNKNOWN' as FailureCategory,
} as const;

const DATABASE_PATTERNS = [
  'java.sql.',
  '.sql',
  'database',
  'jdbc',
  'datasource',
  'sequelize',
  'prisma',
  'typeorm',
  'knex',
  'mongoerror',
  'pg.',
];

const NETWORK_PATTERNS = [
  'java.net.',
  'socket',
  'connect',
  'network',
  'timeout',
  'http',
  'econnrefused',
  'econnreset',
  'etimedout',
  'ehostunreach',
  'enotfound',
  'fetcherror',
];

const VALIDATION_PATTERNS = [
  'validation',
  'constraint',
  'illegalargument',
  'parse',
  'format',
  'rangeerror',
  'urierror',
  'zoderror',
];

const SECURITY_PATTERNS = [
  'security',
  'accessdenied',
  'authentication',
  'authorization',
  'permission',
  'certificate',
  'ssl',
  'crypto',
  'eacces',
  'eperm',
  'unauthorized',
  'forbidden',
];

const SERIALIZATION_PATTERNS = [
  'serialization',
  'deserialization',
  'invalidclass',
  'invalidobject',
  'notserializable',
  'objectstream',
  'streamcorrupted',
  'json',
  'xml',
  'mapping',
  'codec',
  'decode',
  'encode',
  'syntaxerror',
];

const CONFIGURATION_PATTERNS = [
  'configuration',
  'config',
  'property',
  'environment',
  'missingresource',
];

const BUSINESS_PATTERNS = [
  'business',
  'domain',
  'rule',
  'policy',
];

function matchesAny(value: string, patterns: readonly string[]): boolean {
  for (const pattern of patterns) {
    if (value.includes(pattern)) {
      return true;
    }
  }
  return false;
}

export function categorize(rootCauseName: string): FailureCategory {
  if (rootCauseName === null || rootCauseName === undefined) {
    throw new Error('rootCauseName must not be null');
  }
  const lowerName = rootCauseName.toLowerCase();

  if (matchesAny(lowerName, DATABASE_PATTERNS)) {
    return FailureCategory.DATABASE;
  }
  if (matchesAny(lowerName, NETWORK_PATTERNS)) {
    return FailureCategory.NETWORK;
  }
  if (matchesAny(lowerName, VALIDATION_PATTERNS)) {
    return FailureCategory.VALIDATION;
  }
  if (matchesAny(lowerName, SECURITY_PATTERNS)) {
    return FailureCategory.SECURITY;
  }
  if (matchesAny(lowerName, SERIALIZATION_PATTERNS)) {
    return FailureCategory.SERIALIZATION;
  }
  if (matchesAny(lowerName, CONFIGURATION_PATTERNS)) {
    return FailureCategory.CONFIGURATION;
  }
  if (matchesAny(lowerName, BUSINESS_PATTERNS)) {
    return FailureCategory.BUSINESS;
  }

  return FailureCategory.UNKNOWN;
}
