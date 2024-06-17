package allen.town.podcast.core.util.playback;

import static com.google.android.exoplayer2.C.*;

import com.google.android.exoplayer2.audio.AudioProcessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DownMixAudioProcessor extends BaseAudioProcessor{

    /* renamed from: j */
    public int f4373j = -1;

    /* renamed from: k */
    public int f4374k = -1;

    /* renamed from: l */
    public int f4375l = 0;

    /* renamed from: m */
    public ByteBuffer f4376m = AudioProcessor.EMPTY_BUFFER;

    /* renamed from: n */
    public boolean f4377n;

    @Override
    public AudioFormat mo21659h(AudioFormat aVar) throws UnhandledAudioFormatException {
        int i;
        if (aVar == null || !((i = aVar.encoding) == ENCODING_PCM_8BIT || i == ENCODING_PCM_16BIT || i == ENCODING_PCM_24BIT || i == ENCODING_PCM_32BIT)) {
            throw new AudioProcessor.UnhandledAudioFormatException(aVar);
        }
        this.f4373j = aVar.sampleRate;
        this.f4374k = aVar.channelCount;
        this.f4375l = i;
        return aVar;

    }

    public void setEnable(boolean z) {
        this.f4377n = z;
        flush();
    }

    public void mo21668k() {
        this.f4373j = -1;
        this.f4374k = -1;
        this.f4375l = 0;
        this.f4376m = AudioProcessor.EMPTY_BUFFER;
    }

    /* renamed from: n */
    public final void m25982n(ByteBuffer byteBuffer) {
        int remaining = byteBuffer.remaining();
        if (remaining > 0) {
            m21670l(remaining).put(byteBuffer).flip();
        }
    }

    public static void m25983m(ByteBuffer byteBuffer, int i, int i2) {
        while (true) {
            int i3 = i + 3;
            if (i3 < i2) {
                int i4 = i + 1;
                int i5 = i + 2;
                short s = (short) ((((short) ((byteBuffer.get(i) & 255) | (byteBuffer.get(i4) << 8))) * 0.5d) + (((short) ((byteBuffer.get(i5) & 255) | (byteBuffer.get(i3) << 8))) * 0.5d));
                byte b = (byte) (s & 255);
                byteBuffer.put(i, b);
                byte b2 = (byte) (s >> 8);
                byteBuffer.put(i4, b2);
                byteBuffer.put(i5, b);
                byteBuffer.put(i3, b2);
                i += 4;
            } else {
                return;
            }
        }
    }

    @Override
    public void queueInput(ByteBuffer byteBuffer) {
        int i;
        if (this.f4375l == 0 || !this.f4377n || this.f4374k != 2 || !isActive()) {
            m25982n(byteBuffer);
            return;
        }
        try {
            int position = byteBuffer.position();
            int limit = byteBuffer.limit();
            int i2 = limit - position;
            int i3 = this.f4375l;
            if (i3 == 2) {
                i = i2;
            } else if (i3 == 3) {
                i = i2 * 2;
            } else if (i3 == ENCODING_PCM_24BIT) {
                i = (i2 / 3) * 2;
            } else if (i3 == ENCODING_PCM_32BIT) {
                i = i2 / 2;
            } else {
                throw new IllegalStateException();
            }
            if (this.f4376m.capacity() < i) {
                this.f4376m = ByteBuffer.allocateDirect(i).order(ByteOrder.nativeOrder());
            } else {
                this.f4376m.clear();
            }
            int i4 = this.f4375l;
            if (i4 == 2) {
                for (int i5 = position; i5 < limit; i5++) {
                    this.f4376m.put(byteBuffer.get(i5));
                }
            } else if (i4 == 3) {
                for (int i6 = position; i6 < limit; i6++) {
                    this.f4376m.put((byte) 0);
                    this.f4376m.put((byte) ((byteBuffer.get(i6) & 255) - 128));
                }
            } else if (i4 == ENCODING_PCM_24BIT) {
                for (int i7 = position; i7 < limit; i7 += 3) {
                    this.f4376m.put(byteBuffer.get(i7 + 1));
                    this.f4376m.put(byteBuffer.get(i7 + 2));
                }
            } else if (i4 == ENCODING_PCM_32BIT) {
                for (int i8 = position; i8 < limit; i8 += 4) {
                    this.f4376m.put(byteBuffer.get(i8 + 2));
                    this.f4376m.put(byteBuffer.get(i8 + 3));
                }
            } else {
                throw new IllegalStateException();
            }
            byteBuffer.position(byteBuffer.limit());
            this.f4376m.flip();
            if (i2 > 3) {
                m25983m(this.f4376m, position, limit);
            }
            m25982n(this.f4376m);
        } catch (Throwable th) {
            this.f4377n = false;
        }
    }

}
