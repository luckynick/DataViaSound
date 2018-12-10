package com.luckynick.android.test;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

public class MainActivity extends BaseActivity implements View.OnClickListener{
    public static final String LOG_TAG = "Main";

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
        int cachedLoudnessLevel = sharedPrefs.getInt("calibratedLoudness", -1);
        if(cachedLoudnessLevel == -1){
            Intent intent = new Intent(this, CalibrationActivity.class);
            startActivity(intent);
        }
        else {
            setLoudnessLevel(cachedLoudnessLevel);
        }
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
                break;
        }
    }


}
