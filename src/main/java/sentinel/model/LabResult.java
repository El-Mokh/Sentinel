package sentinel.model;

import java.time.LocalDate;

public class LabResult extends MedicalRecord {
    private double value;
    private String unit;
    private String testName; // e.g., "Creatine Kinase"

    public LabResult(LocalDate date, String testName, double value, String unit) {
        super(date, "Lab: " + testName);
        this.testName = testName;
        this.value = value;
        this.unit = unit;
    }

    public double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }
    
    public String getTestName() {
        return testName;
    }

    @Override
    public String toString() {
        return testName + ": " + value + " " + unit;
    }
}