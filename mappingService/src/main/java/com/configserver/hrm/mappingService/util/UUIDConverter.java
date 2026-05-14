package com.configserver.hrm.mappingService.util;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDConverter {

    public static String bytesToHexUUID(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        UUID uuid = new UUID(high, low);
        return uuid.toString();
    }
}
