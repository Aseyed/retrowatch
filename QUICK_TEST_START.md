# Quick Test Start - Android Emulator + Desktop Server

## 🚀 Fast Setup (3 Steps)

### 1. Start Desktop Server
```bash
cd desktop_tools/bluetooth_test_device
../../gradlew build
java -jar build/libs/bluetooth_test_device.jar
```
- Select **"TCP Server (Android connects to PC)"**
- Click **"Start Server"**
- ✅ Server should show "Server Running" in green

### 2. Configure Android Apps

**For Android Emulator, use this IP: `10.0.2.2`**

#### SmartGlasses Companion:
- Server IP: `10.0.2.2`
- Port: `8888`
- Click "Start companion service"
- Click "Connect"

#### RetroWatch:
- TCP Host: `10.0.2.2`
- TCP Port: `8888`
- Click "Connect"

### 3. Test
- ✅ Send messages from Android apps
- ✅ Check desktop server log for received messages
- ✅ Messages should appear as "RECV: TYPE | payload"

## 📱 IP Address Reference

| Device Type | IP Address | Notes |
|------------|------------|-------|
| **Android Emulator** | `10.0.2.2` | Special emulator IP for host PC |
| **Physical Android** | `192.168.52.99` | Your PC's Wi-Fi IP (check with `ipconfig`) |

## ⚠️ Common Issues

**"Connection timeout"**
- ✅ Use `10.0.2.2` for emulator (NOT your actual IP)
- ✅ Server must be running first
- ✅ Check port is `8888`

**"Service not running"**
- ✅ Click "Start companion service" first (SmartGlasses)

**"Not connected"**
- ✅ Click "Connect" button after entering IP/port
- ✅ Check server shows "Client connected" in log

**Buttons don't work**
- ✅ Service must be running
- ✅ Connection must be established
- ✅ Check Android logcat for errors

## 📋 Testing Checklist

- [ ] Desktop server running (green "Server Running")
- [ ] Android app connected (shows "Connected")
- [ ] Send buttons work
- [ ] Server log shows received messages
- [ ] Messages decoded correctly (not raw bytes)

## 🔍 Debug Commands

**Check Android logs:**
```bash
adb logcat | grep -i "CompanionService\|RetroWatch\|TcpConnection"
```

**Check server is listening:**
```bash
netstat -an | findstr 8888
```

**Get PC IP (for physical devices):**
```bash
ipconfig | findstr IPv4
```

