import React from "react";

/**
 * The app mark, redrawn from `web/public/app-icon.svg` so the reel does not
 * depend on a binary asset that could go stale.
 */
export const HookLogo: React.FC<{ size: number; tile?: boolean }> = ({
  size,
  tile = true,
}) => (
  <svg width={size} height={size} viewBox="0 0 180 180">
    <defs>
      <linearGradient id="hook-tile" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stopColor="#ffffff" />
        <stop offset="1" stopColor="#e6e8ec" />
      </linearGradient>
    </defs>
    {tile && (
      <rect
        x="0"
        y="0"
        width="180"
        height="180"
        rx="42"
        fill="url(#hook-tile)"
      />
    )}
    <g
      fill="none"
      stroke={tile ? "#0a0a0a" : "#F2F5FB"}
      strokeWidth="11"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M116 64a44 44 0 1 0 12 30" />
      <path d="M64 116 122 58" />
      <path d="M96 58h26v26" />
    </g>
  </svg>
);
