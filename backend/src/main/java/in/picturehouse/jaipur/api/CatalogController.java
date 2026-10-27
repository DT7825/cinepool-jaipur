package in.picturehouse.jaipur.api;

import in.picturehouse.jaipur.domain.Movie;
import in.picturehouse.jaipur.domain.Showtime;
import in.picturehouse.jaipur.domain.ShowtimeSeat;
import in.picturehouse.jaipur.domain.Theatre;
import in.picturehouse.jaipur.repository.MovieRepository;
import in.picturehouse.jaipur.repository.ShowtimeRepository;
import in.picturehouse.jaipur.repository.ShowtimeSeatRepository;
import in.picturehouse.jaipur.repository.TheatreRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/catalog")
public class CatalogController {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");
    private final MovieRepository movies;
    private final TheatreRepository theatres;
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository seats;

    public CatalogController(MovieRepository movies, TheatreRepository theatres,
            ShowtimeRepository showtimes, ShowtimeSeatRepository seats) {
        this.movies = movies;
        this.theatres = theatres;
        this.showtimes = showtimes;
        this.seats = seats;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public CatalogResponse getCatalog() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        LocalDate today = now.toLocalDate();
        List<Showtime> availableShows = showtimes
                .findByOnSaleTrueAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        now, today.plusDays(8).atStartOfDay());
        Map<Long, List<ShowtimeListing>> showsByTheatre = availableShows.stream()
                .map(CatalogController::toShowtimeListing)
                .collect(Collectors.groupingBy(ShowtimeListing::theatreId));
        List<MovieListing> movieListings = movies.findByActiveTrueOrderByTitleAsc().stream()
                .map(CatalogController::toMovieListing).toList();
        List<TheatreListing> theatreListings = theatres.findByActiveTrueAndCityIgnoreCaseOrderByNameAsc("Jaipur").stream()
                .map(theatre -> new TheatreListing(theatre.getId(), theatre.getName(), theatre.getArea(),
                        theatre.getTheatreType(), theatre.getParkingInfo(),
                        showsByTheatre.getOrDefault(theatre.getId(), List.of())))
                .filter(theatre -> !theatre.showtimes().isEmpty()).toList();
        return new CatalogResponse(movieListings, theatreListings,
                "Development catalog data: confirm real movie, theatre, showtime, and seat details with each theatre before accepting bookings.");
    }

    @GetMapping("/showtimes/{showtimeId}/seats")
    @Transactional(readOnly = true)
    public List<SeatListing> getSeats(@PathVariable Long showtimeId) {
        if (!showtimes.existsById(showtimeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Showtime was not found.");
        }
        return seats.findByShowtimeIdOrderBySeatCodeAsc(showtimeId).stream()
                .map(seat -> new SeatListing(seat.getSeatCode(), seat.isBooked())).toList();
    }

    private static MovieListing toMovieListing(Movie movie) {
        return new MovieListing(movie.getId(), movie.getTitle(), movie.getGenre() + " · " + movie.getLanguage(), movie.getCertification(),
                movie.getDurationMinutes() / 60 + "h " + String.format("%02dm", movie.getDurationMinutes() % 60),
                movie.getPosterImage(), movie.getTone(), movie.getTag(), movie.getScore());
    }

    private static ShowtimeListing toShowtimeListing(Showtime showtime) {
        LocalDateTime startsAt = showtime.getStartsAt();
        Theatre theatre = showtime.getScreen().getTheatre();
        return new ShowtimeListing(showtime.getId(), showtime.getMovie().getId(), showtime.getMovie().getTitle(),
                startsAt.toLocalDate().toString(), startsAt.format(TIME_FORMAT), showtime.getFormat(),
                showtime.getScreen().getName(), theatre.getId());
    }

    public record CatalogResponse(List<MovieListing> movies, List<TheatreListing> theatres, String dataNotice) {}
    public record MovieListing(Long id, String title, String genre, String rating, String length,
            String image, String tone, String tag, String score) {}
    public record TheatreListing(Long id, String name, String area, String type, String parking,
            List<ShowtimeListing> showtimes) {}
    public record ShowtimeListing(Long id, Long movieId, String movieTitle, String date, String time,
            String format, String screen, Long theatreId) {}
    public record SeatListing(String id, boolean booked) {}
}
