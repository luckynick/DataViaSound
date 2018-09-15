package com.luckynick.shared.net;

import static com.luckynick.custom.Utils.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public abstract class UDPServer extends Thread {

    public static final String LOG_TAG = "UDPServer";

    private List<NetworkMessageObserver> observers = new ArrayList<>();

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

            //resolveReceive(packet.getAddress(), received);
            for(NetworkMessageObserver o : this.observers) {
                o.udpMessageReceived(packet.getAddress(), received.trim());
            }

            /*try {
                socket.send(packet);
            }
            catch (IOException e) {
                e.printStackTrace();
            }*/
        }
        socket.close();
    }

    public void stopServer() {
        this.running = false;
        socket.close();
        this.interrupt();
    }

    public void addMessageObserver(NetworkMessageObserver ob) {
        this.observers.add(ob);
    }
}