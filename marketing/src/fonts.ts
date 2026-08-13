import { loadFont as loadInter } from "@remotion/google-fonts/Inter";

// Only the weights and subset the reels actually draw. The default load pulls
// every weight in every subset — 126 requests per render worker, all of which
// have to finish before the first frame is allowed to paint.
const inter = loadInter("normal", {
  weights: ["500", "600", "700", "800"],
  subsets: ["latin"],
});

/**
 * Every text node goes through one of these two stacks.
 *
 * The system fallbacks are load-bearing: if a render machine has no network,
 * Google Fonts silently no-ops and we still want something that looks like the
 * app rather than Times New Roman.
 */
export const FONT = `${inter.fontFamily}, "Segoe UI", Roboto, system-ui, sans-serif`;

/** Burned-in caption face — heavier, tighter, meant to be read at arm's length. */
export const DISPLAY = FONT;
