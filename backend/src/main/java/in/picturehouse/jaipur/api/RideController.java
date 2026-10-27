package in.picturehouse.jaipur.api;

import in.picturehouse.jaipur.domain.RidePost;
import in.picturehouse.jaipur.domain.RideJoinRequest;
import in.picturehouse.jaipur.domain.RideChatMessage;
import in.picturehouse.jaipur.repository.RidePostRepository;
import in.picturehouse.jaipur.repository.RideJoinRequestRepository;
import in.picturehouse.jaipur.repository.RideChatMessageRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rides")
public class RideController {
    private final RidePostRepository rides;
    private final RideJoinRequestRepository requests;
    private final RideChatMessageRepository messages;
    public RideController(RidePostRepository rides,RideJoinRequestRepository requests,RideChatMessageRepository messages) { this.rides=rides;this.requests=requests;this.messages=messages; }

    @GetMapping
    public List<RideResponse> list(@RequestParam LocalDate date,@AuthenticationPrincipal Jwt jwt) {
        String userId=jwt==null?null:jwt.getSubject();
        return rides.findByRideDateAndActiveTrueOrderByDepartureAtAsc(date).stream().map(ride->RideResponse.from(ride,userId)).toList();
    }

    @PostMapping
    public RideResponse post(@Valid @RequestBody RideRequest request, @AuthenticationPrincipal Jwt jwt) {
        if (request.rideDate().isBefore(LocalDate.now()) || request.rideDate().isAfter(LocalDate.now().plusDays(90)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a ride date within the next 90 days.");
        String displayName = request.displayName();
        if (displayName == null || displayName.isBlank()) displayName = jwt.getSubject();
        if(request.departureAt().toLocalDate().equals(request.rideDate())==false || !request.departureAt().isAfter(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Departure must be a future time on the selected ride date.");
        return RideResponse.from(rides.save(new RidePost(jwt.getSubject(), displayName, request.rideDate(),
                request.pickupArea().trim(), request.theatreName().trim(), request.movieTitle().trim(),
                request.showTime().trim(), request.departureAt(), request.availableSeats(), request.contributionRupees() * 100)),jwt.getSubject());
    }

    @PostMapping("/{rideId}/requests")
    @Transactional
    public ConversationResponse requestToJoin(@PathVariable Long rideId,@AuthenticationPrincipal Jwt jwt){
        RidePost ride=rides.findById(rideId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ride not found."));
        if(ride.getClerkUserId().equals(jwt.getSubject()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You cannot request your own ride.");
        if(!ride.isActive()||ride.getAvailableSeats()<1)throw new ResponseStatusException(HttpStatus.CONFLICT,"This ride has no open seats.");
        RideJoinRequest req=requests.findByRide_IdAndRequesterClerkUserId(rideId,jwt.getSubject()).orElseGet(()->requests.save(new RideJoinRequest(ride,jwt.getSubject(),displayName(jwt))));
        return ConversationResponse.from(req,jwt.getSubject());
    }

    @GetMapping("/conversations")
    @Transactional(readOnly=true)
    public List<ConversationResponse> conversations(@AuthenticationPrincipal Jwt jwt){
        List<RideJoinRequest> all=new ArrayList<>(requests.findByRequesterClerkUserIdOrderByCreatedAtDesc(jwt.getSubject()));
        all.addAll(requests.findByRide_ClerkUserIdOrderByCreatedAtDesc(jwt.getSubject()));
        return all.stream().distinct().sorted(Comparator.comparing(RideJoinRequest::getCreatedAt).reversed()).map(req->ConversationResponse.from(req,jwt.getSubject())).toList();
    }

    @GetMapping("/conversations/{requestId}/messages")
    @Transactional(readOnly=true)
    public List<MessageResponse> getMessages(@PathVariable Long requestId,@AuthenticationPrincipal Jwt jwt){
        RideJoinRequest req=participantRequest(requestId,jwt.getSubject());
        return messages.findByRequest_IdOrderByCreatedAtAsc(req.getId()).stream().map(m->new MessageResponse(m.getId(),m.getSenderClerkUserId().equals(jwt.getSubject()),m.getSenderDisplayName(),m.getBody(),m.getCreatedAt())).toList();
    }

    @PostMapping("/conversations/{requestId}/messages")
    @Transactional
    public MessageResponse sendMessage(@PathVariable Long requestId,@Valid @RequestBody MessageRequest body,@AuthenticationPrincipal Jwt jwt){
        RideJoinRequest req=participantRequest(requestId,jwt.getSubject());
        if(req.getStatus()==RideJoinRequest.Status.DECLINED)throw new ResponseStatusException(HttpStatus.CONFLICT,"This ride request was declined; chat is closed.");
        RideChatMessage m=messages.save(new RideChatMessage(req,jwt.getSubject(),displayName(jwt),body.message().trim()));
        return new MessageResponse(m.getId(),true,m.getSenderDisplayName(),m.getBody(),m.getCreatedAt());
    }

    @PutMapping("/conversations/{requestId}/accept")
    @Transactional
    public ConversationResponse accept(@PathVariable Long requestId,@AuthenticationPrincipal Jwt jwt){
        RideJoinRequest req=requests.findByIdForUpdate(requestId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ride request not found."));
        if(!req.getRide().getClerkUserId().equals(jwt.getSubject()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the ride owner can accept a passenger.");
        if(req.getStatus()!=RideJoinRequest.Status.PENDING)throw new ResponseStatusException(HttpStatus.CONFLICT,"This request has already been decided.");
        RidePost locked=rides.findByIdForUpdate(req.getRide().getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ride not found."));
        try{locked.reserveSeat();}catch(IllegalStateException ex){throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage());}
        req.accept();
        return ConversationResponse.from(req,jwt.getSubject());
    }

    @PutMapping("/conversations/{requestId}/decline")
    @Transactional
    public ConversationResponse decline(@PathVariable Long requestId,@AuthenticationPrincipal Jwt jwt){
        RideJoinRequest req=requests.findByIdForUpdate(requestId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ride request not found."));
        if(!req.getRide().getClerkUserId().equals(jwt.getSubject()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the ride owner can decline a passenger.");
        if(req.getStatus()!=RideJoinRequest.Status.PENDING)throw new ResponseStatusException(HttpStatus.CONFLICT,"This request has already been decided.");
        req.decline();return ConversationResponse.from(req,jwt.getSubject());
    }

    private RideJoinRequest participantRequest(Long requestId,String userId){
        RideJoinRequest req=requests.findById(requestId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Conversation not found."));
        if(!req.getRide().getClerkUserId().equals(userId)&&!req.getRequesterClerkUserId().equals(userId))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This conversation does not belong to your account.");
        return req;
    }
    private static String displayName(Jwt jwt){String name=jwt.getClaimAsString("name");return name==null||name.isBlank()?"Moviegoer":name;}

    public record RideRequest(@NotNull LocalDate rideDate, @Size(max=80) String displayName, @NotBlank @Size(max=100) String pickupArea,
            @NotBlank @Size(max=120) String theatreName, @NotBlank @Size(max=120) String movieTitle,
            @NotBlank @Size(max=40) String showTime, @NotNull LocalDateTime departureAt,
            @Min(1) @Max(6) int availableSeats, @Min(1) @Max(10000) int contributionRupees) {}
    public record RideResponse(Long id, String displayName, LocalDate rideDate, String pickupArea,
            String theatreName, String movieTitle, String showTime, LocalDateTime departureAt,
            int availableSeats, int contributionPaise,boolean mine) {
        static RideResponse from(RidePost r,String userId) { return new RideResponse(r.getId(), r.getDisplayName(), r.getRideDate(), r.getPickupArea(), r.getTheatreName(), r.getMovieTitle(), r.getShowTime(), r.getDepartureAt(), r.getAvailableSeats(), r.getContributionPaise(),userId!=null&&userId.equals(r.getClerkUserId())); }
    }
    public record MessageRequest(@NotBlank @Size(max=1200) String message){}
    public record MessageResponse(Long id,boolean mine,String senderName,String message,LocalDateTime createdAt){}
    public record ConversationResponse(Long requestId,Long rideId,String passengerName,String movieTitle,String theatreName,String showTime,LocalDate rideDate,String status,boolean owner,int contributionPaise,int availableSeats){
        static ConversationResponse from(RideJoinRequest r,String currentUser){RidePost ride=r.getRide();boolean owner=ride.getClerkUserId().equals(currentUser);return new ConversationResponse(r.getId(),ride.getId(),owner?r.getRequesterDisplayName():ride.getDisplayName(),ride.getMovieTitle(),ride.getTheatreName(),ride.getShowTime(),ride.getRideDate(),r.getStatus().name(),owner,ride.getContributionPaise(),ride.getAvailableSeats());}
    }
}
