package com.luckynick.shared.net;

import java.net.InetAddress;

public interface UDPMessageObserver {

    public void udpMessageReceived(InetAddress address, String received);
}
