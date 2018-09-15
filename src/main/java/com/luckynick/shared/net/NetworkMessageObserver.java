package com.luckynick.shared.net;

import java.net.InetAddress;

public interface NetworkMessageObserver {

    public void udpMessageReceived(InetAddress address, String received);
}
