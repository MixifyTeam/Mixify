# Mixify logo rendering fix

Launcher artwork was incorrectly reused as an in-app alpha mask. Compose Icon applies a single tint to every non-transparent pixel, including the opaque background, so it appeared as a white square.

The in-app and notification resources now use the original Mixify monochrome paths on a transparent background, cropped to a square viewport. Home and Top Picks use matching square bounds. Launcher adaptive artwork is unchanged.

Apply this patch by merging its app and sharedUi folders into the project root and replacing the five matching files. Stop the running app, rebuild and run. Check the home header, Top Picks corner mark, light/dark themes, onboarding, Replay and notification icon. Resource structure and original path fidelity were checked locally; Android rendering has not been verified here.
