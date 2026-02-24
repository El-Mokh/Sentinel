package sentinel.util;

import sentinel.model.CMAS;
import sentinel.model.MedicalRecord;
import sentinel.model.Patient;
import sentinel.model.RiskLevel;

import java.util.List;

public class FlareUpDetector {

    /**
     * Analyzes a patient's CMAS history to determine risk.
     * Rule: If score drops by 5 or more points consecutively, it's CRITICAL.
     */
    public static RiskLevel analyze(Patient p) {
        List<CMAS> scores = p.getCMASHistory();

        // Not enough data to judge? Assume Stable.
        if (scores.size() < 2) {
            return RiskLevel.STABLE;
        }

        // Loop through history (Oldest -> Newest)
        // We look for sudden drops.
        for (int i = 1; i < scores.size(); i++) {
            int current = scores.get(i).getScore();
            int previous = scores.get(i - 1).getScore();
            
            int drop = previous - current;

            // The Logic: A drop of >5 points is a "Flare-up"
            if (drop >= 5) {
                // If this happened recently (e.g. within the last year of data), flag it.
                // For this demo, we flag it if it EVER happened to show the feature works.
                return RiskLevel.CRITICAL;
            }
        }
        
        return RiskLevel.STABLE;
    }
}