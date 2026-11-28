import appLogo from '@/assets/logo-mark.svg';

/** The official Mixify Android launcher artwork, used unchanged in the player. */
export function LogoMark({ size = 20, className }: { size?: number; className?: string }) {
  return <img src={appLogo} alt="" aria-hidden="true" width={size} height={size} className={className} style={{ width: size, height: size, objectFit: 'contain', flexShrink: 0 }} />;
}
