# Mixify Live Counter Deployment

This directory contains the Cloudflare Worker and Durable Object for the Mixify "Listening Now" feature.

## Requirements
Ensure you have Node.js and npm installed.

## Windows PowerShell Commands

1. **Install dependencies:**
```powershell
cd tools\mixify-live-counter
npm install
```

2. **Run tests using Vitest:**
```powershell
npm run test
```

3. **Start a local development server:**
```powershell
npm run dev
```

4. **Deploy to Cloudflare:**
Make sure you are logged into Wrangler (`npx wrangler login`).

```powershell
npm run deploy
```

## Endpoints
- `POST /api/listening/heartbeat` (Payload: `{"sessionId": "string"}`)
- `POST /api/listening/stop` (Payload: `{"sessionId": "string"}`)
- `GET /api/stats/live` (Returns JSON with `listeningNow`)
- `GET /api/stats/live/badge.svg` (Returns live SVG badge)
