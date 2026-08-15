/**
 * Generates every sound the reels use, into `public/audio/`.
 *
 * These are synthesised from scratch rather than sampled, for one boring but
 * decisive reason: a reel that goes out with a library sample in it is a
 * licensing question, and we do not want to answer that question on a Friday.
 * Everything here is arithmetic, so it is ours.
 *
 *   node scripts/make-audio.mjs      (or: npm run audio)
 *
 * The output is committed, so a normal render needs no build step. Re-run this
 * only when changing the sound design.
 */

import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const SR = 44100;
const OUT = join(dirname(fileURLToPath(import.meta.url)), "..", "public", "audio");

// ---------------------------------------------------------------------------
// primitives
// ---------------------------------------------------------------------------

/** Deterministic noise. Renders must be reproducible; Math.random() is not. */
const makeRng = (seed = 12345) => {
  let s = seed >>> 0;
  return () => {
    s = (s * 1664525 + 1013904223) >>> 0;
    return (s / 0xffffffff) * 2 - 1;
  };
};

const buffer = (seconds) => new Float32Array(Math.ceil(seconds * SR));

/** Exponential decay, the shape almost every percussive envelope wants. */
const decay = (t, tau) => Math.exp(-t / tau);

/** Short attack so a transient doesn't click at the very first sample. */
const attack = (t, ms = 2) => Math.min(1, t / (ms / 1000));

/** Linear-in-octaves sweep, which is how pitch and filter cutoff are heard. */
const glide = (from, to, p) => from * Math.pow(to / from, Math.min(1, Math.max(0, p)));

/**
 * State-variable filter. One object per voice — the state is the filter, so
 * sharing one across two voices smears them together.
 */
const svf = () => {
  let low = 0;
  let band = 0;
  return (x, cutoff, q = 1.2) => {
    const f = 2 * Math.sin((Math.PI * Math.min(cutoff, SR * 0.45)) / SR);
    low += f * band;
    const high = x - low - (1 / q) * band;
    band += f * high;
    return { low, band, high };
  };
};

/** Soft clip. Keeps the transients from squaring off when layers stack up. */
const saturate = (x) => Math.tanh(x * 1.3) / Math.tanh(1.3);

const normalise = (buf, peak = 0.9) => {
  let max = 0;
  for (const v of buf) max = Math.max(max, Math.abs(v));
  if (max === 0) return buf;
  const g = peak / max;
  for (let i = 0; i < buf.length; i++) buf[i] *= g;
  return buf;
};

/** Fade the last few ms so the file cannot end on a non-zero sample. */
const fadeOut = (buf, ms = 8) => {
  const n = Math.min(buf.length, Math.round((ms / 1000) * SR));
  for (let i = 0; i < n; i++) buf[buf.length - 1 - i] *= i / n;
  return buf;
};

const writeWav = (name, buf) => {
  const data = Buffer.alloc(buf.length * 2);
  for (let i = 0; i < buf.length; i++) {
    const s = Math.max(-1, Math.min(1, buf[i]));
    data.writeInt16LE(Math.round(s * 32767), i * 2);
  }

  const header = Buffer.alloc(44);
  header.write("RIFF", 0);
  header.writeUInt32LE(36 + data.length, 4);
  header.write("WAVE", 8);
  header.write("fmt ", 12);
  header.writeUInt32LE(16, 16); // PCM chunk size
  header.writeUInt16LE(1, 20); // PCM
  header.writeUInt16LE(1, 22); // mono
  header.writeUInt32LE(SR, 24);
  header.writeUInt32LE(SR * 2, 28); // byte rate
  header.writeUInt16LE(2, 32); // block align
  header.writeUInt16LE(16, 34); // bits
  header.write("data", 36);
  header.writeUInt32LE(data.length, 40);

  const path = join(OUT, `${name}.wav`);
  writeFileSync(path, Buffer.concat([header, data]));
  console.log(`  ${name}.wav  ${(data.length / 1024).toFixed(0)} KB`);
};

// ---------------------------------------------------------------------------
// the sounds
// ---------------------------------------------------------------------------

/** Camera shutter: two mechanical clicks, the second softer. */
const shutter = () => {
  const buf = buffer(0.22);
  const rng = makeRng(7);
  const f1 = svf();
  const f2 = svf();

  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    const n = rng();

    // First click — the mirror.
    const a = decay(t, 0.007) * attack(t, 0.4);
    // Second click — the shutter closing behind it.
    const t2 = t - 0.055;
    const b = t2 > 0 ? decay(t2, 0.011) * 0.7 : 0;

    const body = f1(n, 3200, 1.1).band * (a + b) * 1.6;
    // A little metal ring on top so it reads as a camera, not a mouse.
    const ring = f2(n, 6400, 5).band * (a + b) * 0.5;

    buf[i] = saturate(body + ring);
  }

  return fadeOut(normalise(buf, 0.82));
};

/** Air moving: the keyboard rising, and the whip between takes. */
const whoosh = () => {
  const buf = buffer(0.55);
  const rng = makeRng(21);
  const f = svf();

  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    const p = t / 0.55;
    // Swell in, fall away — a flat noise burst reads as static, not motion.
    const amp = Math.sin(Math.PI * Math.pow(p, 0.7));
    const cutoff = glide(320, 4200, Math.pow(p, 0.8));
    buf[i] = saturate(f(rng(), cutoff, 2.4).band * amp * 1.4);
  }

  return fadeOut(normalise(buf, 0.55));
};

/** A pebble suggestion arriving. Pitched up, because things that appear rise. */
const pop = () => {
  const buf = buffer(0.2);
  const rng = makeRng(33);
  const f = svf();

  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    const freq = glide(420, 980, Math.min(1, t / 0.05));
    const tone = Math.sin(2 * Math.PI * freq * t) * decay(t, 0.038);
    // Tiny noise transient so it has an edge at low volumes.
    const click = f(rng(), 5200, 1).band * decay(t, 0.006) * 0.45;
    buf[i] = saturate((tone + click) * attack(t, 1));
  }

  return fadeOut(normalise(buf, 0.7));
};

/** A finger on glass. Deliberately dull — a bright tap sounds like a mistake. */
const tap = () => {
  const buf = buffer(0.12);
  const rng = makeRng(41);
  const f = svf();

  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    const body = f(rng(), 2600, 0.9).low * decay(t, 0.012);
    const thump = Math.sin(2 * Math.PI * 165 * t) * decay(t, 0.03) * 0.5;
    buf[i] = saturate((body * 1.4 + thump) * attack(t, 0.6));
  }

  return fadeOut(normalise(buf, 0.6));
};

/** The message leaving: a short rising swish into a bright confirmation ping. */
const send = () => {
  const buf = buffer(0.6);
  const rng = makeRng(55);
  const f = svf();

  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;

    const sweepP = Math.min(1, t / 0.22);
    const sweep =
      f(rng(), glide(600, 7000, sweepP), 2.2).band *
      Math.sin(Math.PI * sweepP) *
      0.9;

    // Ping lands as the swish tops out, not after it.
    const tp = t - 0.17;
    const ping =
      tp > 0
        ? (Math.sin(2 * Math.PI * 1568 * tp) * 0.7 +
            Math.sin(2 * Math.PI * 2350 * tp) * 0.3) *
          decay(tp, 0.13) *
          attack(tp, 1)
        : 0;

    buf[i] = saturate(sweep + ping);
  }

  return fadeOut(normalise(buf, 0.75));
};

/** Hearts. Bell partials, staggered, no fundamental — it should feel like air. */
const sparkle = () => {
  const buf = buffer(1.1);
  const partials = [
    { f: 2093, at: 0.0, d: 0.34 },
    { f: 2637, at: 0.07, d: 0.3 },
    { f: 3136, at: 0.15, d: 0.26 },
    { f: 4186, at: 0.24, d: 0.22 },
    { f: 3520, at: 0.35, d: 0.28 },
    { f: 2794, at: 0.46, d: 0.24 },
  ];

  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    let s = 0;
    for (const p of partials) {
      const tp = t - p.at;
      if (tp <= 0) continue;
      // Slight inharmonicity, or it sounds like a test tone.
      s +=
        (Math.sin(2 * Math.PI * p.f * tp) +
          Math.sin(2 * Math.PI * p.f * 2.76 * tp) * 0.22) *
        decay(tp, p.d) *
        attack(tp, 1) *
        0.4;
    }
    buf[i] = saturate(s);
  }

  return fadeOut(normalise(buf, 0.5), 40);
};

/** The end card landing. Sub drop plus a broadband transient. */
const impact = () => {
  const buf = buffer(1.6);
  const rng = makeRng(77);
  const f = svf();

  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;

    // Pitch falls away, which is what makes it read as weight rather than tone.
    const freq = glide(88, 36, Math.min(1, t / 0.35));
    const sub = Math.sin(2 * Math.PI * freq * t) * decay(t, 0.42) * attack(t, 3);

    const crack = f(rng(), 1400, 1.6).band * decay(t, 0.09) * 0.55;

    buf[i] = saturate(sub * 1.1 + crack);
  }

  return fadeOut(normalise(buf, 0.88), 60);
};

/** Tension into a beat. Used under the last caption before the end card. */
const riser = () => {
  const buf = buffer(1.6);
  const rng = makeRng(91);
  const f = svf();

  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;
    const p = t / 1.6;
    const cutoff = glide(380, 9000, Math.pow(p, 1.5));
    const amp = Math.pow(p, 2.2);
    // A slow tremolo that speeds up sells "something is about to happen".
    const trem = 0.75 + 0.25 * Math.sin(2 * Math.PI * (4 + p * 16) * t);
    buf[i] = saturate(f(rng(), cutoff, 3).band * amp * trem * 1.2);
  }

  return fadeOut(normalise(buf, 0.5), 30);
};

/**
 * The music bed: eight seconds at 100 BPM, written to loop seamlessly.
 *
 * It is deliberately plain — a drone, a sub pulse and a hat. This plays under
 * a trending track chosen in the Instagram composer, so anything with a melody
 * of its own would fight it. Ducked hard by the pulse so it never crowds the
 * captions.
 */
const bed = () => {
  const BPM = 100;
  const beat = 60 / BPM;
  const bars = 4;
  const length = beat * 4 * bars; // exactly 8 bars of pulse = 9.6s
  const buf = buffer(length);
  const rng = makeRng(2024);

  const droneFilter = svf();
  const hatFilter = svf();

  // A minor: root, fifth, minor third above. Ambiguous enough to sit under
  // anything the composer adds on top.
  const voices = [110.0, 164.81, 261.63];

  for (let i = 0; i < buf.length; i++) {
    const t = i / SR;

    // --- pulse ------------------------------------------------------------
    const beatPhase = (t % beat) / beat;
    const pulseEnv = decay(beatPhase * beat, 0.16);
    const sub =
      Math.sin(2 * Math.PI * glide(70, 45, Math.min(1, (beatPhase * beat) / 0.1)) * t) *
      pulseEnv *
      0.55;

    // --- drone ------------------------------------------------------------
    // Phases are locked to the loop length so the last sample meets the first.
    let raw = 0;
    for (const v of voices) {
      const cycles = Math.round(v * length) / length; // snap to a whole cycle
      // A few harmonics stands in for a saw without the aliasing.
      for (let h = 1; h <= 4; h++) {
        raw += Math.sin(2 * Math.PI * cycles * h * t) / (h * h);
      }
    }
    raw /= voices.length;

    const lfo = 0.5 + 0.5 * Math.sin((2 * Math.PI * t) / length); // one sweep per loop
    const drone = droneFilter(raw, 380 + lfo * 900, 1.1).low * 0.5;

    // Sidechain: the drone steps out of the way of every pulse.
    const ducked = drone * (1 - 0.45 * pulseEnv);

    // --- hat --------------------------------------------------------------
    const eighth = (t % (beat / 2)) / (beat / 2);
    const onOff = Math.floor(t / (beat / 2)) % 2 === 1; // offbeats only
    const hat = onOff
      ? hatFilter(rng(), 9000, 1).high * decay(eighth * (beat / 2), 0.014) * 0.16
      : 0;

    buf[i] = saturate(ducked + sub + hat) * 0.75;
  }

  // No fade — this loops, and a fade would punch a hole every 9.6 seconds.
  return normalise(buf, 0.62);
};

// ---------------------------------------------------------------------------

mkdirSync(OUT, { recursive: true });
console.log(`Writing to ${OUT}`);

writeWav("shutter", shutter());
writeWav("whoosh", whoosh());
writeWav("pop", pop());
writeWav("tap", tap());
writeWav("send", send());
writeWav("sparkle", sparkle());
writeWav("impact", impact());
writeWav("riser", riser());
writeWav("bed", bed());

console.log("Done.");
