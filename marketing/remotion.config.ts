import { Config } from "@remotion/cli/config";

// Instagram re-encodes hard, so we hand it the cleanest master we can:
// PNG frames (no intermediate JPEG artefacts on the flat UI colours) and a
// low CRF H.264 that still plays everywhere.
Config.setVideoImageFormat("png");
Config.setCodec("h264");
Config.setCrf(16);
Config.setPixelFormat("yuv420p");
Config.setOverwriteOutput(true);

// The phone mock is full of 1px borders and gradients — scaling at render time
// beats scaling in the encoder.
Config.setChromiumOpenGlRenderer("angle");
