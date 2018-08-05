package com.luckynick.android.test;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        GetFrequencyHandler, IterateForFrequenciesHandler{
    public static final String LOG_TAG = "Main";

    SoundGenerator sg;
    SoundRecognizer sr;
    String rec;
    int frequenciesArray[];

    /**
     * Program may start only after preparation process is done.
     * It includes initialization of SoundRecognizer and SoundGenerator
     * objects.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rec = getExternalFilesDir(null).toString() + "/record.3gp"; //mp3
        frequenciesArray = getResources().getIntArray(R.array.frequencies);
        sg = new SoundGenerator(rec, frequenciesArray);
        sr = new SoundRecognizer(rec, frequenciesArray);
        System.out.println(rec);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.basic_menu, menu);
        return true;
    }

    /**
     * Handle button pressing on activity layouts.
     * @param view clicked button
     */
    public void onClick(final View view)
    {
        switch (view.getId())
        {
            case R.id.recordButton:
                setContentView(R.layout.record_layout);
                break;
            case R.id.recordLayoutStartButton:
                ((TextView)findViewById(R.id.recordLayoutHelpText)).setText("Press \"Stop\" to finish recording");
                new AsyncRecord().execute();
                break;
            case R.id.recordLayoutStopButton:
                sr.stopRecord();
                ((TextView)findViewById(R.id.recordLayoutHelpText)).setText("Press \"Start\" to record");
                setContentView(R.layout.activity_main);
                break;
            case R.id.playButton:
                new AsyncPlay().execute();
                break;
            case R.id.getFrequencyButton:
                setContentView(R.layout.one_frequency_layout);
                break;
            case R.id.frequencyLayoutNextButton:
                int millis = Integer.parseInt(((EditText) findViewById(R.id.timeEdit)).getText().toString());
                new AsyncGetFrequency().execute(millis);
                break;
            case R.id.frequencyLayoutBackButton:
                setContentView(R.layout.activity_main);
                break;
            case R.id.playMessageLayoutBackButton:
                setContentView(R.layout.activity_main);
                break;
            case R.id.makeMessageButton:
                setContentView(R.layout.play_message_layout);
                break;
            case R.id.playMessageLayoutSendButton:
                new AsyncPlayMessage().execute(((EditText) findViewById(R.id.messageEdit)).getText().toString());
                break;
            case R.id.detectTextButton:
                setContentView(R.layout.detect_text_layout);
                break;
            case R.id.detectTextLayoutNextButton:
                new AsyncIterateForFrequencies().execute();
                break;
            case R.id.detectTextLayoutBackButton:
                setContentView(R.layout.activity_main);
        }
    }

    /**
     * Handle choice of menu items.
     * @param view chosen menu item
     * @return
     */
    public boolean onClickMenu(MenuItem view)
    {
        switch (view.getItemId())
        {
            case R.id.settingsItem:

                break;
            case R.id.playRandomFreqItem:
                String message = "";
                for(int i = 0; i < ProjectTools.NUM_OF_RANDOM_BEEPS; i++)
                {
                    message += (char)(Math.random() * 128);
                }
                new AsyncPlayMessage().execute(message);
                break;
            case R.id.recordQuickItem:
                if(!sr.isIfRecord()) new AsyncRecord().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                else sr.stopRecord();
                break;
            case R.id.experimentItem:
                sr.iterateForFrequencies(50);
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
    public void iterateForFrequenciesFinished(String message) {
        ((TextView)(findViewById(R.id.detectedText))).setText(message);
    }

    /**
     * AsyncTask for playMediaPlayer() in SoundGenerator object.
     * Displays information Snackbar if there is no record.
     */
    private class AsyncPlay extends AsyncTask<Void, Void, Boolean>
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
    private class AsyncRecord extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... strings) {
            sr.record();
            return null;
        }
    }

    /**
     * AsyncTask for getFrequency() in SoundRecognizer object.
     * Returns recognized frequency in Hz.
     */
    private class AsyncGetFrequency extends AsyncTask<Integer, Void, String>
    {
        @Override
        protected String doInBackground(Integer... ints) {
            String message = null;
            try {
                List<Short> samples = sr.getSamples();
                message = String.valueOf(sr.getFrequency(samples, ints[0]));
            }
            catch (IndexOutOfBoundsException e)
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
    private class AsyncPlayMessage extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... strings) {
            sg.playMessage(strings[0]);
            return null;
        }
    }

    /**
     * AsyncTask for iterateForFrequencies() in SoundRecognizer object.
     * Returns decoded message.
     */
    private class AsyncIterateForFrequencies extends AsyncTask<Void, Void, String>
    {
        @Override
        protected String doInBackground(Void... voids) {
            try
            {
                return sr.iterateForFrequencies();
            }
            catch (IndexOutOfBoundsException e)
            {
                return "No message detected. Code 1";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            System.out.println(s);
            iterateForFrequenciesFinished(s);
        }
    }
}
