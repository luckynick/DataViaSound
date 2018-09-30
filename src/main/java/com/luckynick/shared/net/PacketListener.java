package com.luckynick.shared.net;

import nl.pvdberg.pnet.client.Client;
import nl.pvdberg.pnet.packet.Packet;

public interface PacketListener {

    void onRequestPacket(Client c, Packet p);

    void onResponsePacket(Client c, Packet p);

    void onUnexpectedPacket(Client c, Packet p);
}
