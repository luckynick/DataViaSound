package com.luckynick.android.test;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;

import com.luckynick.shared.SharedUtils;

import static com.luckynick.custom.Utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SoundRecognizer {
    private static final String LOG_TAG = "Recognizer";

    /**
     * Place where audiofile is stored.
     */
    private String recordPath;
    /**
     * Indicates that recording process has to go on (if true).
     * Record will stop when value is false.
     */
    private volatile boolean ifRecord;
    /**
     * Object for obtaining information about recorded audiofile.
     * Assigned on program start or when new record is created.
     */
    private MediaMetadataRetriever audioData;
    /**
     * Binding of frequencies. Indexes of array represent decimal code of ASCII symbols.
     * Value is actual frequency which correspond to this symbol. If value is -1, then
     * symbol is not used in program.
     */
    private int frequenciesBinding[];
    /**
     * Length in between characters in Hz in array of frequencies.
     * Counted on program start or when new record is created.
     */
    private int biggestDistBetweenFreq;
    /**
     * Raw samples from audiofile.
     * Assigned on program start or when new record is created.
     */
    private List<Short> samples;
    /**
     * Position in milliseconds from which program has to start
     * detecting message in record. Also it points on most optimal
     * place for getting frequencies from beep (silience period + middle of beep). Every next
     * frequency will be counted on midShift + n * BEEP_DURATION position,
     * so all frequencies will be counted on positions where
     * there is no damage because of change from previous frequency.
     * Counted on program start or when new record is created.
     */
    private int midShift; //in ms

    /**
     * Try to open record if exists and extract basic data from it.
     * @param rec
     * @param freqArr
     * @throws IllegalArgumentException if record path or binding of frequencies are empty
     */
    SoundRecognizer(String rec, int freqArr[]) throws IllegalArgumentException
    {
        if(rec.isEmpty() || rec == null || freqArr == null || freqArr.length == 0)
            throw new IllegalArgumentException("You can't pass empty arguments to this constructor.");
        recordPath = rec;
        if(new File(rec).exists())
        {
            audioData = new MediaMetadataRetriever();
            audioData.setDataSource(rec);
            try {
                samples = getSamples();
            } catch (IOException e) {
                Log("IOException", "Problem getting samples");
                samples = null;
            }
        }
        setFreqMapping(freqArr);
        try {
            locateMidShift();
        }
        catch (IndexOutOfBoundsException e) {
            new File(rec).delete();
            /*
            throw new IllegalArgumentException("Record was deleted because IndexOutOfBoundsException happened " +
                    "during locateMidShift()");
            */
        }
    }

    /**
     * Set binding and count biggest distance between used frequencies.
     * @param freqArr binding of ASCII characters to frequencies
     */
    private void setFreqMapping(int freqArr[])
    {
        frequenciesBinding = freqArr;
        int biggestLength = 0;
        for(int i = 1; i < freqArr.length; i++)
        {
            if(freqArr[i] <= 0 || freqArr[i - 1] <= 0) continue;
            int tempLength = freqArr[i] - freqArr[i - 1];
            if(tempLength > biggestLength) biggestLength = tempLength;
        }
        biggestDistBetweenFreq = biggestLength;
        Log(LOG_TAG, "biggestDistBetweenFreq = " + biggestDistBetweenFreq);
    }

    /**
     *
     * @return
     */
    public String iterateForFrequencies()
    {
        if(samples == null || midShift <= 0)
        {
            Log(LOG_TAG, "Samples: " + (samples != null ? samples.isEmpty() : false) + ", midShift: " + midShift);

            return null;
        }
        return iterateForFrequencies(samples, midShift % 100); //rest is experimental
    }

    public String iterateForFrequencies(int time)
    {
        if(samples == null)
        {
            Log(LOG_TAG, "Samples: " + samples.isEmpty());
            return null;
        }
        return iterateForFrequencies(samples, time);
    }

    /**
     *
     * @param samples raw samples data
     * @param startT time in milliseconds from which message has to be detected
     * @return message contained in audiorecord
     * @throws IndexOutOfBoundsException has to be handled; may be thrown if
     * algorithm goes out of record time borders.
     */
    public String iterateForFrequencies(List<Short> samples, final int startT) throws IndexOutOfBoundsException
    {
        List<Integer> roundedFreq = new ArrayList<>();
        int recordLimitTime = Integer.parseInt(audioData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        int time = startT; //experiment
        while(time < recordLimitTime - BEEP_DURATION)
        {
//            roundedFreq.add(filterSum(samples, time));
            roundedFreq.add(filterVote(samples, time)); //filterVote
            time += BEEP_DURATION;
        }
        String wholeStream = "";
        for(int val : roundedFreq)
        {
            char next = ERROR_CHAR;
            for(int i = 0; i < frequenciesBinding.length; i++)
            {
                if(val != -1 && frequenciesBinding[i] == val) next = (char) i;
            }
            //System.out.println("'" + next + "' is: " + val);
            wholeStream += next;
        }
        Log(LOG_TAG, "Offset is " + startT + "ms. Text: " + wholeStream);
        String result = wholeStream.substring(wholeStream.indexOf(START_TAG) + START_TAG.length(), wholeStream.lastIndexOf(END_TAG));
//        String result = wholeStream.substring(wholeStream.indexOf(START_TAG) + START_TAG.length(), wholeStream.length()).trim();


        return fromHex(result);
    }


    /**
     * Algorithm of frequency assuming. Makes statistically more
     * precise results than filterSum method. Principle: get frequencies
     * from neighbour time positions and find frequency which occured the
     * most in whole set.
     * @param samples raw sample data
     * @param time moment of record on which frequency has to be counted
     * @return counted frequency
     */
    public int filterVote(List<Short> samples, int time)
    {
        int step = 2 //in ms, how much to increase position on every iteration
                , iter = 19 //number of iterations = number of assumed neighbor frequencies
                , i = -step*(iter-1)/2; //start getting neighbors from this position
        double cells[] = new double[iter]; //neighbors
        int c = 0;
        try
        {
            for(; i < step*iter/2; i += step)
            {
                cells[c++] = getFrequency(samples, time + i);
            }
        }
        catch (IndexOutOfBoundsException ex)
        {
            return -1; //can't be counted; position is too close to record borders
            //Log.w("Freq counting alg", "Out of bounds: " + (time + i) + "ms");
        }
        c = 0;
        int rounded[] = new int[cells.length];
        for(double val : cells)
        {
            rounded[c++] = closestBinding(val); //find closest frequency in binding to current frequency in set of neighbors
        }
        int counts[] = new int[rounded.length];
        for(int j = 0; j < counts.length; j++) //count number of times each frequency appeared
        {
            int num = 0;
            for(int k = j; k < rounded.length; k++)
            {
                if(rounded[j] == rounded[k]) num++;
            }
            counts[j] = num;
        }
        int leader = 0;
        for(int j = 0; j < counts.length; j++)
        {
            if(leader < counts[j]) leader = j;
        }
        return rounded[leader]; //return frequency with biggest number of occurrences
    }

    /**
     * Algorithm of frequency assuming. Trivial algorythm
     * based on simple finding of middle value between
     * frequencies on neighbor possitions.
     * @param samples raw samples data
     * @param time position in ms which shows where
     *             in record to count frequency
     * @return counted frequency
     */
    public int filterSum(List<Short> samples, int time)
    {
        int step = 5 /*5*/, iter = 7 /*7*/, i = -step*(iter-1)/2;
        double sum = 0;
        try
        {
            for(; i < step*iter/2; i += step)
            {
                sum += getFrequency(samples, time + i);
            }
        }
        catch (IndexOutOfBoundsException ex)
        {
            sum = -iter;
            //Log.w("Freq counting alg", "Out of bounds: " + (time + i) + "ms");
        }
        double dval = sum / iter;
        return closestBinding(dval);
    }

    /**
     * Used in frequency assuming algorithm.
     * @param dval frequency
     * @return closest to argument frequency value in binding
     */
    public int closestBinding(double dval)
    {
        double closestDist = Double.MAX_VALUE;
        int closestFreq = -1;
        for(int ival : frequenciesBinding)
        {
            double tempDist = Math.abs(dval - ival);
            if(tempDist < closestDist)
            {
                closestDist = tempDist;
                closestFreq = ival;
            }
        }
        if(closestDist > biggestDistBetweenFreq/2) closestFreq = -1;
        return closestFreq;
    }

    /**
     * Count best offset in milliseconds to start detecting message in record.
     * Algorithm searches for second beep in START_TAG. This
     * beep must be high on contrast with first and third beep
     * in order for algorithm to work properly.
     * Sets midShift to offset in ms; position in middle of first beep
     */
    public void locateMidShift() throws IndexOutOfBoundsException
    {
        if(samples == null || frequenciesBinding == null || audioData == null) {
            midShift = -1;
            return;
        }
        int recDuration = Integer.parseInt(audioData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        int tempDistance = 0, //length of theoretical first found beep
                increment = BEEP_DURATION/10; //search for beginning of the message every
                                            // BEEP_DURATION/10 ms from record start
        int leftBorder = 0 /*left edge of found beep*/,
                rightBorder = 0; //right edge of found beep
        for(int currMs = 10; currMs < recDuration; currMs += increment)
        {
            double tempFreq;
            try
            {
                tempFreq = getFrequency(samples, currMs);
            }
            catch(IndexOutOfBoundsException ex)
            {
                continue;
            }

            if(tempFreq == -1) continue;
            if(checkFreqProximity(currMs, START_TAG, 0.5))
            {
                if(tempDistance < BEEP_DURATION/2)
                {
                    tempDistance = 0;
                }
                else
                {
                    leftBorder = currMs;
                    break;
                }
            }
            else {
                tempDistance += increment;
            }
        }
        midShift = leftBorder + (int)(BEEP_DURATION * 0.55); //0.5;
        //value 0.55 found experimentally; TODO: check if really points on middle of first beep
        Log(LOG_TAG, "midShift = " + midShift + ", leftBorder = " + leftBorder);
        Log(LOG_TAG, "rightBorder = " + rightBorder + ", tempDistance = " + tempDistance);
    }

    public boolean checkFreqProximity(int offset, String characters, double precision) throws IndexOutOfBoundsException{
        int step = 0;
        for(char c : characters.toCharArray()) {
            if(!checkFreqProximity(getFrequency(samples, offset + step), c, precision)) return false;
            step += BEEP_DURATION;
        }
        Log(LOG_TAG, "Freq proximity succeeded. offset = " + offset);
        return true;
    }

    /**
     *
     * @param freq
     * @param character
     * @param precision define what is the maximum deriviation for given frequency still to be
     *                  considered as given character; scale of biggestDistBetweenFreq
     * @return
     */
    public boolean checkFreqProximity(double freq, char character, double precision){
        // why minus below? because we find first minimum char of tag on contrast with maximum one
        return freq < frequenciesBinding[character] + precision * biggestDistBetweenFreq
                && freq > frequenciesBinding[character] - precision * biggestDistBetweenFreq;
    }

    /**
     * Count frequency on specified moment of record.
     * Based on principle that every graph of explicit frequency has
     * it's zero points. Algorithm finds distance between first
     * zero point on the left and second zero point on the right.
     * Then this distance is a period of sine wave.
     * Frequency = SAMPLE_RATE / wave period.
     * @param samples raw samples data
     * @param milliseconds time position in record (milliseconds)
     * @return counted frequency
     * @throws IndexOutOfBoundsException
     * @throws IllegalStateException
     */
    public double getFrequency(List<Short> samples, int milliseconds) throws IndexOutOfBoundsException, IllegalStateException
    {
        final int startSamplePosition = (int)(SAMPLE_RATE * ((double)milliseconds/1000));
        int posNow = startSamplePosition - 1;
        int posBefore = startSamplePosition;
        int leftZero, rightZero;
        while(!(samples.get(posNow) >= 0 && samples.get(posBefore) < 0) && !(samples.get(posNow) <= 0 && samples.get(posBefore) > 0))
        {
            posNow--;
            posBefore--;
        }
        leftZero = posNow; //go left one time to find left zero
        posNow = startSamplePosition + 1;
        posBefore = startSamplePosition;
        for(int i = 0; i < 2; i++)
        {
            while(!(samples.get(posNow) >= 0 && samples.get(posBefore) < 0) && !(samples.get(posNow) <= 0 && samples.get(posBefore) > 0))
            {
                posNow++;
                posBefore++;
            }
            posNow++;
            posBefore++;
        }
        rightZero = posNow; //go right two times to find right zero
        leftZero++; rightZero--; rightZero--; //above counts are wrong a little
        double wavePeriod = (rightZero - leftZero);
        /*Above counts find only integer part of wave period.
        * But decimal part is extremely important. Left and
        * right additions are counted below.*/
        int leftZeroVal = samples.get(leftZero);
        int leftZeroMinusVal = samples.get(leftZero - 1);
        int rightZeroVal = samples.get(rightZero);
        int rightZeroPlusVal = samples.get(rightZero + 1);
        double leftAddition = calcAddition(leftZeroMinusVal, leftZeroVal, 1, Sides.LEFT);
        double rightAddition = calcAddition(rightZeroVal, rightZeroPlusVal, 1, Sides.RIGHT);
        wavePeriod = wavePeriod + leftAddition + rightAddition;
        return SAMPLE_RATE/wavePeriod;
    }

    /**
     * Calculate decimal addition to wave period.
     * @param left left value of sample
     * @param right right value of sample
     * @param length length between samples
     * @param side left or right from wave side;
     *             has influence on returned value;
     *             enum in ProjectTools.
     * @return decimal addition on specified side from wave
     */
    private double calcAddition(int left, int right, double length, Sides side)
    {
        double smaller;
        left = Math.abs(left);
        right = Math.abs(right);
        if(left > right)
        {
            smaller = right/(double)(left+right) * length;
        }
        else
        {
            smaller = left/(double)(left+right) * length;
        }
        if(side == Sides.LEFT && left > right)
            return smaller;
        else if(side == Sides.LEFT && left < right)
            return length - smaller;
        else if(side == Sides.RIGHT && left > right)
            return length - smaller;
        else if(side == Sides.RIGHT && left < right)
            return smaller;
        return 0;
    }

    /**
     * Get samples from current record. MediaCodec and
     * MediaExtractor classes are used.
     * @return List of raw samples data
     * @throws IOException
     */
    public ArrayList<Short> getSamples() throws IOException
    {
        File existingRec = new File(recordPath);
        Log(LOG_TAG, "Size of existing record: " + existingRec.length() + " bytes.");
        if(existingRec.length() > SharedUtils.MAX_AUDIO_RECORD_SIZE) {
            existingRec.delete();
            return null;
        }
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }
        ArrayList<Short> listOfShorts = new ArrayList<>();
        MediaExtractor extractor = new MediaExtractor(); //MediaCodec can't really get audio
        // file headers for processing; MediaExtractor really has to do it
        extractor.setDataSource(recordPath);
        extractor.selectTrack(0); //we have only one track - our record
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo(); //Object is used because dequeueOutputBuffer(@NotNull BufferInfo),
        // audioData stored in object is not used
        MediaFormat format = extractor.getTrackFormat(0); //MediaExtractor recognizes audio file parameters
        String mime = format.getString(MediaFormat.KEY_MIME);
        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null, null, 0);
        codec.start();

        do {
            int inputBufferIndex = codec.dequeueInputBuffer(10000); //get index of next inputBuffer, timeout 10 sec
            if (inputBufferIndex >= 0) {
                int sampleSize;
                long presentationTime;
                boolean sawInputEOS = false;
                ByteBuffer inputBuffer = null;
                inputBuffer = codec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                if(inputBuffer != null)
                {
                    sampleSize = extractor.readSampleData(inputBuffer, 0);//codec operates on raw audioData which gets from MediaExtractor
                    presentationTime = 0;
                    if (sampleSize < 0) {
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTime = extractor.getSampleTime();
                    }
                }
                else
                {
                    Log("InputBuffer", "is null");
                    continue;
                }
                codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTime,
                        sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);//let codec know that inputBuffer is ready to process
            }
            int outputBufferIndex;
            boolean repeat;
            do
            {
                repeat = false;
                outputBufferIndex = codec.dequeueOutputBuffer(info, 10000);
                switch (outputBufferIndex) //handles info codes which are produced by dequeueOutputBuffer().
                {
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        //repeat = true;
                        Log("MediaCodec", "Try again later");
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        repeat = true;
                        Log("MediaCodec", "Output format changed");
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        repeat = true;
                        Log("MediaCodec", "Output buffers changed");
                        break;
                }
            }
            while(repeat);
            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferIndex);
                if(outputBuffer != null)
                {
                    for(int i = 0; i < outputBuffer.limit(); i += 2)
                    {
                        listOfShorts.add(outputBuffer.getShort(i));
                    }
                    outputBuffer.clear();
                    codec.releaseOutputBuffer(outputBufferIndex, false);
                }
            }
        }
        while(extractor.advance());

        extractor.release();
        codec.stop();
        codec.release();
        return listOfShorts;
    }

    /**
     * Record new audiofile in path set in recordPath variable.
     * Records till ifRecord is set to false from another threads.
     */
    public void record()
    {
        if(ifRecord) return;
        File f = new File(recordPath);
        if(f.exists())
        {
            f.delete();
        }
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ifRecord = true;
        MediaRecorder mr = new MediaRecorder();
        mr.setAudioSource(MediaRecorder.AudioSource.MIC);
        mr.setAudioSamplingRate(SAMPLE_RATE);
        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // MPEG_4 THREE_GPP
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); //AAC AMR_NB
        mr.setOutputFile(recordPath);
        mr.setAudioChannels(1);
        try {
            mr.prepare();
            mr.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(ifRecord)
        {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mr.stop();
        mr.reset();
        mr.release();

        audioData = new MediaMetadataRetriever();
        audioData.setDataSource(recordPath);
        try {
            samples = getSamples();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            locateMidShift();
        }
        catch (IndexOutOfBoundsException e) {
            f.delete();
            /*
            throw new IllegalArgumentException("Record was deleted because IndexOutOfBoundsException happened " +
                    "during locateMidShift()");
            */
        }
    }

    public boolean isIfRecord() {
        return ifRecord;
    }

    public void stopRecord() {
        this.ifRecord = false;
    }
}
