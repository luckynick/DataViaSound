package com.luckynick.android.test;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.luckynick.custom.Utils;
import com.luckynick.shared.model.ReceiveParameters;
import com.luckynick.shared.model.SendParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.luckynick.custom.Utils.*;

public class CalibrationActivity extends BaseActivity {
    public static final String LOG_TAG = "CalibrationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
    }

    /**
     * Handle button pressing on activity layouts.
     * @param view clicked button
     */
    public void onClick(final View view)
    {
        switch (view.getId())
        {
            case R.id.button:
                launchCalibration();
                break;
        }
    }

    protected void launchCalibration() {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);


        calibrate((calibratedLoudness) -> {
            sharedPrefs.edit().putInt("calibratedLoudness", calibratedLoudness.intValue()).commit();
            setLoudnessLevel(calibratedLoudness);
            ((TextView)findViewById(R.id.calibrationOutput)).setText(""+calibratedLoudness.intValue());
        });

        //((TextView)findViewById(R.id.calibrationOutput)).setText(""+calibratedLoudness);
    }

    private void calibrate(CalibrationCallback callback) {

        new AsyncCalibrate((result) -> {
            callback.performProgramTasks(result);
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @FunctionalInterface
    public interface CalibrationCallback {
        void performProgramTasks(Integer calibratedValue);
    }

    /**
     * AsyncTask for getFrequency() in SoundRecognizer object.
     * Returns recognized frequency in Hz.
     */
    public class AsyncCalibrate extends AsyncTask<Void, Void, Integer>
    {
        CalibrationCallback callback;

        public AsyncCalibrate(CalibrationCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            long recordStartTime = System.currentTimeMillis();
            new CalibrationActivity.AsyncRecord().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long beepsStartTime = System.currentTimeMillis();
            long firstBeepMid = beepsStartTime - recordStartTime + Utils.BEEP_DURATION / 2;

            Log(LOG_TAG, "First beep mid: " + firstBeepMid);

            List<Integer> playStarts = new ArrayList<>();

            for(int i = 10; i < 101; i += 10) {

                Log(LOG_TAG, "Calibrating on " + i);
                SendParameters params = new SendParameters();
                params.message = "D";
                params.loudnessLevel = i;
                int playStart = (int)(System.currentTimeMillis() - recordStartTime);
                Log(LOG_TAG, "play time: " + playStart);
                playStarts.add(playStart);
                sg.playMessage(params.frequenciesBindingShift, params.frequenciesBindingScale,
                        params.message, params.loudnessLevel, false);
                try {
                    Thread.sleep(Utils.BEEP_DURATION);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            //sr.stopRecord();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sr.stopRecord();
            int endTimeRecord = (int)(System.currentTimeMillis() - beepsStartTime);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                List<Short> samples = sr.getSamples();

                /*for(int i = (int)firstBeepMid; i < endTimeRecord; i += Utils.BEEP_DURATION * 2) {

                    double detectedLoudness = sr.getLoudness(samples, i);
                    Log(LOG_TAG, "Detected on " + i + ": " + detectedLoudness);
                }*/
                for(int i : playStarts) {

                    double detectedLoudness = sr.getLoudness(samples, i + Utils.BEEP_DURATION); // /2
                    Log(LOG_TAG, "Detected on " + (i + Utils.BEEP_DURATION) + ": " + detectedLoudness);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return 50;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            callback.performProgramTasks(integer);
        }
    }
}
