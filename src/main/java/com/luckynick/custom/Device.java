package com.luckynick.custom;

import com.luckynick.shared.IOClassHandling;
import com.luckynick.shared.IOFieldHandling;
import com.luckynick.shared.SharedUtils;
import com.luckynick.shared.enums.TestRole;

@IOClassHandling(dataStorage = SharedUtils.DataStorage.DEVICES, sendViaNetwork = true)
public class Device {
    @IOFieldHandling(serialize = false, updateOnLoad = true)
    public String macAddress;
    @IOFieldHandling(serialize = false, updateOnLoad = true)
    public String localIP;

    public String vendor;
    public String model;
    public String androidVersion;
    public TestRole roleOfParticipant;
    public boolean isHotspot = false;
}
