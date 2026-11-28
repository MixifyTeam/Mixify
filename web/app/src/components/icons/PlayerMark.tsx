/**
 * The official Mixify M silhouette in the idle player, without the launcher
 * background. The player inherits its muted grey colour from `text-tertiary`,
 * matching the other transport controls. Sidebar and app icons remain coloured.
 */
export function PlayerMark({ size = 30 }: { size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 1133.2 1133.2"
      fill="currentColor"
      aria-hidden="true"
      focusable="false"
      style={{ display: 'block', flexShrink: 0 }}
    >
      <path d="M1011.1,309.9l-222.3,128.3l-222.3,128.3l-0,0l-222.3,-128.3l0,513.3l-222.3,-128.3l0,-513.3l222.3,-128.3l0,0l222.3,128.3l222.3,-128.3l222.3,128.3z" />
      <path d="M1011.1,566.6l0,256.7l-222.3,-128.3l222.3,-128.3z" />
    </svg>
  );
}
