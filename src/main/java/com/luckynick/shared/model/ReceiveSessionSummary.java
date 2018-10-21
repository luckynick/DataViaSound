package com.luckynick.shared.model;

import com.luckynick.custom.Device;
import com.luckynick.shared.IOClassHandling;
import com.luckynick.shared.SharedUtils;

/**
 * Not serialized independently, but inside of test result
 */
@IOClassHandling(sendViaNetwork = true, dataStorage = SharedUtils.DataStorage.NONE)
public class ReceiveSessionSummary {

    public Device summarySource;
    /**
     * Depending on role of device which sent this summary (sender/receiver),
     * it is either sent or decoded data
     */
    public ReceiveParameters receiveParameters;
    public String message;
    public long sessionStartDate;
    public Exception exceptionDuringDecoding;
}
