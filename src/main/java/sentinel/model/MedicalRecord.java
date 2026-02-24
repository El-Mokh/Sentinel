package sentinel.model;

import java.time.LocalDate;

// "implements Comparable" means we can sort a list of these automatically by date
public abstract class MedicalRecord implements Comparable<MedicalRecord> {
    private LocalDate date;
    private String type; // e.g., "CMAS" or "Lab: CK"

    public MedicalRecord(LocalDate date, String type) {
        this.date = date;
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    // This is the magic method that allows Collections.sort() to work
    @Override
    public int compareTo(MedicalRecord other) {
        return this.date.compareTo(other.date);
    }
    
    // Abstract method: Force children to define how they are printed
    @Override
    public abstract String toString();
}