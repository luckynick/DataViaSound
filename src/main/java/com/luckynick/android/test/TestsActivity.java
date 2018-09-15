package com.luckynick.android.test;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.TextView;

import com.luckynick.android.test.net.AndroidNetworkService;
import com.luckynick.android.test.net.AndroidUDPServer;
import com.luckynick.custom.Device;
import com.luckynick.shared.net.NetworkMessageObserver;
import com.luckynick.shared.PureFunctionalInterface;
import com.luckynick.shared.SharedUtils;
import com.luckynick.shared.net.TCPConnection;
import com.luckynick.shared.enums.TestRole;

import java.net.InetAddress;

import static com.luckynick.custom.Utils.*;

public class TestsActivity extends BaseActivity implements NetworkMessageObserver {

    public static final String LOG_TAG = "Tests";

    private TCPConnection connectionToController;
    private boolean controllerConnected = false;

    AndroidNetworkService network;
    AndroidUDPServer udpServer;

    private TextView textStatus;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.asHotspotCkeckbox).setChecked(getAsHotspot());
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        terminate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tests);
        this.textStatus = (TextView) findViewById(R.id.testsText);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        prepareForTests();
    }

    private void prepareForTests() {
        this.network = new AndroidNetworkService(getApplicationContext());

        if(getAsHotspot()) startHotspot();
        else persistConnectWifi();

        udpServer = new AndroidUDPServer();
        new AsyncUDPWaiter().execute();
    }

    private void persistConnectWifi() {
        this.textStatus.setText("Connecting to WiFi...");
        new AsyncConnectWIFI().execute();
    }

    private void startHotspot() {
        this.textStatus.setText("Starting hotspot...");
        network.turnWifiAp(true);
    }

    private void stopTest() {
        udpServer.stopServer();
        network.turnWifiAp(false);
        connectionToController.close();
        controllerConnected = false;
    }

    private void terminate() {
        Log(LOG_TAG, "Terminating...");
        stopTest();
    }

    @Override
    public void udpMessageReceived(InetAddress address, String received) {
        final String ip = address.getHostAddress();
        Log(LOG_TAG, "Via UDP: "+ip + " " + received);
        if(controllerConnected) {
            Log(LOG_TAG, "Already connected.");
            return;
        }
        String params[] = received.split("\\s");
        final String role = params[0], port = params[1];
        if(TestRole.CONTROLLER.toString().equals(role)) {
            this.connectionToController = network.connect(ip, Integer.parseInt(port));
            /*if(this.connectionToController != null) {
                Log(LOG_TAG, "Fuck was connected:");
                Log(LOG_TAG, ""+connectionToController.getSocket().isBound());
                Log(LOG_TAG, ""+connectionToController.getSocket().isConnected());

                controllerConnected = true;
            }
            else return;*/
            this.connectionToController.send(Device.class, getFilledDevice());
            Log(LOG_TAG, "Connected to: " + ip + ':' + port);
            writeStatus("Connected to: " + ip + ':' + port);
            controllerConnected = true;
        }
    }

    public void writeStatus(final String toWrite) {
        TestsActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TestsActivity.this.textStatus.setText(toWrite);
            }
        });
    }

    public Device getFilledDevice() {
        Device toFill = new Device();
        toFill.isHotspot = false;
        if(getAsHotspot()) toFill.isHotspot = true;

        return toFill;
    }

    protected class AsyncUDPWaiter extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... voids) {
            udpServer.start();
            udpServer.addMessageObserver(TestsActivity.this);
            return null;
        }
    }

    protected class AsyncConnectWIFI extends AsyncTask<Void, Void, Void> implements PureFunctionalInterface
    {
        private boolean connected = false;
        @Override
        protected Void doInBackground(Void... voids) {
            network.addWifiConnectedSub(this);
            if(!network.isWifiConnected()) {
                network.connectWiFi(SharedUtils.SSID, SharedUtils.PASSWORD);
                while(!connected) {
                    try {
                        Thread.sleep(WAIT_TIME_AFTER_FAIL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            writeStatus("Connected to SSID " + SharedUtils.SSID);
            return null;
        }

        @Override
        public void performProgramTasks() {
            connected = true;
        }
    }
}
