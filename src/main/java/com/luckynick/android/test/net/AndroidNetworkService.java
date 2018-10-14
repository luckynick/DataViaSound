package com.luckynick.android.test.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.luckynick.shared.net.NetworkService;
import com.luckynick.shared.PureFunctionalInterface;
import com.luckynick.shared.SharedUtils;
import com.luckynick.shared.net.TCPConnection;

import static com.luckynick.custom.Utils.*;

import java.lang.reflect.Method;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AndroidNetworkService extends NetworkService {
    public static final String LOG_TAG = "AndroidNetworkService";

    WifiManager wifiManager;
    ConnectivityManager connManager;
    //public static WifiManager.MulticastLock lock;

    private List<PureFunctionalInterface> wifiConnectedSubs = new ArrayList<>();

    public AndroidNetworkService(Context applicationContext) {
        this.wifiManager = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);

        this.connManager = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }



    /*@RequiresApi(api = Build.VERSION_CODES.O)
    public boolean startWifiApAPI26() {
        wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback(){

            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                super.onStarted(reservation);
                Log.d(LOG_TAG, "Wifi Hotspot is on now");
                mReservation = reservation;
            }

            @Override
            public void onStopped() {
                super.onStopped();
                Log.d(LOG_TAG, "onStopped: ");
            }

            @Override
            public void onFailed(int reason) {
                super.onFailed(reason);
                Log.d(LOG_TAG, "onFailed: ");
            }
        },new Handler());
        return false;
    }*/

    public boolean isWifiConnected(String ssid) {
        NetworkInfo i = connManager.getActiveNetworkInfo();
        WifiInfo wi = wifiManager.getConnectionInfo();
        boolean connectedSomewhere = i != null ? i.isConnectedOrConnecting() && (i.getType() == ConnectivityManager.TYPE_WIFI) : false;
        if(!connectedSomewhere) return false;
        Log(LOG_TAG, "isWifiConnected: " + wi.getSSID() + "==" + "\""+ssid+"\"");
        boolean connectedToSpecific =  ("\""+ssid+"\"").equals(wi.getSSID());
        Log(LOG_TAG, "isWifiConnected: " + connectedToSpecific);
        return connectedToSpecific;
    }

    public boolean isWifiEnabled() {
        return wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED ||
                wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING;
    }

    public WifiManager.MulticastLock connectWiFi(String ssid, String password) {
        Log(LOG_TAG, "Connecting to SSID: " + ssid);
        if(isApOn()) turnWifiAp(false);

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", password);
        //remember id
        int netId = wifiManager.addNetwork(wifiConfig);
        if(!isWifiEnabled()) wifiManager.setWifiEnabled(true);
        if(!isWifiConnected(ssid)) {
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
        }
        for(int i = 0; !isWifiConnected(ssid); i++) {
            try {
                Thread.sleep(WAIT_TIME_AFTER_FAIL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return wifiManager.createMulticastLock("Multitask lock");
        //this.wifiConnectedEvent();
    }

    private boolean createNewNetwork(String ssid, String password) {
        wifiManager.setWifiEnabled(false); // turn off Wifi
        if (isApOn()) {
            turnWifiAp(false);
        } else {
            Log.e(LOG_TAG, "WifiAp is turned off");
        }
        // creating new wifi configuration
        WifiConfiguration myConfig = new WifiConfiguration();
        myConfig.SSID = ssid; // SSID name of netwok
        myConfig.preSharedKey = password; // password for network
        myConfig.allowedKeyManagement.set(4); // 4 is for KeyMgmt.WPA2_PSK which is not exposed by android KeyMgmt class
        myConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN); // Set Auth Algorithms to open
        try {
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            return (Boolean) method.invoke(wifiManager, myConfig, true);  // setting and turing on android wifiap with new configrations
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    public boolean turnWifiAp(boolean stateOn) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            //if(!stateOn && mReservation != null) mReservation.close();
        } else{
            if(stateOn) createNewNetwork(SharedUtils.SSID, SharedUtils.PASSWORD);
            WifiConfiguration wificonfiguration = null;
            try {
                Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method.invoke(wifiManager, wificonfiguration, stateOn);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Check whether wifi hotspot on or off. Test this.
     * @return
     */
    public boolean isApOn() {
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void addWifiConnectedSub(PureFunctionalInterface wifiConnectedSub) {
        this.wifiConnectedSubs.add(wifiConnectedSub);
    }

    private void wifiConnectedEvent() {
        for(PureFunctionalInterface o : this.wifiConnectedSubs) {
            o.performProgramTasks();
        }
    }

    /*public String getMac() {
        return wifiManager.getConnectionInfo().getMacAddress();
    }

    public String getIP() {
        return wifiManager.getConnectionInfo().get
    }*/


    @Override
    public TCPConnection waitForConnection(int port) throws ConnectException {
        try {
            new AsyncWaitConnection().execute(port).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class AsyncWaitConnection extends AsyncTask<Integer, Void, TCPConnection>
    {
        @Override
        protected TCPConnection doInBackground(Integer... ints)  {
            /*Socket s = null;
            try {
                ServerSocket ss = new ServerSocket(ints[0]);
                s = ss.accept();
                SharedUtils.Log(LOG_TAG, "Connected: " + s.isConnected());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;*/
            try {
                return AndroidNetworkService.this.waitForConnection(ints[0]);
            } catch (ConnectException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
