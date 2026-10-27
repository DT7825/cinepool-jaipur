package in.picturehouse.jaipur.domain;

public enum SeatCategory {
    FRONT(250), MIDDLE(300), RECLINER(500);
    private final int basePricePaise;
    SeatCategory(int rupees) { this.basePricePaise = rupees * 100; }
    public int getBasePricePaise() { return basePricePaise; }
}
