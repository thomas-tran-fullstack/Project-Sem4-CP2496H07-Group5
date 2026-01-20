# 🚀 EZMart Livestream - Quick Start Guide

## 📋 Prerequisites

Before starting, make sure you have:
- ✅ Windows 10/11 (or Windows Server)
- ✅ Administrator access
- ✅ Internet connection (for downloads)
- ✅ GlassFish deployed with EZMart app
- ✅ 100 MB free disk space

---

## ⚡ Fast Setup (5 minutes)

### Step 1: Download and Install FFmpeg (2 min)

```powershell
# Download from:
# https://www.gyan.dev/ffmpeg/builds/

# Extract to: C:\ffmpeg\

# Verify installation:
ffmpeg -version
```

### Step 2: Download and Install nginx-rtmp (1 min)

```powershell
# Download from:
# http://nginx-win.ecssocsie.net/

# Extract to: C:\nginx-rtmp\

# Create HLS directory:
mkdir C:\hls

# Give permissions:
icacls C:\hls /grant "Everyone":(OI)(CI)F
```

### Step 3: Run Setup Verification (1 min)

```powershell
# Right-click PowerShell → Run as Administrator
# Then run:
powershell -ExecutionPolicy Bypass -File livestream-manager.ps1 -Action test

# Or run batch script (also admin):
setup-livestream.bat
```

### Step 4: Start Services (1 min)

```powershell
# Start nginx (will run in background):
cd C:\nginx-rtmp
start nginx.exe

# Verify:
powershell -ExecutionPolicy Bypass -File livestream-manager.ps1 -Action status
```

---

## 🎬 Broadcasting Your First Live Stream

### Step 1: Configure OBS (3 min)

1. **Download OBS:** https://obsproject.com/
2. **Launch OBS**
3. **Create Scene:** "Livestream"
4. **Add Sources:**
   - Video Capture Device (webcam)
   - Audio Input Capture (microphone)
5. **Settings → Stream:**
   - Service: `Custom...`
   - Server: `rtmp://localhost:1935/live`
   - Stream Key: `test-stream` (temporary)

### Step 2: Create Session in EZMart (2 min)

1. Open: `http://localhost:8080/EZMart_Supermarket_Management-war/pages/admin/livestream.xhtml`
2. Click **"Create Livestream"**
3. Fill:
   - Title: "My First Livestream"
   - Description: "Testing livestream feature"
   - Staff: Select any staff member
   - Date/Time: Now (or future)
4. Click **"Create Livestream"**
5. Click **"Activate"** to go live
6. Copy the **Stream Key** displayed

### Step 3: Get Stream Key in OBS (1 min)

1. Go to admin panel livestream details
2. Click **"Stream Settings"** button
3. Copy "RTMP Server" and "Stream Key"
4. In OBS Settings → Stream:
   - Paste RTMP Server
   - Paste Stream Key
5. Click **"Apply"** → **"OK"**

### Step 4: Start Broadcasting (1 min)

1. In OBS: Click **"Start Streaming"** button
2. You should see green indicator "Streaming"
3. Check console for RTMP connection success
4. Verify HLS files created:
   ```powershell
   dir C:\hls\
   # Should see: test-stream.m3u8, test-stream-0.ts, etc
   ```

### Step 5: Watch as Customer (1 min)

1. Open customer app: `http://localhost:8080/EZMart_Supermarket_Management-war/pages/user/livestream.xhtml`
2. Should see your session "LIVE"
3. Click to watch
4. Should see video player loading HLS stream
5. Send chat messages
6. See real-time viewer count

---

## 📝 Detailed Guides

For more information, read these files:

- **FFMPEG_SETUP_GUIDE.md** - Detailed FFmpeg + nginx setup
- **OBS_CONFIGURATION_GUIDE.md** - OBS configuration with pictures
- **This file** - Quick reference

---

## 🔧 Management Commands

### PowerShell Commands

```powershell
# Start services
powershell -ExecutionPolicy Bypass -File livestream-manager.ps1 -Action start

# Stop services
powershell -ExecutionPolicy Bypass -File livestream-manager.ps1 -Action stop

# Restart services
powershell -ExecutionPolicy Bypass -File livestream-manager.ps1 -Action restart

# Check status
powershell -ExecutionPolicy Bypass -File livestream-manager.ps1 -Action status

# Clean HLS files
powershell -ExecutionPolicy Bypass -File livestream-manager.ps1 -Action clean

# Run tests
powershell -ExecutionPolicy Bypass -File livestream-manager.ps1 -Action test
```

### Manual nginx Commands

```powershell
# Navigate to nginx
cd C:\nginx-rtmp

# Start
nginx.exe

# Reload configuration
nginx.exe -s reload

# Stop gracefully
nginx.exe -s quit

# Stop immediately
nginx.exe -s stop

# Check logs
type logs\error.log
type logs\access.log
```

---

## 🐛 Troubleshooting

### "Port 1935 already in use"
```powershell
# Find what's using it:
netstat -ano | findstr :1935

# Kill the process:
taskkill /PID 1234 /F
# (replace 1234 with the PID)
```

### "Failed to connect to server" in OBS
```powershell
# Check nginx running:
tasklist | findstr nginx

# Restart nginx:
cd C:\nginx-rtmp
nginx.exe -s quit
nginx.exe

# Check firewall:
netsh advfirewall firewall show rule name=RTMP
```

### "No video in customer app"
```powershell
# Check HLS files created:
dir C:\hls\

# Verify HTTP server (port 8080):
netstat -ano | findstr :8080

# Test URL directly:
# Open browser: http://localhost:8080/hls/[streamkey].m3u8
```

### "High CPU usage in OBS"
1. Reduce bitrate: 2500 → 1500 kbps
2. Lower resolution: 1280x720 → 960x540
3. Reduce FPS: 60 → 30
4. Switch to GPU encoding (NVIDIA/Intel)
5. Close other applications

### "Audio not working"
1. In OBS, verify microphone level (mixer at bottom)
2. Right-click mic → Click on audio source
3. Adjust microphone volume in Windows Settings

---

## 📊 Performance Tips

### Optimal Settings

| Setting | Recommendation |
|---------|-----------------|
| Resolution | 1280x720 |
| FPS | 30 |
| Bitrate | 2500 kbps |
| Encoder | Hardware (NVENC/QSV) if available, else x264 |
| Audio Bitrate | 128 kbps |
| CPU Target | < 70% |

### For Low-End PC

| Setting | Value |
|---------|-------|
| Resolution | 960x540 |
| FPS | 24 |
| Bitrate | 1500 kbps |
| Encoder | x264 (CPU) |

### For High-End PC

| Setting | Value |
|---------|-------|
| Resolution | 1920x1080 |
| FPS | 60 |
| Bitrate | 5000 kbps |
| Encoder | NVIDIA NVENC |

---

## ✅ Day-Of Checklist

Before going live:

- [ ] FFmpeg installed (`ffmpeg -version` works)
- [ ] nginx running (`tasklist | findstr nginx`)
- [ ] HLS directory exists (`C:\hls`)
- [ ] OBS configured with RTMP server
- [ ] OBS scene has video + audio sources
- [ ] Test stream key set in OBS
- [ ] GlassFish running with EZMart deployed
- [ ] Database accessible
- [ ] At least one staff member exists in database
- [ ] Customer app loads without errors
- [ ] Firewall allows ports 1935, 8080

**Ready to go live!**

---

## 🌐 Remote Setup (Multiple Machines)

If OBS on **different machine** than nginx:

### On nginx machine (Windows Server)
```powershell
# Get IP address:
ipconfig

# Example: 192.168.1.100
```

### On OBS machine
```
In OBS Settings → Stream:
  Server: rtmp://192.168.1.100:1935/live
  (instead of localhost)
```

### Firewall
```powershell
# On nginx machine, allow remote connections:
netsh advfirewall firewall add rule name="RTMP Remote" dir=in action=allow protocol=tcp localport=1935
netsh advfirewall firewall add rule name="HLS Remote" dir=in action=allow protocol=tcp localport=8080
```

---

## 📞 Getting Help

### Check Logs

**nginx errors:**
```powershell
type C:\nginx-rtmp\logs\error.log
```

**nginx access:**
```powershell
type C:\nginx-rtmp\logs\access.log
```

**FFmpeg output:**
```powershell
# In OBS: Tools → Log Files
# Will show FFmpeg command output
```

### Common Issues

| Issue | Solution |
|-------|----------|
| No video | Check HLS files in `C:\hls\` |
| No audio | Check microphone in OBS mixer |
| High CPU | Lower resolution/bitrate |
| Connection refused | Check ports 1935, 8080 |
| Buffering | Reduce bitrate by 500 kbps |
| Chat lag | Normal (WebSocket 1-2s delay) |

---

## 🎯 Next Steps

1. **Deploy GlassFish** with cleaned/built code
2. **Run setup script** to verify environment
3. **Start nginx** services
4. **Configure OBS** with RTMP settings
5. **Create test session** in admin panel
6. **Start broadcasting** from OBS
7. **Watch as customer** to verify
8. **Invite team to test** chat and products
9. **Celebrate! 🎉**

---

## 📚 File Locations

| Item | Location |
|------|----------|
| FFmpeg | `C:\ffmpeg\bin\ffmpeg.exe` |
| nginx | `C:\nginx-rtmp\nginx.exe` |
| nginx Config | `C:\nginx-rtmp\conf\nginx.conf` |
| HLS Files | `C:\hls\` |
| Admin UI | `/pages/admin/livestream.xhtml` |
| Staff UI | `/pages/staff/livestream-broadcast.xhtml` |
| Customer UI | `/pages/user/livestream.xhtml` |
| Setup Script | `./setup-livestream.bat` |
| Manager Script | `./livestream-manager.ps1` |

---

## 🎬 Demo Scenario

### Complete Demo (15 minutes)

1. **Setup (5 min)**
   ```powershell
   powershell -ExecutionPolicy Bypass -File livestream-manager.ps1 -Action test
   powershell -ExecutionPolicy Bypass -File livestream-manager.ps1 -Action start
   ```

2. **OBS Config (3 min)**
   - Launch OBS
   - Create scene
   - Add webcam + mic
   - Enter RTMP settings

3. **Admin Panel (2 min)**
   - Create livestream session
   - Go live
   - Copy stream key to OBS

4. **Broadcasting (3 min)**
   - Click "Start Streaming" in OBS
   - Verify in admin dashboard
   - Watch HLS stream play

5. **Customer View (2 min)**
   - Open customer app
   - See live session
   - Click to watch
   - Send chat messages

**Total Time: 15 minutes from zero to live streaming!**

---

**Happy streaming! 🎥**
