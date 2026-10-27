package in.picturehouse.jaipur.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ride_posts", indexes = @Index(columnList = "ride_date, active"))
public class RidePost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String clerkUserId;
    @Column(nullable = false) private String displayName;
    @Column(nullable = false) private LocalDate rideDate;
    @Column(nullable = false) private String pickupArea;
    @Column(nullable = false) private String theatreName;
    @Column(nullable = false) private String movieTitle;
    @Column(nullable = false) private String showTime;
    @Column(nullable = false) private LocalDateTime departureAt;
    @Column(nullable = false) private int availableSeats;
    @Column(nullable = false) private int contributionPaise;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Version private long version;
    protected RidePost() {}
    public RidePost(String clerkUserId, String displayName, LocalDate rideDate, String pickupArea,
            String theatreName, String movieTitle, String showTime, LocalDateTime departureAt,
            int availableSeats, int contributionPaise) {
        this.clerkUserId=clerkUserId; this.displayName=displayName; this.rideDate=rideDate;
        this.pickupArea=pickupArea; this.theatreName=theatreName; this.movieTitle=movieTitle;
        this.showTime=showTime; this.departureAt=departureAt; this.availableSeats=availableSeats;
        this.contributionPaise=contributionPaise;
    }
    public Long getId(){return id;} public String getClerkUserId(){return clerkUserId;} public String getDisplayName(){return displayName;}
    public LocalDate getRideDate(){return rideDate;} public String getPickupArea(){return pickupArea;} public String getTheatreName(){return theatreName;}
    public String getMovieTitle(){return movieTitle;} public String getShowTime(){return showTime;} public LocalDateTime getDepartureAt(){return departureAt;}
    public int getAvailableSeats(){return availableSeats;} public int getContributionPaise(){return contributionPaise;} public boolean isActive(){return active;}
    public void reserveSeat(){if(availableSeats<1)throw new IllegalStateException("No ride seats remain.");availableSeats--;if(availableSeats==0)active=false;}
}
