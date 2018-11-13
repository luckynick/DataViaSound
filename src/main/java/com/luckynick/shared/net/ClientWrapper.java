package com.luckynick.shared.net;

import nl.pvdberg.pnet.client.Client;

public class ClientWrapper {

    int hashCode = 0;

    public Client getWrapped() {
        return wrapped;
    }

    Client wrapped;

    public ClientWrapper(Client c) {
        String toHash = c.getSocket().getInetAddress().getHostAddress() + ":" + c.getSocket().getPort();
        hashCode = toHash.hashCode();
        wrapped = c;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return obj.hashCode() == hashCode();
    }
}
