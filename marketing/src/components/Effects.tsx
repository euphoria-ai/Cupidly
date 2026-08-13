import React from "react";
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { DISPLAY } from "../fonts";
import { pebble, stage } from "../theme";

/**
 * The screenshot moment: the phone snaps in a touch, the frame goes white for
 * three frames, and a thumbnail peels off toward the keyboard.
 *
 * `at` is the frame the shutter fires on, in the enclosing sequence's clock.
 */
export const ScreenFlash: React.FC<{ at: number }> = ({ at }) => {
  const frame = useCurrentFrame();
  const opacity = interpolate(
    frame,
    [at - 1, at, at + 2, at + 9],
    [0, 0.92, 0.72, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" }
  );
  if (opacity <= 0) return null;
  return <AbsoluteFill style={{ background: "#fff", opacity }} />;
};

/** How much the device should shrink on a given frame for the shutter. */
export const shutterScale = (frame: number, at: number) =>
  interpolate(frame, [at - 2, at + 2, at + 12], [1, 0.955, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

/**
 * Hearts leaving the phone after the reply sends. Deterministic, seeded by
 * index — Remotion renders frames out of order, so Math.random() is a bug.
 */
export const HeartBurst: React.FC<{ at: number; count?: number }> = ({
  at,
  count = 14,
}) => {
  const frame = useCurrentFrame();
  const { fps, width, height } = useVideoConfig();
  const t = frame - at;
  if (t < 0) return null;

  return (
    <AbsoluteFill style={{ pointerEvents: "none" }}>
      {new Array(count).fill(0).map((_, i) => {
        const seed = (i * 9301 + 49297) % 233280;
        const rand = seed / 233280;
        const delay = Math.floor(rand * 14);
        const p = spring({
          frame: t - delay,
          fps,
          config: { damping: 200, stiffness: 32, mass: 1.4 },
        });
        if (p <= 0) return null;

        // They leave from the bottom edge of the device and clear the frame,
        // rather than drifting across the middle of the thread we want read.
        const drift = (rand - 0.5) * 620;
        const size = 40 + rand * 54;
        const x = width / 2 + drift + Math.sin((t - delay) / 9 + i) * 30;
        const y = height * 0.9 - p * (height * 0.72);
        const fade = interpolate(p, [0, 0.65, 1], [0, 1, 0]);

        return (
          <span
            key={i}
            style={{
              position: "absolute",
              left: x,
              top: y,
              fontSize: size,
              opacity: fade,
              transform: `rotate(${(rand - 0.5) * 40}deg)`,
              filter: `drop-shadow(0 6px 18px ${stage.hot}88)`,
            }}
          >
            {i % 3 === 0 ? "🔥" : i % 3 === 1 ? "❤️" : "😍"}
          </span>
        );
      })}
    </AbsoluteFill>
  );
};

/**
 * A finger tapping something. Reels need to show the gesture — without it the
 * viewer reads the UI as animating by itself.
 *
 * Meant to be rendered *inside* the phone screen, in the same logical 390x844
 * space as the UI it is pointing at, so it cannot drift out of alignment when
 * the device moves. `size` is therefore in logical pixels too.
 */
export const TapRipple: React.FC<{
  x: number;
  y: number;
  at: number;
  size?: number;
}> = ({ x, y, at, size = 64 }) => {
  const frame = useCurrentFrame();
  const t = frame - at;
  if (t < 0 || t > 24) return null;

  const dot = size * 0.7;
  const grow = interpolate(t, [0, 18], [0.25, 1.5], {
    extrapolateRight: "clamp",
  });
  const fade = interpolate(t, [0, 4, 20], [0.85, 0.55, 0], {
    extrapolateRight: "clamp",
  });
  const dotScale = interpolate(t, [0, 5, 16], [1, 0.78, 1], {
    extrapolateRight: "clamp",
  });

  return (
    <>
      <div
        style={{
          position: "absolute",
          left: x - size / 2,
          top: y - size / 2,
          width: size,
          height: size,
          borderRadius: size,
          border: `2px solid rgba(255,255,255,${fade})`,
          transform: `scale(${grow})`,
          pointerEvents: "none",
        }}
      />
      <div
        style={{
          position: "absolute",
          left: x - dot / 2,
          top: y - dot / 2,
          width: dot,
          height: dot,
          borderRadius: dot,
          background: "rgba(255,255,255,0.3)",
          border: "2px solid rgba(255,255,255,0.75)",
          transform: `scale(${dotScale})`,
          boxShadow: "0 6px 20px rgba(0,0,0,0.45)",
          pointerEvents: "none",
        }}
      />
    </>
  );
};

/** "3 replies in 2 seconds" style stat, punched in over the phone. */
export const StatStamp: React.FC<{ value: string; label: string }> = ({
  value,
  label,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const s = spring({
    frame,
    fps,
    config: { damping: 11, stiffness: 220, mass: 0.5 },
  });
  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        fontFamily: DISPLAY,
        opacity: s,
        transform: `scale(${0.7 + s * 0.3}) rotate(${(1 - s) * -8}deg)`,
      }}
    >
      <span
        style={{
          fontSize: 130,
          fontWeight: 800,
          letterSpacing: -6,
          lineHeight: 1,
          color: "#fff",
          textShadow: `0 0 60px ${pebble.blue}, 0 10px 40px rgba(0,0,0,0.6)`,
        }}
      >
        {value}
      </span>
      <span
        style={{
          fontSize: 34,
          fontWeight: 700,
          letterSpacing: 3,
          textTransform: "uppercase",
          color: pebble.blueLight,
        }}
      >
        {label}
      </span>
    </div>
  );
};
