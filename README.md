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

The backend defaults to the MySQL database `picturehouse` at `localhost:3306`. Set `DATABASE_PASSWORD` for the `picturehouse_app` user, or set the `DATABASE_URL`, `DATABASE_USER`, and `DATABASE_DRIVER` variables for your database. See `backend/README.md` for API details.

## Project files

- `backend/` — Java 21 / Spring Boot project with JPA domain entities, role and booking states, fare categories, provider interfaces and WebSocket setup.
- [Current PRD](docs/Cinepool_Jaipur_PRD.md) and [PDF](output/pdf/Cinepool_Jaipur_PRD.pdf) — verified product scope, user journeys and roadmap.
- [Current SRS](docs/Cinepool_Jaipur_SRS.md) and [PDF](output/pdf/Cinepool_Jaipur_SRS.pdf) — detailed implemented requirements, interfaces, data model and acceptance checks.
- `docs/Picturehouse_Jaipur_PRD.docx` and `docs/Picturehouse_Jaipur_SRS.docx` — older planning drafts; the current Markdown/PDF pair above supersedes them.
- `docs/Picturehouse_Jaipur_Project_Report.docx` — feature operation, frontend/backend/database overview, OOP examples, ER model and normalization.
- `docs/Picturehouse_Jaipur_ER_Diagram.svg` — editable logical entity relationship diagram.

## Prototype boundary

The development movie/theatre/showtime listings remain sample records and must be replaced with participating theatre data. Razorpay Test Mode verifies payment, reserves seats transactionally, and creates a QR ticket in **My tickets**. Ride posts and owner-to-theatre approval are persisted. Email delivery is implemented but requires SMTP credentials and a Clerk email session claim; see [Email and owner setup](docs/EMAIL_OWNER_SETUP.md). WhatsApp delivery, Razorpay webhooks, and theatre gate QR redemption remain future work. See [Clerk setup](docs/CLERK_SETUP.md) and [Razorpay setup](docs/RAZORPAY_SETUP.md).
