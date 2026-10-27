# Razorpay test checkout: beginner setup

This project now shows **Razorpay Checkout as its only payment provider**. The Java server creates each Razorpay order from a database showtime and seat selection, calculates the price on the server, validates the checkout signature, and confirms captured status with Razorpay. After capture, it locks the selected seat rows, stores the confirmed booking and opaque QR ticket credential, and marks those seats booked. Tickets are available under **My tickets** in the signed-in app. Catalog and seat inventory still begin with development examples, not verified theatre data.

Use Test Mode first. The backend accepts only a `rzp_test_` Key ID, so it refuses Live Mode keys.

## Part A — Create or sign in to Razorpay

1. Open the official [Razorpay Dashboard](https://dashboard.razorpay.com/) in your browser.
2. Sign in, or create a Razorpay account if you do not have one. Follow the site’s account verification steps if it asks.
3. Find the **Test Mode** switch in the dashboard and turn Test Mode on. Look for wording such as **Test Mode** near the top of the page. Do not switch to Live Mode for this tutorial.
4. Open **Account & Settings** (sometimes shown as a gear icon) and choose **API Keys**. If you cannot find it, use the dashboard search or open Razorpay’s [API key documentation](https://razorpay.com/docs/payments/dashboard/account-settings/api-keys/).
5. Choose **Generate Test Key** or **Generate Key** while the dashboard is still in Test Mode.
6. Razorpay shows a **Key ID** and a **Key Secret**. Treat the Key Secret like a password. Do not paste it into this chat, a React file, a screenshot, or a `VITE_...` variable.

If the dashboard asks you to complete business verification, follow its own instructions. Live payment collection requires Razorpay account activation and Live Mode credentials; completing code setup does not approve an account for live transactions.

## Part B — Open your project and start the frontend

1. Open **Visual Studio Code** (or the code editor you use for this project).
2. Choose **File → Open Folder…**.
3. Select `C:\Users\DHRUV\Documents\ChatGPT\ACM` and click **Select Folder**.
4. In VS Code, choose **Terminal → New Terminal**. A PowerShell prompt appears at the bottom.
5. Start the React website by entering:

   ```powershell
   npm run dev
   ```

6. Leave this terminal window running. Open the local address it prints, usually `http://localhost:5173`.

The website already needs a Clerk sign-in. Keep using your existing email OTP sign-in. The Java backend also needs the Clerk issuer settings from [the Clerk setup guide](CLERK_SETUP.md).

## If Windows says `mvn` is not recognized

I checked this computer: Java is installed, but the `mvn` command is not currently available. Maven is the tool that starts/builds this Java server.

1. Open the official [Apache Maven download page](https://maven.apache.org/download.cgi).
2. Download the **Binary zip archive**. Do not choose a source-code archive.
3. In File Explorer, open **Downloads**, right-click the Maven ZIP, and choose **Extract All…**. Extract it to a folder you can find, such as `C:\Tools`.
4. Open the extracted Maven folder, then its `bin` folder. Copy the full address from File Explorer’s address bar; it should end in something like `\apache-maven-3.x.x\bin`.
5. Click Windows **Start**, search for **Edit environment variables for your account**, and open it.
6. Under **User variables**, click **Path**, then **Edit**, then **New**. Paste the Maven `bin` folder address and click **OK** on each open window.
7. Close and reopen Visual Studio Code so it reads the updated PATH. Open a new terminal and run `mvn -version`. It should print an Apache Maven version and a Java version.

Apache’s instructions say to extract Maven and add its `bin` directory to `PATH`; see the official [Windows notes](https://maven.apache.org/guides/getting-started/windows-prerequisites.html).

## Part C — Start the Java backend with test keys

1. Open a **second** terminal using **Terminal → New Terminal**. Keep the React terminal open.
2. Set the four Clerk environment values for this PowerShell window. Replace `your-instance` with the actual Clerk development Frontend API host shown in your Clerk Dashboard. Keep the origin values exactly as shown if your website runs on port 5173:

   ```powershell
   $env:CLERK_ISSUER_URL = "https://your-instance.clerk.accounts.dev"
   $env:CLERK_JWKS_URL = "$env:CLERK_ISSUER_URL/.well-known/jwks.json"
   $env:CLERK_ALLOWED_ORIGINS = "http://localhost:5173,http://127.0.0.1:5173"
   $env:APP_CORS_ALLOWED_ORIGINS = $env:CLERK_ALLOWED_ORIGINS
   ```

3. Set your Razorpay credentials in the same terminal. Replace the example text with the Test Mode values from Razorpay. Do not type angle brackets:

   ```powershell
   $env:RAZORPAY_KEY_ID = "rzp_test_your_key_id"
   $env:RAZORPAY_KEY_SECRET = "your_test_key_secret"
   ```

4. Start the Java server:

   ```powershell
   cd backend
   mvn spring-boot:run
   ```

5. Leave this second terminal open too. The two `$env:` values exist only in this PowerShell window. If you close it and start the backend again, enter them again. Do not put the Key Secret in the root `.env` file; Vite exposes `VITE_...` variables to browser code.

## Part D — Try the payment in Test Mode

1. In your browser, sign in to Picturehouse using your email OTP.
2. Pick a movie, a showtime, and one or more available seats. The demo seat prices are calculated on the Java backend: front rows ₹250, middle rows ₹300, recliner row F ₹500.
3. Click **Continue**. The checkout dialog should show **Razorpay Checkout** as its only gateway.
4. Click **Pay ₹… with Razorpay**. Razorpay’s test checkout window should open. If you are signed out, sign in first and the app will return you to checkout.
5. Complete a test payment using Razorpay’s current Test Mode instructions and test credentials. Test Mode simulates a payment; it does not deduct real money. The available test methods and values can change, so use Razorpay’s [official test payment guide](https://razorpay.com/docs/payments/payments/test-card-upi-details/).
6. After checkout reports success, the Java server verifies the signature and confirms the payment is captured. It then books the selected seats and creates a QR ticket. The app opens **My tickets** to show the booking and QR.
7. To check Razorpay’s side, open **Transactions → Payments** in the Razorpay Dashboard while it is in Test Mode. Check for the test payment and its status.

If checkout does not open, confirm both servers are running, you signed into Clerk, the browser is still at the configured origin, and the backend terminal has the right environment values. If payment completes but verification reports that it is not captured, ask the Razorpay account owner to check **Account & Settings → Payments Capture**. Razorpay says payments are normally auto-captured, but certain cases may remain authorized; this app only reports a payment successful after it sees `captured`. Do not share keys or OTPs when asking for help.

## What is and is not connected yet

- **Connected for Test Mode:** Razorpay Standard Checkout, server-side order creation, server-side signature validation, authenticated order ownership, captured-status check, transactional seat reservation, persistent ticket record, QR creation, and the signed-in **My tickets** listing.
- **Seat availability:** The seat picker refreshes its database availability every 2.5 seconds. Payment verification also locks the selected seat rows; if a seat was taken during checkout, the backend attempts a full Razorpay refund rather than issuing a duplicate ticket.
- **Only Razorpay is offered by the app:** Razorpay Checkout can itself show payment instruments that are enabled for your Razorpay account. “Razorpay only” here means no separate Paytm or other gateway choice in Picturehouse.
- **Still not production ready:** the catalog and seat layouts are seeded development data. This implementation creates QR codes but does not email/WhatsApp them or provide a theatre gate scanner and one-time QR redemption. Payment completion depends on the browser returning to the app for verification; a signed Razorpay webhook should be added for reliable recovery if a customer closes the browser during payment. Do not accept live payments until real inventory, webhook reconciliation, support/refund handling, QR admission, and end-to-end testing are completed.

## Before going live

Use the real show and seat database, add signed webhook reconciliation and one-time gate validation, configure delivery and refund support, then run end-to-end and concurrency tests before replacing test credentials with Live credentials. Complete Razorpay account activation and configure a webhook on the Live dashboard. Razorpay requires an order created on the server and server-side verification of the checkout signature; see its [Standard Checkout integration steps](https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/integration-steps/) and [go-live checklist](https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/go-live-checklist/).
