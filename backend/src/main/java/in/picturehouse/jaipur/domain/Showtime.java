package in.picturehouse.jaipur.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="showtimes", indexes=@Index(columnList="starts_at"))
public class Showtime {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) private Movie movie;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) private Screen screen;
    private LocalDateTime startsAt;
    private String format;
    private boolean onSale=true;
    @Version private long version;
    protected Showtime() {}
    public Showtime(Movie movie, Screen screen, LocalDateTime startsAt, String format){this.movie=movie;this.screen=screen;this.startsAt=startsAt;this.format=format;}
    public Long getId(){return id;} public Movie getMovie(){return movie;} public Screen getScreen(){return screen;} public LocalDateTime getStartsAt(){return startsAt;} public String getFormat(){return format;} public boolean isOnSale(){return onSale;} public long getVersion(){return version;}
    public void reschedule(LocalDateTime startsAt,String format){this.startsAt=startsAt;this.format=format;}
}
