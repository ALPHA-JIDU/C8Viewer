/**
 * Meta writes UTF-8 bytes as if they were Latin-1 (ISO-8859-1) letters in JSON.
 * This function reverses that to restore correct UTF-8 text and emojis.
 */
export function fixEncoding(str: string | null | undefined): string {
  if (!str) return '';

  // Check if string contains characters beyond Latin-1 range
  let hasOver255 = false;
  for (let i = 0; i < str.length; i++) {
    if (str.charCodeAt(i) > 255) {
      hasOver255 = true;
      break;
    }
  }

  // If already contains unicode > 255 (e.g. non-Latin-1), return as-is
  if (hasOver255) return str;

  try {
    const bytes = new Uint8Array(str.length);
    for (let i = 0; i < str.length; i++) {
      bytes[i] = str.charCodeAt(i) & 0xff;
    }
    const decoded = new TextDecoder('utf-8', { fatal: false }).decode(bytes);
    // If decoding produced replacement characters and original had normal ASCII, keep original
    if (decoded.includes('\uFFFD') && !str.includes('\uFFFD')) {
      return str;
    }
    return decoded;
  } catch {
    return str;
  }
}

/**
 * Format timestamp as "d MMM yyyy" (e.g., "12 Oct 2024")
 */
export function fmtDate(ts: number): string {
  if (!ts) return '';
  const d = new Date(ts);
  return d.toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

/**
 * Format timestamp as "h:mm a" (e.g., "4:15 PM")
 */
export function fmtTime(ts: number): string {
  if (!ts) return '';
  const d = new Date(ts);
  return d.toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  });
}

/**
 * Get date string for group day comparison (YYYY-MM-DD)
 */
export function dayOf(ts: number): string {
  if (!ts) return '';
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
