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
import { HeartBurst, TapRipple } from "../components/Effects";
import { EndCard } from "../components/EndCard";
import { HookKeyboard } from "../components/HookKeyboard";
import { ChatThumb } from "../components/ChatThumb";
import { ChatMessage, IgChat } from "../components/IgChat";
import { Phone } from "../components/Phone";
import { PHONE_WIDTH, PhoneSlot, Stage } from "../components/Stage";
import { KEYBOARD_HEIGHT, pebble } from "../theme";
import { DISPLAY } from "../fonts";

export const DRY_TEXTER_REEL_DURATION = 420; // 14s at 30fps

/**
 * The comparison reel: same girl, same message, two different keyboards.
 *
 * Take one is the reply the viewer actually sent last week, and the seen
 * receipt that followed it. Take two is Hook. The cut between them is a hard
 * whip, not a dissolve — the joke is the contrast, and a dissolve softens it.
 */

/**
 * Enough thread to fill the screen. A single message floating above the
 * composer reads as a mock-up; three reads as a conversation.
 */
const LEAD_IN: ChatMessage[] = [
  { text: "your gym story was a lot", fromMe: false },
  { text: "I contain multitudes", fromMe: true },
];

const HER = "wyd this weekend? 👀";

const DRY_REPLY = "haha nothing much wbu";

const HOOK_REPLY = "Something you'd have to be there for.";

const HER_COMEBACK = "ok that was smooth 😳 I'm free Saturday";

const B = {
  title: 0,
  dryReply: 34,
  seen: 66,
  verdict: 96,
  cut: 132,
  keyboardUp: 142,
  suggestions: 168,
  replyTap: 214,
  sent: 228,
  comeback: 262,
  punchline: 296,
  endCard: 336,
} as const;

export const DryTexterReel: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const ease = (at: number, damping = 15) =>
    spring({ frame: frame - at, fps, config: { damping, stiffness: 175, mass: 0.7 } });

  const afterCut = frame >= B.cut;

  // The whip: everything slides out left, the new take slides in from right.
  const whip = interpolate(frame, [B.cut - 5, B.cut + 6], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const whipX = Math.sin(whip * Math.PI) * -260;
  const whipBlur = Math.sin(whip * Math.PI) * 14;

  // --- take one: the dry reply -------------------------------------------
  const beforeMessages: ChatMessage[] = [
    ...LEAD_IN,
    { text: HER, fromMe: false },
    { text: DRY_REPLY, fromMe: true, reveal: ease(B.dryReply, 13) },
  ];

  // --- take two: the same opening, answered by Hook ----------------------
  const kbUp = afterCut ? ease(B.keyboardUp, 18) : 0;
  const sentReveal = ease(B.sent, 13);
  const comebackReveal = ease(B.comeback, 12);

  const afterMessages: ChatMessage[] = [
    ...LEAD_IN,
    { text: HER, fromMe: false },
    ...(frame >= B.sent
      ? [{ text: HOOK_REPLY, fromMe: true, reveal: sentReveal }]
      : []),
    ...(frame >= B.comeback
      ? [{ text: HER_COMEBACK, fromMe: false, reveal: comebackReveal }]
      : []),
  ];

  const suggestions = [
    HOOK_REPLY,
    "Depends. Are you in the plan?",
    "Free, allegedly. Convince me.",
  ]
    .map((text, i) => ({
      text,
      reveal: afterCut ? ease(B.suggestions + i * 7, 12) : 0,
      press:
        i === 0
          ? interpolate(frame, [B.replyTap, B.replyTap + 5, B.replyTap + 12], [0, 1, 0], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            })
          : 0,
    }))
    .filter((s) => s.reveal > 0.001);

  const phoneFade = interpolate(frame, [B.endCard - 18, B.endCard - 2], [1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill>
      <Stage over={DRY_TEXTER_REEL_DURATION} to={1.06}>
        <PhoneSlot>
          <div
            style={{
              opacity: phoneFade,
              transform: `translateX(${whipX}px)`,
              filter: whipBlur > 0.4 ? `blur(${whipBlur}px)` : "none",
            }}
          >
            <Phone width={PHONE_WIDTH} float={false}>
              <div style={{ position: "relative", width: "100%", height: "100%" }}>
                {afterCut ? (
                  <>
                    <IgChat
                      matchName="Maya"
                      messages={afterMessages}
                      keyboardInset={kbUp * KEYBOARD_HEIGHT}
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
                        status={
                          frame >= B.suggestions ? "suggestions" : "generating"
                        }
                        suggestions={suggestions}
                        screenshotReveal={ease(B.keyboardUp + 8, 14)}
                        screenshot={
                          <ChatThumb
                            matchName="Maya"
                            messages={[...LEAD_IN, { text: HER, fromMe: false }]}
                            
                          />
                        }
                      />
                    </div>
                    <TapRipple x={250} y={676} at={B.replyTap} size={78} />
                  </>
                ) : (
                  <IgChat
                    matchName="Maya"
                    messages={beforeMessages}
                    seenNote={frame >= B.seen ? "Seen 4h ago" : null}
                  />
                )}
              </div>
            </Phone>
          </div>
        </PhoneSlot>

        {/* Which take we are watching, stamped in the corner. */}
        <TakeLabel
          label={afterCut ? "WITH HOOK" : "WITHOUT HOOK"}
          tone={afterCut ? "good" : "bad"}
        />

        <HeartBurst at={B.comeback + 4} count={10} />
      </Stage>

      <Sequence from={B.title} durationInFrames={B.verdict}>
        <HookTitle
          text="this is why she stopped replying"
          accent={["stopped", "replying"]}
          size={84}
        />
      </Sequence>

      <Sequence from={B.verdict} durationInFrames={B.cut - B.verdict}>
        <CaptionBar text="Four hours. Nothing." tone="hot" />
      </Sequence>

      <Sequence from={B.cut} durationInFrames={B.replyTap - B.cut}>
        <CaptionBar text="Same message. Hook answers it." />
      </Sequence>

      <Sequence from={B.replyTap} durationInFrames={B.punchline - B.replyTap}>
        <CaptionBar text="One tap." />
      </Sequence>

      <Sequence from={B.punchline} durationInFrames={B.endCard - B.punchline}>
        <CaptionBar text="Same girl. Different keyboard." tone="hot" size={56} />
      </Sequence>

      <Sequence from={B.endCard}>
        <EndCard cta="Download Hook — free to try" />
      </Sequence>
    </AbsoluteFill>
  );
};

const TakeLabel: React.FC<{ label: string; tone: "good" | "bad" }> = ({
  label,
  tone,
}) => (
  <div
    style={{
      position: "absolute",
      top: 286,
      left: 0,
      right: 0,
      display: "flex",
      justifyContent: "center",
      fontFamily: DISPLAY,
    }}
  >
    <span
      style={{
        fontSize: 32,
        fontWeight: 800,
        letterSpacing: 5,
        padding: "10px 24px",
        borderRadius: 14,
        color: tone === "good" ? "#fff" : "#FFD9E4",
        background:
          tone === "good"
            ? `linear-gradient(180deg, ${pebble.bubbleTop}, ${pebble.bubbleBottom})`
            : "rgba(229,72,77,0.85)",
        boxShadow: "0 14px 40px rgba(0,0,0,0.45)",
      }}
    >
      {label}
    </span>
  </div>
);
