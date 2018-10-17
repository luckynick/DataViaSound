package com.luckynick.android.test;

import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.luckynick.android.test.net.AndroidNetworkService;
import com.luckynick.android.test.net.AndroidUDPServer;
import com.luckynick.custom.Device;
import com.luckynick.shared.GSONCustomSerializer;
import com.luckynick.shared.enums.PacketID;
import com.luckynick.shared.net.UDPMessageObserver;
import com.luckynick.shared.PureFunctionalInterface;
import com.luckynick.shared.SharedUtils;
import com.luckynick.shared.enums.TestRole;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;

import nl.pvdberg.pnet.client.Client;
import nl.pvdberg.pnet.client.util.PlainClient;
import nl.pvdberg.pnet.event.PNetListener;
import nl.pvdberg.pnet.packet.Packet;
import nl.pvdberg.pnet.packet.PacketBuilder;
import nl.pvdberg.pnet.packet.PacketReader;

import static com.luckynick.custom.Utils.*;

public class TestsActivity extends BaseActivity implements UDPMessageObserver, PNetListener {

    public static final String LOG_TAG = "Tests";

    //private TCPConnection connectionToController;
    //private volatile boolean controllerConnected = false;

    AndroidNetworkService network;
    AndroidUDPServer udpServer;

    long connectionTimestamp = 0;
    Client cli = new PlainClient();

    private TextView textStatus;

    private boolean nowISendInTest = false;
    private boolean nowIRecvInTest = false;
    private String messageToSend = null;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.asHotspotCkeckbox).setChecked(getAsHotspot());
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log(LOG_TAG, "onPause event.");
        terminate();
    }

    @Override
    public void onResume(){
        super.onResume();

        prepareForTests();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tests);
        this.textStatus = (TextView) findViewById(R.id.testsText);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //prepareForTests();
    }

    private void prepareForTests() {
        this.network = new AndroidNetworkService(getApplicationContext());
        cli.setClientListener(this);

        if(getAsHotspot()) {
            startHotspot();
            new AsyncUDPWaiter().execute(null, null);
        }
        else if(network.isApOn()) {
            new AsyncUDPWaiter().execute(null, null);
        }
        else {
            WifiManager.MulticastLock lock = persistConnectWifi();
            new AsyncUDPWaiter().execute(lock);
        }

    }

    private WifiManager.MulticastLock persistConnectWifi() {
        this.textStatus.setText("Connecting to WiFi...");
        try {
            return new AsyncConnectWIFI().execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("MulticastLock has to be returned..");
    }

    private void startHotspot() {
        this.textStatus.setText("Starting hotspot...");
        network.turnWifiAp(true);
    }

    private void stopTest() {
        cli.close();
        //if(connectionToController != null) connectionToController.close();
        //controllerConnected = false;
        connectionTimestamp = 0;
    }

    private void terminate() {
        Log(LOG_TAG, "Terminating...");
        if(udpServer != null) udpServer.stopServer();
        network.turnWifiAp(false);
        stopTest();
    }

    @Override
    public void udpMessageReceived(InetAddress address, String received) {
        final String ip = address.getHostAddress();
        Log(LOG_TAG, "Via UDP from "+ ip + ":  " + received);
        String params[] = received.split("\\s");
        final String role = params[0];
        if(TestRole.CONTROLLER.toString().equals(role)) {
            final String port = params[1], timestampS = params[2];
            long timestamp = Long.parseLong(timestampS);
            if(connectionTimestamp != timestamp) {
                if(cli.isConnected()) cli.close();
                cli.connect(ip, Integer.parseInt(port));
                connectionTimestamp = timestamp;
            }
            else {
                Log(LOG_TAG, "Already connected.");
            }
        }
        //cli.isConnected()
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
        toFill.isHotspot = getAsHotspot();


        return toFill;
    }

    @Override
    public void onConnect(Client c) {
        Log(LOG_TAG, c.getInetAddress().getHostAddress() + ":" + c.getSocket().getPort() + " connected.");
    }

    @Override
    public void onDisconnect(Client c) {
        Log(LOG_TAG, "Disconnected.");
        stopTest();
    }

    @Override
    public void onReceive(Packet p, Client c) throws IOException {
        Log(LOG_TAG, "Received "+p.toString());
        PacketReader packetReader = new PacketReader(p);

        int id = packetReader.getPacketID();
        if(id == PacketID.REQUEST.ordinal()){
            Log(LOG_TAG, "Request received.");
            int expectedAction = packetReader.readInt();
            switch (PacketID.ordinalToEnum(expectedAction)) {
                case DEVICE:
                    writeStatus("Sending back device data");
                    Log(LOG_TAG, "Sending back device data");
                    c.send(new PacketBuilder(Packet.PacketType.Request).withID((short)PacketID.RESPONSE.ordinal())
                            .withInt(PacketID.DEVICE.ordinal())
                            .withString(new GSONCustomSerializer<>(Device.class)
                                    .serializeStr(getFilledDevice()))
                            .build());
                    break;
                case PREP_SEND_MESSAGE:
                    nowISendInTest = true;
                    messageToSend = packetReader.readString();
                    Log(LOG_TAG, "Ready to send message '" + messageToSend + "'");
                    c.send(new PacketBuilder(Packet.PacketType.Request).withID((short)PacketID.RESPONSE.ordinal())
                            .withInt(PacketID.OK.ordinal())
                            .withInt(PacketID.PREP_SEND_MESSAGE.ordinal())
                            .build());
                    break;
                case PREP_RECEIVE_MESSAGE:
                    nowIRecvInTest = true;
                    Log(LOG_TAG, "Ready to receive message");
                    c.send(new PacketBuilder(Packet.PacketType.Request).withID((short)PacketID.RESPONSE.ordinal())
                            .withInt(PacketID.OK.ordinal())
                            .withInt(PacketID.PREP_RECEIVE_MESSAGE.ordinal())
                            .build());
                    break;
                case EXECUTE:
                    Log(LOG_TAG, "Executing.");
                    if(nowISendInTest) {
                        Log(LOG_TAG, "Sending message '"+messageToSend+"' for test.");
                        new AsyncPlayMessage().execute(messageToSend);
                    }
                    else if(nowIRecvInTest) {
                        Log(LOG_TAG, "Receiving message for test.");
                        //new AsyncPlayMessage().execute(messageToSend);
                    }
                    break;
            }
        }
    }

    protected class AsyncUDPWaiter extends AsyncTask<WifiManager.MulticastLock, Void, Void>
    {
        @Override
        protected Void doInBackground(WifiManager.MulticastLock... params) {
            udpServer = new AndroidUDPServer(params[0]);
            udpServer.start();
            udpServer.addMessageObserver(TestsActivity.this);
            return null;
        }
    }

    protected class AsyncConnectWIFI extends AsyncTask<Void, Void, WifiManager.MulticastLock> implements PureFunctionalInterface
    {
        private boolean connected = false;
        @Override
        protected WifiManager.MulticastLock doInBackground(Void... voids) {
            WifiManager.MulticastLock result = network.connectWiFi(SharedUtils.SSID, SharedUtils.PASSWORD);
            //network.addWifiConnectedSub(this);

            writeStatus("Connected to SSID " + SharedUtils.SSID);
            return result;
        }

        @Override
        public void performProgramTasks() {
            connected = true;
        }
    }
}
