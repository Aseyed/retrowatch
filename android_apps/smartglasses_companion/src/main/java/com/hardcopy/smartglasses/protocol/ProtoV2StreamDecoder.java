package com.hardcopy.smartglasses.protocol;

import java.io.ByteArrayOutputStream;

/**
 * Streaming decoder for ProtoV2. Feed raw bytes from BT input; it emits complete frames.
 * Designed to be low-allocation and resilient to junk/partial data.
 */
public final class ProtoV2StreamDecoder {

    public interface Listener {
        void onFrame(byte ver, byte type, byte flags, byte seq, byte[] payload);
        void onBadFrame(String reason);
    }

    private final Listener listener;

    private boolean inFrame = false;
    private boolean escaping = false;
    private final ByteArrayOutputStream buf = new ByteArrayOutputStream(128);

    public ProtoV2StreamDecoder(Listener listener) {
        this.listener = listener;
    }

    public void reset() {
        inFrame = false;
        escaping = false;
        buf.reset();
    }

    public void feed(byte[] data, int len) {
        if (data == null || len <= 0) return;
        for (int i = 0; i < len; i++) feedByte(data[i]);
    }

    public void feedByte(byte b) {
        if (!inFrame) {
            if (b == ProtoV2.SOF) {
                inFrame = true;
                escaping = false;
                buf.reset();
            }
            return;
        }

        if (escaping) {
            escaping = false;
            buf.write((b ^ ProtoV2.ESC_XOR) & 0xFF);
            return;
        }

        if (b == ProtoV2.ESC) {
            escaping = true;
            return;
        }

        if (b == ProtoV2.SOF) { // resync on new SOF
            escaping = false;
            buf.reset();
            return;
        }

        if (b == ProtoV2.EOF) {
            parseFrame(buf.toByteArray());
            inFrame = false;
            escaping = false;
            buf.reset();
            return;
        }

        buf.write(b & 0xFF);
        if (buf.size() > 256) { // hard cap to prevent runaway
            listener.onBadFrame("frame too large");
            reset();
        }
    }

    private void parseFrame(byte[] body) {
        // Body = VER TYPE FLAGS SEQ LEN PAYLOAD CRC16H CRC16L
        if (body.length < 7) {
            listener.onBadFrame("too short");
            return;
        }
        byte ver = body[0];
        if (ver != ProtoV2.VER) {
            listener.onBadFrame("bad version");
            return;
        }
        byte type = body[1];
        byte flags = body[2];
        byte seq = body[3];
        int len = body[4] & 0xFF;
        int expected = 5 + len + 2;
        if (body.length != expected) {
            listener.onBadFrame("length mismatch");
            return;
        }

        int crcRead = ((body[body.length - 2] & 0xFF) << 8) | (body[body.length - 1] & 0xFF);
        byte[] crcData = new byte[body.length - 2];
        System.arraycopy(body, 0, crcData, 0, crcData.length);
        int crcCalc = ProtoV2.crc16CcittFalse(crcData);
        if (crcCalc != crcRead) {
            listener.onBadFrame("crc mismatch");
            return;
        }

        byte[] payload = new byte[len];
        if (len > 0) System.arraycopy(body, 5, payload, 0, len);
        listener.onFrame(ver, type, flags, seq, payload);
    }
}



