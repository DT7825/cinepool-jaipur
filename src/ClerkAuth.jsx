import { useEffect, useState } from 'react';
import { useAuth, useClerk, useSignIn, useSignUp, useUser } from '@clerk/react';
import { clerkApiFetch } from './api';

function friendlyError(error) {
  return error?.errors?.[0]?.longMessage || error?.errors?.[0]?.message || error?.message || 'Clerk could not complete this request. Check your Clerk Dashboard settings and try again.';
}

export function ClerkHeaderAuth({ onSignIn }) {
  const { isSignedIn, user } = useUser();
  const { signOut } = useClerk();
  if (isSignedIn) {
    return <button className="login-button" onClick={() => signOut()} title="Sign out">{user?.firstName || user?.primaryEmailAddress?.emailAddress || 'Account'} · Sign out</button>;
  }
  return <button className="login-button" onClick={onSignIn}>Sign in</button>;
}

function loadRazorpayCheckout() {
  if (window.Razorpay) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const existing = document.querySelector('script[data-razorpay-checkout]');
    if (existing) {
      existing.addEventListener('load', resolve, { once: true });
      existing.addEventListener('error', reject, { once: true });
      return;
    }
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.async = true;
    script.dataset.razorpayCheckout = 'true';
    script.onload = resolve;
    script.onerror = () => reject(new Error('Could not load Razorpay Checkout. Check your internet connection and try again.'));
    document.body.appendChild(script);
  });
}

export function ClerkRazorpayCheckout({ movieTitle, theatreName, showDetails, showtimeId, seats, total, onSignIn, onSuccess, onError }) {
  const { isLoaded, isSignedIn, user } = useUser();
  const { getToken } = useAuth();
  const [busy, setBusy] = useState(false);

  async function startPayment() {
    if (!isSignedIn || busy) return;
    setBusy(true);
    try {
      const order = await clerkApiFetch('/api/payments/razorpay/orders', getToken, {
        method: 'POST',
        body: JSON.stringify({ showtimeId, seats }),
      });
      await loadRazorpayCheckout();
      if (!window.Razorpay) throw new Error('Razorpay Checkout did not finish loading. Please try again.');

      const checkout = new window.Razorpay({
        key: order.keyId,
        amount: order.amount,
        currency: order.currency,
        name: 'Picturehouse Jaipur',
        description: `${movieTitle} · ${seats.join(', ')}`,
        order_id: order.orderId,
        prefill: { email: user?.primaryEmailAddress?.emailAddress || '' },
        theme: { color: '#ed6b40' },
        handler: async (result) => {
          try {
            const verified = await clerkApiFetch('/api/payments/razorpay/verify', getToken, {
              method: 'POST',
              body: JSON.stringify({
                razorpayOrderId: result.razorpay_order_id,
                razorpayPaymentId: result.razorpay_payment_id,
                razorpaySignature: result.razorpay_signature,
              }),
            });
            if (!verified.verified) {
              onError(verified.status === 'REFUNDED'
                ? 'Those seats were booked by someone else during checkout. Razorpay has started a refund.'
                : 'Payment could not be turned into a ticket. Please contact support.');
              return;
            }
            let emailStatus = null;
            let emailError = '';
            try {
              emailStatus = await clerkApiFetch(`/api/bookings/${verified.bookingReference}/email`, getToken, { method: 'POST' });
            } catch (deliveryError) {
              emailError = deliveryError.message || 'Email delivery is not configured yet.';
            }
            onSuccess(verified, emailStatus, emailError);
          } catch (error) {
            onError(error.message || 'Razorpay could not verify this payment.');
          } finally {
            setBusy(false);
          }
        },
        modal: { ondismiss: () => setBusy(false) },
      });
      checkout.on('payment.failed', (event) => {
        setBusy(false);
        onError(event?.error?.description || 'Razorpay could not complete that payment. You can try again.');
      });
      checkout.open();
    } catch (error) {
      setBusy(false);
      onError(error.message || 'Could not start Razorpay Checkout. Check that the Java backend is running.');
    }
  }

  return <button className="full-primary" type="button" disabled={!isLoaded || busy} onClick={isSignedIn ? startPayment : onSignIn}>
    {!isLoaded ? 'Checking sign-in…' : busy ? 'Opening Razorpay…' : isSignedIn ? `Pay ₹${total} with Razorpay` : `Sign in to pay ₹${total}`}
    <span aria-hidden="true">→</span>
  </button>;
}

export function ClerkTicketList({ onSignIn }) {
  const { isLoaded, isSignedIn } = useUser();
  const { getToken } = useAuth();
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [sending, setSending] = useState('');
  const [emailError, setEmailError] = useState('');

  async function resendTicket(reference) {
    setSending(reference); setEmailError('');
    try {
      await clerkApiFetch(`/api/bookings/${reference}/email`, getToken, { method: 'POST' });
      setTickets((items) => items.map((item) => item.bookingReference === reference ? { ...item, emailSent: true } : item));
    } catch (failure) { setEmailError(failure.message || 'Could not send the ticket email.'); }
    finally { setSending(''); }
  }

  useEffect(() => {
    let active = true;
    if (!isLoaded) return () => { active = false; };
    if (!isSignedIn) {
      setTickets([]);
      setLoading(false);
      return () => { active = false; };
    }
    setLoading(true);
    clerkApiFetch('/api/bookings/mine', getToken)
      .then((items) => { if (active) { setTickets(items); setError(''); } })
      .catch((failure) => { if (active) setError(failure.message || 'Could not load your tickets.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [getToken, isLoaded, isSignedIn]);

  if (!isLoaded || loading) return <div className="ticket-status">Loading your tickets…</div>;
  if (!isSignedIn) return <div className="empty-state"><div className="empty-icon">🎟</div><h3>Sign in to see your tickets.</h3><p>Your booking history is connected to your account.</p><button className="primary-button" onClick={onSignIn}>Sign in</button></div>;
  if (error) return <div className="auth-notice ticket-notice" role="alert">{error}</div>;
  if (!tickets.length) return <div className="empty-state"><div className="empty-icon">🎟</div><h3>Your next story starts here.</h3><p>After a successful payment, your confirmed booking and entry QR code will appear here.</p><button className="primary-button" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>Browse movies</button></div>;

  return <>{emailError&&<div className="auth-notice ticket-notice" role="alert">{emailError}</div>}<div className="ticket-list">{tickets.map((ticket) => <article className="ticket-card" key={ticket.bookingReference}>
    <div className="ticket-details"><div className="eyebrow">CONFIRMED BOOKING</div><h3>{ticket.movieTitle}</h3><p>{ticket.theatreName}</p><p>{ticket.showDetails}</p><p>Seats <strong>{ticket.seats}</strong></p><div className="ticket-reference">Booking reference <strong>{ticket.bookingReference}</strong></div><strong className="ticket-price">₹{Math.round(ticket.amountPaise / 100)}</strong><p className="ticket-email-status">{ticket.emailSent ? 'Ticket emailed' : 'Ticket email not sent yet'}</p><button className="primary-button" disabled={sending === ticket.bookingReference} onClick={() => resendTicket(ticket.bookingReference)}>{sending === ticket.bookingReference ? 'Sending…' : ticket.emailSent ? 'Send ticket again' : 'Email my ticket'}</button></div>
    <div className="ticket-qr"><img src={ticket.qrCodeDataUrl} alt={`Entry QR code for ${ticket.movieTitle}`} /><span>Show this QR at the theatre</span></div>
  </article>)}</div></>;
}

export function ClerkAuthFlow({ onClose }) {
  const { signIn, fetchStatus: signInFetchStatus } = useSignIn();
  const { signUp, fetchStatus: signUpFetchStatus } = useSignUp();
  const [mode, setMode] = useState('sign-in');
  const [identifier, setIdentifier] = useState('');
  const [code, setCode] = useState('');
  const [step, setStep] = useState('identifier');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');

  const value = identifier.trim();
  const identifierValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  const clerkReady = signInFetchStatus !== 'fetching' && signUpFetchStatus !== 'fetching';
  const ready = Boolean(identifierValid && clerkReady);

  async function sendCode(event) {
    event.preventDefault();
    if (!ready) return;
    setBusy(true);
    setMessage('');
    try {
      if (mode === 'sign-in') {
        const created = await signIn.create({ identifier: value });
        if (created.error) throw created.error;
        const sent = await signIn.emailCode.sendCode({ emailAddress: value });
        if (sent.error) throw sent.error;
      } else {
        const created = await signUp.create({ emailAddress: value });
        if (created.error) throw created.error;
        const sent = await signUp.verifications.sendEmailCode();
        if (sent.error) throw sent.error;
      }
      setStep('verify');
      setMessage(`A verification code was requested for ${value}. Check your inbox and spam folder.`);
    } catch (error) {
      if (mode === 'sign-in' && signIn.isTransferable) {
        setMessage('No account was found for this identifier. Switch to Create account to sign up.');
      } else if (mode === 'sign-up' && signUp.isTransferable) {
        setMessage('An account already uses this identifier. Switch to Sign in.');
      } else {
        setMessage(friendlyError(error));
      }
    } finally {
      setBusy(false);
    }
  }

  async function verifyCode(event) {
    event.preventDefault();
    if (!/^\d{4,8}$/.test(code)) {
      setMessage('Enter the verification code you received.');
      return;
    }
    setBusy(true);
    setMessage('');
    try {
      if (mode === 'sign-in') {
        const result = await signIn.emailCode.verifyCode({ code });
        if (result.error) throw result.error;
        if (signIn.status !== 'complete') throw new Error(`Additional verification is required (${signIn.status}). Check your Clerk sign-in settings.`);
        const finalized = await signIn.finalize();
        if (finalized.error) throw finalized.error;
      } else {
        const result = await signUp.verifications.verifyEmailCode({ code });
        if (result.error) throw result.error;
        if (signUp.status !== 'complete') throw new Error(`Clerk still requires: ${signUp.missingFields?.join(', ') || signUp.status}. Adjust required sign-up fields in the Dashboard.`);
        const finalized = await signUp.finalize();
        if (finalized.error) throw finalized.error;
      }
      onClose();
    } catch (error) {
      setMessage(friendlyError(error));
    } finally {
      setBusy(false);
    }
  }

  return <div className="modal-backdrop" onClick={onClose}>
    <div className="dialog login-dialog clerk-auth-dialog" onClick={(event) => event.stopPropagation()}>
      <button className="modal-x" onClick={onClose} aria-label="Close">×</button>
      <div className="login-mark">p</div>
      <div className="dialog-kicker">WELCOME TO PICTUREHOUSE</div>
      <h2>{step === 'verify' ? 'Verify it’s you' : 'Your movie night awaits.'}</h2>
      <p>{step === 'verify' ? `Enter the code sent to ${value}.` : 'Sign in or create your account with your email to book tickets and meet your movie crew.'}</p>
      {step === 'identifier' ? <>
        <div className="auth-toggle" aria-label="Authentication mode">
          <button type="button" className={mode === 'sign-in' ? 'active' : ''} onClick={() => { setMode('sign-in'); setMessage(''); }}>Sign in</button>
          <button type="button" className={mode === 'sign-up' ? 'active' : ''} onClick={() => { setMode('sign-up'); setMessage(''); }}>Create account</button>
        </div>
        <form onSubmit={sendCode}>
          <label className="field-label">EMAIL ADDRESS</label>
          <input className="text-field" value={identifier} onChange={(event) => setIdentifier(event.target.value)} placeholder="you@example.com" type="email" autoComplete="email" required />
          <button className="full-primary" type="submit" disabled={!ready || busy}>{busy ? 'Sending…' : 'Send email code'} <span aria-hidden="true">→</span></button>
        </form>
      </> : <form onSubmit={verifyCode}>
        <label className="field-label">VERIFICATION CODE</label>
        <input className="text-field otp-field" value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 8))} placeholder="Enter the code" inputMode="numeric" autoComplete="one-time-code" required />
        <button className="full-primary" type="submit" disabled={busy}>{busy ? 'Verifying…' : 'Verify & continue'} <span aria-hidden="true">→</span></button>
        <button className="text-link" type="button" onClick={() => { setStep('identifier'); setCode(''); setMessage(''); }}>Change email</button>
      </form>}
      {message && <p className="auth-notice" role="status">{message}</p>}
      <div className="login-terms">By continuing, you agree to our Terms & Privacy Policy.</div>
    </div>
  </div>;
}
