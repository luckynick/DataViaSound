package com.luckynick.android.test;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Build;

import com.luckynick.shared.PureFunctionalInterface;
import com.luckynick.shared.enums.SoundProductionUnit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.luckynick.custom.Utils.*;

public class SoundGenerator {

    public interface Listener {
        void playStopped();
    }

	/**
     * Path where existing audio record is stored.
     */
    private String recordPath;
    /**
     * Array of frequencies. Indexes of array represent decimal code of ASCII symbols.
     * Value is actual frequency which correspond to this symbol. If value is -1, then
     * symbol is not used in program.
     */
    public static final String LOG_TAG = "Generator";

    public static List<Listener> playStoppedSubs = new ArrayList<>();

    SoundGenerator(String rec)
    {
        recordPath = rec;
    }

    /**
     * Play message on loudspeakers. This method wraps message in START_TAG and END_TAG,
     * encodes text to frequencies and creates audio data, which is further played through speakers.
     * @param m text message which has to be encoded and played
     */
    public void playMessage(int freqBindingBase, double freqBindingScale, String m)
    {
        int frequenciesArr[] = BaseActivity.getFreqBinding(freqBindingScale);
        String message = JUNK_RIGHT + START_TAG; //junk here for test
        message += toHex(m);
        message += END_TAG + JUNK_RIGHT;
        Log(LOG_TAG, "Playing new message: " + message);
        if(m.equals("")) return;
        int numSamples = SAMPLE_RATE * BEEP_DURATION / 1000;
        double[][] mSound = new double[message.length()][numSamples];
        short[][] mBuffer = new short[message.length()][numSamples];
        for (int i = 0; i < message.length(); i++) {
            int index = (int)message.charAt(i);
            double currentFreq;
            if(index >= frequenciesArr.length) currentFreq = frequenciesArr[ERROR_CHAR]
                    + freqBindingBase;
            else currentFreq = frequenciesArr[message.charAt(i)] + freqBindingBase;
            if(currentFreq == -1.0) currentFreq = frequenciesArr[ERROR_CHAR] + freqBindingBase;
            Log(LOG_TAG, "Freq for symb '" + message.charAt(i) + "' num " + i + ": " + currentFreq);
            for (int j = 0; j < mSound[i].length; j++) {
                mSound[i][j] = Math.sin(2.0 * Math.PI * j / (SAMPLE_RATE / currentFreq));
                mBuffer[i][j] = (short) (mSound[i][j] * Short.MAX_VALUE);
            }
        }
        AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE_OUT, AudioTrack.MODE_STREAM);
        mAudioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
        for(short[] arr : mBuffer)
        {
            play(mAudioTrack, arr);
        }
        mAudioTrack.release();
        for (Listener sub : playStoppedSubs) {
            sub.playStopped();
        }
    }

    /**
     *
     * @param m
     * @param loudnessLevel from 0 to 100
     */
    public void playMessage(int freqBindingBase, double freqBindingScale, String m,
                            final int loudnessLevel, boolean wrapInTags)
    {
        int frequenciesArr[] = BaseActivity.getFreqBinding(freqBindingScale);
        String message = JUNK_RIGHT + START_TAG; //junk here for test
        message += toHex(m);
        message += END_TAG + JUNK_RIGHT + JUNK_RIGHT;
        if(!wrapInTags) message = toHex(m);
        /*String message;
        if(wrapInTags) {
            message = JUNK_RIGHT + START_TAG; //junk here for test
            message += toHex(m);
            message += END_TAG + JUNK_RIGHT + JUNK_RIGHT;
        }
        else {
            message = toHex(m);
        }*/
        Log(LOG_TAG, "Playing new message: " + message);
        if(m.equals("")) return;
        int numSamples = SAMPLE_RATE * BEEP_DURATION / 1000;
        double[][] mSound = new double[message.length()][numSamples];
        short[][] mBuffer = new short[message.length()][numSamples];
        for (int i = 0; i < message.length(); i++) {
            int index = (int)message.charAt(i);
            double currentFreq;
            if(index >= frequenciesArr.length) currentFreq = frequenciesArr[ERROR_CHAR] + freqBindingBase;
            else currentFreq = frequenciesArr[message.charAt(i)] + freqBindingBase;
            if(currentFreq == -1.0) currentFreq = frequenciesArr[ERROR_CHAR] + freqBindingBase;
            Log(LOG_TAG, "Freq for symb '" + message.charAt(i) + "' num " + i + ": " + currentFreq);
            for (int j = 0; j < mSound[i].length; j++) {
                mSound[i][j] = Math.sin(2.0 * Math.PI * j / (SAMPLE_RATE / currentFreq));
                mBuffer[i][j] = (short) (mSound[i][j] * Short.MAX_VALUE);
            }
        }
        AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE_OUT, AudioTrack.MODE_STREAM);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            float log1=(float)(Math.log(100-loudnessLevel)/Math.log(100));
            mAudioTrack.setVolume(1-log1);
            //mAudioTrack.setVolume(loudness);
        }
        else {
            float deviceLoudness = (loudnessLevel / 100.0f) * AudioTrack.getMaxVolume();
            mAudioTrack.setStereoVolume(deviceLoudness, deviceLoudness);
        }
        for(short[] arr : mBuffer)
        {
            play(mAudioTrack, arr);
        }
        mAudioTrack.release();
        for (Listener sub : playStoppedSubs) {
            sub.playStopped();
        }
    }

    /**
     * Play raw data.
     * This version of overloaded method is needed if record
     * is played multiple times (AudioTrack object can't be used after
     * it is released)
     * @param buffer raw data which contains sound
     */
    public void play(short[] buffer)
    {
        AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE_OUT, AudioTrack.MODE_STREAM);
        mAudioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
        mAudioTrack.play();
        mAudioTrack.write(buffer, 0, buffer.length);
        mAudioTrack.stop();
        mAudioTrack.release();
    }

    /**
     * Play raw data.
     * This method of audio output is used when newly encoded messages have
     * to be played.
     * @param mAudioTrack
     * @param buffer raw data which contains sound
     */
    public void play(AudioTrack mAudioTrack, short[] buffer)
    {
        if(buffer == null) System.out.println("buffer is null");
        //mAudioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
        mAudioTrack.play();
        mAudioTrack.write(buffer, 0, buffer.length);
        mAudioTrack.stop();
    }

    /**
     * Play existing audio record. Used for playing previously recorder message from sender.
     * For debug purpose.
     * @return false if there is no record
     */
    public boolean playMediaPlayer()
    {
        if(!new File(recordPath).exists()) return false;
        MediaPlayer mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mp.setDataSource(recordPath);
            mp.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mp.start();
        while(mp.isPlaying())
        {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mp.release();
        return true;
    }

    public static void subscribePlayStoppedEvent(Listener sub) {
        playStoppedSubs.add(sub);
    }
}
