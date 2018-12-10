package com.luckynick.shared.model;

import com.luckynick.shared.IOClassHandling;
import com.luckynick.shared.SharedUtils;
import com.luckynick.shared.enums.SoundProductionUnit;

@IOClassHandling(sendViaNetwork = true, dataStorage = SharedUtils.DataStorage.NONE)
public class SendParameters {

    public SoundProductionUnit soundProductionUnit = SoundProductionUnit.LOUD_SPEAKERS;
    public int loudnessLevel = 100;
    public String message;
    public int frequenciesBindingShift = SharedUtils.DEFAULT_FREQ_BINDING_BASE;
    public double frequenciesBindingScale = SharedUtils.DEFAULT_FREQ_BINDING_SCALE;

}
