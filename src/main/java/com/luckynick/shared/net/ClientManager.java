package com.luckynick.shared.net;

import com.luckynick.shared.enums.PacketID;
import nl.pvdberg.pnet.client.Client;
import nl.pvdberg.pnet.event.DistributerListener;
import nl.pvdberg.pnet.event.PacketDistributer;
import nl.pvdberg.pnet.event.PacketHandler;
import nl.pvdberg.pnet.packet.Packet;
import nl.pvdberg.pnet.packet.PacketBuilder;
import nl.pvdberg.pnet.server.Server;
import nl.pvdberg.pnet.server.util.PlainServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.luckynick.custom.Utils.Log;

@Deprecated
public class ClientManager extends PoolManager<Client>  {

    public static final String LOG_TAG = "ClientManager";

    Server server = null;

    private List<PacketListener> listeners = new ArrayList<>();

    PacketDistributer packetDistributer = new PacketDistributer();

    public ClientManager(int port) {
        super();

        try {
            server = new PlainServer();
            server.setListener(new DistributerListener(packetDistributer){
                @Override
                public void onConnect(final Client c)
                {
                    ClientManager.this.onConnect(c);
                }

                @Override
                public void onDisconnect(final Client c)
                {
                    ClientManager.this.onDisconnect(c);
                }
            });
            packetDistributer.setDefaultHandler((Packet p, Client c) -> {
                Log(LOG_TAG, "Received packet (" + p + "), DOING NOTHING!");
            });
            server.start(port);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restart(int port) {
        server.stop();
        server.start(port);
    }

    public void subscribeConnectionEvents(PacketListener listener) {
        listeners.add(listener);
    }

    public void subscribePacketListener(PacketID id, PacketHandler handler) {
        packetDistributer.addHandler((short)id.ordinal(), handler);
        //listeners.put((short)id.ordinal(), handler);
    }


    public boolean close(Client obj) {
        if(obj != null) {
            obj.close();
            return true;
        }
        return false;
    }

    public void close() {
        for(Client obj: super.getConnectionIterator()) {
            close(obj);
        }
    }

    public void onConnect(Client c) {
        super.add(c);
        Log(LOG_TAG, c.getInetAddress().getHostAddress() + ":" + c.getSocket().getPort() + " connected.");
        for (PacketListener l : listeners) {
            //l.onConnect(c, listeners.size());
        }
    }

    public void onDisconnect(Client c) {
        super.remove(c);
        Log(LOG_TAG, c.getInetAddress().getHostAddress() + ":" + c.getSocket().getPort() + " disconnected.");
        for (PacketListener l : listeners) {
            //l.onDisconnect(c, listeners.size());
        }
    }


    public static void sendPullRequest(Client c, PacketID requiredResponse) {
        c.send(createRequestPacket(PacketID.REQUEST).withInt(requiredResponse.ordinal()).build());
    }

    public static PacketBuilder createRequestPacket(PacketID id) {
        PacketBuilder pb = new PacketBuilder(Packet.PacketType.Request);
        pb.withID((short)id.ordinal());
        return pb;
    }

    public static Packet createRequestPacket(PacketID id, String ... entries) {
        PacketBuilder pb = createRequestPacket(id);
        for(String s : entries) {
            pb.withString(s);
        }
        return pb.build();
    }

    /*@Override
    public void onReceive(Packet p, Client c) throws IOException {
        for (PacketListener l : listeners) {
            l.onReceive(p, c);
        }
    }*/
}
