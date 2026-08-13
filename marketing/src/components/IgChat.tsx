import React from "react";
import { ig } from "../theme";
import { StatusBar } from "./Phone";

export type ChatMessage = {
  text: string;
  fromMe: boolean;
  /** Frame this bubble lands on, relative to the sequence it lives in. */
  at?: number;
  /** 0..1 reveal, driven by the reel. Defaults to fully present. */
  reveal?: number;
};

/**
 * A pixel-for-pixel restatement of the stand-in dating-app chat from
 * `DemoChatStep.kt` — same header, same purple, same bubble radii.
 *
 * The point of the reel is the same as the point of that screen: this is the
 * app they will really be typing into, and Hook is the thing that appears
 * underneath it.
 */
export const IgChat: React.FC<{
  matchName?: string;
  messages: ChatMessage[];
  draft?: string;
  /** Pixels of the screen the keyboard is eating, so the thread can shrink. */
  keyboardInset?: number;
  /** The dark instruction pill from the onboarding demo. */
  coach?: string | null;
  /** 0..1 — send button lit and full colour. */
  sendReady?: number;
  typingCaret?: boolean;
  /** The "Seen 4h ago" line under the last outgoing bubble. */
  seenNote?: string | null;
}> = ({
  matchName = "Jessica",
  messages,
  draft = "",
  keyboardInset = 0,
  coach = null,
  sendReady = 0,
  typingCaret = false,
  seenNote = null,
}) => {
  return (
    <div
      style={{
        width: "100%",
        height: "100%",
        background: ig.white,
        display: "flex",
        flexDirection: "column",
        overflow: "hidden",
      }}
    >
      <StatusBar />
      <ChatHeader matchName={matchName} />

      <div
        style={{
          flex: 1,
          minHeight: 0,
          padding: "0 12px",
          display: "flex",
          flexDirection: "column",
          justifyContent: "flex-end",
          gap: 10,
          paddingBottom: 6,
        }}
      >
        {messages.map((m, i) => (
          <Bubble key={i} message={m} matchName={matchName} />
        ))}
        {seenNote ? (
          <span
            style={{
              alignSelf: "flex-end",
              fontSize: 12,
              color: ig.grayText,
              marginTop: -4,
              marginRight: 4,
            }}
          >
            {seenNote}
          </span>
        ) : null}
      </div>

      {coach ? <Coach text={coach} /> : null}

      <Composer
        draft={draft}
        sendReady={sendReady}
        typingCaret={typingCaret}
      />

      {/* The keyboard is drawn by the caller; this is just the space it takes. */}
      <div style={{ height: keyboardInset, flexShrink: 0 }} />
    </div>
  );
};

const ChatHeader: React.FC<{ matchName: string }> = ({ matchName }) => (
  <div style={{ flexShrink: 0 }}>
    <div
      style={{
        display: "flex",
        alignItems: "center",
        padding: "12px 12px",
        gap: 12,
      }}
    >
      <svg width="26" height="26" viewBox="0 0 24 24" fill="none">
        <path
          d="M19 12H5m0 0 6-6m-6 6 6 6"
          stroke="#000"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
      <span style={{ fontSize: 22, fontWeight: 700, color: "#000", flex: 1 }}>
        {matchName}
      </span>
      <svg width="26" height="26" viewBox="0 0 24 24" fill="#000">
        <circle cx="5" cy="12" r="1.9" />
        <circle cx="12" cy="12" r="1.9" />
        <circle cx="19" cy="12" r="1.9" />
      </svg>
    </div>

    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        gap: 0,
      }}
    >
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
        }}
      >
        <span style={{ fontSize: 17, fontWeight: 600, color: ig.purple }}>
          Chat
        </span>
        <div
          style={{
            width: 96,
            height: 2,
            marginTop: 6,
            background: ig.purple,
          }}
        />
      </div>
      <span
        style={{
          fontSize: 17,
          color: ig.grayText,
          padding: "4px 28px",
        }}
      >
        /
      </span>
      <span style={{ fontSize: 17, color: ig.grayText }}>Profile</span>
    </div>

    <div style={{ height: 10 }} />
    <div style={{ height: 1, background: ig.divider }} />
    <div style={{ height: 10 }} />
  </div>
);

const Bubble: React.FC<{ message: ChatMessage; matchName: string }> = ({
  message,
  matchName,
}) => {
  const r = message.reveal ?? 1;
  if (r <= 0) return null;

  return (
    <div
      style={{
        display: "flex",
        width: "100%",
        justifyContent: message.fromMe ? "flex-end" : "flex-start",
        alignItems: "flex-end",
        gap: 8,
        opacity: r,
        // Bubbles arrive from the side they belong to, the way a real thread
        // animates them in.
        transform: `translate(${(1 - r) * (message.fromMe ? 26 : -26)}px, ${(1 - r) * 12}px) scale(${0.94 + r * 0.06})`,
        transformOrigin: message.fromMe ? "bottom right" : "bottom left",
      }}
    >
      {!message.fromMe && (
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 16,
            background: `${ig.purple}2E`,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: 15,
            fontWeight: 700,
            color: ig.purple,
            flexShrink: 0,
          }}
        >
          {matchName.slice(0, 1)}
        </div>
      )}
      <div
        style={{
          maxWidth: 280,
          borderRadius: 20,
          padding: "12px 16px",
          fontSize: 17,
          lineHeight: "23px",
          background: message.fromMe ? ig.purple : ig.incoming,
          color: message.fromMe ? "#fff" : "#000",
        }}
      >
        {message.text}
      </div>
    </div>
  );
};

const Coach: React.FC<{ text: string }> = ({ text }) => (
  <div style={{ padding: "8px 12px", flexShrink: 0 }}>
    <div
      style={{
        borderRadius: 22,
        background: ig.coach,
        color: "#fff",
        fontSize: 15,
        fontWeight: 600,
        padding: "14px 20px",
      }}
    >
      {text}
    </div>
  </div>
);

const Composer: React.FC<{
  draft: string;
  sendReady: number;
  typingCaret: boolean;
}> = ({ draft, sendReady, typingCaret }) => {
  const ready = sendReady > 0;
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        gap: 10,
        padding: "0 12px 12px",
        flexShrink: 0,
      }}
    >
      <div
        style={{
          flex: 1,
          minWidth: 0,
          borderRadius: 24,
          border: `1px solid ${ig.divider}`,
          padding: "14px 18px",
          fontSize: 17,
          color: draft ? "#000" : ig.grayText,
          whiteSpace: "nowrap",
          overflow: "hidden",
          textOverflow: "ellipsis",
        }}
      >
        {draft || "Send a message"}
        {typingCaret && (
          <span
            style={{
              display: "inline-block",
              width: 2,
              height: 18,
              marginLeft: 2,
              verticalAlign: "-3px",
              background: ig.purple,
            }}
          />
        )}
      </div>
      <div
        style={{
          width: 46,
          height: 46,
          borderRadius: 23,
          flexShrink: 0,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: ready
            ? blend(ig.incoming, ig.purple, sendReady)
            : ig.incoming,
          transform: `scale(${1 + sendReady * 0.06})`,
        }}
      >
        <svg width="20" height="20" viewBox="0 0 24 24">
          <path
            d="M3.4 20.4 21 12 3.4 3.6 3.4 10.2 15.6 12 3.4 13.8Z"
            fill={ready ? "#fff" : ig.grayText}
          />
        </svg>
      </div>
    </div>
  );
};

/** Straight-line mix in sRGB. Good enough for a 6-frame button tint. */
const blend = (a: string, b: string, t: number) => {
  const p = (h: string) => [1, 3, 5].map((i) => parseInt(h.substr(i, 2), 16));
  const [r1, g1, b1] = p(a);
  const [r2, g2, b2] = p(b);
  const m = (x: number, y: number) => Math.round(x + (y - x) * t);
  return `rgb(${m(r1, r2)}, ${m(g1, g2)}, ${m(b1, b2)})`;
};
