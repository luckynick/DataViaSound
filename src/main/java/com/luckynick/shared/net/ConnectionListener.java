package com.luckynick.shared.net;

import nl.pvdberg.pnet.client.Client;
import nl.pvdberg.pnet.packet.Packet;

public interface ConnectionListener {

    void onConnect(Client c);

    void onDisconnect(Client c);

}
