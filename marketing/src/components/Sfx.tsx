import React from "react";
import { Audio, Sequence, interpolate, staticFile } from "remotion";

/**
 * Sound for the reels. Every file in `public/audio/` is synthesised by
 * `scripts/make-audio.mjs` — nothing here is sampled, so there is no licence to
 * clear before a reel goes out.
 *
 * The design assumes the post will carry a trending track chosen in the
 * Instagram composer, mixed over the top. That means:
 *
 *  - the bed sits low and has no melody of its own, so it does not fight
 *    whatever gets laid over it;
 *  - the interface sounds carry the edit, and stay legible even if the viewer
 *    only ever hears them under someone else's song.
 */

export type SfxName =
  | "shutter"
  | "whoosh"
  | "pop"
  | "tap"
  | "send"
  | "sparkle"
  | "impact"
  | "riser";

/**
 * Per-sound trim, so call sites can think in "a bit louder than usual" rather
 * than re-balancing the whole mix every time one is added.
 */
const GAIN: Record<SfxName, number> = {
  shutter: 0.58,
  whoosh: 0.4,
  pop: 0.38,
  tap: 0.42,
  send: 0.52,
  sparkle: 0.42,
  // impact and send are the loudest moments in the reel and set the peak, so
  // they were left near where they were when the mix was lifted. Measured on
  // the finished file: peak about -4 dBFS, nothing clipped.
  impact: 0.62,
  riser: 0.38,
};

/** Frames each sound needs before the sequence cuts it off. */
const LENGTH: Record<SfxName, number> = {
  shutter: 10,
  whoosh: 20,
  pop: 8,
  tap: 6,
  send: 20,
  sparkle: 36,
  impact: 50,
  riser: 50,
};

/** One sound, fired on one frame. */
export const Sfx: React.FC<{
  name: SfxName;
  at: number;
  /** Multiplier on the sound's own gain. 1 = as balanced above. */
  volume?: number;
}> = ({ name, at, volume = 1 }) => (
  <Sequence
    from={at}
    durationInFrames={LENGTH[name]}
    name={`sfx: ${name}`}
    layout="none"
  >
    <Audio src={staticFile(`audio/${name}.wav`)} volume={GAIN[name] * volume} />
  </Sequence>
);

/** The same sound several times, e.g. three suggestions landing in turn. */
export const SfxRepeat: React.FC<{
  name: SfxName;
  at: number;
  every: number;
  times: number;
  volume?: number;
  /** Each repeat quieter than the last, so a run doesn't build up. */
  falloff?: number;
}> = ({ name, at, every, times, volume = 1, falloff = 0.88 }) => (
  <>
    {new Array(times).fill(0).map((_, i) => (
      <Sfx
        key={i}
        name={name}
        at={at + i * every}
        volume={volume * Math.pow(falloff, i)}
      />
    ))}
  </>
);

/**
 * The looping music bed, faded in at the top and out under the end card.
 *
 * `loopVolumeCurveBehavior="extend"` is what makes that fade work: the default
 * restarts the volume curve on every loop, so the fade-out would fire every
 * 9.6 seconds instead of once at the end.
 */
export const MusicBed: React.FC<{
  /** Composition length in frames. */
  over: number;
  volume?: number;
}> = ({ over, volume = 0.42 }) => (
  <Audio
    loop
    src={staticFile("audio/bed.wav")}
    loopVolumeCurveBehavior="extend"
    volume={(f) =>
      interpolate(
        f,
        [0, 18, over - 34, over - 2],
        [0, volume, volume, 0],
        { extrapolateLeft: "clamp", extrapolateRight: "clamp" }
      )
    }
  />
);
