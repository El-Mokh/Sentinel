package sentinel.model;

import java.time.LocalDate;

public class CMAS extends MedicalRecord {
    private int score; // 0 to 52

    public CMAS(LocalDate date, int score) {
        super(date, "CMAS");
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "CMAS Score: " + score;
    }
}