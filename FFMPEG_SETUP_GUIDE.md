# FFmpeg & RTMP/HLS Setup Guide for EZMart Livestream

## Windows Installation

### Step 1: Download FFmpeg
1. Visit: https://ffmpeg.org/download.html
2. Download Windows build from https://www.gyan.dev/ffmpeg/builds/
3. Choose "full" build (has all codecs)
4. Download latest version (e.g., ffmpeg-N-113xxx-xxxxxx-win64-full.7z)

### Step 2: Extract & Setup PATH
1. Extract to: `C:\ffmpeg\`
2. Add to Windows PATH:
   - Open Environment Variables (Win+X → System)
   - System Properties → Environment Variables
   - Edit PATH → Add: `C:\ffmpeg\bin`
   - Restart PowerShell to apply

### Step 3: Verify Installation
```powershell
ffmpeg -version
# Should show version info
```

---

## 🎬 RTMP Server Setup (Using nginx with RTMP module)

### Option A: Using Pre-built nginx-rtmp (Recommended for Windows)

1. Download from: http://nginx-win.ecssocsie.net/
   - Look for "nginx/1.13.5.1-rtmp" or latest

2. Extract to: `C:\nginx-rtmp\`

3. Edit config file: `C:\nginx-rtmp\conf\nginx.conf`
   Replace with:
```nginx
worker_processes  1;

events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;

    sendfile        on;
    keepalive_timeout  65;

    server {
        listen       8080;
        server_name  localhost;

        # Serve HLS files
        location /hls {
            types {
                application/vnd.apple.mpegurl m3u8;
                video/mp2t ts;
            }
            alias C:/hls/;
            expires -1;
            add_header Cache-Control "no-cache, no-store, must-revalidate";
        }

        # Redirect HTTP to local dashboard
        location / {
            root   html;
            index  index.html index.htm;
        }

        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
    }

    rtmp {
        server {
            listen 1935;

            application live {
                live on;
                record off;
                
                # FFmpeg will encode RTMP to HLS
                exec ffmpeg -i rtmp://localhost/live/$name -c:v libx264 -preset medium -c:a aac -b:a 128k -f hls -hls_time 10 -hls_list_size 3 -hls_flags delete_segments C:/hls/$name.m3u8;
            }
        }
    }
}
```

4. Create HLS output directory:
```powershell
mkdir C:\hls
```

5. Start nginx:
```powershell
cd C:\nginx-rtmp
start nginx.exe
# Or for debugging:
nginx.exe
```

6. Check status:
```powershell
# Verify RTMP listening
netstat -ano | findstr :1935
# Verify HTTP listening  
netstat -ano | findstr :8080
```

---

## 🎥 OBS Configuration

### Step 1: Download OBS Studio
- Visit: https://obsproject.com/
- Download for Windows
- Install with default settings

### Step 2: Configure Streaming in OBS
1. **Open OBS Studio**
2. Go to: **Settings → Stream**
3. Fill in:
   - **Service**: Custom...
   - **Server**: `rtmp://localhost:1935/live`
   - **Stream Key**: (Copy from EZMart admin panel when session is PENDING)

### Step 3: Create Scene
1. Click **"+" under Scenes** → Name it "Livestream"
2. Click **"+" under Sources** → Select:
   - **Video Capture Device** (Webcam)
   - **Audio Input Capture** (Microphone)
   - **Window Capture** (for screen sharing)
3. Arrange layers as needed

### Step 4: Audio Setup
1. **Settings → Audio**
2. Microphone: Select your microphone
3. Desktop Audio: (optional, for music/background)
4. Click **OK**

### Step 5: Start Streaming
1. In admin panel: Create livestream session → Go Live
2. Copy Stream Key
3. In OBS: Paste Stream Key in Settings
4. Click **"Start Streaming"** button
5. Monitor:
   - Green indicator = Broadcasting
   - Check CPU/GPU usage (should be < 50%)

---

## 📊 Troubleshooting

### Problem: RTMP Connection Refused
```powershell
# Check if nginx is running
tasklist | findstr nginx

# Check RTMP port
netstat -ano | findstr :1935

# Restart nginx
nginx -s reload
```

### Problem: No HLS Files Generated
1. Check FFmpeg installation:
```powershell
ffmpeg -version
```

2. Check HLS directory permissions:
```powershell
# Give write access to IIS/nginx user
icacls C:\hls /grant "Everyone":(OI)(CI)F
```

3. Check nginx logs:
```powershell
type C:\nginx-rtmp\logs\error.log
```

### Problem: High CPU Usage
- Lower resolution in OBS: 1280x720 instead of 1920x1080
- Reduce bitrate: 2500 kbps instead of 5000 kbps
- Use faster preset: "faster" instead of "medium"

---

## 🌐 Network Configuration (For Remote OBS)

If OBS is on **different machine** than nginx:

### Replace "localhost" with actual IP:
```nginx
# In nginx.conf
exec ffmpeg -i rtmp://YOUR_SERVER_IP/live/$name ...

# In OBS Settings
Server: rtmp://YOUR_SERVER_IP:1935/live
```

### Firewall Rules:
```powershell
# Allow RTMP (1935) inbound
New-NetFirewallRule -DisplayName "RTMP" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 1935

# Allow HTTP (8080) inbound
New-NetFirewallRule -DisplayName "HLS" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080
```

---

## ✅ Verify Setup Works

### Test with FFmpeg directly:
```powershell
# In one PowerShell window, start listener:
ffmpeg -f dshow -i "video=Webcam Name" -f flv rtmp://localhost:1935/live/test

# In another window, check HLS output:
dir C:\hls\
# Should see: test.m3u8, test-0.ts, test-1.ts, etc
```

### Test in Browser:
Open: `http://localhost:8080/hls/test.m3u8`
Should show:
```
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-TARGETDURATION:10
...
```

---

## 🚀 Production Checklist

- [ ] FFmpeg installed and in PATH
- [ ] nginx-rtmp running (port 1935 listening)
- [ ] HLS directory `C:\hls\` exists and writable
- [ ] HTTP server running (port 8080)
- [ ] OBS configured with RTMP server
- [ ] Firewall allows 1935, 8080
- [ ] GlassFish serving API on 8080 or different port
- [ ] Database accessible
- [ ] WebSocket endpoint accessible

---

## Notes
- HLS files are automatically deleted after streaming ends (ttl=3)
- Keep OBS bitrate < 5000 kbps for smooth playback
- Test locally before going live
- Monitor CPU: target < 70% during streaming
