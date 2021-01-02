import java.util.Random;

public class TempInfo {
    private static final Random RANDOM = new Random();

    private final String town;
    private final int temp;

    private TempInfo(String town, int temp) {
        this.town = town;
        this.temp = temp;
    }

    static TempInfo fetch(String town) {
        if (RANDOM.nextInt(10) == 0) {
            throw new RuntimeException("error");
        }
        return new TempInfo(town, RANDOM.nextInt(100));
    }

    @Override
    public String toString() {
        return "TempInfo{" +
                "town='" + town + '\'' +
                ", temp=" + temp +
                '}';
    }
}
