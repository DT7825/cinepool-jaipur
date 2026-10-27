package in.picturehouse.jaipur.config;

import in.picturehouse.jaipur.domain.Movie;
import in.picturehouse.jaipur.domain.Screen;
import in.picturehouse.jaipur.domain.Showtime;
import in.picturehouse.jaipur.domain.ShowtimeSeat;
import in.picturehouse.jaipur.domain.Theatre;
import in.picturehouse.jaipur.repository.MovieRepository;
import in.picturehouse.jaipur.repository.ScreenRepository;
import in.picturehouse.jaipur.repository.ShowtimeRepository;
import in.picturehouse.jaipur.repository.ShowtimeSeatRepository;
import in.picturehouse.jaipur.repository.TheatreRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod")
public class DevelopmentCatalogSeeder {
    private static final Set<String> BOOKED_DEMO_SEATS = Set.of("A2", "A3", "B6", "C1", "C8", "D4", "E2", "E3", "F6");

    @Bean
    CommandLineRunner seedDevelopmentCatalog(MovieRepository movies, TheatreRepository theatres,
            ScreenRepository screens, ShowtimeRepository showtimes, ShowtimeSeatRepository seats) {
        return args -> {
            if (movies.count() > 0 || theatres.count() > 0) return;

            List<Movie> movieRecords = movies.saveAll(List.of(
                    new Movie("The Last Maharaja", "Hindi", "Drama", "UA 13+", 138,
                            "A fictional Jaipur-set drama for the development catalog.", "photo-1489599849927-2ee91cede3ba", "amber", "TRENDING", "8.6"),
                    new Movie("Dhoom Dhaam Jaipur", "Hindi", "Comedy", "U", 124,
                            "A fictional comedy used as development catalog data.", "photo-1440404653325-ab127d49abc1", "rose", "NEW RELEASE", "8.1"),
                    new Movie("The Silent Fort", "Hindi", "Thriller", "UA 16+", 131,
                            "A fictional thriller used as development catalog data.", "photo-1517604931442-7e0c8ed2963c", "blue", "CRITICS PICK", "9.0"),
                    new Movie("Chai & Chandni", "Hindi", "Romance", "U", 116,
                            "A fictional romance used as development catalog data.", "photo-1485846234645-a62644f84728", "violet", "COMING SOON", "8.4")
            ));

            List<TheatreSeed> theatreSeeds = List.of(
                    new TheatreSeed("INOX Jaipur Central", "C Scheme · 2.4 km", "C Scheme, Jaipur", "Dolby Atmos", "Parking available", List.of("10:30 AM", "1:45 PM", "6:20 PM", "9:40 PM")),
                    new TheatreSeed("Raj Mandir Cinema", "C Scheme · 3.1 km", "C Scheme, Jaipur", "Luxury heritage", "Valet parking", List.of("11:15 AM", "2:30 PM", "7:00 PM")),
                    new TheatreSeed("PVR Vaishali Nagar", "Vaishali Nagar · 7.8 km", "Vaishali Nagar, Jaipur", "IMAX · 4K", "Parking available", List.of("12:00 PM", "4:10 PM", "8:25 PM"))
            );

            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
            LocalDate firstShowDate = now.toLocalDate();
            for (TheatreSeed theatreSeed : theatreSeeds) {
                Theatre theatre = theatres.save(new Theatre(theatreSeed.name(), theatreSeed.area(),
                        theatreSeed.address(), theatreSeed.type(), theatreSeed.parking()));
                Screen screen = screens.save(new Screen(theatre, "Screen 1", 48));
                for (int day = 0; day < 8; day++) {
                    LocalDate date = firstShowDate.plusDays(day);
                    for (Movie movie : movieRecords) {
                        for (String time : theatreSeed.times()) {
                            LocalDateTime startsAt = LocalDateTime.of(date, java.time.LocalTime.parse(time,
                                    java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH)));
                            if (!startsAt.isAfter(now)) continue;
                            Showtime showtime = showtimes.save(new Showtime(movie, screen, startsAt, "2D · " + movie.getLanguage()));
                            var inventory = new java.util.ArrayList<ShowtimeSeat>();
                            for (char row = 'A'; row <= 'F'; row++) {
                                for (int col = 1; col <= 8; col++) {
                                    String seatCode = "" + row + col;
                                    inventory.add(new ShowtimeSeat(showtime, seatCode, BOOKED_DEMO_SEATS.contains(seatCode)));
                                }
                            }
                            seats.saveAll(inventory);
                        }
                    }
                }
            }
        };
    }

    private record TheatreSeed(String name, String area, String address, String type, String parking, List<String> times) {}
}
