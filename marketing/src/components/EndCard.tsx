import React from "react";
import { AbsoluteFill, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { DISPLAY } from "../fonts";
import { pebble } from "../theme";
import { HookLogo } from "./HookLogo";

/**
 * The last two seconds: mark, name, tagline, store line, handle.
 *
 * One instruction only. Reels that end on three asks convert on none of them.
 */
export const EndCard: React.FC<{
  cta?: string;
  handle?: string;
}> = ({ cta = "Get Hook free on Google Play", handle = "@hook" }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const pop = spring({
    frame,
    fps,
    config: { damping: 12, stiffness: 170, mass: 0.7 },
  });
  const line = spring({
    frame: frame - 6,
    fps,
    config: { damping: 16, stiffness: 160, mass: 0.6 },
  });
  const btn = spring({
    frame: frame - 12,
    fps,
    config: { damping: 12, stiffness: 190, mass: 0.6 },
  });
  const pulse = 1 + Math.sin(frame / 7) * 0.02;

  return (
    <AbsoluteFill
      style={{
        alignItems: "center",
        justifyContent: "center",
        fontFamily: DISPLAY,
      }}
    >
      <div
        style={{
          transform: `scale(${0.6 + pop * 0.4}) rotate(${(1 - pop) * -10}deg)`,
          filter: `drop-shadow(0 30px 70px ${pebble.blue}55)`,
          borderRadius: 54,
          overflow: "hidden",
        }}
      >
        <HookLogo size={230} />
      </div>

      <span
        style={{
          marginTop: 44,
          fontSize: 128,
          fontWeight: 800,
          letterSpacing: -5,
          color: "#fff",
          opacity: line,
          transform: `translateY(${(1 - line) * 30}px)`,
        }}
      >
        Hook
      </span>

      <span
        style={{
          marginTop: 6,
          fontSize: 44,
          fontWeight: 600,
          letterSpacing: 1,
          color: pebble.blueLight,
          opacity: line,
          transform: `translateY(${(1 - line) * 30}px)`,
        }}
      >
        #1 AI Rizz Keyboard
      </span>

      <div
        style={{
          marginTop: 70,
          padding: "28px 56px",
          borderRadius: 30,
          fontSize: 46,
          fontWeight: 700,
          color: "#fff",
          background: `linear-gradient(180deg, ${pebble.bubbleTop} 0%, ${pebble.bubbleBottom} 100%)`,
          boxShadow: `0 26px 70px ${pebble.blue}77, 0 2px 0 rgba(255,255,255,0.3) inset`,
          opacity: btn,
          transform: `scale(${(0.85 + btn * 0.15) * pulse})`,
        }}
      >
        {cta}
      </div>

      <span
        style={{
          position: "absolute",
          bottom: 130,
          fontSize: 40,
          fontWeight: 600,
          letterSpacing: 2,
          color: "rgba(242,245,251,0.62)",
          opacity: btn,
        }}
      >
        {handle}
      </span>
    </AbsoluteFill>
  );
};
