package com.luckynick.android.test;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import com.luckynick.shared.enums.SoundConsumptionUnit;
import com.luckynick.shared.enums.SoundProductionUnit;
import com.luckynick.shared.model.ReceiveParameters;
import com.luckynick.shared.model.SendParameters;

import static com.luckynick.custom.Utils.*;

import java.io.IOException;
import java.util.List;

import static com.luckynick.custom.Utils.NUM_OF_RANDOM_BEEPS;

public abstract class BaseActivity extends AppCompatActivity implements GetFrequencyHandler,
        IterateForFrequenciesHandler {

    public static final String LOG_TAG = "BaseActivity";

    //TODO:
    //keep screen on during tests

    SoundGenerator sg;
    SoundRecognizer sr;
    String rec;
    Menu menu;

    SharedPreferences sharedPrefs;

    private static boolean isAsHotspot = false;

    private static int frequenciesArray_1_2000[];
    private static int frequenciesArray_600_2000[];
    private static int frequenciesArray_1_1000[];
    private static int frequenciesArray_1_500[];
    //int frequenciesArray_500_15500[];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        frequenciesArray_1_2000 = getResources().getIntArray(R.array.frequencies_1_2000);
        frequenciesArray_600_2000 = getResources().getIntArray(R.array.frequencies_600_2000);
        frequenciesArray_1_1000 = getResources().getIntArray(R.array.frequencies_1_1000);
        frequenciesArray_1_500 = getResources().getIntArray(R.array.frequencies_1_500);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        rec = getExternalFilesDir(null).toString() + "/record.3gp"; //mp3
        //frequenciesArray_500_15500 = getResources().getIntArray(R.array.frequencies_500_15500);
        sg = new SoundGenerator(rec);
        sr = new SoundRecognizer(0, 1, rec);
        Log(LOG_TAG, rec);

        sharedPrefs = getSharedPreferences("DataViaSound", 0);
        setAsHotspot(sharedPrefs.getBoolean("asHotspot", false));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.basic_menu, menu);
        menu.findItem(R.id.asHotspotCkeckbox).setChecked(this.isAsHotspot);
        this.menu = menu;
        return true;
    }

    @Override
    public void onResume(){
        super.onResume();
        Log(LOG_TAG, "onResume event.");
        if(menu == null) return;
        MenuItem hotspotCheck = menu.findItem(R.id.asHotspotCkeckbox);
        hotspotCheck.setChecked(this.isAsHotspot);
        //if(hotspotCheck != null) hotspotCheck.setChecked(this.isAsHotspot);
    }

    /**
     * Handle choice of menu items.
     * @param item chosen menu item
     * @return
     */
    public boolean onClickMenu(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.settingsItem:

                break;
            case R.id.playRandomFreqItem:
                String message = "";
                for(int i = 0; i < NUM_OF_RANDOM_BEEPS; i++)
                {
                    message += (char)(Math.random() * 128);
                }
                new MainActivity.AsyncPlayMessage().execute(message);
                break;
            case R.id.recordQuickItem:
                if(!sr.isIfRecord()) new MainActivity.AsyncRecord()
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                else sr.stopRecord();
                break;
            case R.id.experimentItem:
                sr.iterateForFrequencies(0, 50);
                break;
            case R.id.joinTestsItem:
                Intent intent = new Intent(this, TestsActivity.class);
                //intent.putExtra("asHotspot", this.isAsHotspot);
                //intent.putExtra("asHotspot", new PAr);
                startActivity(intent);
                break;
            case R.id.asHotspotCkeckbox:
                item.setChecked(!item.isChecked());
                setAsHotspot(item.isChecked());
                break;
        }

        return true;
    }



    /**
     * Invoked when AsyncGetFrequency finishes it's work.
     * @param freqText text which contain frequency
     */
    @Override
    public void getFrequencyFinished(String freqText) {
        ((TextView) findViewById(R.id.frequencyText)).setText(freqText);
    }

    /**
     * Invoked when AsyncIterateForFrequencies finishes it's work.
     * @param message decoded message
     */
    @Override
    public void iterateForFrequenciesFinished(String message, Exception e) {
        ((TextView)(findViewById(R.id.detectedText))).setText(message);
    }

    public void setAsHotspot(boolean newVal) {
        isAsHotspot = newVal;
        sharedPrefs.edit().putBoolean("asHotspot", isAsHotspot).commit();
        Log(LOG_TAG, "Set hotspot?: " + newVal);
    }

    public boolean getAsHotspot() {
        return isAsHotspot;
    }

    public static int[] getFreqBinding(double forValue) {
        if(BaseActivity.frequenciesArray_600_2000 == null) return null;
        if(forValue == 1.33) {
            return BaseActivity.frequenciesArray_1_2000;
        }
        if(forValue == 1) {
            return BaseActivity.frequenciesArray_600_2000;
        }
        else if(forValue == 0.5) {
            return BaseActivity.frequenciesArray_1_1000;
        }
        else if(forValue == 0.25) {
            return BaseActivity.frequenciesArray_1_500; //looks like the spectre is not wide enough
        }
        else {
            //return BaseActivity.frequenciesArray_600_2000;
            throw new IllegalArgumentException("No frequency binding available for value " + forValue);
        }
    }


    /**
     * AsyncTask for playMediaPlayer() in SoundGenerator object.
     * Displays information Snackbar if there is no record.
     */
    public class AsyncPlay extends AsyncTask<Void, Void, Boolean>
    {
        @Override
        protected Boolean doInBackground(Void... voids) {
            return sg.playMediaPlayer();
        }

        @Override
        protected void onPostExecute(Boolean b) {
            if(!b)
            {
                Snackbar.make(findViewById(android.R.id.content), "No record found", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    /**
     * AsyncTask for record() in SoundRecognizer object.
     */
    public class AsyncRecord extends AsyncTask<Void, Void, Void>
    {
        ReceiveParameters recvParams;

        public AsyncRecord() {
            this.recvParams = new ReceiveParameters();
        }

        public AsyncRecord(ReceiveParameters recvParams) {
            this.recvParams = recvParams;
        }

        @Override
        protected Void doInBackground(Void... strings) {
            sr.record(recvParams.frequenciesBindingShift, recvParams.frequenciesBindingScale);
            return null;
        }
    }

    /**
     * AsyncTask for getFrequency() in SoundRecognizer object.
     * Returns recognized frequency in Hz.
     */
    public class AsyncGetFrequency extends AsyncTask<Integer, Void, String>
    {
        @Override
        protected String doInBackground(Integer... ints) {
            String message = null;
            try {
                List<Short> samples = sr.getSamples();
                message = String.valueOf(sr.getFrequency(samples, ints[0]));
            }
            catch (IndexOutOfBoundsException | IllegalStateException e)
            {
                message = String.valueOf(-1);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return message;
        }

        @Override
        protected void onPostExecute(String message) {
            getFrequencyFinished(message);
        }
    }

    /**
     * AsyncTask for doInBackground() in SoundGenerator object.
     */
    public class AsyncPlayMessage extends AsyncTask<String, Void, Void>
    {
        SendParameters params;

        AsyncPlayMessage() {
            SendParameters params = new SendParameters();
            this.params = params;
        }

        AsyncPlayMessage(SendParameters params) {
            this.params = params;
        }

        @Override
        protected Void doInBackground(String... strings) {
            Log(LOG_TAG, "Device loudness (params.loudnessLevel): " + params.loudnessLevel);
            sg.playMessage(params.frequenciesBindingShift, params.frequenciesBindingScale,
                    strings[0], params.loudnessLevel);
            return null;
        }
    }

    /**
     * AsyncTask for iterateForFrequencies() in SoundRecognizer object.
     * Returns decoded message.
     */
    protected class AsyncIterateForFrequencies extends AsyncTask<Void, Void, String>
    {
        private Exception exception = null;
        ReceiveParameters params;

        public AsyncIterateForFrequencies() {
            this.params = new ReceiveParameters();
            this.params.soundConsumptionUnit = SoundConsumptionUnit.MICROPHONE;
        }

        public AsyncIterateForFrequencies(ReceiveParameters params) {
            this.params = params;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try
            {
                return sr.iterateForFrequencies(params.frequenciesBindingShift,
                        params.frequenciesBindingScale);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            Log(LOG_TAG, s);
            iterateForFrequenciesFinished(s, exception);
        }
    }
}

