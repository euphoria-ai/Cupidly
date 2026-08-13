import React from "react";
import { useCurrentFrame } from "remotion";
import { SCREEN } from "../theme";
import { FONT } from "../fonts";

/**
 * An Android handset the screen content is authored inside at logical 390x844,
 * then scaled up to whatever `width` the reel wants.
 *
 * Everything inside the frame therefore gets written in ordinary phone-sized
 * numbers (17px bubbles, 300px keyboard) exactly as in the Compose source, and
 * only this component knows about reel pixels.
 */
export const Phone: React.FC<{
  width: number;
  children: React.ReactNode;
  /** Screenshot-flash / press feedback, 1 = resting. */
  scale?: number;
  rotate?: number;
  /** Set false for the bare screen (used inside the keyboard's screenshot card). */
  chrome?: boolean;
  float?: boolean;
}> = ({
  width,
  children,
  scale = 1,
  rotate = 0,
  chrome = true,
  float = true,
}) => {
  const frame = useCurrentFrame();
  const s = width / SCREEN.width;
  const height = SCREEN.height * s;

  // A hair of movement so the device never reads as a flat PNG.
  const bob = float ? Math.sin(frame / 46) * 6 : 0;
  const tilt = float ? Math.sin(frame / 62) * 0.35 : 0;

  const bezel = chrome ? 12 * s : 0;
  const radius = 46 * s;

  return (
    <div
      style={{
        width: width + bezel * 2,
        height: height + bezel * 2,
        borderRadius: radius + bezel,
        padding: bezel,
        transform: `translateY(${bob}px) rotate(${rotate + tilt}deg) scale(${scale})`,
        background: chrome
          ? "linear-gradient(160deg, #3A4256 0%, #10141F 42%, #05070C 100%)"
          : "transparent",
        boxShadow: chrome
          ? "0 60px 120px rgba(0,0,0,0.65), 0 0 0 1px rgba(255,255,255,0.09) inset, 0 2px 0 rgba(255,255,255,0.14) inset"
          : "none",
        position: "relative",
      }}
    >
      <div
        style={{
          width,
          height,
          borderRadius: radius,
          overflow: "hidden",
          position: "relative",
          background: "#000",
        }}
      >
        {/* Author at 390x844, present at whatever the reel needs. */}
        <div
          style={{
            width: SCREEN.width,
            height: SCREEN.height,
            transform: `scale(${s})`,
            transformOrigin: "top left",
            position: "absolute",
            top: 0,
            left: 0,
            fontFamily: FONT,
          }}
        >
          {children}
        </div>
      </div>
    </div>
  );
};

/** Android status bar — sold separately so the chat screens can tint it. */
export const StatusBar: React.FC<{ dark?: boolean }> = ({ dark = false }) => {
  const color = dark ? "#F2F5FB" : "#0E1524";
  return (
    <div
      style={{
        height: 44,
        paddingLeft: 24,
        paddingRight: 22,
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        color,
        fontSize: 13,
        fontWeight: 600,
        letterSpacing: 0.2,
        flexShrink: 0,
      }}
    >
      <span>9:41</span>
      <div style={{ display: "flex", alignItems: "center", gap: 5 }}>
        <Signal color={color} />
        <Wifi color={color} />
        <Battery color={color} />
      </div>
    </div>
  );
};

const Signal: React.FC<{ color: string }> = ({ color }) => (
  <svg width="17" height="11" viewBox="0 0 17 11">
    {[0, 1, 2, 3].map((i) => (
      <rect
        key={i}
        x={i * 4.4}
        y={11 - (i + 1) * 2.6}
        width="3"
        height={(i + 1) * 2.6}
        rx="1"
        fill={color}
      />
    ))}
  </svg>
);

const Wifi: React.FC<{ color: string }> = ({ color }) => (
  <svg width="16" height="11" viewBox="0 0 16 11">
    <path
      d="M8 10.2 1 3.6a10 10 0 0 1 14 0Z"
      fill={color}
      opacity="0.95"
    />
  </svg>
);

const Battery: React.FC<{ color: string }> = ({ color }) => (
  <svg width="25" height="12" viewBox="0 0 25 12">
    <rect
      x="0.6"
      y="0.6"
      width="21"
      height="10.8"
      rx="3"
      fill="none"
      stroke={color}
      strokeOpacity="0.45"
    />
    <rect x="2.4" y="2.4" width="14" height="7.2" rx="1.8" fill={color} />
    <path d="M23 4.2v3.6a2 2 0 0 0 0-3.6Z" fill={color} opacity="0.45" />
  </svg>
);
