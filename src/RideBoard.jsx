import { useCallback, useEffect, useState } from 'react';
import { useAuth, useUser } from '@clerk/react';
import { clerkApiFetch, publicApiFetch } from './api';
import RideChat from './RideChat';

const todayKey = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; };
const futureKey = () => { const d = new Date(); d.setDate(d.getDate()+90); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; };
const dateText = (key) => new Date(`${key}T00:00:00`).toLocaleDateString('en-IN',{day:'numeric',month:'long',year:'numeric'});

export default function RideBoard({ onSignIn }) {
  const { isLoaded, isSignedIn, user } = useUser();
  const { getToken } = useAuth();
  const [date,setDate]=useState(todayKey()); const [rides,setRides]=useState([]); const [catalog,setCatalog]=useState({movies:[],theatres:[]});
  const [loading,setLoading]=useState(false); const [error,setError]=useState(''); const [notice,setNotice]=useState(''); const [showForm,setShowForm]=useState(false); const [busy,setBusy]=useState(false);
  const [form,setForm]=useState({pickupArea:'',theatreName:'',movieTitle:'',showTime:'',departureAt:'',availableSeats:'2',contributionRupees:'100'});
  const [conversations,setConversations]=useState([]);const [activeConversation,setActiveConversation]=useState(null);
  const reload=useCallback(async()=>{setLoading(true);setError('');try{const path=`/api/rides?date=${encodeURIComponent(date)}`;setRides(isSignedIn?await clerkApiFetch(path,getToken):await publicApiFetch(path));}catch(e){setError(e.message);}finally{setLoading(false);}},[date,getToken,isSignedIn]);
  useEffect(()=>{reload();},[reload]);
  useEffect(()=>{publicApiFetch('/catalog').then(setCatalog).catch(()=>{});},[]);
  const loadConversations=useCallback(async()=>{if(!isSignedIn){setConversations([]);return;}try{setConversations(await clerkApiFetch('/api/rides/conversations',getToken));}catch(e){setError(e.message);}},[getToken,isSignedIn]);
  useEffect(()=>{loadConversations();},[loadConversations]);
  const update=(key,value)=>setForm((old)=>({...old,[key]:value}));
  async function submit(e){e.preventDefault();setBusy(true);setError('');setNotice('');try{
    if(!form.pickupArea.trim()||!form.theatreName||!form.movieTitle||!form.showTime||!form.departureAt)throw new Error('Fill in each ride detail before posting.');
    await clerkApiFetch('/api/rides',getToken,{method:'POST',body:JSON.stringify({...form,displayName:user?.fullName||user?.primaryEmailAddress?.emailAddress?.split('@')[0]||'Moviegoer',rideDate:date,availableSeats:Number(form.availableSeats),contributionRupees:Number(form.contributionRupees),departureAt:form.departureAt})});
    setNotice('Your ride has been posted and is now visible for this date.');setShowForm(false);await reload();
  }catch(e){setError(e.message);}finally{setBusy(false);}}
  async function startChat(ride){if(!isSignedIn){onSignIn();return;}setError('');try{const c=await clerkApiFetch(`/api/rides/${ride.id}/requests`,getToken,{method:'POST'});setActiveConversation(c);await loadConversations();}catch(e){setError(e.message);}}
  const ridesOnDate=rides;
  return <section className="page-view">
    <div className="page-hero rides-hero"><div className="eyebrow">JAIPUR, ON THE WAY</div><h1>Good films.<br/><em>Better company.</em></h1><p>Find or post a ride to a Jaipur theatre and share the fare with moviegoers.</p><div className="ride-search"><span>Jaipur, Rajasthan</span><input aria-label="Choose ride date" type="date" min={todayKey()} max={futureKey()} value={date} onChange={e=>setDate(e.target.value)}/><button type="button" onClick={reload}>Find a ride</button></div></div>
    <div className="rides-toolbar"><div><div className="eyebrow">MEET YOU THERE</div><h2>Rides for {dateText(date)} <span>· Jaipur</span></h2></div><button className="primary-button" onClick={()=>{if(!isSignedIn){onSignIn();return;}setNotice('');setError('');setShowForm(v=>!v);}}>{showForm?'Close':'＋ Post your ride'}</button></div>
    {notice&&<div className="inline-success" role="status">{notice}</div>}{error&&<div className="auth-notice" role="alert">{error}</div>}
    {showForm&&<form className="owner-table ride-post-form" onSubmit={submit}><h3>Post a Jaipur ride</h3><div className="ride-form-grid">
      <label>Pickup area<input required maxLength="100" value={form.pickupArea} onChange={e=>update('pickupArea',e.target.value)} placeholder="e.g. Malviya Nagar"/></label>
      <label>Theatre<select required value={form.theatreName} onChange={e=>update('theatreName',e.target.value)}><option value="">Choose a theatre</option>{catalog.theatres.map(t=><option key={t.id} value={t.name}>{t.name}</option>)}</select></label>
      <label>Movie<select required value={form.movieTitle} onChange={e=>update('movieTitle',e.target.value)}><option value="">Choose a movie</option>{catalog.movies.map(m=><option key={m.id} value={m.title}>{m.title}</option>)}</select></label>
      <label>Showtime<input required maxLength="40" value={form.showTime} onChange={e=>update('showTime',e.target.value)} placeholder="e.g. 6:30 PM"/></label>
      <label>When do you leave?<input required type="datetime-local" value={form.departureAt} onChange={e=>update('departureAt',e.target.value)}/></label>
      <label>Seats available<select value={form.availableSeats} onChange={e=>update('availableSeats',e.target.value)}>{[1,2,3,4,5,6].map(n=><option key={n}>{n}</option>)}</select></label>
      <label>Contribution per person (₹)<input required type="number" min="1" max="10000" value={form.contributionRupees} onChange={e=>update('contributionRupees',e.target.value)}/></label>
    </div><button className="full-primary" disabled={busy||!isLoaded}>{busy?'Posting…':'Post ride'}</button></form>}
    {loading?<div className="ticket-status">Looking for rides…</div>:ridesOnDate.length===0?<div className="empty-state"><div className="empty-icon">🚗</div><h3>No rides posted for this date yet.</h3><p>Choose another date or sign in and post the first ride.</p><button className="primary-button" onClick={()=>{if(isSignedIn)setShowForm(true);else onSignIn();}}>Post a ride</button></div>:<div className="ride-list">{ridesOnDate.map(ride=><article className="ride-card" key={ride.id}><div className="ride-avatar peach">{(ride.displayName||'J').slice(0,2).toUpperCase()}</div><div className="ride-person"><h3>{ride.displayName}{ride.mine?' · Your ride':''}</h3><p>{ride.availableSeats} seat{ride.availableSeats===1?'':'s'} open</p><div className="ride-route"><strong>{ride.pickupArea}</strong><span/><strong>{ride.theatreName}</strong></div></div><div className="ride-show"><span>{ride.movieTitle.toUpperCase()}</span><strong>{ride.showTime}</strong><small>Leaves {new Date(ride.departureAt).toLocaleTimeString('en-IN',{hour:'numeric',minute:'2-digit'})}</small></div><div className="ride-price"><strong>₹{Math.round(ride.contributionPaise/100)} / person</strong><span>suggested contribution</span><button disabled={ride.mine} onClick={()=>startChat(ride)}>{ride.mine?'Your ride':isSignedIn?'Chat & request':'Sign in to request'}</button></div></article>)}</div>}
    {isSignedIn&&conversations.length>0&&<div className="ride-conversations"><h2>Your ride chats</h2>{conversations.map(c=><button key={c.requestId} onClick={()=>setActiveConversation(c)}><strong>{c.passengerName}</strong><span>{c.movieTitle} · {c.theatreName} · {c.status}</span></button>)}</div>}
    <div className="ride-trust"><p><strong>Ride safely.</strong> Agree on the pickup point and fare with your ride partner before travelling.</p></div>
    <RideChat conversation={activeConversation} onClose={()=>setActiveConversation(null)} onChanged={async()=>{await reload();await loadConversations();}}/>
  </section>;
}
