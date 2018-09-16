package com.luckynick.shared.net;

import nl.pvdberg.pnet.client.Client;
import nl.pvdberg.pnet.packet.Packet;

public interface ClientManagerListener {

    void onConnect(Client c, int connectionsNum);

    void onDisconnect(Client c, int connectionsNum);
}
