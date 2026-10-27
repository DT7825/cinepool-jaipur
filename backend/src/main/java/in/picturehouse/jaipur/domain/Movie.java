package in.picturehouse.jaipur.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;

@Entity @Table(name = "movies")
public class Movie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank private String title;
    @NotBlank private String language;
    private String genre;
    private String certification;
    private int durationMinutes;
    private String synopsis;
    private String posterImage;
    private String tone;
    private String tag;
    private String score;
    private boolean active = true;
    protected Movie() {}
    public Movie(String title, String language, String genre, String certification, int durationMinutes, String synopsis) { this.title=title; this.language=language; this.genre=genre; this.certification=certification; this.durationMinutes=durationMinutes; this.synopsis=synopsis; }
    public Movie(String title, String language, String genre, String certification, int durationMinutes, String synopsis, String posterImage, String tone, String tag, String score) { this(title, language, genre, certification, durationMinutes, synopsis); this.posterImage=posterImage; this.tone=tone; this.tag=tag; this.score=score; }
    public Long getId(){return id;} public String getTitle(){return title;} public String getLanguage(){return language;} public String getGenre(){return genre;} public String getCertification(){return certification;} public int getDurationMinutes(){return durationMinutes;} public Duration getDuration(){return Duration.ofMinutes(durationMinutes);} public String getSynopsis(){return synopsis;} public String getPosterImage(){return posterImage;} public String getTone(){return tone;} public String getTag(){return tag;} public String getScore(){return score;} public boolean isActive(){return active;}
    public void updateDetails(String title, String language, String genre, String certification, int durationMinutes, String synopsis) { this.title=title; this.language=language; this.genre=genre; this.certification=certification; this.durationMinutes=durationMinutes; this.synopsis=synopsis; }
}
