import { useCallback, useEffect, useMemo, useState } from 'react';
import { ClerkAuthFlow, ClerkHeaderAuth, ClerkRazorpayCheckout, ClerkTicketList } from './ClerkAuth';
import { publicApiFetch } from './api';
import RideBoard from './RideBoard';
import OwnerPortal from './OwnerPortal';

const clerkConfigured = Boolean(import.meta.env.VITE_CLERK_PUBLISHABLE_KEY);

const seatRows = ['A', 'B', 'C', 'D', 'E', 'F'];
const seatCols = Array.from({ length: 8 }, (_, i) => i + 1);

function localDateKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function dateLabel(dateKey) {
  return new Date(`${dateKey}T00:00:00`).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

function Icon({ name, size = 18 }) {
  const paths = {
    pin: <><path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/></>,
    search: <><circle cx="10.8" cy="10.8" r="6.7"/><path d="m16 16 5 5"/></>,
    ticket: <><path d="M3 8V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v3a3 3 0 0 0 0 6v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-3a3 3 0 0 0 0-6Z"/><path d="M13 5v2m0 4v2m0 4v2"/></>,
    car: <><path d="m5 11 1.5-4.5A2 2 0 0 1 8.4 5h7.2a2 2 0 0 1 1.9 1.5L19 11"/><path d="M3 11h18l1 3v5h-3v-2H5v2H2v-5l1-3Z"/><circle cx="6.5" cy="14" r=".5"/><circle cx="17.5" cy="14" r=".5"/></>,
    moon: <path d="M20.9 13A8.8 8.8 0 0 1 11 3.1 9 9 0 1 0 20.9 13Z"/>,
    sun: <><circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42"/></>,
    user: <><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></>,
    arrow: <><path d="M5 12h14"/><path d="m13 6 6 6-6 6"/></>,
    close: <><path d="m18 6-12 12M6 6l12 12"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    star: <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9L12 3Z"/>,
    shield: <><path d="M12 22s8-3.5 8-10V5l-8-3-8 3v7c0 6.5 8 10 8 10Z"/><path d="m9 12 2 2 4-4"/></>,
    chat: <><path d="M21 11.5a8.4 8.4 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.4 8.4 0 0 1-3.8-.9L3 21l1.9-5.7a8.4 8.4 0 0 1-.9-3.8A8.5 8.5 0 0 1 8.7 3.9a8.4 8.4 0 0 1 3.8-.9h.5a8.5 8.5 0 0 1 8 8v.5Z"/></>,
  };
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}

function App() {
  const [dark, setDark] = useState(false);
  const [view, setView] = useState('movies');
  const [catalog, setCatalog] = useState({ movies: [], theatres: [], dataNotice: '' });
  const [catalogError, setCatalogError] = useState('');
  const [selectedFilm, setSelectedFilm] = useState(null);
  const [selectedTheatre, setSelectedTheatre] = useState(null);
  const [selectedShowtime, setSelectedShowtime] = useState(null);
  const [selectedShow, setSelectedShow] = useState('');
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [seatInventory, setSeatInventory] = useState([]);
  const [seatLoading, setSeatLoading] = useState(false);
  const [modal, setModal] = useState('');
  const [date, setDate] = useState(() => localDateKey(new Date()));
  const [toast, setToast] = useState('');
  const [search, setSearch] = useState('');
  const [authReturnToCheckout, setAuthReturnToCheckout] = useState(false);
  const loadCatalog = useCallback(async () => {
    const data = await publicApiFetch('/catalog');
    setCatalog(data);
    setCatalogError('');
    setSelectedFilm((current) => data.movies.find((movie) => movie.id === current?.id) || data.movies[0] || null);
  }, []);
  const films = catalog.movies;
  const venues = catalog.theatres;
  const bookingDates = useMemo(() => Array.from({ length: 8 }, (_, offset) => {
    const day = new Date();
    day.setDate(day.getDate() + offset);
    return localDateKey(day);
  }), []);
  const soldSeats = useMemo(() => new Set(seatInventory.filter((seat) => seat.booked).map((seat) => seat.id)), [seatInventory]);
  const visibleFilms = useMemo(() => films.filter((f) => f.title.toLowerCase().includes(search.toLowerCase())), [films, search]);
  const selectedTotal = selectedSeats.reduce((sum, s) => sum + (s[0] === 'F' ? 500 : 'DE'.includes(s[0]) ? 300 : 250), 0);
  const notify = (message) => { setToast(message); window.setTimeout(() => setToast(''), 2800); };
  const toggleSeat = (seat) => setSelectedSeats((old) => old.includes(seat) ? old.filter((x) => x !== seat) : [...old, seat]);
  const openSeatSelection = (venue, showtime) => {
    const film = films.find((item) => item.id === showtime.movieId);
    if (!film) return;
    setSelectedTheatre(venue);
    setSelectedFilm(film);
    setSelectedShowtime(showtime);
    setSelectedShow(showtime.time);
    setSelectedSeats([]);
    setModal('seats');
  };

  useEffect(() => {
    let cancelled = false;
    const refreshCatalog = () => {
      loadCatalog().catch((error) => {
        if (!cancelled) setCatalogError(error.message || 'Could not load movies from the database.');
      });
    };
    const refreshWhenVisible = () => {
      if (document.visibilityState === 'visible') refreshCatalog();
    };
    refreshCatalog();
    const refreshTimer = window.setInterval(refreshCatalog, 15000);
    window.addEventListener('focus', refreshCatalog);
    document.addEventListener('visibilitychange', refreshWhenVisible);
    return () => {
      cancelled = true;
      window.clearInterval(refreshTimer);
      window.removeEventListener('focus', refreshCatalog);
      document.removeEventListener('visibilitychange', refreshWhenVisible);
    };
  }, [loadCatalog]);

  useEffect(() => {
    if (modal !== 'seats' || !selectedShowtime?.id) return undefined;
    let cancelled = false;
    const refreshSeats = (initial = false) => {
      if (initial) setSeatLoading(true);
      publicApiFetch(`/catalog/showtimes/${selectedShowtime.id}/seats`)
        .then((inventory) => {
          if (cancelled) return;
          setSeatInventory(inventory);
          const booked = new Set(inventory.filter((seat) => seat.booked).map((seat) => seat.id));
          setSelectedSeats((selected) => selected.filter((seat) => !booked.has(seat)));
        })
        .catch((error) => { if (!cancelled && initial) { setSeatInventory([]); setCatalogError(error.message || 'Could not load seat availability.'); } })
        .finally(() => { if (!cancelled && initial) setSeatLoading(false); });
    };
    refreshSeats(true);
    const refreshTimer = window.setInterval(() => refreshSeats(), 2500);
    return () => { cancelled = true; window.clearInterval(refreshTimer); };
  }, [modal, selectedShowtime?.id]);

  const dateShows = (venue, movieId = null) => venue.showtimes.filter((show) => show.date === date && (movieId === null || show.movieId === movieId));
  const closeAuth = () => {
    setModal(authReturnToCheckout ? 'checkout' : '');
    setAuthReturnToCheckout(false);
  };

  return <div className={`app ${dark ? 'dark' : ''}`}>
    <header className="topbar"><a className="brand" href="#top" onClick={() => setView('movies')}><span className="brand-mark">p</span><span>picture<span className="brand-accent">house</span></span></a><div className="location"><span className="pin"><Icon name="pin" size={16}/></span><div><small>YOUR CITY</small><strong>Jaipur, Rajasthan</strong></div><span className="chevron">⌄</span></div><div className="top-search"><Icon name="search" size={18}/><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search movies, theatres..."/><kbd>⌘ K</kbd></div><div className="header-actions"><button className="theme-button" onClick={() => setDark(!dark)} aria-label="Toggle theme"><Icon name={dark ? 'sun' : 'moon'} size={18}/></button>{clerkConfigured ? <ClerkHeaderAuth onSignIn={() => setModal('login')} /> : <button className="login-button" onClick={() => setModal('login')}><Icon name="user" size={16}/> Sign in</button>}</div></header>
    <nav className="main-nav"><div className="nav-inner"><button className={view === 'movies' ? 'active' : ''} onClick={() => setView('movies')}><Icon name="ticket"/> Movies</button><button className={view === 'rides' ? 'active' : ''} onClick={() => setView('rides')}><Icon name="car"/> Ride together <span className="nav-new">NEW</span></button><button className={view === 'mytickets' ? 'active' : ''} onClick={() => setView('mytickets')}><Icon name="ticket"/> My tickets</button><span className="nav-spacer"/><button className={view === 'owner' ? 'active' : ''} onClick={() => setView('owner')}>Owner portal</button></div></nav>

    <main id="top">
      {view === 'movies' && <>
        <section className="hero"><div className="hero-bg"/><div className="hero-copy"><div className="hero-badge"><span/> JAIPUR’S BIG SCREEN GUIDE</div><h1>Make a night<br/>of <em>it.</em></h1><p>The best stories in the Pink City, waiting for your seat.</p><button className="hero-cta" onClick={() => document.getElementById('now-showing')?.scrollIntoView({ behavior: 'smooth' })}>Find your film <Icon name="arrow" size={16}/></button></div><div className="hero-note"><span className="note-line"/><div><strong>Plan the whole evening</strong><small>Tickets, rides & good company</small></div></div><div className="hero-stamp">PINK<br/>CITY<br/><span>✳</span></div></section>
        <section className="content-section" id="now-showing"><div className="section-heading"><div><div className="eyebrow"><span className="live-dot"/> DATABASE LISTINGS · JAIPUR</div><h2>Now showing <span>in Jaipur</span></h2></div><a className="see-all" href="#all" onClick={(e) => e.preventDefault()}>See all films <Icon name="arrow" size={15}/></a></div>
          {catalogError && <div className="auth-notice" role="status">{catalogError} Make sure the Java backend is running on port 8080.</div>}
          <div className="catalog-notice">{catalog.dataNotice || 'Connecting to the Jaipur movie database…'}</div>
          <div className="film-grid">{visibleFilms.map((film, index) => <article className={`film-card card-${index}`} key={film.id}><button className="poster" onClick={() => { setSelectedFilm(film); setModal('shows'); }} style={{ '--poster': `url(https://images.unsplash.com/${film.image}?auto=format&fit=crop&w=900&q=85)` }}><span className={`film-tag ${film.tone}`}>{film.tag}</span><span className="poster-score"><Icon name="star" size={13}/>{film.score}</span><span className="poster-open">Book tickets <Icon name="arrow" size={15}/></span></button><div className="film-meta"><div><h3>{film.title}</h3><p>{film.genre} · Jaipur</p></div><span className="rating">{film.rating}</span></div><div className="film-submeta"><span><Icon name="clock" size={13}/>{film.length}</span><span>Hindi · English subs</span></div></article>)}</div>
        </section>
        <section className="content-section theatres-section"><div className="section-heading"><div><div className="eyebrow"><span className="live-dot"/> AROUND THE CITY</div><h2>Find your <span>screen</span></h2></div><div className="date-select"><span>SHOWING ON</span><select value={date} onChange={(e) => setDate(e.target.value)}>{bookingDates.map((day) => <option key={day} value={day}>{dateLabel(day)}</option>)}</select></div></div>
          <div className="theatre-list">{venues.map((venue, vi) => { const shows = dateShows(venue, selectedFilm?.id); return <article className="theatre-card" key={venue.id}><div className="theatre-identity"><div className={`theatre-symbol symbol-${vi}`}>{venue.name.charAt(0)}</div><div><h3>{venue.name}</h3><p>{venue.area}</p><div className="theatre-chips"><span>{venue.type}</span><span>{venue.parking}</span></div></div></div><div className="showtimes"><small>{selectedFilm?.title || 'SELECT A MOVIE'} · 2D</small><div>{shows.map((show) => <button key={show.id} className={selectedShowtime?.id === show.id ? 'picked' : ''} onClick={() => openSeatSelection(venue, show)}>{show.time}</button>)}{shows.length === 0 && <span>No shows for this movie and date.</span>}</div></div><button className="theatre-arrow" disabled={!shows.length} onClick={() => openSeatSelection(venue, shows[0])}><Icon name="arrow" size={18}/></button></article>; })}</div>
        </section>
        <section className="ride-banner"><div className="ride-art"><span className="sun-disc"/><span className="ride-road">⌁</span><div className="ride-car">✳</div><span className="ride-spark">✦</span></div><div className="ride-copy"><div className="eyebrow">GOOD FILMS. BETTER COMPANY.</div><h2>Going the same way?</h2><p>Share a ride to the theatre. Split the fare and maybe meet your new movie buddy.</p><button onClick={() => setView('rides')}>Explore shared rides <Icon name="arrow" size={16}/></button></div></section>
      </>}

      {view === 'rides' && (clerkConfigured ? <RideBoard onSignIn={() => setModal('login')} /> : <section className="page-view"><div className="auth-notice">Configure Clerk sign-in to browse and post rides.</div></section>)}

      {view === 'mytickets' && <section className="page-view"><div className="section-heading"><div><div className="eyebrow">YOUR MOVIE NIGHTS</div><h2>My <span>tickets</span></h2></div></div>{clerkConfigured ? <ClerkTicketList onSignIn={() => setModal('login')} /> : <div className="auth-notice">Configure Clerk sign-in to view tickets.</div>}</section>}

      {view === 'owner' && (clerkConfigured ? <OwnerPortal onSignIn={() => setModal('login')} /> : <section className="page-view"><div className="auth-notice">Configure Clerk sign-in to access the owner portal.</div></section>)}
      <footer className="footer"><span className="brand small-brand"><span className="brand-mark">p</span><span>picture<span className="brand-accent">house</span></span></span><span>Made for movie nights in the Pink City <b>✳</b></span><span>© 2026 Picturehouse Jaipur</span></footer>
    </main>

    {modal === 'shows' && <div className="modal-backdrop" onClick={() => setModal('')}><div className="dialog show-dialog" onClick={(e) => e.stopPropagation()}><button className="modal-x" onClick={() => setModal('')}><Icon name="close"/></button><div className="dialog-kicker">PICK YOUR SHOW</div><h2>{selectedFilm?.title}</h2><p>{selectedFilm?.genre} · {selectedFilm?.length} · {dateLabel(date)}</p>{venues.map((venue) => { const shows = dateShows(venue, selectedFilm?.id); return <div className="dialog-venue" key={venue.id}><div><strong>{venue.name}</strong><small>{venue.area}</small></div><div>{shows.slice(0, 3).map((show) => <button key={show.id} onClick={() => openSeatSelection(venue, show)}>{show.time}</button>)}{shows.length === 0 && <small>No shows on this date.</small>}</div></div>; })}</div></div>}
    {modal === 'seats' && <div className="modal-backdrop" onClick={() => setModal('')}><div className="dialog seat-dialog" onClick={(e) => e.stopPropagation()}><button className="modal-x" onClick={() => setModal('')}><Icon name="close"/></button><div className="dialog-kicker">SELECT YOUR SEATS</div><h2>{selectedFilm?.title}</h2><p>{selectedTheatre?.name} · {selectedShowtime?.screen} · {dateLabel(selectedShowtime?.date || date)}, {selectedShow}</p><div className="screen-label"><span/> SCREEN THIS WAY <span/></div>{seatLoading ? <p className="catalog-notice">Loading seat availability from the database…</p> : <div className="seat-map">{seatRows.map((row) => <div className="seat-row" key={row}><small>{row}</small>{seatCols.map((col) => { const id = `${row}${col}`; const sold = soldSeats.has(id); return <button key={id} disabled={sold} onClick={() => toggleSeat(id)} className={`${sold ? 'sold' : ''} ${selectedSeats.includes(id) ? 'chosen' : ''} ${row === 'F' ? 'recliner' : ''}`} title={sold ? 'Already booked' : `${id} · ₹${row === 'F' ? 500 : 'DE'.includes(row) ? 300 : 250}`}>{row === 'F' ? '▰' : ''}</button>; })}<small>{row}</small></div>)}</div>}<div className="seat-legend"><span><i/> Available</span><span><i className="legend-selected"/> Selected</span><span><i className="legend-sold"/> Booked</span><span className="price-note">Front ₹250 · Middle ₹300 · Recliner ₹500</span></div><div className="seat-checkout"><span>{selectedSeats.length ? `${selectedSeats.join(', ')} · ₹${selectedTotal}` : 'Select seats to continue'}</span><button disabled={seatLoading || !selectedSeats.length} onClick={() => setModal('checkout')}>Continue <Icon name="arrow" size={15}/></button></div></div></div>}
    {modal === 'checkout' && <div className="modal-backdrop" onClick={() => setModal('')}><div className="dialog checkout-dialog" onClick={(e) => e.stopPropagation()}><button className="modal-x" onClick={() => setModal('')}><Icon name="close"/></button><div className="dialog-kicker">ALMOST MOVIE TIME</div><h2>Confirm your booking</h2><div className="booking-summary"><strong>{selectedFilm?.title}</strong><span>{selectedTheatre?.name} · {dateLabel(selectedShowtime?.date || date)}, {selectedShow}</span><span>Seats {selectedSeats.join(', ')}</span><b>₹{selectedTotal} <small>incl. taxes</small></b></div><label className="field-label">PAYMENT METHOD</label><div className="payment-options"><div className="selected razorpay-option" aria-label="Only payment provider: Razorpay">Razorpay Checkout</div></div><div className="checkout-note"><Icon name="shield" size={16}/> Secure Razorpay Checkout · Pay using the methods enabled in your Razorpay account</div>{clerkConfigured ? <ClerkRazorpayCheckout movieTitle={selectedFilm?.title} theatreName={selectedTheatre?.name} showDetails={`${selectedShowtime?.date} - ${selectedShow}`} showtimeId={selectedShowtime?.id} seats={selectedSeats} total={selectedTotal} onSignIn={() => { setAuthReturnToCheckout(true); setModal('login'); }} onSuccess={(_verified, emailStatus, emailError) => { setModal(''); setView('mytickets'); notify(emailStatus ? 'Payment verified, seats booked, QR created, and ticket email sent.' : `Payment verified and ticket booked. Email not sent yet: ${emailError}`); }} onError={(message) => notify(message)} /> : <button className="full-primary" onClick={() => setModal('login')}>Sign in to pay ₹{selectedTotal} <Icon name="arrow" size={16}/></button>}<small className="fine-print">A confirmed test payment books the selected seats and creates an entry QR ticket in My tickets.</small></div></div>}
    {modal === 'login' && (clerkConfigured ? <ClerkAuthFlow onClose={closeAuth} /> : <div className="modal-backdrop" onClick={closeAuth}><div className="dialog login-dialog" onClick={(e) => e.stopPropagation()}><button className="modal-x" onClick={closeAuth}><Icon name="close"/></button><div className="login-mark">p</div><div className="dialog-kicker">CLERK SETUP REQUIRED</div><h2>Enable secure sign-in</h2><p>Set <code>VITE_CLERK_PUBLISHABLE_KEY</code> in your frontend environment and enable email verification codes in your Clerk Dashboard.</p><div className="auth-notice" role="status">No code has been requested. Add your Clerk development publishable key and restart the frontend.</div></div></div>)}
    {toast && <div className="toast"><span>✓</span>{toast}</div>}
  </div>;
}

export default App;
