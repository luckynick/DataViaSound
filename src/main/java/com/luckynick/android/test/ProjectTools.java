package com.luckynick.android.test;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;

import java.io.File;

public class ProjectTools {
    private ProjectTools() {};

    public static final char ERROR_CHAR = (char) 0;
    public static final String START_TAG = "1E2" /*!z#*/, //contrast required; middle char is max high
            END_TAG = "D1E"/*x"z*/, JUNK_RIGHT = "AAAA", /*.*/
            DIV_TAG = "%p'";
    public static final int SAMPLE_RATE = 44100; //8000
    public static final int MIN_BEEP_DURATION = 30;
    public static final int BEEP_DURATION = 100; //100
    public static final int BUFFER_SIZE_OUT = AudioTrack.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    public static final int BUFFER_SIZE_IN = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);

    /**
     * Enumeration for showing where from wave we are looking on
     * while counting decimal addition to wave period in samples
     */
    public enum Sides
    {
        LEFT, RIGHT
    }

    public static char symbolFromFrequency(int[] binding, int freq)
    {
        for(int i = 0; i < binding.length; i++)
        {
            if(binding[i] == freq) return (char) i;
        }
        return ERROR_CHAR;
    }

    public static int frequencyFromSymbol(int[] binding, char symbol)
    {
        return binding[symbol];
    }

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

    public static String toHex(String message)
    {
        String s = "";
        for(int i = 0; i < message.length(); i++)
        {
            s += String.format("%02X", (byte)message.charAt(i));
        }
        return s;
    }

    public static String fromHex(String hex)
    {
        String res = "";
        try
        {
            for(int i = 0; i < hex.length(); i += 2)
            {
                String one = "0x" + hex.charAt(i) + "" + hex.charAt(i + 1);
                System.out.println("One is " + one);
                res += (char) (int) Integer.decode(one);
            }
        }
        catch(NumberFormatException e)
        {
            return "No message found. Code 2";
        }
        catch(StringIndexOutOfBoundsException e)
        {

        }
        System.out.println(res);
        return res;
    }
}
