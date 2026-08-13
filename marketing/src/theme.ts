/**
 * Lifted verbatim from the app so the ads and the product cannot drift apart.
 *
 *  - pebble.*      -> app/src/main/java/com/tomfricks/hook/ui/theme/Color.kt
 *  - ig.*          -> the fixed "somebody else's app" colours in DemoChatStep.kt
 *
 * If a colour changes in Kotlin, change it here too — a reel that shows the
 * wrong blue is a reel of a different app.
 */

export const pebble = {
  blueLight: "#4DA2FF",
  blue: "#1E6FFF",
  blueDeep: "#0B4ED6",
  blueInk: "#06318C",

  bubbleTop: "#4C9BFF",
  bubbleBottom: "#0B57E3",

  darkBackground: "#070B14",
  darkSurface: "#0D1220",
  darkSurfaceVariant: "#161D2E",
  darkOnBackground: "#F2F5FB",
  darkOnSurfaceVariant: "#9AA6BF",
  darkOutline: "#243049",
  darkScreenshotCard: "#141B2B",

  slate: "#1C2130",
} as const;

/** The chat the demo pretends to be. Deliberately not Hook's theme. */
export const ig = {
  white: "#FFFFFF",
  purple: "#6E2A63",
  incoming: "#EFEFEF",
  grayText: "#8E8E8E",
  divider: "#DBDBDB",
  coach: "#3A3A3A",
} as const;

/** Backdrop the phone floats on. Not an app colour — this is ad furniture. */
export const stage = {
  top: "#0B1224",
  bottom: "#04060D",
  glow: "#1E6FFF",
  hot: "#FF3B7F",
} as const;

/** Logical phone screen size everything inside the device frame is authored in. */
export const SCREEN = { width: 390, height: 844 } as const;

/** Keyboard panel height, matching PanelHeight = 300.dp in KeyboardPanel.kt. */
export const KEYBOARD_HEIGHT = 300;

export const REEL = { width: 1080, height: 1920, fps: 30 } as const;
