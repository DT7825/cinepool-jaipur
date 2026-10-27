# Picturehouse Jaipur API

Java / Spring Boot 3 backend for the Jaipur movie ticket and ride-share product. Clerk delivers email OTPs and Spring Security validates Clerk session tokens on `/api/**`. Razorpay test-mode order creation and payment verification are implemented. A captured payment locks and books the selected database seats and creates a unique ticket record with an opaque QR credential. Authenticated customers can retrieve their own tickets from `/api/bookings/mine` and request an SMTP email with QR attachment from `/api/bookings/{reference}/email`.

## Run locally

```powershell
cd backend
mvn spring-boot:run
```

Before starting, set `CLERK_ISSUER_URL`, `CLERK_JWKS_URL`, `CLERK_ALLOWED_ORIGINS`, and `APP_CORS_ALLOWED_ORIGINS`; see [the Clerk setup guide](../docs/CLERK_SETUP.md). For the Razorpay demo, also set `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` to Test Mode credentials; see [the Razorpay setup guide](../docs/RAZORPAY_SETUP.md). The API intentionally refuses to start without the Clerk issuer and JWKS configuration.

The local database is H2 in file mode at `backend/data/picturehouse`, so database records survive a backend restart. On first start, the default development profile creates clearly labeled demo movies, theatres, showtimes, and seat availability. The React site fetches these from `GET /catalog` and `GET /catalog/showtimes/{showtimeId}/seats`. Ride posts persist at `GET /api/rides?date=YYYY-MM-DD` (public browse) and authenticated `POST /api/rides`. Replace seed records with listings supplied by participating theatres before treating them as real schedules. The React app runs separately with `npm run dev` from the repository root.

To connect PostgreSQL instead, set `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, and `DATABASE_DRIVER`. Set the Spring profile to `prod` to disable automatic development catalog seeding.

## Design notes

- `domain`: entities and enums. Prices are represented as integer paise; `SeatCategory` applies the requested Jaipur seed prices.
- `integration`: provider clients for Razorpay orders/payments and provider boundaries for ticket delivery. OTP generation, delivery, and verification belong to Clerk.
- `config`: live-update transport setup. The WebSocket endpoint is `/ws`; show inventory events should be published to a topic scoped to a showtime after a committed database transaction.
- Roles are `CUSTOMER`, `THEATRE_OWNER`, and `ADMIN`. The `/api/owner/**` endpoints check Clerk user-to-theatre memberships in the backend and expose sales only for approved theatres. Only IDs in backend environment variable `ADMIN_CLERK_USER_IDS` can approve owner access requests.

The movie, theatre, showtime, and seat listings are read from the database; the initial records are development examples, not verified theatre inventory. Ride posts and the theatre-owner dashboard and listing management are backed by the database and backend access checks. SMTP ticket email requires the setup in [the email and owner guide](../docs/EMAIL_OWNER_SETUP.md). WhatsApp delivery, and gate scanning/one-time redemption are not implemented. Do not accept live payments until real listings and seat layouts are loaded, and the complete payment, refund, delivery, and admission workflows have been tested.
