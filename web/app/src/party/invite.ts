/**
 * Cross-platform Mixify Listen Together invitations.
 * Current Android builds share https://mixify.co.in/listen?code=ABC123&v=107.
 * Keep accepting /invite/ABC123 and older custom-scheme links for compatibility.
 */

export const INVITE_ORIGIN = 'https://mixify.co.in';
export const CODE_LENGTH = 6;
const INVITE_VERSION = '107';

/** Letters and digits only, upper case; null unless it's six of them. */
export function cleanCode(raw: string): string | null {
  const cleaned = raw.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
  return cleaned.length === CODE_LENGTH ? cleaned : null;
}

export function sanitizeServerUrl(raw: string | null | undefined): string | null {
  const trimmed = raw?.trim().replace(/\/+$/, '');
  if (!trimmed) return null;
  const withScheme = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  try {
    return new URL(withScheme).host ? withScheme : null;
  } catch {
    return null;
  }
}

export function parseInvite(value: string): { code: string; serverUrl: string | null } | null {
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    return null;
  }
  const server = sanitizeServerUrl(url.searchParams.get('server'));
  if ((url.protocol === 'mixify:' || url.protocol === 'bitchord:') && url.host.toLowerCase() === 'party') {
    const code = cleanCode(url.pathname.replace(/^\/+|\/+$/g, '') || url.searchParams.get('code') || '');
    return code ? { code, serverUrl: server } : null;
  }
  if (url.protocol !== 'https:' && url.protocol !== 'http:') return null;

  // The Android app's canonical invitation URL. Support room= for old senders.
  if (url.hostname.toLowerCase() === new URL(INVITE_ORIGIN).hostname && url.pathname === '/listen') {
    const code = cleanCode(url.searchParams.get('code') || url.searchParams.get('room') || '');
    return code ? { code, serverUrl: server } : null;
  }

  // Retain the original /invite/:code route for previously shared links.
  const legacy = url.pathname.match(/^\/invite\/([A-Za-z0-9]{6})$/);
  return legacy ? { code: legacy[1]!.toUpperCase(), serverUrl: server } : null;
}

/** Share a link recognised by both the Mixify website and the Android app. */
export function inviteUrl(code: string, customServer: string | null): string {
  const url = new URL('/listen', INVITE_ORIGIN);
  url.searchParams.set('code', code.toUpperCase());
  url.searchParams.set('v', INVITE_VERSION);
  const server = sanitizeServerUrl(customServer);
  if (server) url.searchParams.set('server', server);
  return url.toString();
}
