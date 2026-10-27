package in.picturehouse.jaipur.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity @Table(name = "theatres")
public class Theatre {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank private String name;
    @NotBlank private String area;
    @NotBlank private String address;
    private String city = "Jaipur";
    private String theatreType;
    private String parkingInfo;
    private boolean active = true;
    protected Theatre() {}
    public Theatre(String name, String area, String address) { this.name=name; this.area=area; this.address=address; }
    public Theatre(String name, String area, String address, String theatreType, String parkingInfo) { this(name, area, address); this.theatreType=theatreType; this.parkingInfo=parkingInfo; }
    public Long getId(){return id;} public String getName(){return name;} public String getArea(){return area;} public String getAddress(){return address;} public String getCity(){return city;} public String getTheatreType(){return theatreType;} public String getParkingInfo(){return parkingInfo;} public boolean isActive(){return active;}
    public void updateDetails(String name, String area, String address) { this.name=name; this.area=area; this.address=address; }
}
