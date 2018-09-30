package com.luckynick.shared.enums;

import java.util.Arrays;

public enum PacketID {
    UNDEFINED,

    REQUEST,
    RESPONSE,

    DEVICE,
    JOIN,

    OK,
    ERROR,
    ;

    public static PacketID ordinalToEnum(int ordinal) {
        return Arrays.stream(PacketID.values()).filter((pid) -> pid.ordinal() == ordinal).findAny().orElse(UNDEFINED);
    }

}
