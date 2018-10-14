package com.luckynick.android.test.net;

import android.net.wifi.WifiManager;

import com.luckynick.shared.net.UDPMessageObserver;
import com.luckynick.shared.net.UDPServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

import static com.luckynick.custom.Utils.Log;
import static com.luckynick.shared.SharedUtils.UDP_COMMUNICATION_PORT;

public class AndroidUDPServer extends UDPServer {

    WifiManager.MulticastLock udpLock;

    public AndroidUDPServer(WifiManager.MulticastLock lock) {
        super();
        udpLock = lock;
    }

    @Override
    public void run() {
        running = true;
        if(udpLock != null) udpLock.acquire();

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
        if(udpLock != null) udpLock.release();
    }

    /*@Override
    public void resolveReceive(InetAddress address, String received) {
        SharedUtils.Log(LOG_TAG, address.getHostAddress() + " " + received);
    }*/
}
