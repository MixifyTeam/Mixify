/** Official Mixify origin; set MIXIFY_SITE_ORIGINS if the web player is hosted elsewhere. */
export const PRODUCTION_ORIGIN = 'https://mixify.co.in';

/** Vite development server (web/app/vite.config.ts keeps port 5173 stable). */
const DEV_ORIGIN = 'http://localhost:5173';

/**
 * The only web origins permitted to communicate with the extension.
 * Store builds omit localhost. Override the list at extension build time with
 * MIXIFY_SITE_ORIGINS=https://web.example.com,http://localhost:5173.
 * The first origin is used for the popup's "Open Mixify" link.
 */
export const SITE_ORIGINS: string[] = (
  process.env.MIXIFY_SITE_ORIGINS ??
  [PRODUCTION_ORIGIN, ...(process.env.MIXIFY_STORE_BUILD === '1' ? [] : [DEV_ORIGIN])].join(',')
)
  .split(',')
  .map((o) => o.trim().replace(/\/+$/, ''))
  .filter(Boolean);

/** WebExtension match patterns don't accept ports; exact origins are
 * independently verified by the background's sender checks.
 */
export function matchPatternsFor(origins: string[]): string[] {
  return [...new Set(origins.map((o) => {
    const url = new URL(o);
    return `${url.protocol}//${url.hostname}/*`;
  }))];
}
