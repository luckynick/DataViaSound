package com.luckynick.shared.net;

import com.luckynick.shared.SharedUtils;

import static com.luckynick.custom.Utils.*;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public abstract class UDPServer extends Thread {

    public static final String LOG_TAG = "UDPServer";

    private List<UDPMessageObserver> observers = new ArrayList<>();

    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];

    public UDPServer() {
        try {
            socket = new DatagramSocket(UDP_COMMUNICATION_PORT);
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        running = true;

        while (running) {
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (SocketException e) {
                Log(LOG_TAG, "UDP socket was unexpectedly closed.");
                return;

            } catch (IOException e) {
                e.printStackTrace();
            }

            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);
            String received
                    = new String(packet.getData(), 0, packet.getLength());

            for(UDPMessageObserver o : this.observers) {
                o.udpMessageReceived(packet.getAddress(), received.trim());
            }
        }
        socket.close();
    }

    public void stopServer() {
        this.running = false;
        socket.close();
        this.interrupt();
    }

    public void addMessageObserver(UDPMessageObserver ob) {
        this.observers.add(ob);
    }

    public static Thread broadcastThread(final String message) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        broadcast(message);
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
}