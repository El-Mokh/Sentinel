package sentinel.util;

import sentinel.model.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataLoader {

    private static DataLoader instance;
    private static final List<DateTimeFormatter> DATE_FORMATS = Arrays.asList(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyyHH:mm")
    );

    private DataLoader() {}

    public static DataLoader getInstance() {
        if (instance == null) instance = new DataLoader();
        return instance;
    }

    // FIX: Accept the target ID as a parameter instead of hardcoding it
    public Map<String, Patient> loadAllData(String cmasTargetId) {
        Map<String, Patient> patients = new HashMap<>();
        System.out.println("Loading data...");

        loadLabResults(patients, "data/LabResult.csv", "data/Measurement.csv");

        Patient target = patients.get(cmasTargetId);
        
        if (target != null) {
            System.out.println("Merging CMAS data into Patient: " + cmasTargetId);
            loadCMAS(target, "data/CMAS.csv");
        } else {
            System.err.println("Warning: Target patient not found. Assigning CMAS to first available.");
            if (!patients.isEmpty()) loadCMAS(patients.values().iterator().next(), "data/CMAS.csv");
        }

        return patients;
    }

    private void loadCMAS(Patient patient, String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String header = br.readLine();
            String values = br.readLine();
            if (header == null || values == null) return;

            String[] dates = header.split(",");
            String[] scores = values.split(",");

            for (int i = 1; i < dates.length; i++) {
                if (i >= scores.length) break;
                LocalDate date = parseDate(dates[i]);
                String scoreStr = scores[i].replaceAll("[^0-9.]", ""); 
                if (!scoreStr.isEmpty()) {
                    patient.addRecord(new CMAS(date, (int) Double.parseDouble(scoreStr)));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadLabResults(Map<String, Patient> patients, String labFile, String measureFile) {
        Map<String, LabInfo> labMeta = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(labFile))) {
            br.readLine(); 
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    labMeta.put(parts[0].trim(), new LabInfo(parts[2].trim(), parts[3].trim(), parts.length > 4 ? parts[4].trim() : ""));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        try (BufferedReader br = new BufferedReader(new FileReader(measureFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4 && labMeta.containsKey(parts[1].trim())) {
                    LabInfo info = labMeta.get(parts[1].trim());
                    Patient p = patients.computeIfAbsent(info.patientId, k -> new Patient(k));
                    
                    try {
                        double val = Double.parseDouble(parts[3].trim());
                        p.addRecord(new LabResult(parseDate(parts[2]), info.testName, val, info.unit));
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private LocalDate parseDate(String dateStr) {
        dateStr = dateStr.trim();
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try { return LocalDate.parse(dateStr, fmt); } catch (Exception e) {}
            try { return java.time.LocalDateTime.parse(dateStr, fmt).toLocalDate(); } catch (Exception e) {}
        }
        return LocalDate.now();
    }

    private static class LabInfo {
        String patientId, testName, unit;
        public LabInfo(String p, String t, String u) { patientId = p; testName = t; unit = u; }
    }
}