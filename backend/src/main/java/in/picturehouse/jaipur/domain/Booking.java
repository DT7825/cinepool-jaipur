package in.picturehouse.jaipur.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="bookings", uniqueConstraints=@UniqueConstraint(columnNames="public_reference"))
public class Booking {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="public_reference", nullable=false) private String publicReference=UUID.randomUUID().toString();
    @ManyToOne(optional=false, fetch=FetchType.LAZY) private Account customer;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) private Showtime showtime;
    @Enumerated(EnumType.STRING) private BookingStatus status=BookingStatus.PENDING_PAYMENT;
    private int totalPaise;
    private Instant createdAt=Instant.now();
    @SuppressWarnings("unused") // JPA persists this legacy field through reflection.
    private String entryTokenHash;
    protected Booking() {}
    public Booking(Account customer,Showtime showtime,int totalPaise){this.customer=customer;this.showtime=showtime;this.totalPaise=totalPaise;}
    public Long getId(){return id;} public String getPublicReference(){return publicReference;} public Account getCustomer(){return customer;} public Showtime getShowtime(){return showtime;} public BookingStatus getStatus(){return status;} public int getTotalPaise(){return totalPaise;} public Instant getCreatedAt(){return createdAt;}
    public void confirm(String tokenHash){if(status!=BookingStatus.PENDING_PAYMENT)throw new IllegalStateException("Only pending bookings can be confirmed");status=BookingStatus.CONFIRMED;entryTokenHash=tokenHash;}
    public void cancel(){if(status==BookingStatus.CONFIRMED||status==BookingStatus.PENDING_PAYMENT)status=BookingStatus.CANCELLED;}
}
