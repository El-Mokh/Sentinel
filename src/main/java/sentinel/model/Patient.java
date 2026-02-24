package sentinel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Patient {
    private String id;
    private List<MedicalRecord> history;

    public Patient(String id) {
        this.id = id;
        this.history = new ArrayList<>();
    }

    public void addRecord(MedicalRecord record) {
        this.history.add(record);
        // Automatically keep the list sorted by date whenever we add something
        Collections.sort(this.history);
    }

    public String getId() {
        return id;
    }

    public List<MedicalRecord> getHistory() {
        return history;
    }
    
    // Helper to get only CMAS scores (for the graph later)
    public List<CMAS> getCMASHistory() {
        List<CMAS> cmasList = new ArrayList<>();
        for (MedicalRecord record : history) {
            if (record instanceof CMAS) {
                cmasList.add((CMAS) record);
            }
        }
        return cmasList;
    }
}