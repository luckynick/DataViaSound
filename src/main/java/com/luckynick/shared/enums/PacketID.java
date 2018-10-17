package com.luckynick.shared.enums;

import java.util.Arrays;

public enum PacketID {
    UNDEFINED,

    REQUEST,
    RESPONSE,

    DEVICE,
    JOIN,
    PREP_SEND_MESSAGE,
    PREP_RECEIVE_MESSAGE,
    /**
     * Device executes action to which it was prepared.
     */
    EXECUTE,

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
