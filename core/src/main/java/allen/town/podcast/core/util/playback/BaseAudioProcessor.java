package allen.town.podcast.core.util.playback;

import com.google.android.exoplayer2.audio.AudioProcessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class BaseAudioProcessor implements AudioProcessor {

    /* renamed from: b */
    public AudioProcessor.AudioFormat f7554b;

    /* renamed from: c */
    public AudioProcessor.AudioFormat f7555c;

    /* renamed from: d */
    public AudioProcessor.AudioFormat f7556d;

    /* renamed from: e */
    public AudioProcessor.AudioFormat f7557e;

    /* renamed from: f */
    public ByteBuffer f7558f;

    /* renamed from: g */
    public ByteBuffer f7559g;

    /* renamed from: h */
    public boolean f7560h;


    public BaseAudioProcessor() {
        ByteBuffer byteBuffer = AudioProcessor.EMPTY_BUFFER;
        this.f7558f = byteBuffer;
        this.f7559g = byteBuffer;
        AudioProcessor.AudioFormat aVar = AudioFormat.NOT_SET;
        this.f7556d = aVar;
        this.f7557e = aVar;
        this.f7554b = aVar;
        this.f7555c = aVar;
    }


    @Override
    public AudioFormat configure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
        this.f7556d = inputAudioFormat;
        this.f7557e = mo21659h(inputAudioFormat);
        return isActive() ? this.f7557e : AudioProcessor.AudioFormat.NOT_SET;

    }

    @Override
    public boolean isActive() {
        return this.f7557e != AudioProcessor.AudioFormat.NOT_SET;
    }


    @Override
    public void queueEndOfStream() {
        this.f7560h = true;
        mo21671j();
    }

    @Override
    public ByteBuffer getOutput() {
        ByteBuffer byteBuffer = this.f7559g;
        this.f7559g = AudioProcessor.EMPTY_BUFFER;
        return byteBuffer;

    }

    /* renamed from: i */
    public void mo21669i() {
    }

    /* renamed from: j */
    public void mo21671j() {
    }

    /* renamed from: k */
    public void mo21668k() {
    }

    @Override
    public boolean isEnded() {
        return this.f7560h && this.f7559g == AudioProcessor.EMPTY_BUFFER;
    }

    @Override
    public void flush() {
        this.f7559g = AudioProcessor.EMPTY_BUFFER;
        this.f7560h = false;
        this.f7554b = this.f7556d;
        this.f7555c = this.f7557e;
        mo21669i();

    }

    @Override
    public void reset() {
        flush();
        this.f7558f = AudioProcessor.EMPTY_BUFFER;
        AudioProcessor.AudioFormat aVar = AudioProcessor.AudioFormat.NOT_SET;
        this.f7556d = aVar;
        this.f7557e = aVar;
        this.f7554b = aVar;
        this.f7555c = aVar;
        mo21668k();
    }

    public final ByteBuffer m21670l(int i) {
        if (this.f7558f.capacity() < i) {
            this.f7558f = ByteBuffer.allocateDirect(i).order(ByteOrder.nativeOrder());
        } else {
            this.f7558f.clear();
        }
        ByteBuffer byteBuffer = this.f7558f;
        this.f7559g = byteBuffer;
        return byteBuffer;
    }

    public abstract AudioProcessor.AudioFormat mo21659h(AudioProcessor.AudioFormat aVar) throws AudioProcessor.UnhandledAudioFormatException;


}
