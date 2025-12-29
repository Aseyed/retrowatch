/*
 * SmartGlasses Pro Micro v2 (ATmega32U4) - robust Bluetooth protocol + OLED output
 *
 * Target board: Arduino Pro Micro (ATmega32U4)
 * Bluetooth: classic SPP module on Serial1 (pins 0/1)
 * Display: SSD1306 128x64 I2C (0x3C)
 *
 * Protocol: see ../../smartglasses_companion/PROTOCOL.md
 *
 * This sketch is intentionally self-contained and conservative with RAM:
 * - fixed buffers
 * - no dynamic String usage
 * - tolerant streaming parser with resync + CRC
 */

#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

#if defined(__AVR_ATmega32U4__)
  #define BTSerial Serial1
  #define DebugSerial Serial
#else
  #error "This sketch is intended for ATmega32U4 (Pro Micro)."
#endif

#define OLED_RESET 9
Adafruit_SSD1306 display(OLED_RESET);

// =========================
// Protocol v2 constants
// =========================
static const uint8_t SOF = 0x7E;
static const uint8_t EOF_ = 0x7F;
static const uint8_t ESC = 0x7D;
static const uint8_t ESC_XOR = 0x20;
static const uint8_t VER = 0x02;

static const uint8_t TYPE_STATUS = 0x01;
static const uint8_t TYPE_TIME   = 0x02;
static const uint8_t TYPE_CALL   = 0x03;
static const uint8_t TYPE_NOTIFY = 0x04;
static const uint8_t TYPE_PING   = 0x05;
static const uint8_t TYPE_ACK    = 0x10;

static const uint8_t FLAG_ACK_REQ = 0x01;

static const uint8_t STATUS_CONNECTED = 0x01;
static const uint8_t STATUS_DISCONNECTED = 0x02;

static const uint8_t MAX_PAYLOAD_LEN = 64;

// =========================
// UI state
// =========================
static volatile bool linkUp = false;

static uint16_t year_ = 2025;
static uint8_t month_ = 1, day_ = 1, hour_ = 0, minute_ = 0, second_ = 0;

static char lastLine1[22]; // enough for 128px at text size 1 (~21 chars)
static char lastLine2[22];

static unsigned long lastTickMs = 0;
static unsigned long lastDrawMs = 0;

// =========================
// Streaming decoder state
// =========================
static bool inFrame = false;
static bool escaping = false;
static uint8_t frameBuf[128]; // body only (VER..CRC16)
static uint8_t frameLen = 0;

static uint8_t txSeq = 0;

// CRC-16/CCITT-FALSE
static uint16_t crc16_ccitt_false(const uint8_t* data, uint16_t len) {
  uint16_t crc = 0xFFFF;
  for (uint16_t i = 0; i < len; i++) {
    crc ^= (uint16_t)data[i] << 8;
    for (uint8_t b = 0; b < 8; b++) {
      if (crc & 0x8000) crc = (crc << 1) ^ 0x1021;
      else crc <<= 1;
    }
  }
  return crc;
}

static void bt_write_escaped(uint8_t b) {
  if (b == SOF || b == EOF_ || b == ESC) {
    BTSerial.write(ESC);
    BTSerial.write((uint8_t)(b ^ ESC_XOR));
  } else {
    BTSerial.write(b);
  }
}

static void send_ack(uint8_t ackType, uint8_t ackSeq, uint8_t result) {
  // Body: VER TYPE FLAGS SEQ LEN PAYLOAD CRC16
  uint8_t body[5 + 3 + 2];
  body[0] = VER;
  body[1] = TYPE_ACK;
  body[2] = 0x00;
  body[3] = txSeq++;
  body[4] = 3;
  body[5] = ackType;
  body[6] = ackSeq;
  body[7] = result;
  uint16_t crc = crc16_ccitt_false(body, 5 + 3);
  body[8] = (uint8_t)(crc >> 8);
  body[9] = (uint8_t)(crc & 0xFF);

  BTSerial.write(SOF);
  for (uint8_t i = 0; i < sizeof(body); i++) bt_write_escaped(body[i]);
  BTSerial.write(EOF_);
}

static void safe_copy_text(char* dst, uint8_t dstLen, const uint8_t* src, uint8_t srcLen) {
  if (dstLen == 0) return;
  uint8_t n = (srcLen < (dstLen - 1)) ? srcLen : (dstLen - 1);
  for (uint8_t i = 0; i < n; i++) {
    char c = (char)src[i];
    if (c < 0x20 || c > 0x7E) c = ' '; // simple ASCII sanitize
    dst[i] = c;
  }
  dst[n] = '\0';
}

static void set_two_lines(const char* line1, const char* line2) {
  strncpy(lastLine1, line1, sizeof(lastLine1) - 1);
  lastLine1[sizeof(lastLine1) - 1] = '\0';
  strncpy(lastLine2, line2, sizeof(lastLine2) - 1);
  lastLine2[sizeof(lastLine2) - 1] = '\0';
}

static void handle_frame(const uint8_t* body, uint8_t bodyLen) {
  // Body: VER TYPE FLAGS SEQ LEN PAYLOAD CRC16H CRC16L
  if (bodyLen < 7) return;
  if (body[0] != VER) return;
  uint8_t type = body[1];
  uint8_t flags = body[2];
  uint8_t seq = body[3];
  uint8_t len = body[4];
  if (len > MAX_PAYLOAD_LEN) return;
  if ((uint16_t)bodyLen != (uint16_t)(5 + len + 2)) return;

  uint16_t crcRead = ((uint16_t)body[bodyLen - 2] << 8) | body[bodyLen - 1];
  uint16_t crcCalc = crc16_ccitt_false(body, (uint16_t)(bodyLen - 2));
  if (crcCalc != crcRead) return;

  const uint8_t* payload = &body[5];

  if (type == TYPE_STATUS && len >= 1) {
    if (payload[0] == STATUS_CONNECTED) linkUp = true;
    else if (payload[0] == STATUS_DISCONNECTED) linkUp = false;
  } else if (type == TYPE_TIME && len >= 7) {
    year_ = (uint16_t)payload[0] | ((uint16_t)payload[1] << 8);
    month_ = payload[2];
    day_ = payload[3];
    hour_ = payload[4];
    minute_ = payload[5];
    second_ = payload[6];
  } else if (type == TYPE_CALL) {
    char name[22];
    safe_copy_text(name, sizeof(name), payload, len);
    set_two_lines("CALL", name);
  } else if (type == TYPE_NOTIFY) {
    char msg[22];
    safe_copy_text(msg, sizeof(msg), payload, len);
    set_two_lines("NOTIFY", msg);
  } else if (type == TYPE_PING) {
    // no-op
  }

  if (flags & FLAG_ACK_REQ) send_ack(type, seq, 0x00);
}

static void feed_byte(uint8_t b) {
  if (!inFrame) {
    if (b == SOF) {
      inFrame = true;
      escaping = false;
      frameLen = 0;
    }
    return;
  }

  if (escaping) {
    escaping = false;
    b = (uint8_t)(b ^ ESC_XOR);
  } else if (b == ESC) {
    escaping = true;
    return;
  } else if (b == SOF) {
    // resync
    escaping = false;
    frameLen = 0;
    return;
  } else if (b == EOF_) {
    handle_frame(frameBuf, frameLen);
    inFrame = false;
    escaping = false;
    frameLen = 0;
    return;
  }

  if (frameLen < sizeof(frameBuf)) {
    frameBuf[frameLen++] = b;
  } else {
    // overflow -> drop frame
    inFrame = false;
    escaping = false;
    frameLen = 0;
  }
}

static void poll_bt() {
  while (BTSerial.available()) {
    feed_byte((uint8_t)BTSerial.read());
  }
}

static void tick_time(unsigned long nowMs) {
  if (nowMs - lastTickMs < 1000) return;
  lastTickMs += 1000;
  second_++;
  if (second_ >= 60) { second_ = 0; minute_++; }
  if (minute_ >= 60) { minute_ = 0; hour_++; }
  if (hour_ >= 24) { hour_ = 0; /* date increment omitted for MCU simplicity */ }
}

static void draw_ui(unsigned long nowMs) {
  if (nowMs - lastDrawMs < 200) return;
  lastDrawMs = nowMs;

  display.clearDisplay();
  display.setTextColor(WHITE);
  display.setTextSize(1);

  // Status + time row
  display.setCursor(0, 0);
  display.print("BT:");
  display.print(linkUp ? "OK " : "-- ");
  display.print(year_);
  display.print("-");
  if (month_ < 10) display.print("0");
  display.print(month_);
  display.print("-");
  if (day_ < 10) display.print("0");
  display.print(day_);

  display.setCursor(0, 10);
  if (hour_ < 10) display.print("0");
  display.print(hour_);
  display.print(":");
  if (minute_ < 10) display.print("0");
  display.print(minute_);
  display.print(":");
  if (second_ < 10) display.print("0");
  display.print(second_);

  // Message area
  display.setCursor(0, 28);
  display.print(lastLine1);
  display.setCursor(0, 40);
  display.print(lastLine2);

  display.display();
}

void setup() {
  // Optional debug over USB; comment out for max RAM safety if needed
  // DebugSerial.begin(115200);

  Wire.begin();
  display.begin(SSD1306_SWITCHCAPVCC, 0x3C);
  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(WHITE);
  display.setCursor(0, 0);
  display.println("SmartGlasses v2");
  display.println("Waiting BT...");
  display.display();

  BTSerial.begin(9600);

  set_two_lines("READY", " ");
  lastTickMs = millis();
  lastDrawMs = 0;
}

void loop() {
  unsigned long nowMs = millis();
  poll_bt();
  tick_time(nowMs);
  draw_ui(nowMs);
}



