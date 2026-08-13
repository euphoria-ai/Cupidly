import React from "react";
import { Composition } from "remotion";
import { REEL } from "./theme";
import {
  ScreenshotReel,
  SCREENSHOT_REEL_DURATION,
} from "./videos/ScreenshotReel";
import {
  DryTexterReel,
  DRY_TEXTER_REEL_DURATION,
} from "./videos/DryTexterReel";
import {
  HowItWorksReel,
  HOW_IT_WORKS_REEL_DURATION,
} from "./videos/HowItWorksReel";

/**
 * Three angles on the same product, all 1080x1920 / 30fps so they can be posted
 * as Reels, Stories or Shorts without re-cropping:
 *
 *  - ScreenshotReel  — the demo. What Hook does, in one take.
 *  - DryTexterReel   — the contrast. Why you'd want it.
 *  - HowItWorksReel  — the explainer. How to actually set it up.
 */
export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Composition
        id="ScreenshotReel"
        component={ScreenshotReel}
        durationInFrames={SCREENSHOT_REEL_DURATION}
        fps={REEL.fps}
        width={REEL.width}
        height={REEL.height}
      />
      <Composition
        id="DryTexterReel"
        component={DryTexterReel}
        durationInFrames={DRY_TEXTER_REEL_DURATION}
        fps={REEL.fps}
        width={REEL.width}
        height={REEL.height}
      />
      <Composition
        id="HowItWorksReel"
        component={HowItWorksReel}
        durationInFrames={HOW_IT_WORKS_REEL_DURATION}
        fps={REEL.fps}
        width={REEL.width}
        height={REEL.height}
      />
    </>
  );
};
