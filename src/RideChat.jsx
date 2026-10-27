import { useCallback, useEffect, useState } from 'react';
import { useAuth, useUser } from '@clerk/react';
import { clerkApiFetch } from './api';

export default function RideChat({conversation,onClose,onChanged}){
 const {getToken}=useAuth();const {user}=useUser();const [messages,setMessages]=useState([]);const [text,setText]=useState('');const [error,setError]=useState('');const [busy,setBusy]=useState(false);
 const load=useCallback(async()=>{if(!conversation)return;try{setMessages(await clerkApiFetch(`/api/rides/conversations/${conversation.requestId}/messages`,getToken));setError('');}catch(e){setError(e.message);}},[conversation,getToken]);
 useEffect(()=>{load();const timer=window.setInterval(load,3000);return()=>window.clearInterval(timer);},[load]);
 async function send(e){e.preventDefault();if(!text.trim()||busy)return;setBusy(true);try{const m=await clerkApiFetch(`/api/rides/conversations/${conversation.requestId}/messages`,getToken,{method:'POST',body:JSON.stringify({message:text.trim()})});setMessages(old=>[...old,m]);setText('');setError('');}catch(e){setError(e.message);}finally{setBusy(false);}}
 async function decide(action){setBusy(true);try{await clerkApiFetch(`/api/rides/conversations/${conversation.requestId}/${action}`,getToken,{method:'PUT'});await onChanged();onClose();}catch(e){setError(e.message);}finally{setBusy(false);}}
 if(!conversation)return null;
 const isOwner=conversation.owner;const status=conversation.status;
 return <div className="modal-backdrop" onClick={onClose}><div className="dialog chat-dialog persisted-chat" onClick={e=>e.stopPropagation()}><button className="modal-x" onClick={onClose} aria-label="Close chat">×</button><div className="dialog-kicker">RIDE PARTNER CHAT · JAIPUR</div><h2>{conversation.passengerName}</h2><p>{conversation.movieTitle} · {conversation.theatreName} · {conversation.showTime} · {conversation.rideDate}</p><div className="ride-chat-status">Request status: <strong>{status}</strong> · ₹{Math.round(conversation.contributionPaise/100)} per person</div>
   <div className="chat-thread">{messages.length?messages.map(m=><div className={`chat-bubble ${m.mine?'mine':''}`} key={m.id}><small>{m.mine?'You':m.senderName}</small><p>{m.message}</p></div>):<p className="ticket-status">No messages yet. Send a message to discuss pickup and the ride.</p>}</div>
   {error&&<div className="auth-notice" role="alert">{error}</div>}
   {status!=='DECLINED'&&<form className="chat-compose" onSubmit={send}><input value={text} onChange={e=>setText(e.target.value)} maxLength={1200} placeholder="Discuss pickup or the ride…"/><button disabled={busy||!text.trim()} type="submit">Send</button></form>}
   {isOwner&&status==='PENDING'&&<div className="ride-chat-decisions"><button disabled={busy||conversation.availableSeats<1} className="full-primary" onClick={()=>decide('accept')}>Accept passenger</button><button disabled={busy} className="secondary-button" onClick={()=>decide('decline')}>Decline</button></div>}
   {status==='ACCEPTED'&&<small className="fine-print">Ride seat accepted. Confirm the final pickup and fare directly in this chat.</small>}
 </div></div>;
}
