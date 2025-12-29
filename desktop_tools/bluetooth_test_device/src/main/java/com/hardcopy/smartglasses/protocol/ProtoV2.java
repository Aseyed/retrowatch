package com.hardcopy.smartglasses.protocol;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Protocol v2: framed + byte-stuffed + CRC16.
 * Copied from Android app for compatibility.
 */
public final class ProtoV2 {
    private ProtoV2() {}

    public static final byte SOF = 0x7E;
    public static final byte EOF = 0x7F;
    public static final byte ESC = 0x7D;
    public static final byte ESC_XOR = 0x20;

    public static final byte VER = 0x02;

    // Message types (Android -> Arduino)
    public static final byte TYPE_STATUS = 0x01;
    public static final byte TYPE_TIME   = 0x02;
    public static final byte TYPE_CALL   = 0x03;
    public static final byte TYPE_NOTIFY = 0x04;
    public static final byte TYPE_PING   = 0x05;

    // Arduino -> Android
    public static final byte TYPE_ACK    = 0x10;

    public static final byte FLAG_ACK_REQ = 0x01;

    public static final byte STATUS_CONNECTED = 0x01;
    public static final byte STATUS_DISCONNECTED = 0x02;

    public static final int MAX_PAYLOAD_LEN = 64;

    public static byte[] encode(byte type, byte flags, byte seq, byte[] payload) {
        if (payload == null) payload = new byte[0];
        if (payload.length > MAX_PAYLOAD_LEN) payload = Arrays.copyOf(payload, MAX_PAYLOAD_LEN);

        ByteArrayOutputStream raw = new ByteArrayOutputStream(8 + payload.length);
        raw.write(VER);
        raw.write(type);
        raw.write(flags);
        raw.write(seq);
        raw.write((byte) payload.length);
        raw.write(payload, 0, payload.length);

        int crc = crc16CcittFalse(raw.toByteArray());
        raw.write((crc >> 8) & 0xFF);
        raw.write(crc & 0xFF);

        byte[] body = raw.toByteArray();

        ByteArrayOutputStream stuffed = new ByteArrayOutputStream(body.length + 4);
        stuffed.write(SOF);
        for (byte b : body) {
            writeEscaped(stuffed, b);
        }
        stuffed.write(EOF);
        return stuffed.toByteArray();
    }

    private static void writeEscaped(ByteArrayOutputStream out, byte b) {
        if (b == SOF || b == EOF || b == ESC) {
            out.write(ESC);
            out.write((b ^ ESC_XOR) & 0xFF);
        } else {
            out.write(b & 0xFF);
        }
    }

    /**
     * CRC-16/CCITT-FALSE: poly 0x1021, init 0xFFFF, xorOut 0, refin=false refout=false.
     */
    public static int crc16CcittFalse(byte[] data) {
        int crc = 0xFFFF;
        for (byte value : data) {
            crc ^= (value & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) crc = (crc << 1) ^ 0x1021;
                else crc <<= 1;
                crc &= 0xFFFF;
            }
        }
        return crc & 0xFFFF;
    }
}


