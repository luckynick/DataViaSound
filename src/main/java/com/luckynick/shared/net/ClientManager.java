package com.luckynick.shared.net;

import com.luckynick.shared.SharedUtils;
import com.luckynick.shared.enums.PacketID;
import nl.pvdberg.pnet.client.Client;
import nl.pvdberg.pnet.event.DistributerListener;
import nl.pvdberg.pnet.event.PNetListener;
import nl.pvdberg.pnet.event.PacketDistributer;
import nl.pvdberg.pnet.event.PacketHandler;
import nl.pvdberg.pnet.packet.Packet;
import nl.pvdberg.pnet.server.Server;
import nl.pvdberg.pnet.server.util.PlainServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.luckynick.custom.Utils.Log;

public class ClientManager extends PoolManager<Client>  {

    public static final String LOG_TAG = "ClientManager";

    Server server = null;

    private List<ClientManagerListener> listeners = new ArrayList<>();

    PacketDistributer packetDistributer = new PacketDistributer();

    public ClientManager(PacketHandler defaultHandler) {
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
            packetDistributer.setDefaultHandler(defaultHandler);
            server.start(SharedUtils.TCP_COMMUNICATION_PORT);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void subscribeConnectionEvents(ClientManagerListener listener) {
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
        for (ClientManagerListener l : listeners) {
            l.onConnect(c, listeners.size());
        }
    }

    public void onDisconnect(Client c) {
        super.remove(c);
        Log(LOG_TAG, c.getInetAddress().getHostAddress() + ":" + c.getSocket().getPort() + " disconnected.");
        for (ClientManagerListener l : listeners) {
            l.onDisconnect(c, listeners.size());
        }
    }

    /*@Override
    public void onReceive(Packet p, Client c) throws IOException {
        for (ClientManagerListener l : listeners) {
            l.onReceive(p, c);
        }
    }*/
}
