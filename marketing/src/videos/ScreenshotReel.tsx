import React from "react";
import {
  AbsoluteFill,
  Sequence,
  interpolate,
  spring,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { CaptionBar, Eyebrow, HookTitle } from "../components/Captions";
import {
  HeartBurst,
  ScreenFlash,
  StatStamp,
  TapRipple,
  shutterScale,
} from "../components/Effects";
import { EndCard } from "../components/EndCard";
import { HookKeyboard } from "../components/HookKeyboard";
import { ChatThumb } from "../components/ChatThumb";
import { ChatMessage, IgChat } from "../components/IgChat";
import { Phone } from "../components/Phone";
import { MusicBed, Sfx, SfxRepeat } from "../components/Sfx";
import { PHONE_WIDTH, PhoneSlot, Stage } from "../components/Stage";
import { KEYBOARD_HEIGHT } from "../theme";

export const SCREENSHOT_REEL_DURATION = 480; // 16s at 30fps

/**
 * The hero reel: the whole product in one unbroken take.
 *
 * She asks something you have no answer to -> screenshot -> Hook reads the
 * thread -> three replies -> tap -> sent. No cuts inside the demo, because the
 * single most persuasive fact about Hook is how few steps it is, and a cut is
 * exactly where a viewer assumes a step was hidden.
 */

/** The practice conversation, straight out of `DemoChatStep.kt`. */
const THREAD: ChatMessage[] = [
  { text: "I'm scared you might distract me at the gym 😏", fromMe: true },
  { text: "You must see something you like… 👀", fromMe: false },
  {
    text: "Question is: can you handle it if I give it my full attention?",
    fromMe: true,
  },
  { text: "Do you think you can handle me?", fromMe: false },
];

/**
 * Kept short on purpose. The panel gives the transcript ~170px, so anything
 * longer than two lines each pushes the first suggestion off the top — and the
 * first suggestion is the one the reel taps.
 */
const REPLIES = [
  "Handle you? I'd need a spotter.",
  "One way to find out. Thursday?",
  "Bold of you to assume I'd be gentle.",
];

/** The one she gets. Index into REPLIES. */
const CHOSEN = 1;

const B = {
  herQuestion: 16,
  stuck: 54,
  shutter: 98,
  keyboardUp: 106,
  screenshotCard: 120,
  generateTap: 138,
  generating: 148,
  suggestions: 186,
  replyTap: 250,
  draftFill: 258,
  sendTap: 296,
  sent: 306,
  stat: 336,
  endCard: 384,
} as const;

export const ScreenshotReel: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const ease = (at: number, damping = 16) =>
    spring({ frame: frame - at, fps, config: { damping, stiffness: 170, mass: 0.7 } });

  // --- thread -------------------------------------------------------------
  const herReveal = ease(B.herQuestion, 13);
  const sentReveal = ease(B.sent, 13);

  const draftChars = Math.round(
    interpolate(frame, [B.draftFill, B.draftFill + 22], [0, REPLIES[CHOSEN].length], {
      extrapolateLeft: "clamp",
      extrapolateRight: "clamp",
    })
  );
  const draft = frame >= B.sent ? "" : REPLIES[CHOSEN].slice(0, draftChars);

  const messages: ChatMessage[] = [
    ...THREAD.slice(0, 3),
    { ...THREAD[3], reveal: herReveal },
    ...(frame >= B.sent
      ? [{ text: REPLIES[CHOSEN], fromMe: true, reveal: sentReveal }]
      : []),
  ];

  // --- keyboard -----------------------------------------------------------
  const kbUp = ease(B.keyboardUp, 18);
  const kbInset = kbUp * KEYBOARD_HEIGHT;

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
      i === CHOSEN
        ? interpolate(frame, [B.replyTap, B.replyTap + 5, B.replyTap + 12], [0, 1, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          })
        : 0,
  })).filter((s) => (s.reveal ?? 0) > 0.001);

  const generatePress = interpolate(
    frame,
    [B.generateTap, B.generateTap + 5, B.generateTap + 12],
    [0, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" }
  );

  const sendReady = interpolate(frame, [B.draftFill, B.draftFill + 8], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  // --- device -------------------------------------------------------------
  const shutter = shutterScale(frame, B.shutter);
  const phoneExit = interpolate(frame, [B.endCard - 18, B.endCard], [1, 0.72], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const phoneFade = interpolate(frame, [B.endCard - 18, B.endCard - 2], [1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill>
      <Stage over={SCREENSHOT_REEL_DURATION} to={1.08}>
        <PhoneSlot>
          <div style={{ opacity: phoneFade }}>
            <Phone width={PHONE_WIDTH} scale={shutter * phoneExit} float={false}>
              <div style={{ position: "relative", width: "100%", height: "100%" }}>
                <IgChat
                  messages={messages}
                  draft={draft}
                  keyboardInset={kbInset}
                  sendReady={sendReady}
                  typingCaret={frame >= B.draftFill && frame < B.sent}
                />

                {/* The keyboard rides up over the thread, as an IME does. */}
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
                    screenshotReveal={ease(B.screenshotCard, 14)}
                    screenshot={<ChatThumb messages={THREAD}  />}
                    generatePress={generatePress}
                    freeRemaining={3}
                  />
                </div>

                {/* Gestures live in the same 390x844 space as the UI. */}
                <TapRipple x={195} y={812} at={B.generateTap} size={90} />
                <TapRipple x={250} y={676} at={B.replyTap} size={78} />
                <TapRipple x={355} y={509} at={B.sendTap} size={62} />

                <ScreenFlash at={B.shutter} />
              </div>
            </Phone>
          </div>
        </PhoneSlot>

        <HeartBurst at={B.sent + 6} />
      </Stage>

      {/* ---------------- burned-in captions ---------------- */}

      <Sequence from={0} durationInFrames={B.stuck}>
        <Eyebrow text="POV" />
        <HookTitle
          text="she said this and you froze"
          accent={["froze"]}
          size={92}
        />
      </Sequence>

      <Sequence from={B.stuck} durationInFrames={B.shutter - B.stuck}>
        <CaptionBar text="Don't type. Screenshot." tone="hot" />
      </Sequence>

      <Sequence
        from={B.shutter}
        durationInFrames={B.suggestions - B.shutter}
      >
        <CaptionBar text="Hook reads the whole conversation" />
      </Sequence>

      <Sequence
        from={B.suggestions}
        durationInFrames={B.replyTap - B.suggestions}
      >
        <CaptionBar text="Three replies. Written for her." />
      </Sequence>

      <Sequence from={B.replyTap} durationInFrames={B.stat - B.replyTap}>
        <CaptionBar text="Tap. Send. Done." tone="hot" />
      </Sequence>

      <Sequence from={B.stat} durationInFrames={B.endCard - B.stat}>
        <AbsoluteFill
          style={{ alignItems: "center", justifyContent: "center" }}
        >
          <StatStamp value="4 sec" label="from stuck to sent" />
        </AbsoluteFill>
      </Sequence>

      <Sequence from={B.endCard}>
        <EndCard />
      </Sequence>

      {/* ---------------- sound ---------------- */}

      <MusicBed over={SCREENSHOT_REEL_DURATION} />

      <Sfx name="pop" at={B.herQuestion} volume={0.8} />
      <Sfx name="shutter" at={B.shutter} />
      <Sfx name="whoosh" at={B.keyboardUp} />
      <Sfx name="tap" at={B.generateTap} />
      <SfxRepeat name="pop" at={B.suggestions} every={7} times={REPLIES.length} />
      <Sfx name="tap" at={B.replyTap} />
      <Sfx name="tap" at={B.sendTap} volume={0.85} />
      <Sfx name="send" at={B.sent} />
      <Sfx name="sparkle" at={B.sent + 6} />
      <Sfx name="impact" at={B.stat} volume={0.7} />
      {/* Riser runs into the end card, so it resolves on the impact. */}
      <Sfx name="riser" at={B.endCard - 42} />
      <Sfx name="impact" at={B.endCard} />
    </AbsoluteFill>
  );
};

