# Picturehouse Jaipur

Jaipur-first cinema booking and ride-sharing prototype. The React client provides film and theatre discovery, showtime selection, live seat availability, Clerk email-code sign-in, Razorpay test checkout, light/dark themes, persisted ride posts, and a separate theatre-owner portal for show management and sales, backed by backend authorization.

## Run the prototype

```powershell
npm install
npm run dev
```

The React app loads movie, theatre, showtime, and seat-status listings from the Java API. On a new local database, the backend adds labeled development examples; replace them with real theatre-provided data before bookings. Clerk email OTP is enabled after configuring the publishable key and Dashboard strategies; see [Clerk setup](docs/CLERK_SETUP.md). Razorpay Test Mode checkout also requires backend test credentials; follow [the beginner Razorpay setup guide](docs/RAZORPAY_SETUP.md). To start the Java backend (Maven is installed on this computer):

```powershell
cd backend
mvn spring-boot:run
```

The backend defaults to file-based H2 at `backend/data/picturehouse.mv.db`, so its catalogue survives restarts. `backend/README.md` describes the API and PostgreSQL environment variables.

## Project files

- `backend/` — Java 21 / Spring Boot project with JPA domain entities, role and booking states, fare categories, provider interfaces and WebSocket setup.
- `docs/Picturehouse_Jaipur_PRD.docx` — product goals, scope, user roles, journeys and rollout.
- `docs/Picturehouse_Jaipur_SRS.docx` — functional and quality requirements, integration contracts and acceptance criteria.
- `docs/Picturehouse_Jaipur_Project_Report.docx` — feature operation, frontend/backend/database overview, OOP examples, ER model and normalization.
- `docs/Picturehouse_Jaipur_ER_Diagram.svg` — editable logical entity relationship diagram.

## Prototype boundary

The development movie/theatre/showtime listings remain sample records and must be replaced with participating theatre data. Razorpay Test Mode verifies payment, reserves seats transactionally, and creates a QR ticket in **My tickets**. Ride posts and owner-to-theatre approval are persisted. Email delivery is implemented but requires SMTP credentials and a Clerk email session claim; see [Email and owner setup](docs/EMAIL_OWNER_SETUP.md). WhatsApp delivery, Razorpay webhooks, and theatre gate QR redemption remain future work. See [Clerk setup](docs/CLERK_SETUP.md) and [Razorpay setup](docs/RAZORPAY_SETUP.md).
