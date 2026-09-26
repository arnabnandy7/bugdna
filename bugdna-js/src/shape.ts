export interface SignatureParts {
  readonly className: string;
  readonly methodName: string;
}

export function parseSignature(qualifiedSignature: string): SignatureParts {
  const separator = qualifiedSignature.lastIndexOf('#');
  if (separator < 0) {
    return { className: qualifiedSignature, methodName: '' };
  }
  return {
    className: qualifiedSignature.substring(0, separator),
    methodName: qualifiedSignature.substring(separator + 1),
  };
}

export function simpleClassName(className: string): string {
  const packageSeparator = className.lastIndexOf('.');
  const simpleName =
    packageSeparator >= 0 ? className.substring(packageSeparator + 1) : className;
  return simpleName.replace(/\$/g, '.');
}

export function equalScore(first: string, second: string, weight: number): number {
  return first === second ? weight : 0;
}

export function methodSimilarity(first: string, second: string): number {
  if (first === second) {
    return 1.0;
  }
  if (first.length === 0 || second.length === 0) {
    return 0.0;
  }
  if (first.startsWith(second) || second.startsWith(first)) {
    return 0.67;
  }

  const firstTokens = methodTokens(first);
  const secondTokens = methodTokens(second);
  if (firstTokens.size === 0 || secondTokens.size === 0) {
    return 0.0;
  }

  return setOverlap(firstTokens, secondTokens);
}

export function frameSimilarity(
  first: Iterable<string>,
  second: Iterable<string>
): number {
  const firstFrames = new Set(first);
  const secondFrames = new Set(second);
  if (firstFrames.size === 0 || secondFrames.size === 0) {
    return 0.0;
  }

  let total = 0.0;
  for (const firstFrame of firstFrames) {
    let best = 0.0;
    for (const secondFrame of secondFrames) {
      best = Math.max(best, singleFrameSimilarity(firstFrame, secondFrame));
    }
    total += best;
  }

  return total / Math.max(firstFrames.size, secondFrames.size);
}

export function overlap(first: Iterable<string>, second: Iterable<string>): number {
  const firstValues = new Set(first);
  const secondValues = new Set(second);
  if (firstValues.size === 0 || secondValues.size === 0) {
    return 0.0;
  }

  return setOverlap(firstValues, secondValues);
}

function singleFrameSimilarity(first: string, second: string): number {
  if (first === second) {
    return 1.0;
  }

  const firstFrame = parseSignature(first);
  const secondFrame = parseSignature(second);
  if (firstFrame.className !== secondFrame.className) {
    return 0.0;
  }

  return 0.7 + 0.3 * methodSimilarity(firstFrame.methodName, secondFrame.methodName);
}

function methodTokens(methodName: string): Set<string> {
  const normalized = methodName
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/_/g, ' ')
    .replace(/-/g, ' ')
    .toLowerCase();
  const parts = normalized.split(/\s+/);
  const tokens = new Set<string>();
  for (const part of parts) {
    if (part.length > 0) {
      tokens.add(part);
    }
  }
  return tokens;
}

function setOverlap(firstValues: Set<string>, secondValues: Set<string>): number {
  let intersectionSize = 0;
  for (const v of firstValues) {
    if (secondValues.has(v)) {
      intersectionSize++;
    }
  }
  const union = new Set<string>(firstValues);
  for (const v of secondValues) {
    union.add(v);
  }
  return intersectionSize / union.size;
}
