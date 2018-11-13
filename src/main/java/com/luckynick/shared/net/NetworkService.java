package com.luckynick.shared.net;

import com.luckynick.shared.SharedUtils;
import com.luckynick.shared.enums.TestRole;

import static com.luckynick.custom.Utils.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public abstract class NetworkService {

    public static final String LOG_TAG = "NetworkService";

    public static final String SSID = SharedUtils.SSID, PASSWORD = SharedUtils.PASSWORD;
    public static final boolean THIS_IS_WIFI_HOTSPOT = false;
    public static final String WIFI_SUBNET = SharedUtils.WIFI_SUBNET;
    public static final String configFolder = DataStorage.CONFIG.toString();
    public static final String wifiProfilePath = SharedUtils.formPathString(configFolder, SSID + ".xml");

    private List<Socket> connectionPool = new ArrayList<>();



    public TCPConnection connect(String ip, int port) {
        try {
            Log(LOG_TAG, "Attempt to connect: " + ip);
            return new TCPConnection(new Socket(ip, port));
        }
        catch (ConnectException e) { }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public TCPConnection connect(String ip, int port, int timeout) {
        try {
            Log(LOG_TAG, "Attempt to connect: " + ip);
            Socket toStart = new Socket();
            try {
                toStart.connect(new InetSocketAddress(ip, port), timeout);
            } catch (SocketTimeoutException e) {
                Log(LOG_TAG, "Connection didn't happen before timeout.");
                return null;
            }
            return new TCPConnection(toStart);
        }
        catch (ConnectException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /*public TCPConnection waitForConnection(final int port) throws ConnectException {
        long currentTimestamp = System.currentTimeMillis();
        Thread bThr = UDPServer.broadcastThread(TestRole.CONTROLLER.toString() + " " + port + " "
                + currentTimestamp);
        bThr.start();
        try {
            ServerSocket ss = new ServerSocket(port);
            Socket s = ss.accept();
            bThr.interrupt();
            Log(LOG_TAG, "Received connection: " + s.getInetAddress().getHostAddress() + ':'
                    + s.getLocalPort());
            return new TCPConnection(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }*/

    /**
     * Convert byte array to hex string
     * @param bytes toConvert
     * @return hexValue
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sbuf = new StringBuilder();
        for(int idx=0; idx < bytes.length; idx++) {
            int intVal = bytes[idx] & 0xff;
            if (intVal < 0x10) sbuf.append("0");
            sbuf.append(Integer.toHexString(intVal).toUpperCase());
        }
        return sbuf.toString();
    }

    /**
     * Get utf8 byte array.
     * @param str which to be converted
     * @return  array of NULL if error was found
     */
    public static byte[] getUTF8Bytes(String str) {
        try { return str.getBytes("UTF-8"); } catch (Exception ex) { return null; }
    }

    /**
     * Load UTF8withBOM or any ansi text file.
     * @param filename which to be converted to string
     * @return String value of File
     * @throws IOException if error occurs
     */
    public static String loadFileAsString(String filename) throws IOException {
        final int BUFLEN=1024;
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
            byte[] bytes = new byte[BUFLEN];
            boolean isUTF8=false;
            int read,count=0;
            while((read=is.read(bytes)) != -1) {
                if (count==0 && bytes[0]==(byte)0xEF && bytes[1]==(byte)0xBB && bytes[2]==(byte)0xBF ) {
                    isUTF8=true;
                    baos.write(bytes, 3, read-3); // drop UTF8 bom marker
                } else {
                    baos.write(bytes, 0, read);
                }
                count+=read;
            }
            return isUTF8 ? new String(baos.toByteArray(), "UTF-8") : new String(baos.toByteArray());
        } finally {
            try{ is.close(); } catch(Exception ignored){}
        }
    }

    /**
     * Returns MAC address of the given interface name.
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return  mac address or empty string
     */
    public static String getMACAddress(String interfaceName, Enumeration<NetworkInterface> interfacesEnumer) {
        try {
            List<NetworkInterface> interfaces = Collections.list(interfacesEnumer);
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac==null) return "";
                StringBuilder buf = new StringBuilder();
                for (byte aMac : mac) buf.append(String.format("%02X:",aMac));
                if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
                return buf.toString();
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
        /*try {
            // this is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
    }

    /**
     * Returns MAC address of the given interface name.
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return  mac address or empty string
     */
    public static String getMACAddress(String interfaceName) {
        try {
            return getMACAddress(interfaceName, NetworkInterface.getNetworkInterfaces());
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }
}
