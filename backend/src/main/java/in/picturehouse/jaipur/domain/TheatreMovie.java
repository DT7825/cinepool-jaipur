package in.picturehouse.jaipur.domain;

import jakarta.persistence.*;

@Entity
@Table(name="theatre_movies",uniqueConstraints=@UniqueConstraint(columnNames={"theatre_id","movie_id"}))
public class TheatreMovie {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="theatre_id") private Theatre theatre;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="movie_id") private Movie movie;
    protected TheatreMovie(){}
    public TheatreMovie(Theatre theatre,Movie movie){this.theatre=theatre;this.movie=movie;}
    public Long getId(){return id;} public Theatre getTheatre(){return theatre;} public Movie getMovie(){return movie;}
}
