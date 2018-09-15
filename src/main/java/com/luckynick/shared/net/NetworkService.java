package com.luckynick.shared.net;

import com.luckynick.shared.SharedUtils;
import com.luckynick.shared.enums.TestRole;

import static com.luckynick.custom.Utils.*;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public abstract class NetworkService {

    public static final String LOG_TAG = "NetworkService";

    //TODO:
    //arp doesn't see hotspot host sometimes, try to initiate connection from hotspot

    public static final String SSID = SharedUtils.SSID, PASSWORD = SharedUtils.PASSWORD;
    public static final boolean THIS_IS_WIFI_HOTSPOT = false;
    public static final String WIFI_SUBNET = SharedUtils.WIFI_SUBNET;
    public static final String configFolder = DataStorage.CONFIG.toString();
    public static final String wifiProfilePath = SharedUtils.formPathString(configFolder, SSID + ".xml");

    private List<Socket> connectionPool = new ArrayList<>();



    public TCPConnection connect(String ip, int port) {
        try {
            Log(LOG_TAG, "Attempt to connect: " + ip);
            return new TCPConnection(new Socket(ip, port));
        }
        catch (ConnectException e) { }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public TCPConnection connect(String ip, int port, int timeout) {
        try {
            Log(LOG_TAG, "Attempt to connect: " + ip);
            Socket toStart = new Socket();
            try {
                toStart.connect(new InetSocketAddress(ip, port), timeout);
            } catch (SocketTimeoutException e) {
                Log(LOG_TAG, "Connection didn't happen before timeout.");
                return null;
            }
            return new TCPConnection(toStart);
        }
        catch (ConnectException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public TCPConnection waitForConnection(final int port) throws ConnectException {
        Thread broadcastThread = new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        broadcast(TestRole.CONTROLLER.toString() + ' ' + port);
                        Thread.sleep(1000);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    catch (InterruptedException e) {
                        return;
                    }
                } while(true);
            }
        });
        broadcastThread.start();
        try {
            ServerSocket ss = new ServerSocket(port);
            Socket s = ss.accept();
            broadcastThread.interrupt();
            Log(LOG_TAG, "Received connection: " + s.getInetAddress().getHostAddress() + ':'
                    + s.getLocalPort());
            return new TCPConnection(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    public static void broadcast(String broadcastMessage) throws IOException {
        for(InetAddress a : listAllBroadcastAddresses()) {
            Log(LOG_TAG, "Broadcasting on " + a.getHostAddress() + ":"
                    + SharedUtils.UDP_COMMUNICATION_PORT + " message: " + broadcastMessage);
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] buffer = broadcastMessage.getBytes();

            DatagramPacket packet
                    = new DatagramPacket(buffer, buffer.length, a, SharedUtils.UDP_COMMUNICATION_PORT);
            socket.send(packet);
            socket.close();
        }

    }

    public static List<InetAddress> listAllBroadcastAddresses() throws SocketException {
        List<InetAddress> broadcastList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces
                = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            for(InterfaceAddress a : networkInterface.getInterfaceAddresses()) {
                InetAddress add = a.getBroadcast();
                if(add != null) broadcastList.add(add);
            }
        }
        return broadcastList;
    }

    /*public static boolean isPortAvailable(String host, int port) {
        // Assume port is available.
        boolean result = true;

        try {
            (new Socket(host, port)).close();

            // Successful connection means the port is taken.
            result = false;
        }
        catch(SocketException e) {
            // Could not connect.
        }

        return result;
    }*/
}
