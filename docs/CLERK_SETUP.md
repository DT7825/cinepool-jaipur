# Clerk OTP and Java API setup

This app uses Clerk for email verification codes, verification, and browser sessions. Phone and SMS are not authentication methods in this application. Phone numbers remain in the movie booking and ride/contact domain where relevant. The Spring API verifies Clerk's signed session JWT on every `/api/**` request. The browser's signed-in state by itself is not trusted by the API.

## 1. Create and configure a Clerk application

1. Create a Clerk account and an application in the [Clerk Dashboard](https://dashboard.clerk.com/).
2. In **Configure > User & authentication > Email**, enable email sign-up and sign-in with **verification codes**. Enable email verification during sign-up.
3. Do not make phone number a required authentication field. Phone and SMS authentication can stay disabled; phone numbers used by booking, rides, or contact features are separate app data.
4. In **API Keys**, copy the **Publishable key** (starts with `pk_test_` for development). It is meant for frontend use. Do not copy a Clerk secret key into Vite, source files, or Git.
5. In your local frontend `.env`, set `VITE_CLERK_PUBLISHABLE_KEY=pk_test_...`. Restart `npm run dev` after changing `.env`.
6. In the Clerk Dashboard, note the Frontend API / issuer host. Set the Java environment values below to the exact issuer and its JWKS endpoint.

## 2. Configure the Java backend

Copy `backend/.env.example` as a reference and set these environment variables in the same shell/process that runs the Spring app:

- `CLERK_ISSUER_URL`: exact `iss` value from a Clerk session token (for example, your Clerk development Frontend API URL).
- `CLERK_JWKS_URL`: that URL plus `/.well-known/jwks.json`.
- `CLERK_ALLOWED_ORIGINS`: comma-separated frontend origins permitted in Clerk's `azp` token claim. Include the exact origin you use (`http://localhost:5173` or `http://127.0.0.1:5173`).
- `APP_CORS_ALLOWED_ORIGINS`: same browser origins, allowed to call the API.

These values are public identifiers and URLs, not credentials. The API retrieves Clerk's public signing keys from JWKS and checks signature, issuer, expiry/not-before, and allowed `azp`. It does not need a Clerk secret key. `/api/auth/me` is a small protected endpoint demonstrating the verified Clerk user ID. All `/api/**` endpoints require a valid bearer token.

Example PowerShell session (replace the sample URL with the value shown in your Clerk Dashboard):

```powershell
$env:CLERK_ISSUER_URL = "https://your-instance.clerk.accounts.dev"
$env:CLERK_JWKS_URL = "$env:CLERK_ISSUER_URL/.well-known/jwks.json"
$env:CLERK_ALLOWED_ORIGINS = "http://localhost:5173,http://127.0.0.1:5173"
$env:APP_CORS_ALLOWED_ORIGINS = $env:CLERK_ALLOWED_ORIGINS
cd backend
mvn spring-boot:run
```

The frontend helper `src/api.js` adds `Authorization: Bearer <Clerk session token>` to API requests. In a React component, pass `getToken` from Clerk's `useAuth()` hook to `clerkApiFetch()`.

## 3. Test email code delivery

- Use a real email inbox you control. Select Email OTP on the sign-in dialog, request a code, and verify it in the same browser session. Repeat using Sign in after the new account exists.
- Clerk development instances support special test identities, but test email addresses such as those containing `+clerk_test` use a fixed test code and do not deliver a real message. A successful test identity is not proof of actual delivery.
- For real email delivery, use a real inbox. For production, configure and verify your sending domain and its DNS records as Clerk instructs.
- Clerk's development email quota applies; requests may be rejected after the documented monthly cap. Production email sending domain setup may require DNS records.
- No OTP can arrive until `VITE_CLERK_PUBLISHABLE_KEY` and Clerk Dashboard authentication strategies are configured. This repository cannot verify inbox/SMS delivery without your Clerk instance and recipient numbers.

## 4. Send an authenticated API request

```jsx
import { useAuth } from '@clerk/react';
import { clerkApiFetch } from './api';

function ProfileButton() {
  const { getToken } = useAuth();
  async function loadProfile() {
    const profile = await clerkApiFetch('/api/auth/me', getToken);
    console.log(profile.userId);
  }
  return <button onClick={loadProfile}>Load profile</button>;
}
```

Never accept a user ID or role from browser state as proof of identity. Use the validated JWT subject on the Java side. Add application roles from trusted backend data/Clerk claims and authorize each owner operation on the server before implementing theatre management.
