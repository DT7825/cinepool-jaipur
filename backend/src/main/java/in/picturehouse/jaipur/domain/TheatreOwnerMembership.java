package in.picturehouse.jaipur.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="theatre_owner_memberships", uniqueConstraints=@UniqueConstraint(columnNames={"clerk_user_id","theatre_id"}))
public class TheatreOwnerMembership {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="clerk_user_id", nullable=false) private String clerkUserId;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="theatre_id") private Theatre theatre;
    @Column(nullable=false) private boolean approved=false;
    @Column(nullable=false) private Instant requestedAt=Instant.now();
    protected TheatreOwnerMembership() {}
    public TheatreOwnerMembership(String clerkUserId, Theatre theatre) { this.clerkUserId=clerkUserId; this.theatre=theatre; }
    public Long getId(){return id;} public String getClerkUserId(){return clerkUserId;} public Theatre getTheatre(){return theatre;}
    public boolean isApproved(){return approved;} public Instant getRequestedAt(){return requestedAt;} public void approve(){approved=true;}
}
