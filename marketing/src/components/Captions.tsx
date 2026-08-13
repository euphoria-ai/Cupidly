import React from "react";
import { interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { DISPLAY } from "../fonts";
import { pebble, stage } from "../theme";

/**
 * Burned-in captions. Instagram autoplays muted, so every word that matters is
 * on screen — these are not decoration, they are the soundtrack.
 */

/** The opening line. Big, top-of-frame, lands one word at a time. */
export const HookTitle: React.FC<{
  text: string;
  /** Words matching these get the blue highlight slab. */
  accent?: string[];
  top?: number;
  size?: number;
  /** Frames between each word landing. */
  stagger?: number;
}> = ({ text, accent = [], top = 150, size = 96, stagger = 3 }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const words = text.split(" ");
  const accentSet = new Set(accent.map((a) => a.toLowerCase()));

  return (
    <div
      style={{
        position: "absolute",
        top,
        left: 70,
        right: 70,
        display: "flex",
        flexWrap: "wrap",
        justifyContent: "center",
        gap: "10px 16px",
        fontFamily: DISPLAY,
      }}
    >
      {words.map((word, i) => {
        const s = spring({
          frame: frame - i * stagger,
          fps,
          config: { damping: 13, stiffness: 190, mass: 0.6 },
        });
        const hot = accentSet.has(word.toLowerCase().replace(/[^a-z']/g, ""));
        return (
          <span
            key={i}
            style={{
              display: "inline-block",
              fontSize: size,
              fontWeight: 800,
              letterSpacing: -2.5,
              lineHeight: 1.02,
              color: hot ? "#fff" : "#F2F5FB",
              background: hot
                ? `linear-gradient(180deg, ${pebble.bubbleTop} 0%, ${pebble.bubbleBottom} 100%)`
                : "transparent",
              borderRadius: hot ? 16 : 0,
              padding: hot ? "2px 16px 8px" : "2px 0 8px",
              boxShadow: hot ? `0 12px 34px ${pebble.blue}66` : "none",
              textShadow: hot ? "none" : "0 6px 26px rgba(0,0,0,0.75)",
              opacity: s,
              transform: `translateY(${(1 - s) * 42}px) scale(${0.82 + s * 0.18})`,
            }}
          >
            {word}
          </span>
        );
      })}
    </div>
  );
};

/**
 * The lower caption bar — one short line at a time, sitting under the phone.
 * Swaps by cutting, not crossfading: a fade reads as slow at reel pace.
 */
export const CaptionBar: React.FC<{
  text: string;
  bottom?: number;
  size?: number;
  tone?: "neutral" | "hot";
}> = ({ text, bottom = 150, size = 52, tone = "neutral" }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const s = spring({
    frame,
    fps,
    config: { damping: 15, stiffness: 200, mass: 0.5 },
  });

  return (
    <div
      style={{
        position: "absolute",
        bottom,
        left: 60,
        right: 60,
        display: "flex",
        justifyContent: "center",
        fontFamily: DISPLAY,
      }}
    >
      <span
        style={{
          fontSize: size,
          fontWeight: 700,
          letterSpacing: -1,
          textAlign: "center",
          lineHeight: 1.18,
          color: "#fff",
          padding: "16px 30px",
          borderRadius: 22,
          background:
            tone === "hot"
              ? `linear-gradient(180deg, ${stage.hot}EE 0%, #B3175A 100%)`
              : "rgba(6,10,20,0.72)",
          border:
            tone === "hot"
              ? "none"
              : "1px solid rgba(255,255,255,0.12)",
          backdropFilter: "blur(14px)",
          boxShadow: "0 20px 60px rgba(0,0,0,0.5)",
          opacity: s,
          transform: `translateY(${(1 - s) * 26}px) scale(${0.94 + s * 0.06})`,
        }}
      >
        {text}
      </span>
    </div>
  );
};

/** Small all-caps eyebrow, used for "POV:" style setups. */
export const Eyebrow: React.FC<{ text: string; top?: number }> = ({
  text,
  top = 96,
}) => {
  const frame = useCurrentFrame();
  const opacity = interpolate(frame, [0, 6], [0, 1], {
    extrapolateRight: "clamp",
  });
  return (
    <div
      style={{
        position: "absolute",
        top,
        left: 0,
        right: 0,
        textAlign: "center",
        fontFamily: DISPLAY,
        fontSize: 34,
        fontWeight: 700,
        letterSpacing: 6,
        textTransform: "uppercase",
        color: pebble.blueLight,
        opacity,
      }}
    >
      {text}
    </div>
  );
};
