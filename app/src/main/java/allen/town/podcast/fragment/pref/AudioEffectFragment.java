package allen.town.podcast.fragment.pref;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import org.greenrobot.eventbus.EventBus;

import allen.town.focus_common.common.prefs.supportv7.ATESwitchPreference;
import allen.town.podcast.MyApp;
import allen.town.podcast.R;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.core.storage.DBWriter;
import allen.town.podcast.core.util.playback.PlaybackController;
import allen.town.podcast.event.playback.TitleChangeEvent;
import allen.town.podcast.event.settings.LoudnessChangedEvent;
import allen.town.podcast.event.settings.MonoChangedEvent;
import allen.town.podcast.event.settings.SkipSilenceChangedEvent;
import allen.town.podcast.model.feed.Feed;
import allen.town.podcast.model.feed.FeedPreferences;

public class AudioEffectFragment extends AbsSettingsFragment {
    private static final String PREF_FEED_AUDIO_EFFECT = "feed_audio_effect_pref";
    private static final String PREF_FEED_SKIP_SILENCE = "feed_skip_silence";
    private static final String PREF_FEED_MONO = "feed_mono_pref";
    private static final String PREF_FEED_LOUDNESS = "feed_loudness_pref";
    private FeedPreferences feedPreferences;
    private Feed feed;
    private PlaybackController controller;

    public AudioEffectFragment(FeedPreferences feedPreferences, Feed feed) {
        this.feedPreferences = feedPreferences;
        this.feed = feed;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_audio_effect);
        init();
    }

    @Override
    public void onStop() {
        super.onStop();
        controller.release();
        controller = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
//                setupAudioEffectPreference();
            }
        };
        controller.init();
        setupAudioEffectPreference();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        EventBus.getDefault().post(new TitleChangeEvent(R.string.audio_effects));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    ATESwitchPreference audioEffectPreference;
    ATESwitchPreference skipSilencePreference;
    ATESwitchPreference monoPreference;
    ATESwitchPreference loudnessPreference;

    private void init() {
        audioEffectPreference = findPreference(PREF_FEED_AUDIO_EFFECT);
        skipSilencePreference = findPreference(PREF_FEED_SKIP_SILENCE);
        monoPreference = findPreference(PREF_FEED_MONO);
        loudnessPreference = findPreference(PREF_FEED_LOUDNESS);
    }

    private void setupAudioEffectPreference() {
        audioEffectPreference.setChecked(feedPreferences.isUseFeedEffect());
        audioEffectPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if((boolean) newValue && !MyApp.getInstance().checkSupporter(getContext(),true)){
                    return false;
                }
                feedPreferences.setUseFeedEffect((Boolean) newValue);
                DBWriter.setFeedPreferences(feedPreferences);

                setupAudioEffectPreferenceChangeInner((boolean) newValue);
                EventBus.getDefault().post(
                        new SkipSilenceChangedEvent(skipSilencePreference.isChecked(), feed.getId()));
                EventBus.getDefault().post(
                        new MonoChangedEvent(monoPreference.isChecked(), feed.getId()));
                EventBus.getDefault().post(
                        new LoudnessChangedEvent(loudnessPreference.isChecked(), feed.getId()));
                return true;
            }
        });

        skipSilencePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            feedPreferences.setSkipSilence((Boolean) newValue);
            DBWriter.setFeedPreferences(feedPreferences);
            EventBus.getDefault().post(
                    new SkipSilenceChangedEvent((Boolean) newValue, feed.getId()));

            return true;
        });

        monoPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            feedPreferences.setMono((Boolean) newValue);
            DBWriter.setFeedPreferences(feedPreferences);
            EventBus.getDefault().post(
                    new MonoChangedEvent((Boolean) newValue, feed.getId()));

            return true;
        });

        loudnessPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            feedPreferences.setLoudness((Boolean) newValue);
            DBWriter.setFeedPreferences(feedPreferences);
            EventBus.getDefault().post(
                    new LoudnessChangedEvent((Boolean) newValue, feed.getId()));

            return true;
        });
        setupAudioEffectPreferenceChangeInner(feedPreferences.isUseFeedEffect());
    }

    private void setupAudioEffectPreferenceChangeInner(boolean isUseFeedEffect) {
        skipSilencePreference.setChecked(!isUseFeedEffect ? Prefs.isSkipSilence() : feedPreferences.isSkipSilence());
        monoPreference.setChecked(!isUseFeedEffect ? Prefs.stereoToMono() : feedPreferences.isMono());
        loudnessPreference.setChecked(!isUseFeedEffect ? Prefs.audioLoudness() : feedPreferences.isLoudness());
    }

    @Override
    public void invalidateSettings() {

    }
}
