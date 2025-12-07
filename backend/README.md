# Suvidha - Business Management Platform

A comprehensive SaaS platform designed for Indian MSMEs, featuring inventory management, billing, CRM, and analytics with complete activity tracking.

## 🚀 Features

- **Smart Billing & Invoicing** - GST-compliant invoices
- **Inventory Management** - Real-time stock tracking
- **Customer Management** - Built-in CRM
- **Financial Analytics** - Comprehensive dashboards
- **Activity Tracking** - Monitor every user interaction
- **IP Logging** - Track user locations and devices
- **Multi-language Support** - Hindi, English, and more

---

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Node.js** (v20.19+ or v22.12+) - [Download here](https://nodejs.org/)
- **Python** (v3.8+) - [Download here](https://www.python.org/downloads/)
- **MongoDB Atlas Account** - [Sign up free](https://www.mongodb.com/cloud/atlas)
- **Git** - [Download here](https://git-scm.com/)

---

## 🛠️ Installation Steps

### 1. Clone the Repository

```bash
git clone https://github.com/Sri8397/-SUVIDHA.git
cd suvidha
git checkout develop-python
```

### 2. Backend Setup (Python FastAPI)

#### Install Python Dependencies

```bash
cd backend
pip install -r requirements.txt
```

#### Configure Environment Variables

Create a `.env` file in the `backend` directory:

```env
MONGODB_URI=mongodb+srv://YOUR_USERNAME:YOUR_PASSWORD@YOUR_CLUSTER.mongodb.net/suvidha
SERVER_PORT=8080
APP_NAME=backend
```

**How to get MongoDB URI:**
1. Go to [MongoDB Atlas](https://cloud.mongodb.com)
2. Create a free cluster
3. Click "Connect" → "Connect your application"
4. Copy the connection string
5. Replace `<username>`, `<password>`, and cluster details

#### Run Backend Server

```bash
# From the backend directory
python -m uvicorn main:app --reload --host 0.0.0.0 --port 8080
```

You should see:
```
INFO:     Uvicorn running on http://0.0.0.0:8080
INFO:     Application startup complete.
```

### 3. Frontend Setup (React + Vite)

#### Install Node Dependencies

```bash
# From the root directory
npm install
```

#### Run Frontend Server

```bash
npm run dev
```

You should see:
```
VITE v7.2.6  ready in 636 ms
➜  Local:   http://localhost:5173/
```

---

## 🎯 Running the Application

### Quick Start (Both Servers)

**Terminal 1 - Backend:**
```bash
cd backend
python -m uvicorn main:app --reload --host 0.0.0.0 --port 8080
```

**Terminal 2 - Frontend:**
```bash
npm run dev
```

**Access the app:** http://localhost:5173/

---

## 📊 MongoDB Setup

### Database Collections

The application automatically creates these collections:

1. **`user_visits`** - Tracks user visit counts and metadata
2. **`activity_logs`** - Stores all user activity and click events

### Viewing Your Data

1. Go to [MongoDB Atlas](https://cloud.mongodb.com)
2. Click "Browse Collections"
3. Select database: `suvidha`
4. View collections: `user_visits` and `activity_logs`

---

## 🔍 Testing the Tracking System

### Test Activity Logging

```bash
# Check analytics overview
curl http://localhost:8080/api/analytics/overview

# Check specific user timeline
curl http://localhost:8080/api/analytics/user/YOUR_USER_ID

# Check all events
curl http://localhost:8080/api/analytics/events
```

### What Gets Tracked

Every interaction is logged with:
- User ID
- IP Address
- Device Type (desktop/mobile/tablet)
- Browser (Chrome, Firefox, Safari, etc.)
- Operating System
- Event Type (click, page_view, etc.)
- Button/Link Name
- Timestamp
- Screen Resolution

**Total Tracked Elements:** 56
- Navbar: 8 elements
- Hero: 2 buttons
- Features: 1 button
- Solutions: 10 buttons
- Pricing: 3 buttons
- Testimonials: 8 navigation buttons
- Footer: 24 links + social buttons

---

## 📁 Project Structure

```
suvidha/
├── backend/                # Python FastAPI backend
│   ├── main.py            # Application entry point
│   ├── database.py        # MongoDB connection
│   ├── models.py          # Pydantic models
│   ├── routers/           # API endpoints
│   │   ├── tracking.py    # User + activity tracking
│   │   └── analytics.py   # Analytics endpoints
│   ├── utils/             # Helper functions
│   │   └── user_agent_parser.py
│   ├── requirements.txt   # Python dependencies
│   └── .env              # Environment variables (create this)
├── src/                   # React frontend
│   ├── components/        # UI components
│   ├── hooks/            # React hooks
│   │   ├── useUserTracker.js      # Auto tracking
│   │   └── useActivityTracker.js  # Manual tracking
│   └── utils/            # Utilities
│       └── analytics.js  # API client
├── package.json          # Node dependencies
└── README.md            # This file
```

---

## 🔧 API Endpoints

### Tracking Endpoints

- `POST /api/track` - Track user visit
- `POST /api/activity` - Log user activity/event

### Analytics Endpoints

- `GET /api/analytics/overview` - Dashboard overview
- `GET /api/analytics/user/{userId}` - User activity timeline
- `GET /api/analytics/events` - Event statistics

---

## 🐛 Troubleshooting

### Backend Won't Start

**Problem:** `ModuleNotFoundError`
```bash
# Solution: Install dependencies
pip install -r requirements.txt
```

**Problem:** MongoDB connection failed
```bash
# Solution: Check your .env file
# Ensure MONGODB_URI is correct
# Check MongoDB Atlas IP whitelist (allow your IP)
```

### Frontend Won't Start

**Problem:** `'vite' is not recognized`
```bash
# Solution: Install dependencies
npm install
```

**Problem:** Node version error
```bash
# Solution: Update Node.js to v20.19+ or v22.12+
```

### Tracking Not Working

**Problem:** Activities not logging
```bash
# Check backend is running on port 8080
# Check MongoDB connection
# Check browser console for errors
# Verify .env file is configured
```

---

## 🌐 Deployment

### Backend (Python)

Deploy to:
- **Railway.app** - Easy Python deployments
- **Render** - Free tier available
- **AWS EC2** - Full control
- **Google Cloud Run** - Serverless

### Frontend (React)

Deploy to:
- **Vercel** - Recommended for Vite
- **Netlify** - Easy setup
- **AWS S3 + CloudFront** - CDN delivery

### Environment Variables for Production

Update these for production:
```env
MONGODB_URI=your_production_mongodb_uri
SERVER_PORT=8080
APP_NAME=suvidha-production
```

Update frontend API URL in `src/utils/analytics.js`:
```javascript
const API_BASE_URL = 'https://your-backend-url.com/api';
```

---

## 📝 Key Configuration Files

### Backend `.env`
```env
MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/suvidha
SERVER_PORT=8080
APP_NAME=backend
```

### Frontend `vite.config.js`
Already configured for React + Vite

---

## 👥 Support

For issues or questions:
1. Check this README
2. Review MongoDB Atlas setup
3. Verify all dependencies are installed
4. Check terminal for error messages

---

## 📄 License

This project is proprietary software.

---

## ✅ Quick Checklist

- [ ] Node.js v20.19+ installed
- [ ] Python 3.8+ installed
- [ ] MongoDB Atlas account created
- [ ] Repository cloned
- [ ] Backend dependencies installed (`pip install -r requirements.txt`)
- [ ] Frontend dependencies installed (`npm install`)
- [ ] `.env` file created in `backend/` directory
- [ ] MongoDB URI configured in `.env`
- [ ] Backend running on port 8080
- [ ] Frontend running on port 5173
- [ ] Can access http://localhost:5173/
- [ ] Activity tracking working (check MongoDB Atlas)

**All set? Start building! 🚀**
