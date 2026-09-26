const EMAIL_PATTERN = /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/gi;
const NUMBER_PATTERN = /\b\d+\b/g;

/**
 * Replaces common high-cardinality or personally identifiable values with
 * stable tokens before they are used as fingerprint evidence.
 */
export function normalize(value: string): string {
  if (value === null || value === undefined) {
    throw new Error('value must not be null');
  }
  return value.replace(EMAIL_PATTERN, '{EMAIL}').replace(NUMBER_PATTERN, '{NUMBER}');
}
