package in.picturehouse.jaipur.api;

import in.picturehouse.jaipur.domain.*;
import in.picturehouse.jaipur.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/owner")
public class OwnerPortalController {
    private final TheatreOwnerMembershipRepository memberships;
    private final TheatreRepository theatres;
    private final ShowtimeRepository showtimes;
    private final TicketBookingRepository bookings;
    private final MovieRepository movies;
    private final ScreenRepository screens;
    private final ShowtimeSeatRepository seatInventory;
    private final TheatreMovieRepository theatreMovies;
    private final Set<String> adminIds;

    public OwnerPortalController(TheatreOwnerMembershipRepository memberships, TheatreRepository theatres,
            ShowtimeRepository showtimes, TicketBookingRepository bookings,MovieRepository movies,
            ScreenRepository screens,ShowtimeSeatRepository seatInventory,TheatreMovieRepository theatreMovies,
            @Value("${app.admin.clerk-user-ids:}") String adminIds) {
        this.memberships=memberships; this.theatres=theatres; this.showtimes=showtimes; this.bookings=bookings;
        this.movies=movies;this.screens=screens;this.seatInventory=seatInventory;this.theatreMovies=theatreMovies;
        this.adminIds=Arrays.stream(adminIds.split(",")).map(String::trim).filter(s -> !s.isBlank()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @GetMapping("/access")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AccessResponse access(@AuthenticationPrincipal Jwt jwt) {
        return new AccessResponse(memberships.findByClerkUserIdOrderByRequestedAtDesc(jwt.getSubject()).stream()
                .map(m -> new MembershipResponse(m.getId(), m.getClerkUserId(), m.getTheatre().getId(), m.getTheatre().getName(), m.isApproved())).toList());
    }

    @PostMapping("/access-requests")
    @org.springframework.transaction.annotation.Transactional
    public MembershipResponse requestAccess(@Valid @RequestBody AccessRequest request, @AuthenticationPrincipal Jwt jwt) {
        Theatre theatre=theatres.findById(request.theatreId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Theatre not found."));
        TheatreOwnerMembership membership=memberships.findByClerkUserIdAndTheatreId(jwt.getSubject(), theatre.getId()).orElseGet(() -> memberships.save(new TheatreOwnerMembership(jwt.getSubject(), theatre)));
        return new MembershipResponse(membership.getId(), membership.getClerkUserId(), theatre.getId(), theatre.getName(), membership.isApproved());
    }

    @GetMapping("/dashboard")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public OwnerDashboard dashboard(@AuthenticationPrincipal Jwt jwt) {
        List<TheatreOwnerMembership> approved=memberships.findByClerkUserIdOrderByRequestedAtDesc(jwt.getSubject()).stream().filter(TheatreOwnerMembership::isApproved).toList();
        if (approved.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No theatre access has been approved for this account yet.");
        List<Long> theatreIds=approved.stream().map(m -> m.getTheatre().getId()).toList();
        List<Showtime> shows=showtimes.findByScreen_Theatre_IdInAndOnSaleTrueOrderByStartsAtAsc(theatreIds);
        Map<Long,List<TicketBooking>> byShow=shows.isEmpty()?Map.of():bookings.findByShowtimeIdIn(shows.stream().map(Showtime::getId).toList()).stream().collect(java.util.stream.Collectors.groupingBy(TicketBooking::getShowtimeId));
        List<ShowSales> sales=shows.stream().map(s -> {
            List<TicketBooking> sold=byShow.getOrDefault(s.getId(),List.of());
            return new ShowSales(s.getId(),s.getMovie().getTitle(),s.getScreen().getTheatre().getName(),s.getScreen().getName(),s.getStartsAt().toString(),sold.size(),sold.stream().mapToLong(TicketBooking::getAmountPaise).sum());
        }).toList();
        return new OwnerDashboard(approved.stream().map(m -> new TheatreSummary(m.getTheatre().getId(),m.getTheatre().getName())).toList(), sales,
                sales.stream().mapToLong(ShowSales::bookings).sum(), sales.stream().mapToLong(ShowSales::salesPaise).sum());
    }

    @GetMapping("/manage-options")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ManageOptions manageOptions(@AuthenticationPrincipal Jwt jwt){
        List<Long> theatreIds=approvedTheatreIds(jwt.getSubject());
        Map<Long,Movie> available=new LinkedHashMap<>();
        theatreMovies.findByTheatre_IdIn(theatreIds).forEach(link->available.put(link.getMovie().getId(),link.getMovie()));
        showtimes.findByScreen_Theatre_IdInAndOnSaleTrueOrderByStartsAtAsc(theatreIds).forEach(show->available.put(show.getMovie().getId(),show.getMovie()));
        List<MovieSummary> movieRows=available.values().stream().filter(Movie::isActive).map(m->new MovieSummary(m.getId(),m.getTitle(),m.getLanguage(),m.getGenre(),m.getCertification(),m.getDurationMinutes(),m.getSynopsis())).toList();
        List<ScreenSummary> screenRows=screens.findByTheatre_IdIn(theatreIds).stream().map(s->new ScreenSummary(s.getId(),s.getName(),s.getTheatre().getId(),s.getTheatre().getName(),s.getSeatCount())).toList();
        List<TheatreSummary> theatreRows=memberships.findByClerkUserIdOrderByRequestedAtDesc(jwt.getSubject()).stream().filter(TheatreOwnerMembership::isApproved).map(m->new TheatreSummary(m.getTheatre().getId(),m.getTheatre().getName())).toList();
        return new ManageOptions(theatreRows,movieRows,screenRows);
    }

    @PostMapping("/theatres/{theatreId}/movies")
    @org.springframework.transaction.annotation.Transactional
    public MovieSummary addMovie(@PathVariable Long theatreId,@Valid @RequestBody MovieRequest request,@AuthenticationPrincipal Jwt jwt){
        requireTheatreAccess(jwt.getSubject(),theatreId);
        Movie movie=movies.findByTitleIgnoreCase(request.title().trim()).orElseGet(()->movies.save(new Movie(request.title().trim(),request.language().trim(),request.genre().trim(),request.certification().trim(),request.durationMinutes(),request.synopsis().trim())));
        if(theatreMovies.findByTheatre_IdAndMovie_Id(theatreId,movie.getId()).isEmpty())theatreMovies.save(new TheatreMovie(theatres.findById(theatreId).orElseThrow(),movie));
        return movieSummary(movie);
    }

    @PutMapping("/theatres/{theatreId}/movies/{movieId}")
    @org.springframework.transaction.annotation.Transactional
    public MovieSummary updateMovie(@PathVariable Long theatreId,@PathVariable Long movieId,@Valid @RequestBody MovieRequest request,@AuthenticationPrincipal Jwt jwt){
        requireTheatreAccess(jwt.getSubject(),theatreId);
        TheatreMovie assignment=theatreMovies.findByTheatre_IdAndMovie_Id(theatreId,movieId).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"This movie is not assigned to your theatre."));
        List<Showtime> movieShows=showtimes.findByMovie_Id(movieId);
        if(theatreMovies.countByMovie_Id(movieId)>1||movieShows.stream().anyMatch(s->!s.getScreen().getTheatre().getId().equals(theatreId)))throw new ResponseStatusException(HttpStatus.CONFLICT,"This movie is shared with another theatre; contact the catalogue administrator to edit shared details.");
        Movie movie=assignment.getMovie();movie.updateDetails(request.title().trim(),request.language().trim(),request.genre().trim(),request.certification().trim(),request.durationMinutes(),request.synopsis().trim());
        return movieSummary(movie);
    }

    @PostMapping("/theatres/{theatreId}/showtimes")
    @org.springframework.transaction.annotation.Transactional
    public ShowSales addShowtime(@PathVariable Long theatreId,@Valid @RequestBody ShowtimeRequest request,@AuthenticationPrincipal Jwt jwt){
        requireTheatreAccess(jwt.getSubject(),theatreId);
        Screen screen=screens.findById(request.screenId()).filter(s->s.getTheatre().getId().equals(theatreId)).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Choose a screen in a theatre assigned to your account."));
        Movie movie=movies.findById(request.movieId()).filter(Movie::isActive).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Movie not found."));
        if(!request.startsAt().isAfter(LocalDateTime.now())||request.startsAt().toLocalDate().isAfter(LocalDate.now().plusDays(90)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Showtime must be in the future and within 90 days.");
        if(theatreMovies.findByTheatre_IdAndMovie_Id(theatreId,movie.getId()).isEmpty())theatreMovies.save(new TheatreMovie(screen.getTheatre(),movie));
        Showtime show=showtimes.save(new Showtime(movie,screen,request.startsAt(),request.format().trim()));
        List<ShowtimeSeat> inventory=new ArrayList<>();for(char row='A';row<='F';row++)for(int col=1;col<=8;col++)inventory.add(new ShowtimeSeat(show,""+row+col,false));
        seatInventory.saveAll(inventory);
        return showSales(show,0,0);
    }

    @PutMapping("/showtimes/{showtimeId}")
    @org.springframework.transaction.annotation.Transactional
    public ShowSales updateShowtime(@PathVariable Long showtimeId,@Valid @RequestBody ShowtimeRequest request,@AuthenticationPrincipal Jwt jwt){
        Showtime show=showtimes.findById(showtimeId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Showtime not found."));
        Long theatreId=show.getScreen().getTheatre().getId();requireTheatreAccess(jwt.getSubject(),theatreId);
        Screen screen=screens.findById(request.screenId()).filter(s->s.getTheatre().getId().equals(theatreId)).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Choose a screen in the same assigned theatre."));
        if(!request.startsAt().isAfter(LocalDateTime.now()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Showtime must be in the future.");
        show.reschedule(request.startsAt(),request.format().trim());
        return showSales(show,0,0);
    }

    private List<Long> approvedTheatreIds(String userId){List<Long> ids=memberships.findByClerkUserIdOrderByRequestedAtDesc(userId).stream().filter(TheatreOwnerMembership::isApproved).map(m->m.getTheatre().getId()).toList();if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No theatre access has been approved for this account yet.");return ids;}
    private void requireTheatreAccess(String userId,Long theatreId){if(memberships.findByClerkUserIdAndTheatreId(userId,theatreId).filter(TheatreOwnerMembership::isApproved).isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This theatre is not assigned to your account.");}
    private static MovieSummary movieSummary(Movie m){return new MovieSummary(m.getId(),m.getTitle(),m.getLanguage(),m.getGenre(),m.getCertification(),m.getDurationMinutes(),m.getSynopsis());}
    private static ShowSales showSales(Showtime s,long bookings,long amount){return new ShowSales(s.getId(),s.getMovie().getTitle(),s.getScreen().getTheatre().getName(),s.getScreen().getName(),s.getStartsAt().toString(),bookings,amount);}

    @GetMapping("/admin/requests")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<MembershipResponse> pending(@AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        return memberships.findByApprovedFalseOrderByRequestedAtAsc().stream().map(m -> new MembershipResponse(m.getId(),m.getClerkUserId(),m.getTheatre().getId(),m.getTheatre().getName(),false)).toList();
    }

    @PutMapping("/admin/requests/{id}/approve")
    @org.springframework.transaction.annotation.Transactional
    public MembershipResponse approve(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        TheatreOwnerMembership membership=memberships.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Access request not found."));
        membership.approve();
        return new MembershipResponse(membership.getId(),membership.getClerkUserId(),membership.getTheatre().getId(),membership.getTheatre().getName(),true);
    }
    private void requireAdmin(Jwt jwt) { if (!adminIds.contains(jwt.getSubject())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Administrator access is required."); }
    public record AccessRequest(@NotNull Long theatreId) {}
    public record MovieRequest(@NotBlank @Size(max=120) String title,@NotBlank @Size(max=50) String language,@NotBlank @Size(max=80) String genre,@NotBlank @Size(max=30) String certification,@Min(1) int durationMinutes,@NotBlank @Size(max=2000) String synopsis){}
    public record ShowtimeRequest(@NotNull Long screenId,@NotNull Long movieId,@NotNull LocalDateTime startsAt,@NotBlank @Size(max=40) String format){}
    public record MembershipResponse(Long membershipId,String clerkUserId,Long theatreId,String theatreName,boolean approved) {}
    public record AccessResponse(List<MembershipResponse> memberships) {}
    public record TheatreSummary(Long id,String name) {}
    public record ShowSales(Long showtimeId,String movieTitle,String theatreName,String screenName,String startsAt,long bookings,long salesPaise) {}
    public record OwnerDashboard(List<TheatreSummary> theatres,List<ShowSales> shows,long bookingCount,long salesPaise) {}
    public record MovieSummary(Long id,String title,String language,String genre,String certification,int durationMinutes,String synopsis){}
    public record ScreenSummary(Long id,String name,Long theatreId,String theatreName,int seatCount){}
    public record ManageOptions(List<TheatreSummary> theatres,List<MovieSummary> movies,List<ScreenSummary> screens){}
}
