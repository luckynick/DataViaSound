package com.luckynick.shared.model;

import com.luckynick.custom.Device;
import com.luckynick.shared.IOClassHandling;
import com.luckynick.shared.SharedUtils;

import java.util.Date;

/**
 * Not serialized independently, but inside of test result
 */
@IOClassHandling(sendViaNetwork = true, dataStorage = SharedUtils.DataStorage.NONE)
public class SendSessionSummary {

    public Device summarySource;
    /**
     * Depending on role of device which sent this summary (sender/receiver),
     * it is either sent or decoded data
     */
    public SendParameters sendParameters;
    public long sessionStartDate;
}
