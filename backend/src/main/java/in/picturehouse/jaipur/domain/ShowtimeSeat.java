package in.picturehouse.jaipur.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "showtime_seats", uniqueConstraints = @UniqueConstraint(columnNames = { "showtime_id", "seat_code" }))
public class ShowtimeSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @Column(name = "seat_code", nullable = false, length = 3)
    private String seatCode;

    @Column(nullable = false)
    private boolean booked;

    protected ShowtimeSeat() {}

    public ShowtimeSeat(Showtime showtime, String seatCode, boolean booked) {
        this.showtime = showtime;
        this.seatCode = seatCode;
        this.booked = booked;
    }

    public Long getId() { return id; }
    public Showtime getShowtime() { return showtime; }
    public String getSeatCode() { return seatCode; }
    public boolean isBooked() { return booked; }
    public void markBooked() { this.booked = true; }
}
