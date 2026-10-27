from pathlib import Path
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs'
OUT.mkdir(exist_ok=True)

def shade(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd'); shd.set(qn('w:fill'), fill); tc_pr.append(shd)

def set_cell_text(cell, text, bold=False, color='292624', size=9):
    cell.text = ''
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(3)
    r = p.add_run(str(text)); r.bold = bold; r.font.name = 'Aptos'; r.font.size = Pt(size); r.font.color.rgb = RGBColor.from_string(color)
    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER

def page_field(paragraph):
    paragraph.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    r = paragraph.add_run('PICTUREHOUSE JAIPUR  ·  '); r.font.size = Pt(8); r.font.color.rgb = RGBColor(137,131,125)
    fld = OxmlElement('w:fldSimple'); fld.set(qn('w:instr'), 'PAGE'); paragraph._p.append(fld)

def make_doc(title, subtitle, label):
    d = Document(); sec = d.sections[0]
    sec.top_margin=Inches(.72); sec.bottom_margin=Inches(.68); sec.left_margin=Inches(.78); sec.right_margin=Inches(.78)
    styles=d.styles
    for name in ['Normal','Body Text']:
        styles[name].font.name='Aptos'; styles[name].font.size=Pt(9.5); styles[name].font.color.rgb=RGBColor(55,52,49)
        styles[name].paragraph_format.space_after=Pt(6); styles[name].paragraph_format.line_spacing=1.1
    for name, size in [('Title',30),('Heading 1',19),('Heading 2',13),('Heading 3',10)]:
        st=styles[name]; st.font.name='Aptos Display'; st.font.size=Pt(size); st.font.bold=(name!='Title'); st.font.color.rgb=RGBColor(0,0,0); st.paragraph_format.space_before=Pt(11); st.paragraph_format.space_after=Pt(5)
    styles['Title'].font.bold=True; styles['Title'].paragraph_format.space_before=Pt(0); styles['Title'].paragraph_format.space_after=Pt(9)
    title_ppr=styles['Title']._element.get_or_add_pPr()
    for border in title_ppr.findall(qn('w:pBdr')): title_ppr.remove(border)
    footer=sec.footer.paragraphs[0]; page_field(footer)
    p=d.add_paragraph(); p.paragraph_format.space_before=Pt(38); r=p.add_run(label.upper()); r.bold=True; r.font.size=Pt(9); r.font.color.rgb=RGBColor(222,104,62)
    d.add_paragraph(title, style='Title')
    title_para=d.paragraphs[-1]
    title_ppr=title_para._p.get_or_add_pPr()
    for border in title_ppr.findall(qn('w:pBdr')): title_ppr.remove(border)
    p=d.add_paragraph(); p.paragraph_format.space_after=Pt(17); r=p.add_run(subtitle); r.font.size=Pt(12); r.font.color.rgb=RGBColor(112,105,98)
    p=d.add_paragraph(); p.paragraph_format.space_before=Pt(4); r=p.add_run('Jaipur, Rajasthan  |  Version 1.0  |  18 October 2026'); r.font.size=Pt(9); r.font.color.rgb=RGBColor(137,131,125)
    d.add_paragraph('')
    return d

def para(d, text, bold_lead=None):
    p=d.add_paragraph()
    if bold_lead and text.startswith(bold_lead):
        p.add_run(bold_lead).bold=True; p.add_run(text[len(bold_lead):])
    else: p.add_run(text)
    return p

def bullets(d, items):
    for item in items:
        p=d.add_paragraph(style='List Bullet'); p.paragraph_format.space_after=Pt(3); p.add_run(item)

def table(d, headers, rows, widths=None):
    t=d.add_table(rows=1, cols=len(headers)); t.alignment=WD_TABLE_ALIGNMENT.CENTER; t.style='Table Grid'
    t.autofit=True
    for c,h in zip(t.rows[0].cells,headers): shade(c,'314F43'); set_cell_text(c,h,True,'FFFFFF',8.5)
    tr_pr=t.rows[0]._tr.get_or_add_trPr(); repeat=OxmlElement('w:tblHeader'); repeat.set(qn('w:val'),'true'); tr_pr.append(repeat)
    for ri,row in enumerate(rows):
        cells=t.add_row().cells
        tr_pr=t.rows[-1]._tr.get_or_add_trPr(); cant_split=OxmlElement('w:cantSplit'); tr_pr.append(cant_split)
        for c,val in zip(cells,row):
            if ri%2==1: shade(c,'F4F1EB')
            set_cell_text(c,val,size=8.3)
    d.add_paragraph().paragraph_format.space_after=Pt(1)
    return t

def h(d, text, level=1): d.add_heading(text, level=level)

def build_prd():
    d=make_doc('Picturehouse Jaipur Product Requirements Document','A local-first movie booking experience with shared rides to the theatre','Product requirements document')
    h(d,'1. Product summary')
    para(d,'Picturehouse Jaipur helps residents discover films, compare nearby theatre showtimes, book seats and coordinate a shared ride for the same screening. It combines a familiar Indian ticket-booking flow with a Jaipur-specific venue catalogue and a conversational carpool feature. The first release is a focused city MVP; payment, identity and messaging providers are pluggable and must be configured before live transactions.')
    h(d,'Product direction',2); para(d,'Make planning a cinema visit feel complete: choose the film, choose the seats, pay in INR, receive an entry QR, and optionally coordinate a ride with verified phone-number users who are going to the same show.')
    h(d,'2. Goals and measures')
    table(d,['Goal','MVP measure'],[
      ['Fast booking','A signed-in customer can reach seat selection from a film or theatre listing in three clear steps.'],
      ['Seat trust','Every active show has a single authoritative seat state; competing requests cannot sell the same seat.'],
      ['Jaipur relevance','Venue and ride discovery starts with Jaipur and supports neighbourhood-level pickup/dropoff.'],
      ['Complete visit','Confirmed tickets produce an email QR ticket; WhatsApp delivery is attempted through an approved business integration.'],
      ['Partner control','Verified theatre owners can manage only their own venue, catalogue and showtimes.']
    ])
    h(d,'3. Users and roles')
    table(d,['User','Needs','Permissions'],[
      ['Customer','Discover, book, pay, retrieve QR, post/join rides and chat.','Browse Jaipur listings; manage own profile, bookings and rides.'],
      ['Theatre owner','Publish accurate venue, movie and show information.','Create/update only mapped theatres, screens, movies and showtimes; inspect occupancy.'],
      ['Platform admin','Keep catalogue trustworthy and resolve abuse.','Approve owners, manage city catalogue, moderate ride posts and audit operations.']
    ])
    h(d,'4. MVP scope')
    bullets(d,['Jaipur-only city configuration with theatre location, neighbourhood and distance labels.','Customer onboarding with email and Indian mobile verification; first-use signup before protected actions.','Film discovery by date, language, genre, certification, theatre and showtime.','Interactive seat map with live inventory updates, short-lived holds and the requested base fare bands: front ₹250, middle ₹300 and recliner ₹500.','Checkout provider boundary for Razorpay-hosted UPI/cards/net banking and supported provider methods; expose familiar Google Pay, PhonePe and Paytm options where the provider supports them.','Ticket confirmation, unique entry QR, email delivery, WhatsApp template delivery and scan validation at entry.','Ride posts linked to a theatre/show, pickup area, departure, seats, estimated share, verified user profile, chat and join request.','Customer and theatre-owner role-based views; light and dark themes; responsive mobile-first layout.'])
    h(d,'5. Out of scope for the first release')
    bullets(d,['Operation outside Jaipur, seat maps for real venues until inventory is onboarded, dynamic surge pricing, driver dispatch, vehicle tracking, cash settlement, subscriptions, loyalty points and public reviews.','Storing UPI credentials or processing card data directly. The system delegates payment collection to an approved payment provider.','Guaranteeing WhatsApp delivery without an approved WhatsApp Business sender, consent-compliant templates and a configured provider.'])
    h(d,'6. Core journeys')
    h(d,'Customer booking',2); para(d,'Open Jaipur listings → choose a film and showtime → inspect live seats and price → hold seats for a short payment window → sign in or create an account → verify mobile and email → pay through the provider checkout → confirm only from a verified payment webhook → issue ticket and QR → send email and WhatsApp notifications.')
    h(d,'Shared ride',2); para(d,'After choosing a show, a customer posts a pickup area, departure time, available seats and fare contribution, or filters matching posts. They request to join and use in-app chat. The ride owner accepts or declines; joining a ride does not alter the movie booking or represent a taxi dispatch.')
    h(d,'Theatre owner',2); para(d,'An approved owner signs in → selects an owned theatre → edits venue and screen information → adds or updates a movie listing and showtime → publishes availability and monitors bookings. Every write is authorized against the owner-to-theatre relationship.')
    h(d,'7. Product rules')
    bullets(d,['Money is stored as integer paise; display amounts are formatted in INR. User-supplied prices cannot bypass the published seat-category fare rules without an explicit authorized override.','Seat holds expire automatically if payment is not completed. A database transaction and unique active-seat constraint arbitrate simultaneous requests; clients receive updates after commit.','A booking is confirmed only after a signed provider webhook has passed amount, currency, booking and idempotency checks.','QR tokens contain no personal data, are signed or opaque, expire with the show-entry window, and are validated once by authorized theatre staff.','Email and WhatsApp are delivery channels, not proof of booking. The booking receipt is always available in the account.','Ride contact details remain hidden until a request is accepted; abuse report, block, cancellation and moderation actions are logged.','Theme preference persists locally; contrast and keyboard focus remain usable in both themes.'])
    h(d,'8. Experience and content')
    para(d,'Use conversational Indian English, rupee formatting and recognisable payment terminology. Show certification, language, duration, show format, neighbourhood, parking and clear cancellation terms. Mobile layouts prioritize location, date, showtimes, large seat controls and a concise booking summary. Colour is paired with labels for seat state and status.')
    h(d,'9. Launch assumptions and dependencies')
    bullets(d,['The company obtains theatre agreements, verified owner identities, venue screen/seat layouts and a source of accurate show inventory.','Production identity uses an OTP provider with delivery, retry, rate limits and secure verification; mobile OTP normally uses an India DLT-compliant route.','Payment gateway merchant onboarding and webhook secrets are available for the launch entity.','Email sending domain and WhatsApp Business account/templates are approved.','Privacy notice, retention schedule, cancellation/refund policy, local ride-sharing terms and customer-support process are approved before public release.'])
    h(d,'10. Rollout')
    table(d,['Stage','Exit condition'],[
      ['Prototype','Browse, role switch, seat selection, ride views and theme work with sample data.'],
      ['Private pilot','Verified Jaipur venues onboarded; provider sandboxes pass; concurrency and webhook flows reviewed; owner access is scoped.'],
      ['Public Jaipur MVP','Production providers approved; monitoring/support staffed; QR scanning and refund operations rehearsed.'],
      ['Expansion gate','Seat accuracy, payment reconciliation, delivery, safety and adoption metrics meet agreed operating thresholds.']
    ])
    d.save(OUT/'Picturehouse_Jaipur_PRD.docx')

def build_srs():
    d=make_doc('Picturehouse Jaipur Software Requirements Specification','Functional and quality requirements for the first Jaipur release','Software requirements specification')
    h(d,'1. Purpose and system boundary')
    para(d,'This specification defines the Jaipur movie-ticket and carpool platform across its React web client, Java Spring Boot API, relational database and external providers. The browser presents catalog, show, seat, checkout, ticket, owner and ride experiences. The API owns identity, authorization, inventory, booking state, price calculations, QR validation and integration workflows. Payment, SMS/OTP, email and WhatsApp providers remain external systems.')
    h(d,'2. System context')
    table(d,['Component','Responsibility'],[
      ['React frontend','Responsive web UI, accessibility, theme preference, seat map, live updates and customer/owner workspaces.'],
      ['Java backend','REST APIs and WebSocket events; domain rules; auth/roles; transactional booking; provider adapters.'],
      ['PostgreSQL','Durable accounts, theatre catalogue, screen/show inventory, holds, bookings, payments, rides, memberships and audit data.'],
      ['External services','OTP delivery, payment checkout and signed webhooks, email delivery, WhatsApp Business notifications and QR scan device/app.']
    ])
    h(d,'3. Functional requirements')
    requirements=[
      ('FR-01','The system shall show Jaipur as the initial city and filter theatres and rides by area, date and showtime.'),
      ('FR-02','A visitor attempting a protected action shall be offered sign-in or signup before continuing.'),
      ('FR-03','An account shall store unique email and E.164 mobile number and track verification of both channels.'),
      ('FR-04','The system shall provide customer, theatre-owner and administrator roles; owner writes shall be limited to mapped theatres.'),
      ('FR-05','Customers shall browse films, certifications, languages, venues, dates, formats and showtimes.'),
      ('FR-06','For each show, the system shall return seat states and category prices: front ₹250, middle ₹300, recliner ₹500 as configurable INR price values.'),
      ('FR-07','Seat selection shall create an expiring hold. Competing holds or bookings for a seat/show combination shall be rejected atomically.'),
      ('FR-08','Seat-state changes shall be published only after transaction commit and clients shall refresh inventory after reconnect.'),
      ('FR-09','The system shall create provider checkout requests with amount, INR currency and idempotency reference; confirmation shall follow a verified webhook.'),
      ('FR-10','A confirmed booking shall have a unique reference and privacy-safe, signed/opaque QR credential with show and entry-window validation.'),
      ('FR-11','The system shall queue email ticket/QR delivery and approved WhatsApp template delivery; delivery failures shall be retryable and visible operationally.'),
      ('FR-12','Authorized staff shall scan/validate an entry QR once and record the validation result and time.'),
      ('FR-13','Theatre owners shall create/update venue, screen, movie and showtime details and view aggregate occupancy.'),
      ('FR-14','Customers shall create ride posts linked to a show with pickup, dropoff, departure, open seats and per-person contribution.'),
      ('FR-15','Ride participants shall request to join, accept/decline requests and chat after authentication; contact details shall be access-controlled.'),
      ('FR-16','The system shall support a light/dark theme and preserve user theme preference.'),
      ('FR-17','Booking, payment, ticket, seat-hold and owner-write state transitions shall be auditable and idempotent where retried.')]
    table(d,['ID','Requirement'],requirements)
    h(d,'4. Data model and ER diagram')
    para(d,'The following logical entities show the principal relationships. The detailed schema and normalization notes are in the Project Report; implementation migrations should use PostgreSQL constraints as the final authority for concurrency and integrity.')
    para(d,'Core path: ACCOUNT → THEATRE_MEMBER → THEATRE → SCREEN → SHOWTIME ← MOVIE. Customers own BOOKINGS with BOOKING_SEAT and PAYMENT_ATTEMPT records; confirmed bookings have one TICKET_QR. SHOWTIME anchors SEAT_HOLD and RIDE records. RIDE_PARTICIPANT connects accounts to rides, while RIDE_MESSAGE stores ride chat. See the full ER diagram in the Project Report.')
    h(d,'5. External interface requirements')
    table(d,['Interface','Contract'],[
      ['REST API','Versioned `/api/v1`; JSON; validation errors include stable error code, safe message and correlation ID.'],
      ['Seat updates','WebSocket/STOMP endpoint; topic scoped to a show; events include show id, seat id, state and event version, never personal data.'],
      ['Payment','Provider checkout creation and signed webhook; verify signature, amount, INR currency, booking reference, event id and replay/idempotency.'],
      ['OTP','Separate mobile and email challenge flows; expire codes; limit resend and attempts; store only secure verifier/hash where provider does not own verification.'],
      ['Ticket delivery','Transactional outbox or durable queue for email and WhatsApp retries after confirmed payment.'],
      ['QR validation','Authenticated staff endpoint; hash/verify credential; show-time window; atomic one-time redemption.']
    ])
    h(d,'6. Non-functional requirements')
    table(d,['Area','Requirement'],[
      ['Concurrency','Database-level arbitration prevents duplicate active seat allocations regardless of WebSocket message order.'],
      ['Performance','Target 95th percentile under 500 ms for cached catalogue and under 1.5 s for seat map, excluding provider latency; validate against pilot load.'],
      ['Availability','Target 99.5% monthly for the initial service, excluding scheduled maintenance and external provider outages.'],
      ['Security','TLS; secure session/token lifecycle; passwordless OTP controls; least privilege; owner resource scoping; encrypted secrets; webhook signature verification; audit log.'],
      ['Privacy','Collect only needed profile and ride data; consent and retention controls for notifications; redact sensitive data from logs.'],
      ['Accessibility','Keyboard operable, labelled seat states, visible focus, reduced motion support and WCAG AA contrast target.'],
      ['Localization','Jaipur-local date/time, INR, India country code and English-first copy; later add Hindi strings without changing API semantics.'],
      ['Recovery','Database backups, payment reconciliation, expiry worker recovery and queued-notification retry/dead-letter handling.']
    ])
    h(d,'7. State and error handling')
    bullets(d,['Seat: AVAILABLE → HELD → BOOKED, or HELD → AVAILABLE at expiry/release. Holds have a server-controlled expiry.','Booking: PENDING_PAYMENT → CONFIRMED after verified payment; pending bookings may expire/cancel; refunds are tracked independently from cancellation.','Ride request: REQUESTED → ACCEPTED or DECLINED; accepted membership consumes one available seat atomically.','Retries with the same idempotency key return the prior result. A late payment after hold expiry follows a reconciliation/refund path and never silently reallocates an occupied seat.'])
    h(d,'8. Acceptance criteria')
    bullets(d,['Two clients selecting the same final seat cannot both receive a confirmed booking; the losing client receives an updated unavailable seat state.','An expired unpaid hold becomes available again and a retrying client sees the refreshed state.','A booking remains pending when a checkout redirect claims success but a valid webhook has not arrived.','Only a verified owner of a theatre can edit its catalogue or shows.','A confirmed booking creates one QR credential, can be delivered through configured channels, and cannot be redeemed twice.','A ride request exposes participant chat only to the ride owner and accepted/requesting members according to the product policy.'])
    d.save(OUT/'Picturehouse_Jaipur_SRS.docx')

def build_report():
    d=make_doc('Picturehouse Jaipur Project Report','Design and implementation plan for movie booking with shared rides','Project report')
    h(d,'Executive summary')
    para(d,'Picturehouse Jaipur is a Jaipur-focused web product concept that connects cinema discovery and seat booking with optional ride sharing for moviegoers attending the same show. The workspace now contains a responsive clickable frontend prototype and a Java Spring Boot domain foundation. The external transaction providers and production database deployment are intentionally integration points; the prototype does not create real bookings or send real OTPs, payments, tickets or WhatsApp messages.')
    h(d,'1. Problem and objectives')
    para(d,'Moviegoers commonly plan tickets, transport and coordination in separate apps. Theatre owners also need a controlled way to publish listings and showtimes. This project brings those tasks into one Jaipur-first flow while preserving clear role boundaries and strong seat-allocation rules.')
    bullets(d,['Make nearby films and showtimes easy to compare.','Prevent duplicate seat sales while keeping seat maps fresh across clients.','Issue a verifiable QR ticket only after payment confirmation.','Let customers coordinate a shared ride around the same screening.','Demonstrate object-oriented design through cohesive domain objects, encapsulated state changes, interfaces and role-specific operations.'])
    h(d,'2. Essential feature operation')
    table(d,['Feature','How it works'],[
      ['Signup and verification','First-time customers create an account, verify a mobile number and email, and obtain the customer role. Owner accounts are separately approved and assigned to one or more theatres.'],
      ['Discovery','Customer chooses a Jaipur date, film or neighbourhood; the catalogue returns matching active shows with language, certification, format and venue details.'],
      ['Live seat selection','A client reads the show inventory and subscribes to its seat-update topic. Selecting a seat requests a short hold from the API. A transaction with a database uniqueness/locking rule decides the winner; after commit the API broadcasts state.'],
      ['Pricing and payment','Server pricing maps front/middle/recliner categories to ₹250/₹300/₹500 seed prices. The checkout request uses integer paise and INR. A signed, idempotent gateway webhook confirms the booking.'],
      ['QR ticket and notifications','After confirmation, the server creates an opaque signed entry token, prepares a QR image and queues email and approved WhatsApp deliveries. Staff validation checks the show window and consumes the token once.'],
      ['Ride share','A customer posts a ride tied to a show. Other authenticated customers ask to join and use access-controlled chat. Acceptance reserves a ride seat; the fare contribution is disclosed before confirmation.'],
      ['Owner portal','Owner-scoped endpoints allow authorized changes to theatre, screen, film and showtime records. Aggregate occupancy is visible without exposing customer payment or contact details.'],
      ['Theme','The frontend applies a light or dark design token set and stores the choice on the device.']
    ])
    h(d,'3. Frontend, backend and database')
    h(d,'Frontend',2); para(d,'The user interface is built with React and Vite. It is a responsive single-page application with movie cards, theatre showtimes, a seat picker, checkout and Clerk email-code authentication, customer navigation, ride listings/chat and an owner view. Current interactions use local sample data and are not connected to the Java API. CSS design tokens support light/dark themes.')
    h(d,'Backend',2); para(d,'The backend uses Java 21 and Spring Boot. Domain classes such as `Movie`, `Theatre`, `Showtime`, `Booking`, `CarpoolRide` and `Account` model business concepts. Enums constrain roles, booking states and seat categories. Domain methods encapsulate state transitions and fare values. `PaymentGateway`, `OtpGateway` and `TicketDeliveryGateway` are interfaces that allow provider implementations to be substituted without coupling business rules to a vendor. Spring WebSocket configuration provides the transport foundation for live inventory events.')
    h(d,'Database',2); para(d,'PostgreSQL is the intended durable database, with H2 configured for local startup. Relational tables separate accounts, theatre ownership, venues, screens, movies, showtimes, physical seats, temporary holds, bookings, booking-seat snapshots, payment attempts, ticket credentials, ride posts, participants and chat messages. Amounts are integer paise; timestamps are stored in UTC and rendered in Jaipur local time. Database constraints and transaction isolation are essential for seat and ride-seat concurrency.')
    h(d,'4. Object-oriented programming demonstration')
    table(d,['OOP concept','Project example'],[
      ['Encapsulation','`Booking.confirm()` protects allowed booking transitions; `CarpoolRide.reserveSeat()` prevents overselling available places; fields are private.'],
      ['Abstraction','Provider interfaces define payment, OTP and ticket delivery operations without exposing vendor details to booking logic.'],
      ['Polymorphism','Different gateway implementations can satisfy one interface: sandbox, Razorpay, email provider or WhatsApp provider can be selected by configuration.'],
      ['Inheritance and composition','JPA entities share identity through persistence conventions; richer design should favor composition for account roles, theatre membership and notification preferences. The current model does not add artificial inheritance merely to demonstrate it.'],
      ['Single responsibility','Services coordinate transactions; controllers handle HTTP; repositories persist; adapters call providers.']
    ])
    h(d,'5. ER model and relationships')
    para(d,'The main relationships are: an account may own multiple theatre memberships; a theatre contains multiple screens; a screen hosts showtimes; a movie appears in many showtimes; a customer creates bookings for a showtime; bookings contain seat snapshots and payment attempts; a confirmed booking has a ticket credential; and a customer posts or joins ride records linked to showtimes. Chat messages belong to a ride and an account. Constraints enforce uniqueness of venue/show identity, seat/show active allocation, user membership and provider idempotency keys.')
    rows=[('ACCOUNT','Identity, verified email/mobile, role/status'),('THEATRE','Jaipur venue, neighbourhood, address'),('THEATRE_MEMBER','Owner-to-theatre role mapping'),('SCREEN','Venue screen and seat layout'),('MOVIE','Title, language, certification, runtime'),('SHOWTIME','Movie + screen + start time + format'),('SEAT','Physical seat label and category'),('SEAT_HOLD','Temporary seat allocation and expiry'),('BOOKING','Customer, show, status, INR total, reference'),('BOOKING_SEAT','Booked seat and price snapshot'),('PAYMENT_ATTEMPT','Provider, idempotency key, status, amount'),('TICKET_QR','Opaque token hash, expiry, redemption status'),('RIDE','Driver, show, route, time, capacity, fare share'),('RIDE_PARTICIPANT','Ride membership/request state'),('RIDE_MESSAGE','Access-controlled chat content and timestamp')]
    table(d,['Entity','Purpose'],rows)
    h(d,'6. ER diagram')
    para(d,'Simplified logical view; junction and operational tables are expanded in the entity list above.')
    erd='''ACCOUNT ──< THEATRE_MEMBER >── THEATRE ──< SCREEN ──< SHOWTIME >── MOVIE
   │                                              │                  │
   ├──< BOOKING ──< BOOKING_SEAT >── SEAT        ├──< SEAT_HOLD      │
   │       ├──< PAYMENT_ATTEMPT                   │                  │
   │       └── 1 TICKET_QR                         └───────┬──────────┘
   │                                                       │
   └──< RIDE_PARTICIPANT >── RIDE ─────────────────────────┘
                                  └──< RIDE_MESSAGE >── ACCOUNT'''
    p=d.add_paragraph(); r=p.add_run(erd); r.font.name='Consolas'; r.font.size=Pt(8)
    h(d,'7. Normalization')
    para(d,'The target operational schema is designed to Third Normal Form (3NF). Each relation stores one kind of fact; attributes are atomic; primary keys identify rows; non-key attributes depend on the whole key and not on another non-key attribute. For example, movie metadata belongs to MOVIE instead of being copied into every show, while screen and theatre details are stored once and linked by foreign keys. Theatre owner access is a junction relation rather than comma-separated owner IDs. Booking-seat rows store the seat and fare snapshot used for that transaction.')
    para(d,'Why stop at 3NF for the transactional core: it removes the common insert/update/delete anomalies while keeping joins, constraints and transaction behavior understandable for a small booking system. Moving every relation to BCNF or 4NF can add junction tables and join cost without a demonstrated dependency or multivalued-fact problem. Normalize based on actual functional dependencies, not a higher normal-form label alone.')
    para(d,'How to improve further: inspect real functional dependencies; decompose any relation whose determinant is not a candidate key (BCNF); isolate independent multivalued facts such as film languages or theatre amenities when they are stored as repeating values (4NF); use migrations and constraints to enforce the decompositions; and retain carefully selected read models/materialized views for performance. Denormalized reporting copies must remain derived, refreshable and outside the source-of-truth transaction model.')
    h(d,'8. Security, safety and operational considerations')
    bullets(d,['Use provider-hosted payment pages or tokenization; never store card/UPI credentials. Verify webhook signatures, amount, currency and idempotency.','Protect OTP with rate limits, expiry, attempt limits, abuse monitoring and secure account recovery. Keep provider secrets out of source control.','Use opaque QR credentials and one-time atomic redemption. Do not place personal details in QR content.','Enforce owner theatre scoping at the API and database query level, not only in the UI.','Provide ride report/block tools, moderation logs and limited disclosure of contact details. Clearly state the platform is coordinating peer-to-peer cost sharing, not dispatching a taxi.','Monitor payment settlement versus booking state; ticket message delivery retries; seat hold expiry; WebSocket fanout; and QR validation failures.'])
    h(d,'9. Current implementation status and next work')
    table(d,['Available now','Required before real customer bookings'],[
      ['Clickable React prototype; theme switch; browsing and sample Jaipur venues; date/showtime selection; visual seat state and seeded price tiers; checkout method selection; OTP form; ride post form; owner-facing demo; Java domain entities and integration interfaces.','REST controllers and application services; PostgreSQL migrations; complete account/owner authorization; live seat inventory and seat holds; provider adapters; signed webhooks and refunds; QR generation/delivery/scanning; durable outbox; chat; audit/monitoring; deployment and support processes.']
    ])
    para(d,'Implementation sequence: (1) agree on venue inventory and policies, (2) complete database migrations and identity, (3) build transactional seat holds plus concurrency handling, (4) integrate payment sandbox and idempotent webhook flow, (5) produce QR and delivery outbox, (6) add owner catalog tools and ride chat, (7) run security, concurrency, accessibility and pilot operations review.')
    d.save(OUT/'Picturehouse_Jaipur_Project_Report.docx')

def build_erd_svg():
    svg='''<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="900" viewBox="0 0 1200 900"><rect width="1200" height="900" fill="#f8f6f1"/><text x="55" y="58" font-family="Arial" font-size="26" font-weight="700" fill="#292624">Picturehouse Jaipur · Logical ER diagram</text><text x="55" y="86" font-family="Arial" font-size="13" fill="#89837d">Core booking, venue and ride-share entities · Jaipur MVP</text>'''
    boxes=[('ACCOUNT',['PK account_id','email · mobile · role'],70,150),('THEATRE_MEMBER',['PK account_id · theatre_id','owner role'],350,150),('THEATRE',['PK theatre_id','name · Jaipur area · address'],650,150),('SCREEN',['PK screen_id','FK theatre_id · seat layout'],930,150),('MOVIE',['PK movie_id','title · language · certification'],70,380),('SHOWTIME',['PK showtime_id','FK movie_id · screen_id · starts_at'],350,380),('SEAT',['PK seat_id','screen_id · label · category'],650,380),('SEAT_HOLD',['PK hold_id','showtime_id · seat_id · expires_at'],930,380),('BOOKING',['PK booking_id','customer · showtime · status · total'],70,620),('BOOKING_SEAT',['booking_id · seat_id','price snapshot'],350,620),('PAYMENT_ATTEMPT',['PK payment_id','booking · provider · status'],650,620),('TICKET_QR',['PK ticket_id','booking · token hash · redeemed'],930,620),('RIDE',['PK ride_id','driver · showtime · pickup · fare'],350,790),('RIDE_PARTICIPANT',['PK ride_id · account_id','request state'],650,790),('RIDE_MESSAGE',['PK message_id','ride · sender · timestamp'],930,790)]
    for title,lines,x,y in boxes:
        svg+=f'<rect x="{x}" y="{y}" width="205" height="94" rx="8" fill="#ffffff" stroke="#d8d1c7"/><path d="M{x+8} {y}h189a8 8 0 0 1 8 8v23h-205v-23a8 8 0 0 1 8-8" fill="#355b4b"/><text x="{x+12}" y="{y+22}" font-family="Arial" font-size="13" font-weight="700" fill="#fff">{title}</text>'
        for i,line in enumerate(lines): svg+=f'<text x="{x+12}" y="{y+49+i*19}" font-family="Arial" font-size="11" fill="#4b4742">{line}</text>'
    edges=[((275,197),(350,197),'1..*'),((555,197),(650,197),'*..1'),((855,197),(930,197),'1..*'),((1032,244),(1032,380),'1..*'),((275,427),(350,427),'1..*'),((555,427),(650,427),'1..*'),((855,427),(930,427),'1..*'),((452,244),(452,380),'1..*'),((452,474),(452,620),'1..*'),((275,667),(350,667),'1..*'),((555,667),(650,667),'1..*'),((855,667),(930,667),'1..1'),((170,244),(170,620),'1..*'),((452,474),(452,790),'1..*'),((555,837),(650,837),'1..*'),((855,837),(930,837),'1..*')]
    for (x1,y1),(x2,y2),label in edges:
        mx=(x1+x2)/2; my=(y1+y2)/2
        svg+=f'<path d="M{x1} {y1} L{x2} {y2}" stroke="#bd7454" stroke-width="1.5" fill="none"/><text x="{mx+3}" y="{my-4}" font-family="Arial" font-size="9" fill="#bd7454">{label}</text>'
    svg+='</svg>'
    (OUT/'Picturehouse_Jaipur_ER_Diagram.svg').write_text(svg,encoding='utf-8')

build_prd(); build_srs(); build_report(); build_erd_svg()
print('Created 3 DOCX documents and the ER diagram SVG in', OUT)
