package com.luckynick.custom;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.util.Log;

import com.luckynick.shared.SharedUtils;

import java.io.File;

public class Utils extends SharedUtils {

    public static final char ERROR_CHAR = (char) 0; //
    //contrast required; middle char is max high
    public static final String
            START_TAG = "G" /*explicit short version for hex*/,
            //START_TAG = "1E2" /*long for hex*/,
            //START_TAG = "!z#" /*for non-hex version of program*/,
            END_TAG = "G" /*explicit short version for hex*/,
            //END_TAG = "D1E" /*long for hex*/,
            //END_TAG = "x"z" /*for non-hex version of program*/,
            JUNK_RIGHT = "AA"; //placed in the end of message because android cuts last beep while playing //AAAA
    public static final int SAMPLE_RATE = 44100;
    public static final int MIN_BEEP_DURATION = 60; //Just reminder; some algorithms don't accept lower beep durations
    public static final int BEEP_DURATION = 100;
    public static final int BUFFER_SIZE_OUT = AudioTrack.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    public static final int BUFFER_SIZE_IN = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    /**
     * How many beeps to play when launching debug feature playRandomFreq.
     * Without start and end tags.
     */
    public static final int NUM_OF_RANDOM_BEEPS = 10;

    /**
     * Enum to indicate side from wave
     * while counting decimal addition to wave period
     */
    public enum Sides
    {
        LEFT, RIGHT
    }

    /**
     * Count checksum from message.
     * @param message text for which checkSum has to be counted
     * @return addition of symbols codes in message
     */
    public static int checkSum(String message)
    {
        int sum = 0;
        for(int i = 0; i < message.length(); i++)
        {
            sum += (int) message.charAt(i);
        }
        System.out.println("Checksum is " + sum);
        return sum;
    }



    public static void Log(String tag, String consoleLog) {
        if(SharedUtils.DEBUG_MODE && consoleLog != null) Log.i(tag, consoleLog);
    }
    /**
     * Make string with hex 8-bit numbers from text.
     * @param message text for which hex string has to be counted
     * @return string which contains hex representation of every single character in message
     */
    public static String toHex(String message)
    {
        if(message == null) return null;
        String s = "";
        for(int i = 0; i < message.length(); i++)
        {
            s += String.format("%02X", (byte)message.charAt(i));
        }
        return s;
    }

    /**
     * Decode string with hex 8-bit numbers to text.
     * @param hex string which contains hex representation of characters
     * @return text decoded from hex string
     */
    public static String fromHex(String hex) throws StringIndexOutOfBoundsException
    {
        String res = "";
        for(int i = 0; i < hex.length(); i += 2)
        {
            String one = "0x" + hex.charAt(i) + "" + hex.charAt(i + 1);
            System.out.println("One is " + one);
            try {
                res += (char) (int) Integer.decode(one);
            }
            catch (NumberFormatException e) { //decoding session shouldn't fail on this exception
                res += "?";
            }
        }
        System.out.println(res);
        return res;
    }
}
