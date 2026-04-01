# Social Exchange Backend

Small Node.js backend to exchange OAuth authorization codes for Facebook and Twitter/X access tokens.

## 1) Setup

1. Copy `.env.example` to `.env`.
2. Fill real secrets:
   - `FB_CLIENT_SECRET`
   - `X_CLIENT_SECRET`

## 2) Install and run

```bash
npm install
npm start
```

Server default:
- `http://localhost:8080`

Health check:
- `GET /health`

Exchange endpoint:
- `POST /api/social/exchange`

## 3) Request format

```json
{
  "provider": "facebook",
  "code": "oauth_code_here",
  "state": "optional_state",
  "code_verifier": "pkce_verifier_for_twitter",
  "redirect_uri": "zenin://auth/callback?provider=facebook"
}
```

## 4) Response format

```json
{
  "access_token": "token",
  "refresh_token": "optional_refresh_token",
  "expires_in": 3600
}
```

## 5) Android config

Set in `app/src/main/java/com/zenin/activity/AuthBridgeActivity.java`:

```java
private static final String BACKEND_EXCHANGE_URL = "https://YOUR_DOMAIN/api/social/exchange";
```
