package com.luckynick.shared.net;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class ConnectionManager implements Closeable {

    private List<TCPConnection> deviceConnections = new ArrayList<>();

    public void addConnection(TCPConnection conn) {
        deviceConnections.add(conn);
    }

    public TCPConnection getConnection(int index) {
        return deviceConnections.get(index);
    }

    public Iterable<TCPConnection> getConnectionIterator() {
        return deviceConnections;
    }


    @Override
    public void close() {
        for(TCPConnection c : deviceConnections) {
            c.close();
        }
    }
}
