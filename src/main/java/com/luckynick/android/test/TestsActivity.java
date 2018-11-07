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
import com.luckynick.shared.model.ReceiveParameters;
import com.luckynick.shared.model.ReceiveSessionSummary;
import com.luckynick.shared.model.SendParameters;
import com.luckynick.shared.model.SendSessionSummary;
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

//TODO:
//BUG: loudness level doesn't take an effect

public class TestsActivity extends BaseActivity implements UDPMessageObserver, PNetListener, SoundGenerator.Listener {

    public static final String LOG_TAG = "Tests";

    AndroidNetworkService network;
    AndroidUDPServer udpServer;

    volatile long connectionTimestamp = 0;
    volatile Client cli = new PlainClient();

    private TextView textStatus;

    private volatile boolean nowISendInTest = false;
    private volatile boolean nowIRecvInTest = false;
    //private String messageToSend = null;
    volatile long sessionStartTimestamp = -1;
    volatile SendParameters sendParams;
    volatile ReceiveParameters receiveParameters;

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
        SoundGenerator.subscribePlayStoppedEvent(this);

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
        sr.stopRecord();
        connectionTimestamp = 0;
        System.gc();
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
                connectionTimestamp = timestamp;
                if(cli.isConnected()) cli.close();
                cli.connect(ip, Integer.parseInt(port));
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

    private void resetState() {
        nowISendInTest = false;
        nowIRecvInTest = false;
        sessionStartTimestamp = -1;
        sendParams = null;
        receiveParameters = null;
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
                    resetState();
                    nowISendInTest = true;
                    GSONCustomSerializer<SendParameters> sendPSerializer = new GSONCustomSerializer<>(SendParameters.class);
                    String sendPSer = packetReader.readString();
                    sendParams = sendPSerializer.deserialize(sendPSer);
                    Log(LOG_TAG, "Ready to send message '" + sendParams.message + "'");
                    writeStatus("Ready to send message '" + sendParams.message + "'");
                    c.send(new PacketBuilder(Packet.PacketType.Request).withID((short)PacketID.RESPONSE.ordinal())
                            .withInt(PacketID.OK.ordinal())
                            .withInt(PacketID.PREP_SEND_MESSAGE.ordinal())
                            .build());
                    break;
                case PREP_RECEIVE_MESSAGE:
                    resetState();
                    nowIRecvInTest = true;
                    GSONCustomSerializer<ReceiveParameters> recvPSerializer = new GSONCustomSerializer<>(ReceiveParameters.class);
                    String recvPSer = packetReader.readString();
                    receiveParameters = recvPSerializer.deserialize(recvPSer);
                    Log(LOG_TAG, "Ready to receive message");
                    writeStatus("Ready to receive message");
                    c.send(new PacketBuilder(Packet.PacketType.Request).withID((short)PacketID.RESPONSE.ordinal())
                            .withInt(PacketID.OK.ordinal())
                            .withInt(PacketID.PREP_RECEIVE_MESSAGE.ordinal())
                            .build());
                    break;
                case SEND_MESSAGE:
                    if(nowISendInTest) {
                        Log(LOG_TAG, "Sending message '"+sendParams.message+"' for test.");
                        writeStatus("Sending message '"+sendParams.message+"' for test.");
                        sessionStartTimestamp = System.currentTimeMillis();
                        new AsyncPlayMessage(sendParams).execute(sendParams.message);
                        c.send(new PacketBuilder(Packet.PacketType.Request).withID((short)PacketID.RESPONSE.ordinal())
                                .withInt(PacketID.OK.ordinal())
                                .withInt(PacketID.SEND_MESSAGE.ordinal())
                                .build());
                    }
                    else {
                        Log(LOG_TAG, "This device is not ready to receive a text.");
                        c.send(new PacketBuilder(Packet.PacketType.Request).withID((short)PacketID.RESPONSE.ordinal())
                                .withInt(PacketID.ERROR.ordinal())
                                .withInt(PacketID.SEND_MESSAGE.ordinal())
                                .build());
                    }
                    break;
                case RECEIVE_MESSAGE:
                    if(nowIRecvInTest) {
                        Log(LOG_TAG, "Receiving message for test.");
                        writeStatus("Receiving message for test.");
                        //new AsyncPlayMessage().execute(messageToSend);

                        sessionStartTimestamp = System.currentTimeMillis();
                        new AsyncRecord().execute();
                        c.send(new PacketBuilder(Packet.PacketType.Request).withID((short)PacketID.RESPONSE.ordinal())
                                .withInt(PacketID.OK.ordinal())
                                .withInt(PacketID.RECEIVE_MESSAGE.ordinal())
                                .build());
                    }
                    else {
                        Log(LOG_TAG, "This device is not ready to send a text.");
                        c.send(new PacketBuilder(Packet.PacketType.Request).withID((short)PacketID.RESPONSE.ordinal())
                                .withInt(PacketID.ERROR.ordinal())
                                .withInt(PacketID.RECEIVE_MESSAGE.ordinal())
                                .build());
                    }
                    break;
                case TEXT:
                    Log(LOG_TAG, "Generating the text.");
                    writeStatus("Generating the text.");
                    sr.stopRecord();

                    new AsyncIterateForFrequencies().execute();
                    break;
                default:
                    Log(LOG_TAG, "Unknown REQUEST: " + PacketID.ordinalToEnum(expectedAction));
            }
        }
    }

    /**
     * Invoked when AsyncIterateForFrequencies finishes it's work.
     * @param message decoded message
     */
    @Override
    public void iterateForFrequenciesFinished(String message, Exception e) {
        Log(LOG_TAG, "iterateForFrequencies was finished. Result: " + message + ", exc: " + e);
        //((TextView)(findViewById(R.id.detectedText))).setText(message);
        //finished resolving the text
        String m = message;
        if(m == null) m = "null";
        ReceiveSessionSummary summary = new ReceiveSessionSummary();
        summary.message = m;
        summary.summarySource = getFilledDevice();
        summary.sessionStartDate = sessionStartTimestamp;
        summary.receiveParameters = receiveParameters;
        summary.exceptionDuringDecoding = e;
        GSONCustomSerializer<ReceiveSessionSummary> serializer = new GSONCustomSerializer<>(ReceiveSessionSummary.class);
        String serializedSummary = serializer.serializeStr(summary);
        new AsyncSendPacket().execute(new PacketBuilder(Packet.PacketType.Request).withID((short)PacketID.RESPONSE.ordinal())
                .withInt(PacketID.TEXT.ordinal())
                .withString(serializedSummary)
                .build());
    }

    @Override
    public void playStopped() {
        Log(LOG_TAG, "Play was stopped.");

        SendSessionSummary summary = new SendSessionSummary();
        summary.summarySource = getFilledDevice();
        summary.sessionStartDate = sessionStartTimestamp;
        summary.sendParameters = sendParams;
        GSONCustomSerializer<SendSessionSummary> serializer = new GSONCustomSerializer<>(SendSessionSummary.class);
        String serializedSummary = serializer.serializeStr(summary);

        cli.send(new PacketBuilder(Packet.PacketType.Request).withID((short)PacketID.RESPONSE.ordinal())
                .withInt(PacketID.JOIN.ordinal())
                .withInt(PacketID.SEND_MESSAGE.ordinal())
                .withString(serializedSummary)
                .build());
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

    protected class AsyncSendPacket extends AsyncTask<Packet, Void, Void> implements PureFunctionalInterface
    {
        private boolean connected = false;
        @Override
        protected Void doInBackground(Packet... voids) {
            cli.send(voids[0]);

            return null;
        }

        @Override
        public void performProgramTasks() {
            connected = true;
        }
    }
}
