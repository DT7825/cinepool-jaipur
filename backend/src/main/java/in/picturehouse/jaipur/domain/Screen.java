package in.picturehouse.jaipur.domain;

import jakarta.persistence.*;

@Entity @Table(name = "screens", uniqueConstraints=@UniqueConstraint(columnNames={"theatre_id","name"}))
public class Screen {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="theatre_id") private Theatre theatre;
    private String name;
    private int seatCount;
    protected Screen() {}
    public Screen(Theatre theatre,String name,int seatCount){this.theatre=theatre;this.name=name;this.seatCount=seatCount;}
    public Long getId(){return id;} public Theatre getTheatre(){return theatre;} public String getName(){return name;} public int getSeatCount(){return seatCount;}
}
