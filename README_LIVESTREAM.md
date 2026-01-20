# EZMart Livestream Shopping Feature - Complete Setup

> TikTok Live/Shopee Live style livestream shopping integrated into EZMart supermarket management system

## 📊 Project Status

### Backend ✅ COMPLETE
- [x] Database schema (6 tables)
- [x] JPA Entity classes (6 entities)
- [x] EJB Stateless facades (6 facades)
- [x] Admin REST APIs (CRUD + stats)
- [x] Staff REST APIs (broadcasting + products)
- [x] Customer REST APIs (feed + chat + history)
- [x] WebSocket chat endpoint

### Frontend ✅ COMPLETE
- [x] Admin Dashboard (livestream.xhtml)
- [x] Admin Create (livestream-create.xhtml)
- [x] Admin Details (livestream-edit.xhtml)
- [x] Staff Broadcast Panel (livestream-broadcast.xhtml)
- [x] Customer Feed (livestream.xhtml)
- [x] Customer History (livestream-history.xhtml)

### Infrastructure ⏳ SETUP GUIDE PROVIDED
- [x] FFmpeg setup guide
- [x] nginx RTMP setup guide
- [x] OBS configuration guide
- [x] Automation scripts

---

## 🚀 Quick Start (First Time Setup)

### 1️⃣ Clone/Update Code
```bash
# Pull latest changes from GitHub
git pull origin main

# Or if starting fresh:
git clone https://github.com/your-repo/Project-Sem4-CP2496H07-Group5.git
```

### 2️⃣ Deploy to GlassFish
```bash
# In NetBeans or via console:
1. Clean & Build project
2. Right-click project → Deploy
3. Wait for "Build successful"
4. GlassFish auto-deploys
```

### 3️⃣ Download & Install FFmpeg
```bash
# Visit: https://www.gyan.dev/ffmpeg/builds/
# Download: ffmpeg-N-113xxx-xxxxxx-win64-full.7z
# Extract to: C:\ffmpeg\
# Add C:\ffmpeg\bin to PATH
```

### 4️⃣ Download & Install nginx-rtmp
```bash
# Visit: http://nginx-win.ecssocsie.net/
# Download: nginx/1.13.5.1-rtmp version
# Extract to: C:\nginx-rtmp\
# Create: mkdir C:\hls
```

### 5️⃣ Run Setup Verification
```powershell
# Right-click PowerShell → "Run as Administrator"
# Then run:
powershell -ExecutionPolicy Bypass -File livestream-manager.ps1 -Action test
```

### 6️⃣ Start Services
```powershell
# Start nginx:
cd C:\nginx-rtmp
start nginx.exe

# Verify:
powershell -ExecutionPolicy Bypass -File livestream-manager.ps1 -Action status
```

### 7️⃣ Open in Browser
```
Admin:    http://localhost:8080/EZMart.../pages/admin/livestream.xhtml
Staff:    http://localhost:8080/EZMart.../pages/staff/livestream-broadcast.xhtml
Customer: http://localhost:8080/EZMart.../pages/user/livestream.xhtml
```

---

## 📁 Files Guide

### Documentation
| File | Purpose |
|------|---------|
| `QUICKSTART.md` | ⭐ Read this first! 5-min setup guide |
| `FFMPEG_SETUP_GUIDE.md` | Detailed FFmpeg + nginx installation |
| `OBS_CONFIGURATION_GUIDE.md` | Step-by-step OBS setup with screenshots |
| `nginx-config-template.conf` | nginx config file (copy to C:\nginx-rtmp\conf\) |

### Automation Scripts
| File | Purpose |
|------|---------|
| `setup-livestream.bat` | One-click verification (needs admin) |
| `livestream-manager.ps1` | Service manager (start/stop/status) |

### Backend Code
| File | Location | Purpose |
|------|----------|---------|
| `LiveSessionFacade.java` | `sessionbeans/` | Database queries |
| `LiveProductFacade.java` | `sessionbeans/` | Product management |
| `LiveChatFacade.java` | `sessionbeans/` | Chat messages |
| `LiveSessionViewerFacade.java` | `sessionbeans/` | Viewer tracking |
| `LiveSessionStatFacade.java` | `sessionbeans/` | Statistics |
| `LiveProductDiscountFacade.java` | `sessionbeans/` | Price history |
| `LiveStreamAdminServlet.java` | `controllers/` | Admin APIs |
| `LiveStreamStaffServlet.java` | `controllers/` | Staff APIs |
| `LiveStreamCustomerServlet.java` | `controllers/` | Customer APIs |
| `LiveStreamChatEndpoint.java` | `websocket/` | WebSocket chat |

### Frontend Pages
| File | Location | Role | Features |
|------|----------|------|----------|
| `livestream.xhtml` | `pages/admin/` | Admin | Dashboard, session list |
| `livestream-create.xhtml` | `pages/admin/` | Admin | Schedule new sessions |
| `livestream-edit.xhtml` | `pages/admin/` | Admin | Session details, stats |
| `livestream-broadcast.xhtml` | `pages/staff/` | Staff | Broadcasting control panel |
| `livestream.xhtml` | `pages/user/` | Customer | Live feed, HLS player |
| `livestream-history.xhtml` | `pages/user/` | Customer | Watch history |

### Database Schema
| Table | Purpose |
|-------|---------|
| `LiveSession` | Session info (title, status, RTMP, HLS) |
| `LiveProduct` | Products on sale during livestream |
| `LiveProductDiscount` | Price change history |
| `LiveChat` | Chat messages |
| `LiveSessionStat` | Analytics snapshots |
| `LiveSessionViewer` | Viewer tracking |

---

## 🔧 System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  EZMart Livestream System                │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ ADMIN PANEL (Admin)                                      │
│ - Create sessions                                        │
│ - Schedule livestreams                                   │
│ - Activate/Monitor streams                               │
│ - View statistics                                        │
└─────────────────────────────────────────────────────────┘
         ↓ (REST API)
┌─────────────────────────────────────────────────────────┐
│ GLASSFISH SERVER                                         │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Admin API         Staff API       Customer API      │ │
│ │ (CRUD)            (Broadcast)     (Feed/Chat)      │ │
│ └─────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ WebSocket Endpoint (ws://) - Real-time Chat         │ │
│ └─────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Facades / Database (JPA/Hibernate)                  │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
         ↓              ↓              ↓
    ┌────────┐     ┌────────┐    ┌─────────┐
    │ ADMIN  │     │ STAFF  │    │CUSTOMER │
    │ PANEL  │     │ OBS    │    │ APP     │
    └────────┘     └────────┘    └─────────┘

┌─────────────────────────────────────────────────────────┐
│ STREAMING INFRASTRUCTURE                                │
├─────────────────────────────────────────────────────────┤
│ OBS Studio (Staff)                                       │
│   ↓ (RTMP Stream at rtmp://localhost:1935/live)       │
│ nginx-rtmp Server (RTMP Listener)                        │
│   ↓ (FFmpeg Transcoding)                                │
│ FFmpeg (H.264 → HLS)                                     │
│   ↓ (HTTP Output)                                        │
│ HLS Files (C:\hls\)                                      │
│   ↓ (HTTP Stream at http://localhost:8080/hls/)       │
│ Customer App (HLS Video Player)                          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ DATABASE (SQL Server)                                   │
│ - Users, Customers, Products (existing)                │
│ - LiveSession, LiveProduct, LiveChat, etc (new)        │
└─────────────────────────────────────────────────────────┘
```

---

## 📡 API Endpoints

### Admin APIs
```
GET    /api/livestream/admin/sessions               - List all sessions
GET    /api/livestream/admin/sessions/{id}          - Get session details
GET    /api/livestream/admin/sessions/{id}/details  - With products + stats
GET    /api/livestream/admin/dashboard              - Global statistics
POST   /api/livestream/admin/sessions               - Create session
PUT    /api/livestream/admin/sessions/{id}          - Update session
POST   /api/livestream/admin/sessions/{id}/activate - Go live
DELETE /api/livestream/admin/sessions/{id}          - Delete session
```

### Staff APIs
```
GET  /api/livestream/staff/my-sessions                    - Pending sessions
GET  /api/livestream/staff/active-sessions                - Active sessions
POST /api/livestream/staff/sessions/{id}/go-live          - Start broadcasting
POST /api/livestream/staff/sessions/{id}/add-product      - Add product
PUT  /api/livestream/staff/sessions/{id}/products/{id}/price - Update price
POST /api/livestream/staff/sessions/{id}/end-live         - Stop broadcasting
GET  /api/livestream/staff/sessions/{id}/stream-key       - Get RTMP details
```

### Customer APIs
```
GET  /api/livestream/customer/live-sessions               - Live feed
GET  /api/livestream/customer/sessions/{id}               - Session details
GET  /api/livestream/customer/sessions/{id}/products      - Products on sale
GET  /api/livestream/customer/sessions/{id}/chat          - Recent messages
GET  /api/livestream/customer/my-history                  - Watch history
POST /api/livestream/customer/sessions/{id}/join          - Join session
POST /api/livestream/customer/sessions/{id}/send-message  - Send chat
```

### WebSocket
```
ws://localhost:8080/.../ws/livestream/{sessionId}/{userId}
  - user-joined     (Someone joins chat)
  - user-left       (Someone leaves chat)
  - message         (Chat message)
  - system          (System notification)
```

---

## 🎬 Typical Workflow

### Admin: Schedule Livestream
1. Go to Admin Dashboard
2. Click "Create Livestream"
3. Fill title, description, assign staff
4. Set date/time (future)
5. Click "Create"

### Admin: Go Live
1. Go to Admin Dashboard
2. See pending session
3. Click "Activate" → becomes ACTIVE
4. View streaming information
5. Share stream key with staff

### Staff: Configure OBS
1. Download OBS Studio
2. Create Scene with webcam + mic
3. Go to Settings → Stream
4. Enter RTMP Server: `rtmp://localhost:1935/live`
5. Enter Stream Key: (from admin panel)

### Staff: Start Broadcasting
1. Assign pending session → see it in "My Sessions"
2. Select session from dropdown
3. See "Go Live" button enabled
4. Click "Start Broadcasting"
5. See green "LIVE" indicator
6. Can add products / moderate chat

### Customer: Watch Livestream
1. Open Customer app → Livestream
2. See live sessions in feed (TikTok-style cards)
3. Click session to watch
4. See HLS video player
5. See featured products with discounts
6. Send chat messages in real-time
7. See viewer count increase

---

## 🆘 Troubleshooting

### GlassFish Issues
```
- Restart: bin\asadmin restart-domain
- Logs: domains\domain1\logs\
- Console: http://localhost:4848
```

### FFmpeg/nginx Issues
```
- Port 1935 in use: netstat -ano | findstr :1935
- HLS files not created: Check C:\hls\ permissions
- Connection refused: Check nginx running (tasklist | findstr nginx)
```

### OBS Issues
```
- No connection: Verify Server/Stream Key in Settings
- High CPU: Reduce bitrate/resolution
- Audio issues: Check microphone levels in Mixer
```

### Customer App Issues
```
- Stream not playing: Check http://localhost:8080/hls/ directly
- Chat lag: Normal (WebSocket 1-2s)
- Viewers not counting: Refresh page
```

See detailed guides:
- `FFMPEG_SETUP_GUIDE.md` - FFmpeg troubleshooting
- `OBS_CONFIGURATION_GUIDE.md` - OBS troubleshooting
- `QUICKSTART.md` - General troubleshooting

---

## 📊 Database Queries

### View all livestreams
```sql
SELECT * FROM LiveSession ORDER BY SessionID DESC;
```

### View active viewers
```sql
SELECT SessionID, COUNT(*) as Viewers 
FROM LiveSessionViewer 
WHERE LeftAt IS NULL
GROUP BY SessionID;
```

### View products on sale
```sql
SELECT lp.*, p.ProductName, p.UnitPrice
FROM LiveProduct lp
JOIN Products p ON lp.ProductID = p.ProductID
WHERE lp.SessionID = ?
ORDER BY lp.AddedAt DESC;
```

### View chat history
```sql
SELECT ChatMessageID, Username, MessageText, CreatedAt
FROM LiveChat lc
JOIN Users u ON lc.UserID = u.UserID
WHERE SessionID = ? AND IsDeleted = 0
ORDER BY CreatedAt DESC;
```

---

## 🎯 Performance Targets

| Metric | Target |
|--------|--------|
| Concurrent Sessions | 10+ |
| Concurrent Viewers per Session | 1000+ |
| Chat Latency | < 2 seconds |
| Video Latency | 10-20 seconds (HLS) |
| Server CPU | < 70% |
| Network Bandwidth | 2.5 Mbps per stream |

---

## 📦 Dependencies

### Frontend
- Tailwind CSS (utility-first CSS)
- Material Symbols (icons)
- Jakarta Faces (JSF/Facelets)

### Backend
- Jakarta EE (formerly Java EE)
- JPA/Hibernate (ORM)
- WebSocket (real-time chat)

### Streaming
- FFmpeg (video encoding)
- nginx RTMP (RTMP server)
- HLS (video streaming protocol)

### Client Tools
- OBS Studio (broadcasting)
- Any modern browser (watching)

---

## 🔐 Security Notes

### Current Implementation
- ✅ Role-based access control (Admin/Staff/Customer)
- ✅ Session authentication (HttpSession)
- ✅ Chat message moderation (soft delete)
- ✅ Stream key uniqueness (UUID)

### Recommendations for Production
- [ ] HTTPS/TLS for all traffic
- [ ] Token-based auth (JWT) instead of sessions
- [ ] Input validation on all APIs
- [ ] Rate limiting on chat
- [ ] Spam detection (duplicate messages)
- [ ] IP whitelist for RTMP ingest
- [ ] Encrypt stream keys in database

---

## 🚀 Deployment Checklist

- [ ] GlassFish running
- [ ] Database accessible
- [ ] FFmpeg installed
- [ ] nginx-rtmp running
- [ ] HLS directory writable
- [ ] Ports 1935, 8080 open in firewall
- [ ] OBS configured
- [ ] Test session created and activated
- [ ] Customer can see and watch stream
- [ ] Chat working
- [ ] Products visible with discounts

---

## 📞 Support

### Quick Reference
- **Admin Dashboard:** `/pages/admin/livestream.xhtml`
- **Staff Panel:** `/pages/staff/livestream-broadcast.xhtml`
- **Customer Feed:** `/pages/user/livestream.xhtml`
- **API Base:** `/resources/api/livestream/`

### Documentation
- `QUICKSTART.md` - 5-minute setup
- `FFMPEG_SETUP_GUIDE.md` - Streaming setup
- `OBS_CONFIGURATION_GUIDE.md` - Broadcasting setup

### Logs
- GlassFish: `domains/domain1/logs/`
- nginx error: `C:\nginx-rtmp\logs\error.log`
- nginx access: `C:\nginx-rtmp\logs\access.log`

---

## 📈 Future Enhancements

Potential features for Phase 2:
- [ ] Multi-bitrate HLS (adaptive quality)
- [ ] Stream recording & VOD
- [ ] Advanced moderation (automatic spam filter)
- [ ] Gamification (gifts, badges)
- [ ] Analytics dashboard (viewer retention, peak times)
- [ ] Live polls/voting
- [ ] Countdown timers
- [ ] Integration with payment system
- [ ] Mobile app (React Native)
- [ ] Social media sharing
- [ ] Restream to multiple platforms (Facebook, TikTok)
- [ ] AI product recommendations during stream

---

## 📝 Version History

- **v1.0** (Jan 2026) - Initial release
  - Full livestream feature
  - Admin, Staff, Customer UIs
  - WebSocket chat
  - HLS streaming
  - Database schema & APIs

---

## 👥 Team Credits

**Backend Development**
- Database schema design
- Entity/Facade implementation
- REST API development
- WebSocket chat endpoint

**Frontend Development**
- Admin dashboard
- Staff broadcast panel
- Customer livestream feed
- Responsive UI design

**Infrastructure**
- FFmpeg setup guide
- nginx RTMP configuration
- OBS integration guide
- Automation scripts

---

**Ready to go live! 🎥🚀**

For step-by-step setup, start with: **QUICKSTART.md**
