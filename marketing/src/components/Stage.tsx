import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { Backdrop } from "./Backdrop";

/** Every reel places its phone the same way, so the set cuts together. */
export const PHONE_WIDTH = 560;
export const PHONE_TOP = 380;

/**
 * Backdrop + a slow camera push on whatever is handed in.
 *
 * The push is the cheapest possible "this is a film, not a screen recording"
 * signal, and it costs nothing at encode time because it is a whole-layer
 * transform.
 */
export const Stage: React.FC<{
  children: React.ReactNode;
  /** Total frames the push runs over — usually the composition length. */
  over: number;
  from?: number;
  to?: number;
}> = ({ children, over, from = 1, to = 1.07 }) => {
  const frame = useCurrentFrame();
  const scale = interpolate(frame, [0, over], [from, to], {
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill>
      <Backdrop />
      <AbsoluteFill style={{ transform: `scale(${scale})` }}>
        {children}
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

/** Puts the device at the shared mark. */
export const PhoneSlot: React.FC<{
  children: React.ReactNode;
  top?: number;
}> = ({ children, top = PHONE_TOP }) => (
  <div
    style={{
      position: "absolute",
      top,
      left: 0,
      right: 0,
      display: "flex",
      justifyContent: "center",
    }}
  >
    {children}
  </div>
);
