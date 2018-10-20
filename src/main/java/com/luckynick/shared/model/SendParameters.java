package com.luckynick.shared.model;

import com.luckynick.custom.Device;
import com.luckynick.shared.IOClassHandling;
import com.luckynick.shared.SharedUtils;
import com.luckynick.shared.enums.SoundConsumptionUnit;
import com.luckynick.shared.enums.SoundProductionUnit;

/**
 * Not serialized independently, but inside of test result
 */
@IOClassHandling(sendViaNetwork = true, dataStorage = SharedUtils.DataStorage.NONE)
public class SendParameters {

    public SoundProductionUnit soundProductionUnit;
    public int loudnessLevel;
    public String message;

}
