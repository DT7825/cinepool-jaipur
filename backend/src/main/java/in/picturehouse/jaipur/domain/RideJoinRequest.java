package in.picturehouse.jaipur.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="ride_join_requests",uniqueConstraints=@UniqueConstraint(columnNames={"ride_post_id","requester_clerk_user_id"}))
public class RideJoinRequest {
    public enum Status { PENDING, ACCEPTED, DECLINED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="ride_post_id") private RidePost ride;
    @Column(name="requester_clerk_user_id",nullable=false) private String requesterClerkUserId;
    @Column(nullable=false) private String requesterDisplayName;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status=Status.PENDING;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
    protected RideJoinRequest(){}
    public RideJoinRequest(RidePost ride,String userId,String name){this.ride=ride;this.requesterClerkUserId=userId;this.requesterDisplayName=name;}
    public Long getId(){return id;} public RidePost getRide(){return ride;} public String getRequesterClerkUserId(){return requesterClerkUserId;}
    public String getRequesterDisplayName(){return requesterDisplayName;} public Status getStatus(){return status;} public LocalDateTime getCreatedAt(){return createdAt;}
    public void accept(){status=Status.ACCEPTED;} public void decline(){status=Status.DECLINED;}
}
