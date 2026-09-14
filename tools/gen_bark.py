"""FocusPets 小狗叫声：合成一声短促、音量较小的「汪汪」WAV（22050Hz 16-bit mono）。

输出：app/src/main/res/raw/bark.wav
两个短音节（uF-wuf），带起音噪声与音高下滑，模拟狗叫。
"""
import math
import struct
import wave

SR = 22050


def bark_syllable(t0: float, t1: float, f0: float, f1: float, amp: float):
    """生成一段带音高下滑的 bark 音节，返回采样列表。"""
    n = int(SR * (t1 - t0))
    out = []
    dur = t1 - t0
    for i in range(n):
        t = i / SR
        lt = t - t0
        # 频率按指数从 f0 滑到 f1
        freq = f0 * math.exp(math.log(f1 / f0) * (lt / dur))
        # 振幅包络：快速起音 + 衰减
        env = math.exp(-7.0 * lt) * (1 - math.exp(-45 * lt))
        # 起音处叠加一点点噪声，模拟"h"的爆破感
        noise = (0.12 if lt < 0.012 else 0.0) * (1 - lt / dur)
        s = amp * env * math.sin(2 * math.pi * freq * t) + noise * math.sin(2 * math.pi * 1800 * t)
        out.append(max(-1.0, min(1.0, s)))
    return out


def main():
    data = []
    data += bark_syllable(0.00, 0.13, 520, 280, 0.32)  # 第一声：高→低
    data += [0.0] * int(SR * 0.05)                      # 短停顿
    data += bark_syllable(0.18, 0.32, 430, 240, 0.30)  # 第二声：略低

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
