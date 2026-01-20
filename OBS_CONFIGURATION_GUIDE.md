# OBS Studio Configuration for EZMart Livestream

## 📥 Installation

1. Download: https://obsproject.com/download
2. Run installer, choose default settings
3. Launch OBS Studio

---

## ⚙️ Initial Setup Wizard

On first launch, OBS shows setup wizard:
- Choose "Optimize just for streaming"
- Select resolution: **1280x720** (good balance)
- Select FPS: **30fps** (or 60fps for action)
- Click "Finish"

---

## 🎬 Step-by-Step Configuration

### 1. Create Your Livestream Scene

**In OBS main window:**

```
Scenes panel (bottom left)
└─ Click "+" button
└─ Name: "Livestream"
└─ Click "Create"
```

### 2. Add Video Source (Webcam)

```
Sources panel (bottom middle)
└─ Click "+" button
└─ Select "Video Capture Device"
└─ Name: "Webcam"
└─ Click "Create New"
└─ Select your webcam from dropdown
└─ Click "OK"
```

**Position & Size webcam:**
- Drag corner to resize (usually 30-50% of screen)
- Drag to position (usually top-right or bottom-right)

### 3. Add Audio (Microphone)

```
Sources panel
└─ Click "+" button
└─ Select "Audio Input Capture"
└─ Name: "Microphone"
└─ Select your microphone
└─ Click "OK"
```

### 4. (Optional) Add Desktop Audio

For background music or system sounds:

```
Sources panel
└─ Click "+" button
└─ Select "Audio Output Capture"
└─ Name: "Desktop Audio"
└─ Select your speakers
└─ Click "OK"
```

### 5. (Optional) Add Screen Share/Window

```
Sources panel
└─ Click "+" button
└─ Select "Window Capture" (to share specific window)
└─ OR Select "Display Capture" (to share full monitor)
└─ Position behind/before other elements
└─ Click "OK"
```

**Example Layout for Product Showcase:**
```
┌────────────────────────┐
│  Product Demo (Screen) │
│  ┌──────────────────┐  │
│  │                  │  │
│  │              ┌────┐│
│  │              │ 📹 ││  <- Webcam (bottom-right)
│  │              └────┘│
│  │                    │
│  └──────────────────┘  │
└────────────────────────┘
```

---

## 🎙️ Audio Configuration

### Microphone Levels

1. **In OBS:**
   - Look at bottom mixer panel
   - You should see mic icon with levels
   - Speak into microphone
   - Green bars should move (-20dB to -10dB is ideal)
   - Never go into red (clipping)

2. **If too quiet:**
   - Right-click mic in Mixer → Audio Filters
   - Click "+" → Add "Compressor"
   - Adjust Ratio to 4:1

3. **If too loud (clipping):**
   - Reduce microphone volume in Windows Settings
   - Or reduce input gain in OBS mixer

### Desktop Audio (Optional)

If streaming product videos with sound:
- Add music via "Media Source" (add MP3 file)
- Balance: Mic should be louder than desktop audio

---

## 🌐 Stream Settings (RTMP Configuration)

### For **Local Network** (Testing):

1. Go to: **Settings → Stream** (top menu)

2. Fill in:
   ```
   Service: Custom...
   
   Server: rtmp://localhost:1935/live
   (Or rtmp://127.0.0.1:1935/live)
   
   Stream Key: (depends on how you set it up)
   ```

3. Click **"Apply"** → **"OK"**

### For **Remote RTMP** (If nginx on different PC):

1. Find nginx server IP address:
   ```powershell
   # On nginx machine, run:
   ipconfig
   # Look for IPv4 Address: 192.168.x.x or 10.x.x.x
   ```

2. In OBS Settings → Stream:
   ```
   Server: rtmp://192.168.1.100:1935/live
   (Replace IP with your nginx server IP)
   
   Stream Key: your_stream_name
   ```

3. Click **"Apply"** → **"OK"**

---

## 🔑 Getting Stream Key from EZMart Admin Panel

1. **Open EZMart Admin Dashboard**
   - Navigate to: Livestream Management
   - Create new session OR click "Go Live" on pending session

2. **In "Broadcasting Information" section:**
   - See "RTMP Server URL" → Copy this
   - See "Stream Key" → Copy this

3. **In OBS:**
   - Paste RTMP Server URL into "Server" field
   - Paste Stream Key into "Stream Key" field

---

## 🎬 Video Settings Optimization

### For **Smooth Streaming** (Recommended):

1. Go to: **Settings → Video**

2. Set:
   ```
   Base Resolution: 1920x1080 (or native monitor)
   Output Resolution: 1280x720 (streaming resolution)
   Fps Common Values: 30
   ```

3. This will **downscale** your monitor for streaming (saves bandwidth)

### For **Lower CPU Usage**:
```
Base Resolution: 1920x1080
Output Resolution: 960x540 (smaller)
FPS: 24 or 30
```

### For **Higher Quality** (needs strong PC):
```
Base Resolution: 1920x1080
Output Resolution: 1920x1080 (1:1)
FPS: 60
```

---

## 🚀 Encoder Settings (Advanced)

### For **Intel/NVIDIA GPU encoding** (Fastest):

1. **Settings → Output**
2. Output Mode: **Advanced**
3. Encoder:
   - If NVIDIA GPU: Select **"NVIDIA NVENC H.264"**
   - If Intel: Select **"Intel QSV H.264"**
   - Otherwise: **"x264"** (CPU)

### Bitrate Settings:

| Quality | Bitrate | Resolution | FPS |
|---------|---------|------------|-----|
| Low (Mobile) | 1000 kbps | 640x360 | 30 |
| Medium | 2500 kbps | 1280x720 | 30 |
| High | 5000 kbps | 1280x720 | 60 |
| Very High | 8000 kbps | 1920x1080 | 60 |

**Recommended:** Start with **2500 kbps** at 1280x720, 30fps

1. **Settings → Output**
2. Output Mode: **Advanced**
3. Set "Bitrate" field
4. Click "Apply"

---

## 🎨 Scene Transitions (Optional)

Add smooth transitions between scenes:

1. At bottom of OBS, see "Transitions" dropdown
2. Choose: "Fade" or "Slide"
3. Set duration: 300-500ms
4. Click scene to switch smoothly

---

## 📊 Monitor Stream Health

### Before Starting:

1. Look at **Top Menu → Tools → Stats**
2. Should show:
   - **FPS**: Should be at target (30 or 60)
   - **CPU Usage**: < 70%
   - **Memory**: Normal usage

3. **Click "Start Streaming"**
4. Monitor these in real-time:
   - Green bars = Good connection
   - Yellow = Slight lag
   - Red = Major lag (might need to reduce bitrate)

---

## ▶️ Starting Your Livestream

### Day-Of Checklist:

- [ ] OBS scene created with all sources
- [ ] Microphone levels tested (green, not red)
- [ ] RTMP Server URL entered in OBS
- [ ] Stream Key copied from EZMart admin panel
- [ ] Video resolution/bitrate set
- [ ] Test stream for 30 seconds
- [ ] Check EZMart dashboard to confirm viewers see stream
- [ ] Check HLS file being generated: `C:\hls\[streamkey].m3u8`

### Start Broadcasting:

1. **On EZMart Admin Panel:**
   - Create session
   - Set title, description, staff
   - Click "Activate" or "Go Live"
   - Copy RTMP Server & Stream Key

2. **In OBS:**
   - Fill "Server" and "Stream Key"
   - Click **"Start Streaming"** button
   - See green indicator
   - Monitor CPU/Network

3. **Verify Customers Can Watch:**
   - Open `http://localhost:8080/hls/[streamkey].m3u8`
   - Or in EZMart customer app → see live session

---

## 🆘 Common Issues

### "Failed to connect to server"
```
Cause: RTMP server not running
Fix: 
  1. Check nginx running: tasklist | findstr nginx
  2. Port 1935 listening: netstat -ano | findstr :1935
  3. Restart nginx: nginx -s reload
```

### "No audio/video"
```
Cause: Source not added
Fix:
  1. Click Sources area
  2. Click "+" and add "Video Capture Device"
  3. Add "Audio Input Capture"
  4. Wait 2 seconds for source to activate
```

### "Very low FPS / Lag"
```
Cause: Bitrate too high or PC too slow
Fix:
  1. Lower bitrate: 2500 → 1500 kbps
  2. Lower resolution: 1280x720 → 960x540
  3. Switch to GPU encoder (NVENC or QSV)
  4. Close other applications
```

### "CPU Usage 100%"
```
Cause: Encoding too expensive
Fix:
  1. Use GPU encoder (NVIDIA/Intel)
  2. Reduce output resolution
  3. Reduce FPS (60 → 30)
  4. Reduce bitrate
```

---

## 📱 Mobile Broadcast Setup (NDI Alternative)

If you want to broadcast from phone instead of PC:

1. Download **OBS.Ninja** app on phone
2. Create broadcast link
3. In OBS on PC: Add "Browser Source" with OBS.Ninja link
4. Phone stream appears in OBS scene

---

## 📹 Recording While Streaming (Optional)

To save stream locally:

1. **Settings → Output**
2. Under "Recording" section:
   - Set "Recording Path": `C:\Recordings\`
   - Set "Recording Format": `mp4`
3. During stream, click **"Start Recording"** button (separate from streaming)

---

## ✅ Verification

### Test Full Setup:

```powershell
# Terminal 1: Start nginx
cd C:\nginx-rtmp
nginx.exe

# Terminal 2: Start OBS, click "Start Streaming"

# Terminal 3: Check HLS files
dir C:\hls\
# Should see .m3u8 and .ts files being created

# Terminal 4: Verify in browser
# Open: http://localhost:8080/hls/[streamkey].m3u8
# Should display playlist content
```

### Test Customer View:

1. Open EZMart customer app
2. Go to "Livestream"
3. Should see your session "LIVE"
4. Click to watch
5. Should see HLS video playing
6. Send chat messages

---

## 🎯 Performance Targets

| Metric | Target |
|--------|--------|
| CPU Usage | < 70% |
| FPS | 30 (or 60) |
| Bitrate | 2500-4000 kbps |
| Resolution | 1280x720 |
| Network Latency | < 100ms |
| Chat Latency | < 2 seconds |

If any exceeds target, adjust settings above.

---

## 📚 Additional Resources

- OBS Studio Docs: https://obsproject.com/wiki/
- nginx RTMP Module: https://github.com/arut/nginx-rtmp-module
- FFmpeg Documentation: https://ffmpeg.org/documentation.html

**Support Contact:** For issues, check server logs in `C:\nginx-rtmp\logs\`
