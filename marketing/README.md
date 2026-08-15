# Hook — Instagram reels

Remotion project for Hook's paid and organic video. Everything renders at
**1080x1920 / 30fps**, so the same master posts as a Reel, a Story or a Short
without re-cropping.

```bash
npm install
npm run studio        # live editor at localhost:3000
npm run render:all    # all three reels into out/
```

## The three cuts

| Composition | Length | The job it does |
|---|---|---|
| `ScreenshotReel` | 16s | **The demo.** One unbroken take: she asks something you can't answer, screenshot, Hook reads the thread, three replies, tap, sent. The one to run as an ad. |
| `DryTexterReel` | 14s | **The contrast.** Same girl, same message, two keyboards — a seen receipt versus a reply that lands. The one that makes people want it. |
| `HowItWorksReel` | 17s | **The explainer.** Three steps, starting with the keyboard switch. The one that answers "I installed it and nothing happened". |

Render one at a time:

```bash
npm run render:screenshot
```

A cover frame for the grid:

```bash
npm run still:cover
```

## How it's put together

The UI inside the phone is **not** a screen recording and not a redraw from
memory — it is the app's own screens rebuilt in React from the Kotlin:

- `src/theme.ts` — colours copied from `app/.../ui/theme/Color.kt` and the fixed
  Instagram palette in `DemoChatStep.kt`. **If a colour changes in Kotlin,
  change it here.** A reel showing the wrong blue is a reel of a different app.
- `src/components/IgChat.tsx` — the stand-in dating-app chat from
  `DemoChatStep.kt`, down to the bubble radii and the `/ Profile` header.
- `src/components/HookKeyboard.tsx` — the keyboard surface from
  `KeyboardPanel.kt`: allowance chip, screenshot card, typing bubble, pebble
  suggestions, and the Generate / gear / backspace bar.
- `src/components/Phone.tsx` — the device. Everything inside is authored at a
  logical **390x844** and scaled up, so screen code uses ordinary phone numbers
  (17px bubbles, a 300px keyboard) exactly like the Compose source.
- `public/hook-logo-*.png` — copies of `branding/darkmode_filled.png` (light
  tile, dark mark) and `branding/darkmode_nobg.png` (white mark, no tile).
  Remotion can only serve assets out of `public/`, so these are copies, not
  references — **re-copy them when `branding/` changes.** Both are the
  dark-mode set because every reel plays on the dark backdrop; if that ever
  goes light, swap in the lightmode files rather than recolouring in code.

Because of that last point, tap targets are given in logical screen
coordinates and rendered *inside* the phone, so they cannot drift out of
alignment when the device is scaled or moved.

## Sound

Every sound is **synthesised from scratch** by `scripts/make-audio.mjs` — no
sampled or licensed material, so nothing needs clearing before a reel goes out.
The `.wav` files are committed, so a normal render needs no build step:

```bash
npm run audio     # only when changing the sound design
```

The mix assumes a trending track gets laid over the top in the Instagram
composer, which is where reach actually comes from. So the bed is low and has
no melody of its own (a drone, a sub pulse and a hat at 100 BPM, written to
loop seamlessly), and the interface sounds carry the edit — they stay legible
even if the viewer only ever hears them under someone else's song.

Measured on the finished files: peak about **−4.5 dBFS**, RMS about
**−22 dBFS**, nothing clipped. That is deliberately quieter than a
stand-alone social post would be mastered; if you ever post one of these
*without* adding music, raise `MusicBed`'s `volume` in
`src/components/Sfx.tsx` before rendering.

Balance lives in one place — the `GAIN` table in `src/components/Sfx.tsx`.
`impact` and `send` set the peak, so raise those last.

## Editing a reel

Each file in `src/videos/` opens with a `B` object — every beat in the reel and
the frame it fires on. Retiming is editing those numbers; nothing else needs to
move. Frames are 1/30s, so `+30` is one second later.

Captions are burned in on purpose: Instagram autoplays muted, so any word that
matters has to be on screen.

## Before posting

Captions, hashtags, first comments, alt text, and bio copy live in
[`POSTING.md`](./POSTING.md). Paste from there. Don't rewrite in the composer.

- **Audio.** These render with their own sound design, but still add a trending
  track in the Instagram composer — that is what the mix leaves room for, and
  it is where the reach is.
- **Suggestion copy.** The replies in `src/videos/*.tsx` are written, not
  generated. If you show output the model would not actually produce, the
  comments will say so.
- **Store claims.** The end card says "free to try", matching the free
  allowance the keyboard's `AllowanceChip` shows. Keep those two in step.
