package in.picturehouse.jaipur.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="carpool_rides")
public class CarpoolRide {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) private Account driver;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) private Showtime destinationShowtime;
    private String pickupArea;
    private String dropoffArea;
    private LocalDateTime departureAt;
    private int availableSeats;
    private int contributionPaise;
    private boolean active=true;
    protected CarpoolRide() {}
    public CarpoolRide(Account driver,Showtime showtime,String pickupArea,String dropoffArea,LocalDateTime departureAt,int availableSeats,int contributionPaise){this.driver=driver;this.destinationShowtime=showtime;this.pickupArea=pickupArea;this.dropoffArea=dropoffArea;this.departureAt=departureAt;this.availableSeats=availableSeats;this.contributionPaise=contributionPaise;}
    public Long getId(){return id;} public Account getDriver(){return driver;} public Showtime getDestinationShowtime(){return destinationShowtime;} public String getPickupArea(){return pickupArea;} public String getDropoffArea(){return dropoffArea;} public LocalDateTime getDepartureAt(){return departureAt;} public int getAvailableSeats(){return availableSeats;} public int getContributionPaise(){return contributionPaise;} public boolean isActive(){return active;}
    public void reserveSeat(){if(!active||availableSeats<1)throw new IllegalStateException("Ride is full");availableSeats--;}
}
