package com.luckynick.shared.model;

import com.luckynick.shared.IOClassHandling;
import com.luckynick.shared.SharedUtils;
import com.luckynick.shared.enums.SoundProductionUnit;

@IOClassHandling(sendViaNetwork = true, dataStorage = SharedUtils.DataStorage.NONE)
public class SendParameters {

    public SoundProductionUnit soundProductionUnit;
    public int loudnessLevel;
    public String message;
    public int frequenciesBindingShift = 0;

}
