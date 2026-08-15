import React from "react";
import {
  AbsoluteFill,
  Sequence,
  interpolate,
  spring,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { CaptionBar, HookTitle } from "../components/Captions";
import { ScreenFlash, TapRipple, shutterScale } from "../components/Effects";
import { EndCard } from "../components/EndCard";
import { HookKeyboard } from "../components/HookKeyboard";
import { ChatThumb } from "../components/ChatThumb";
import { ChatMessage, IgChat } from "../components/IgChat";
import { Phone } from "../components/Phone";
import { MusicBed, Sfx, SfxRepeat } from "../components/Sfx";
import { PHONE_WIDTH, PhoneSlot, Stage } from "../components/Stage";
import { DISPLAY } from "../fonts";
import { KEYBOARD_HEIGHT, ig, pebble } from "../theme";

export const HOW_IT_WORKS_REEL_DURATION = 510; // 17s at 30fps

/**
 * The explainer. This is the one people send to a friend, and the one that
 * answers the review that says "I installed it and nothing happened".
 *
 * Step one is the keyboard switch on purpose — it is the step the onboarding
 * had to build a whole scrim around, so it is the step the ad should teach.
 */

const THREAD: ChatMessage[] = [
  { text: "you've been typing for like a minute", fromMe: false },
  { text: "quality takes time", fromMe: true },
  { text: "so are you actually going to ask me out or", fromMe: false },
];

const REPLIES = [
  "I was building suspense. Working?",
  "Friday. Bad lighting, good food.",
  "Asking implies you might say no.",
];

const STEPS = [
  { n: 1, title: "Add Hook as a keyboard", from: 60, to: 190 },
  { n: 2, title: "Screenshot the chat", from: 190, to: 320 },
  { n: 3, title: "Tap the reply you like", from: 320, to: 420 },
] as const;

const B = {
  title: 0,
  step1: 60,
  pickerTap: 132,
  step2: 190,
  shutter: 236,
  keyboardUp: 246,
  generating: 262,
  step3: 320,
  suggestions: 326,
  replyTap: 376,
  endCard: 424,
} as const;

export const HowItWorksReel: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const ease = (at: number, damping = 15) =>
    spring({ frame: frame - at, fps, config: { damping, stiffness: 175, mass: 0.7 } });

  const scrim = interpolate(frame, [B.step1, B.step1 + 10, B.step2 - 14, B.step2], [0, 1, 1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  const kbUp = frame >= B.keyboardUp ? ease(B.keyboardUp, 18) : 0;

  const status: "empty" | "generating" | "suggestions" =
    frame >= B.suggestions
      ? "suggestions"
      : frame >= B.generating
        ? "generating"
        : "empty";

  const suggestions = REPLIES.map((text, i) => ({
    text,
    reveal: ease(B.suggestions + i * 7, 12),
    press:
      i === 1
        ? interpolate(frame, [B.replyTap, B.replyTap + 5, B.replyTap + 12], [0, 1, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          })
        : 0,
  })).filter((s) => s.reveal > 0.001);

  const phoneFade = interpolate(frame, [B.endCard - 18, B.endCard - 2], [1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill>
      <Stage over={HOW_IT_WORKS_REEL_DURATION} to={1.05}>
        <PhoneSlot>
          <div style={{ opacity: phoneFade }}>
            <Phone
              width={PHONE_WIDTH}
              scale={shutterScale(frame, B.shutter)}
              float={false}
            >
              <div style={{ position: "relative", width: "100%", height: "100%" }}>
                <IgChat
                  matchName="Priya"
                  messages={THREAD}
                  keyboardInset={kbUp * KEYBOARD_HEIGHT}
                  coach={
                    frame >= B.step2 && frame < B.keyboardUp
                      ? "Take a screenshot (and save it)"
                      : frame >= B.suggestions && frame < B.replyTap
                        ? "Choose a message to send!"
                        : null
                  }
                />

                <div
                  style={{
                    position: "absolute",
                    left: 0,
                    right: 0,
                    bottom: 0,
                    transform: `translateY(${(1 - kbUp) * KEYBOARD_HEIGHT}px)`,
                  }}
                >
                  <HookKeyboard
                    status={status}
                    suggestions={suggestions}
                    screenshotReveal={ease(B.keyboardUp + 10, 14)}
                    screenshot={
                      <ChatThumb
                        matchName="Priya"
                        messages={THREAD}
                        
                      />
                    }
                  />
                </div>

                {scrim > 0.01 && <SwitchKeyboardScrim opacity={scrim} />}

                {/* Centred on "Show keyboard chooser", not the 🌐 panel. */}
                <TapRipple x={195} y={596} at={B.pickerTap} size={92} />
                <TapRipple x={250} y={676} at={B.replyTap} size={78} />

                <ScreenFlash at={B.shutter} />
              </div>
            </Phone>
          </div>
        </PhoneSlot>

        {/* The step rail — three chips, the live one lit. */}
        {frame >= B.step1 && frame < B.endCard && <StepRail frame={frame} />}
      </Stage>

      <Sequence from={B.title} durationInFrames={B.step1}>
        <HookTitle
          text="how to never be dry again"
          accent={["never"]}
          size={92}
        />
      </Sequence>

      {STEPS.map((s) => (
        <Sequence key={s.n} from={s.from} durationInFrames={s.to - s.from}>
          <CaptionBar text={`${s.n}. ${s.title}`} />
        </Sequence>
      ))}

      <Sequence from={420} durationInFrames={B.endCard - 420}>
        <CaptionBar text="That's the whole app." tone="hot" />
      </Sequence>

      <Sequence from={B.endCard}>
        <EndCard cta="Get Hook free on Google Play" />
      </Sequence>

      {/* ---------------- sound ---------------- */}

      <MusicBed over={HOW_IT_WORKS_REEL_DURATION} />

      {/* One pop per step chip lighting up — the spine of the explainer. */}
      {STEPS.map((s) => (
        <Sfx key={s.n} name="pop" at={s.from} volume={0.9} />
      ))}

      <Sfx name="tap" at={B.pickerTap} />
      <Sfx name="shutter" at={B.shutter} />
      <Sfx name="whoosh" at={B.keyboardUp} />
      <SfxRepeat name="pop" at={B.suggestions} every={7} times={REPLIES.length} />
      <Sfx name="tap" at={B.replyTap} />
      <Sfx name="send" at={B.replyTap + 8} />
      <Sfx name="riser" at={B.endCard - 42} />
      <Sfx name="impact" at={B.endCard} />
    </AbsoluteFill>
  );
};

/**
 * The onboarding's switch-keyboard scrim, reproduced from `DemoChatStep.kt`.
 * People recognise the screen they are about to see, which is the point.
 */
const SwitchKeyboardScrim: React.FC<{ opacity: number }> = ({ opacity }) => (
  <div
    style={{
      position: "absolute",
      inset: 0,
      background: "rgba(0,0,0,0.62)",
      opacity,
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      padding: "0 32px",
    }}
  >
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        textAlign: "center",
        width: "100%",
      }}
    >
      <span style={{ fontSize: 24, fontWeight: 700, color: "#fff" }}>
        Switch to the Hook keyboard
      </span>
      <span
        style={{
          fontSize: 15,
          marginTop: 12,
          color: "rgba(255,255,255,0.8)",
          lineHeight: "21px",
        }}
      >
        Tap the button below and pick Hook from the list. You can also tap and
        hold the 🌐 key or the space bar on your keyboard.
      </span>

      <div
        style={{
          marginTop: 20,
          width: "100%",
          height: 180,
          borderRadius: 20,
          background: "rgba(255,255,255,0.12)",
          border: "1px solid rgba(255,255,255,0.25)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: 44,
        }}
      >
        🌐
      </div>

      <span
        style={{
          marginTop: 20,
          fontSize: 16,
          fontWeight: 600,
          color: ig.coach,
          background: "#fff",
          borderRadius: 18,
          padding: "14px 28px",
        }}
      >
        Show keyboard chooser
      </span>
    </div>
  </div>
);

const StepRail: React.FC<{ frame: number }> = ({ frame }) => (
  <div
    style={{
      position: "absolute",
      top: 250,
      left: 0,
      right: 0,
      display: "flex",
      justifyContent: "center",
      gap: 18,
      fontFamily: DISPLAY,
    }}
  >
    {STEPS.map((s) => {
      const live = frame >= s.from && frame < s.to;
      const done = frame >= s.to;
      return (
        <div
          key={s.n}
          style={{
            width: 66,
            height: 66,
            borderRadius: 24,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: 34,
            fontWeight: 800,
            color: live || done ? "#fff" : "rgba(242,245,251,0.45)",
            background:
              live || done
                ? `linear-gradient(180deg, ${pebble.bubbleTop}, ${pebble.bubbleBottom})`
                : "rgba(255,255,255,0.08)",
            border: live
              ? "3px solid rgba(255,255,255,0.85)"
              : "3px solid transparent",
            boxShadow: live ? `0 0 44px ${pebble.blue}AA` : "none",
            transform: `scale(${live ? 1.1 : 1})`,
          }}
        >
          {done ? "✓" : s.n}
        </div>
      );
    })}
  </div>
);
