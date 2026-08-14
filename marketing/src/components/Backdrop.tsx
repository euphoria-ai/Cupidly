import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { stage } from "../theme";

/**
 * The stage the phone floats on: a slow blue/pink light drift over near-black.
 *
 * It moves the whole reel long — a completely static background is what makes
 * a screen recording look like a screen recording.
 */
export const Backdrop: React.FC<{ intensity?: number }> = ({
  intensity = 1,
}) => {
  const frame = useCurrentFrame();

  const drift = interpolate(frame, [0, 600], [0, 1], {
    extrapolateRight: "extend",
  });
  const blueX = 30 + Math.sin(drift * Math.PI * 2) * 18;
  const blueY = 22 + Math.cos(drift * Math.PI * 2) * 12;
  const hotX = 74 + Math.cos(drift * Math.PI * 2 + 1.2) * 16;
  const hotY = 80 + Math.sin(drift * Math.PI * 2 + 1.2) * 10;

  return (
    <AbsoluteFill style={{ backgroundColor: stage.bottom }}>
      <AbsoluteFill
        style={{
          background: `linear-gradient(180deg, ${stage.top} 0%, ${stage.bottom} 70%)`,
        }}
      />
      <AbsoluteFill
        style={{
          background: `radial-gradient(60% 40% at ${blueX}% ${blueY}%, ${stage.glow}55 0%, transparent 70%)`,
          opacity: intensity,
        }}
      />
      <AbsoluteFill
        style={{
          background: `radial-gradient(48% 34% at ${hotX}% ${hotY}%, ${stage.hot}3A 0%, transparent 72%)`,
          opacity: intensity,
        }}
      />
      {/* Vignette, so the burned-in captions always sit on something darker. */}
      <AbsoluteFill
        style={{
          background:
            "radial-gradient(70% 55% at 50% 45%, transparent 40%, rgba(0,0,0,0.55) 100%)",
        }}
      />
      <Grain />
    </AbsoluteFill>
  );
};

/**
 * A little static noise. Instagram's encoder eats flat gradients and hands back
 * banding; grain gives it something to chew on.
 */
const Grain: React.FC = () => (
  <AbsoluteFill
    style={{
      opacity: 0.05,
      mixBlendMode: "overlay",
      backgroundImage:
        "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='120'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3'/%3E%3C/filter%3E%3Crect width='120' height='120' filter='url(%23n)'/%3E%3C/svg%3E\")",
      backgroundSize: "240px 240px",
    }}
  />
);
