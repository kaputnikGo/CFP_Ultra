package com.cityfreqs.cfp_ultra.util;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;

import com.cityfreqs.cfp_ultra.MainActivity;
import com.cityfreqs.cfp_ultra.R;

import static com.cityfreqs.cfp_ultra.util.AudioSettings.AUDIO_BUNDLE_KEYS;
import static com.cityfreqs.cfp_ultra.util.AudioSettings.AUDIO_CHANNEL_OUT;
import static com.cityfreqs.cfp_ultra.util.AudioSettings.AUDIO_ENCODING;

public class AudioChecker {
    private static final String TAG = "AudioChecker";
    private Context context;
    private Bundle audioBundle;
    private boolean DEBUG;

    public AudioChecker(Context context, Bundle audioBundle) {
        this.context = context;
        this.audioBundle = audioBundle;
        DEBUG = audioBundle.getBoolean(AUDIO_BUNDLE_KEYS[16], true);
    }

    public Bundle getAudioBundle() {
        return audioBundle;
    }

    private int getClosestPowersHigh(int reported) {
        // return the next highest power from the minimum reported
        // 512, 1024, 2048, 4096, 8192, 16384
        for (int power : AudioSettings.POWERS_TWO_HIGH) {
            if (reported <= power) {
                return power;
            }
        }
        // didn't find power, return reported
        return reported;
    }

    public boolean determineRecordAudioType() {
        // guaranteed default for Android is 44.1kHz, PCM_16BIT, CHANNEL_IN_DEFAULT
        // test change to audio source:: AUDIO_SOURCE_VOICE_COMMUNICATION (7)
        // FOR PRIORITY BUMP IN ANDROID 10 (API29)
        int audioSource = MediaRecorder.AudioSource.DEFAULT; //VOICE_COMMUNICATION;// 7 //.DEFAULT; // 0

        for (int rate : AudioSettings.SAMPLE_RATES) {
            for (short audioFormat : new short[] {
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioFormat.ENCODING_PCM_8BIT}) {

                for (short channelInConfig : new short[] {
                        AudioFormat.CHANNEL_IN_DEFAULT, // 1 - switched by OS, not native?
                        AudioFormat.CHANNEL_IN_MONO,    // 16, also CHANNEL_IN_FRONT == 16
                        AudioFormat.CHANNEL_IN_STEREO }) {  // 12
                    try {
                        if (DEBUG) {
                            entryLogger("Try AudioRecord rate " + rate + "Hz, bits: " + audioFormat + ", channelInConfig: " + channelInConfig);
                        }
                        int buffSize = AudioRecord.getMinBufferSize(rate, channelInConfig, audioFormat);
                        // force buffSize to powersOfTwo if it isnt (ie.S5)
                        buffSize = getClosestPowersHigh(buffSize);

                        if (buffSize != AudioRecord.ERROR_BAD_VALUE) {
                            AudioRecord recorder = new AudioRecord(
                                    audioSource,
                                    rate,
                                    channelInConfig,
                                    audioFormat,
                                    buffSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                // AudioRecord.getChannelCount() is number of input audio channels (1 is mono, 2 is stereo)
                                if (DEBUG) {
                                    entryLogger("AudioRecord found: " + rate + ", buffer: " + buffSize + ", channel count: " + recorder.getChannelCount());
                                }
                                // set found values
                                audioBundle.putInt(AUDIO_BUNDLE_KEYS[0], audioSource);
                                audioBundle.putInt(AUDIO_BUNDLE_KEYS[1], rate);
                                audioBundle.putInt(AUDIO_BUNDLE_KEYS[2], channelInConfig);
                                audioBundle.putInt(AUDIO_BUNDLE_KEYS[3], audioFormat);
                                audioBundle.putInt(AUDIO_BUNDLE_KEYS[4], buffSize);

                                recorder.release();
                                return true;
                            }
                        }
                    }
                    catch (Exception e) {
                        if (DEBUG) {
                            entryLogger("Error, keep trying.");
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean checkAudioRecord() {
        // return if can start new audioRecord object
        AudioRecord audioRecord;
        try {
            audioRecord = new AudioRecord(
                    audioBundle.getInt(AUDIO_BUNDLE_KEYS[0]),
                    audioBundle.getInt(AUDIO_BUNDLE_KEYS[1]),
                    audioBundle.getInt(AUDIO_BUNDLE_KEYS[2]),
                    audioBundle.getInt(AUDIO_BUNDLE_KEYS[3]),
                    audioBundle.getInt(AUDIO_BUNDLE_KEYS[4]));
            entryLogger(context.getString(R.string.audio_check_4));
            // need to start reading buffer to trigger an exception
            audioRecord.startRecording();
            short[] buffer = new short[audioBundle.getInt(AUDIO_BUNDLE_KEYS[4])];
            int audioStatus = audioRecord.read(buffer, 0, audioBundle.getInt(AUDIO_BUNDLE_KEYS[4]));

            // check for error on pre 6.x and 6.x API
            if(audioStatus == AudioRecord.ERROR_INVALID_OPERATION
                    || audioStatus == AudioRecord.STATE_UNINITIALIZED) {
                entryLogger(context.getString(R.string.audio_check_6) + audioStatus);
                // audioStatus == 0(uninitialized) is an error, does not throw exception
                entryLogger(context.getString(R.string.audio_check_5));
                audioRecord.stop();
                audioRecord.release();
                return false;
            }
            audioRecord.stop();
            audioRecord.release();
        }
        catch(Exception e) {
            entryLogger(context.getString(R.string.audio_check_7));
            entryLogger(context.getString(R.string.audio_check_9));
            return false;
        }
        // no errors
        entryLogger(context.getString(R.string.audio_check_8));
        return true;
    }

    /*********************/
    public String saveFormatToString() {
        return (audioBundle.getInt(AUDIO_BUNDLE_KEYS[1]) + " Hz, "
                + AUDIO_ENCODING[audioBundle.getInt(AUDIO_BUNDLE_KEYS[3])] + ", "
                + AUDIO_CHANNEL_OUT[audioBundle.getInt(AUDIO_BUNDLE_KEYS[5])]);
    }

    private void entryLogger(String entry) {
        MainActivity.logger(TAG, entry);
    }
}
/*
                S5 returns:
                bands: 5
                minEQ: -1500 (-15 dB)
                maxEQ: 1500  (+15 dB)
                eqLevelRange: 2
                band 0
                    ctr: 60
                    min: 30
                    max: 120
                band 1
                    ctr: 230
                    min: 120
                    max: 460
                band 2
                    ctr: 910
                    min: 460
                    max: 1800
                band 3
                    ctr: 3600
                    min: 1800
                    max: 7000
                band 4
                    ctr: 14000
                    min: 7000
                    max: 0

notes: media/libeffects/lvm/lib/Eq/lib/LVEQNB.h
    /*      Gain        is in integer dB, range -15dB to +15dB inclusive                    */
/*      Frequency   is the centre frequency in Hz, range DC to Nyquist                  */
/*      QFactor     is the Q multiplied by 100, range 0.25 (25) to 12 (1200)            */
/*                                                                                      */
/*  Example:                                                                            */
/*      Gain = 7            7dB gain                                                    */
/*      Frequency = 2467    Centre frequency = 2.467kHz                                 */
    /*      QFactor = 1089      Q = 10.89

    // --> THERE'S A Q ?

*/
