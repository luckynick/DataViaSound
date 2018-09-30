package com.luckynick.custom;

import android.os.Build;

import com.luckynick.android.test.BuildConfig;
import com.luckynick.shared.IOClassHandling;
import com.luckynick.shared.IOFieldHandling;
import com.luckynick.shared.SharedUtils;
import com.luckynick.shared.enums.TestRole;
import com.luckynick.shared.net.NetworkService;

@IOClassHandling(dataStorage = SharedUtils.DataStorage.DEVICES, sendViaNetwork = true)
public class Device {
    public String macAddress;
    @IOFieldHandling(updateOnLoad = true)
    public String localIP;

    public String vendor;
    public String model;
    public String os;
    public String osVersion;
    public TestRole roleOfParticipant;
    public boolean isHotspot = false;

    public String nameOfModelClass = this.getClass().getSimpleName();
    @IOFieldHandling(serialize = false)
    protected String filenamePrefix = nameOfModelClass;
    @IOFieldHandling(serialize = false)
    public String fileRoot = SharedUtils.DataStorage.DEVICES.toString();
    @IOFieldHandling(serialize = false)
    public String filename= filenamePrefix + SharedUtils.JSON_EXTENSION;
    public String wholePath = fileRoot + filename;

    public Device() {
        macAddress = NetworkService.getMACAddress("wlan0");
        localIP = NetworkService.getIPAddress(true);
        roleOfParticipant = TestRole.PEER;
        os = "Android";
        osVersion = Build.VERSION.RELEASE;
        vendor = Build.MANUFACTURER;
        model = Build.MODEL;
        setFilename();
    }

    public void setFilename() {
        setFilename("device_" + vendor + '_' + model + '_' + SharedUtils.getDateStringForFileName()
                + SharedUtils.JSON_EXTENSION);
    }

    public void setFilename(String filename) {
        this.filename = filename;
        wholePath = fileRoot + filename;
    }
}
