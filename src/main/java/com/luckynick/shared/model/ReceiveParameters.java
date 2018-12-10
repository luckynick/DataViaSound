package com.luckynick.shared.model;

import com.luckynick.shared.IOClassHandling;
import com.luckynick.shared.SharedUtils;
import com.luckynick.shared.enums.SoundConsumptionUnit;
import com.luckynick.shared.enums.SoundProductionUnit;

/**
 * Not serialized independently, but inside of test result
 */
@IOClassHandling(sendViaNetwork = true, dataStorage = SharedUtils.DataStorage.NONE)
public class ReceiveParameters {

    public SoundConsumptionUnit soundConsumptionUnit = SoundConsumptionUnit.MICROPHONE;
    public int frequenciesBindingShift = SharedUtils.DEFAULT_FREQ_BINDING_BASE;
    public double frequenciesBindingScale = SharedUtils.DEFAULT_FREQ_BINDING_SCALE;

}
