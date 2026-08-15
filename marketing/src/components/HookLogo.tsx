import React from "react";
import { Img, staticFile } from "remotion";

/**
 * The app mark, served from `marketing/public/` so the reels use the same
 * artwork as `branding/`.
 *
 *  - hook-logo-tile.png -> branding/darkmode_filled.png (light tile, dark mark)
 *  - hook-logo-mark.png -> branding/darkmode_nobg.png   (white mark, no tile)
 *
 * Both variants are the dark-mode set because every reel plays on the dark
 * `stage` backdrop. If the backdrop ever goes light, swap in the lightmode
 * files rather than recolouring here.
 */
export const HookLogo: React.FC<{ size: number; tile?: boolean }> = ({
  size,
  tile = true,
}) => (
  <Img
    src={staticFile(tile ? "hook-logo-tile.png" : "hook-logo-mark.png")}
    style={{
      width: size,
      height: size,
      // The source art is 1024x1049, so the tile crops the 2% overhang instead
      // of letterboxing white edges inside the rounded container.
      objectFit: tile ? "cover" : "contain",
    }}
  />
);
