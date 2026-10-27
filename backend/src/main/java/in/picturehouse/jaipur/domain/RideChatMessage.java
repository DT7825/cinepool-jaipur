package in.picturehouse.jaipur.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="ride_chat_messages",indexes=@Index(columnList="join_request_id, created_at"))
public class RideChatMessage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="join_request_id") private RideJoinRequest request;
    @Column(nullable=false) private String senderClerkUserId;
    @Column(nullable=false) private String senderDisplayName;
    @Column(nullable=false,length=1200) private String body;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
    protected RideChatMessage(){}
    public RideChatMessage(RideJoinRequest request,String senderId,String senderName,String body){this.request=request;this.senderClerkUserId=senderId;this.senderDisplayName=senderName;this.body=body;}
    public Long getId(){return id;} public RideJoinRequest getRequest(){return request;} public String getSenderClerkUserId(){return senderClerkUserId;}
    public String getSenderDisplayName(){return senderDisplayName;} public String getBody(){return body;} public LocalDateTime getCreatedAt(){return createdAt;}
}
