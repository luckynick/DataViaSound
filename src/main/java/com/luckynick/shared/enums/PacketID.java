package com.luckynick.shared.enums;

import java.util.Arrays;

public enum PacketID {
    UNDEFINED,

    REQUEST,
    RESPONSE,

    DEVICE,
    TEXT,
    /**
     * Device finished executing last task.
     */
    JOIN,
    PREP_SEND_MESSAGE,
    PREP_RECEIVE_MESSAGE,

    SEND_MESSAGE,
    RECEIVE_MESSAGE,

    OK,
    ERROR,
    ;

    public static PacketID ordinalToEnum(int ordinal) {
        for (PacketID pid : PacketID.values()) {
            if (pid.ordinal() == ordinal) {
                return pid;
            }
        }
        return UNDEFINED;
    }

}
