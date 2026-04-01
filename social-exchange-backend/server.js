require("dotenv").config();
const express = require("express");
const axios = require("axios");
const cors = require("cors");

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 8080;

app.get("/health", (req, res) => {
  res.json({ ok: true, service: "social-exchange-backend" });
});

app.post("/api/social/exchange", async (req, res) => {
  try {
    const { provider, code, redirect_uri, code_verifier } = req.body || {};
    if (!provider || !code || !redirect_uri) {
      return res.status(400).json({ error: "missing_required_fields" });
    }

    if (provider === "facebook") {
      const result = await axios.get(
        "https://graph.facebook.com/v19.0/oauth/access_token",
        {
          params: {
            client_id: process.env.FB_CLIENT_ID,
            client_secret: process.env.FB_CLIENT_SECRET,
            redirect_uri,
            code,
          },
          timeout: 15000,
        }
      );

      return res.json({
        access_token: result.data.access_token || "",
        refresh_token: "",
        expires_in: result.data.expires_in || 0,
      });
    }

    if (provider === "twitter") {
      const payload = new URLSearchParams({
        grant_type: "authorization_code",
        code,
        redirect_uri,
        code_verifier: code_verifier || "",
      });

      const basicAuth = Buffer.from(
        `${process.env.X_CLIENT_ID}:${process.env.X_CLIENT_SECRET}`
      ).toString("base64");

      const result = await axios.post(
        "https://api.twitter.com/2/oauth2/token",
        payload.toString(),
        {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            Authorization: `Basic ${basicAuth}`,
          },
          timeout: 15000,
        }
      );

      return res.json({
        access_token: result.data.access_token || "",
        refresh_token: result.data.refresh_token || "",
        expires_in: result.data.expires_in || 0,
      });
    }

    return res.status(400).json({ error: "unsupported_provider" });
  } catch (error) {
    const details = error.response ? error.response.data : error.message;
    return res.status(500).json({
      error: "exchange_failed",
      details,
    });
  }
});

app.listen(PORT, () => {
  console.log(`social-exchange-backend running on port ${PORT}`);
});
