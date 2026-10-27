# Email tickets and theatre-owner access

## Ticket email setup

The Java API sends a booking confirmation and entry QR as an image attachment through SMTP. It never sends a ticket from React. A working mailbox/provider account and SMTP credentials are required before email can be delivered.

1. In the Clerk Dashboard, open **Configure → Sessions → Customize session token**.
2. Add this claim in the JSON claims editor and save:

   ```json
   {
     "email": "{{user.primary_email_address}}"
   }
   ```

   Clerk documents custom session-token claims [here](https://clerk.com/docs/guides/sessions/customize-session-tokens). Sign out and sign back in after saving, so the new token contains the claim.
3. Choose an SMTP email provider. Set these variables in the same PowerShell terminal you will use to start Spring Boot:

   ```powershell
   $env:SMTP_HOST = "smtp.gmail.com"
   $env:SMTP_PORT = "587"
   $env:SMTP_USERNAME = "your-sending-address@gmail.com"
   $env:TICKET_EMAIL_FROM = "your-sending-address@gmail.com"
   $secure = Read-Host "Enter your SMTP app password" -AsSecureString
   $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
   try { $env:SMTP_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
   finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
   ```

   Replace the example addresses with the sending mailbox. For Gmail, use an **app password** generated for an account with 2-Step Verification; do not use your normal Gmail password. For another provider, use its SMTP host, port, username and app password/API password instead. Never put these values in `VITE_*`, React files, Git, or chat.
4. Start (or restart) the Java backend from that same terminal: `cd backend`, then `mvn spring-boot:run`. The backend must run on port 8080.
5. Open **My tickets** while signed in. For the ticket already booked, click **Email my ticket**. A successful send updates it to **Ticket emailed**. Check Inbox and Spam. A new successful booking also attempts delivery automatically; if SMTP or the Clerk claim is missing, the booking and QR remain available in **My tickets**, and the page shows the delivery error.

This sends email only. WhatsApp ticket delivery has not been added because it requires a separate WhatsApp Business provider and template approval.

## Ride posting

Ride listings now persist in the H2 database. Pick any date from today through 90 days ahead, sign in, choose **Post your ride**, fill in the pickup, Jaipur theatre/movie/showtime, departure time, open seats and per-person contribution, then post. The app refreshes the date’s list from the API, so the new ride appears and remains after a backend restart. Empty dates show an empty state rather than sample drivers. A ride request creates a saved chat between the requesting moviegoer and ride owner. The owner can accept or decline; accepting reserves one open seat. Conversations remain visible under **Your ride chats**.

## Owner portal and approval

Customers and theatre partners use the same Clerk identity service, but Picturehouse has a separate **Owner portal** screen. The old Customer view/Theatre owner toggle was only a visual mock and has been removed. An owner requests access to a theatre. Only a Clerk ID listed in backend setting `ADMIN_CLERK_USER_IDS` can see and approve requests. Owner dashboard data is filtered on the server by approved theatre IDs; frontend role selection cannot grant access.

To bootstrap the app administrator locally:

1. Open the Clerk Dashboard **Users** page and copy your own Clerk user ID (it starts with `user_`).
2. In the PowerShell terminal that starts the backend, set `$env:ADMIN_CLERK_USER_IDS = "user_replace_with_your_id"` before `mvn spring-boot:run`.
3. Sign in to Picturehouse, open **Owner portal**, request the correct theatre, and approve that pending request from the administrator section on that page.
4. The approved account will see the dashboard for that theatre, with confirmed booking counts and gross ticket sales grouped by show. The owner can add a movie to their theatre, update movie details that are not shared with another theatre, create a showtime (with a 48-seat inventory), and reschedule a showtime. Other customers cannot view owner sales or call those management APIs.

Keep administrator IDs in backend-only environment settings. Before launch, replace local H2 with a backed-up production database and use the host's private environment-variable settings rather than source files for credentials.
