package com.luckynick.shared.net;

import com.luckynick.shared.GSONCustomSerializer;
import com.luckynick.shared.SharedUtils;

import static com.luckynick.custom.Utils.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class TCPConnection implements Closeable {

    public static final String LOG_TAG = "TCPConnection";

    private Socket connSocket;

    protected DataInputStream in;
    protected DataOutputStream out;

    TCPConnection(Socket s) {
        connSocket = s;
        try {
            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public <T> T receive(Class<T> objClass) throws SocketException {
        try {
            String response = in.readUTF();
            if(SharedUtils.CONNECTION_CLOSED.equals(response)) {
                close();
                throw new SocketException("Connection was closed from other side.");
            }
            T result = new GSONCustomSerializer<>(objClass).deserialize(response);
            return result;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> void send(Class<T> objClass, T object) {
        try {
            String json = new GSONCustomSerializer<T>(objClass).serializeStr(object);
            out.writeUTF(json);
            out.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket getSocket() {
        return connSocket;
    }

    @Override
    public String toString() {
        if(connSocket == null) return super.toString();
        return this.connSocket.getInetAddress().getHostAddress() + ":" + this.connSocket.getPort();
    }

    @Override
    public void close() {
        Log(LOG_TAG, "Closing TCP connection " + this);
        send(String.class, CONNECTION_CLOSED);
        try {
            connSocket.getInputStream().close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                connSocket.getOutputStream().close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                try {
                    connSocket.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
