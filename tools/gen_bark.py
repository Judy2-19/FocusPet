""" FocusPets bark generator: a short quiet 22050 Hz, 16-bit mono WAV at app/src/main/res/raw/bark.wav. Two short syllables (uF-wuf) with attack noise and falling pitch mimic a bark."""
import math
import struct
import wave

SR = 22050


def bark_syllable(t0: float, t1: float, f0: float, f1: float, amp: float):
    """ Generate one falling-pitch bark syllable and return its samples."""
    n = int(SR * (t1 - t0))
    out = []
    dur = t1 - t0
    for i in range(n):
        t = i / SR
        lt = t - t0
        # Slide frequency exponentially from f0 to f1.
        freq = f0 * math.exp(math.log(f1 / f0) * (lt / dur))
        # Amplitude envelope: fast attack and decay.
        env = math.exp(-7.0 * lt) * (1 - math.exp(-45 * lt))
        # Add a little attack noise to mimic an h-like burst.
        noise = (0.12 if lt < 0.012 else 0.0) * (1 - lt / dur)
        s = amp * env * math.sin(2 * math.pi * freq * t) + noise * math.sin(2 * math.pi * 1800 * t)
        out.append(max(-1.0, min(1.0, s)))
    return out


def main():
    data = []
    data += bark_syllable(0.00, 0.13, 520, 280, 0.32)  # First bark: high to low.
    data += [0.0] * int(SR * 0.05)                      # Short pause.
    data += bark_syllable(0.18, 0.32, 430, 240, 0.30)  # Second bark: slightly lower.

    raw = bytearray()
    for s in data:
        val = int(max(-1.0, min(1.0, s)) * 32767)
        raw.extend(struct.pack("<h", val))

    out_path = "app/src/main/res/raw/bark.wav"
    with wave.open(out_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(raw)
    print(f"OK: {out_path} ({len(raw)} bytes, {len(data) / SR:.2f}s)")


if __name__ == "__main__":
    main()
