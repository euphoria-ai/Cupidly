import React from "react";
import { SCREEN } from "../theme";
import { ChatMessage, IgChat } from "./IgChat";

/**
 * The contents of the keyboard's screenshot attachment card: the chat, shrunk
 * to card width and cropped.
 *
 * `ScreenshotCard` in the app anchors its crop to the top so the start of the
 * conversation shows. A literal top crop here would be the status bar and the
 * header and nothing else, because the card is 150px of an 844px screen — so
 * this crops from `focusY`, the first line of the actual thread.
 */
export const ChatThumb: React.FC<{
  matchName?: string;
  messages: ChatMessage[];
  /** Logical y of the screen that should sit at the top of the card. */
  focusY?: number;
  width?: number;
  height?: number;
  // 480 puts the bottom of the card just above the composer, so the card is
  // all conversation and no "Send a message" placeholder.
}> = ({ matchName, messages, focusY = 480, width = 210, height = 150 }) => {
  const s = width / SCREEN.width;
  return (
    <div style={{ width, height, overflow: "hidden", position: "relative" }}>
      <div
        style={{
          position: "absolute",
          top: -focusY * s,
          left: 0,
          width: SCREEN.width,
          height: SCREEN.height,
          transform: `scale(${s})`,
          transformOrigin: "top left",
        }}
      >
        <IgChat matchName={matchName} messages={messages} />
      </div>
    </div>
  );
};
