package sentinel.db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:sentinel.db";

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {


            String createPatientsTable = "CREATE TABLE IF NOT EXISTS patients (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT, " +
                    "role TEXT DEFAULT 'PATIENT', " +   
                    "password TEXT" + 
                    ");";
            stmt.execute(createPatientsTable);

            String createLabsTable = "CREATE TABLE IF NOT EXISTS lab_results (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "patient_id TEXT, " +
                    "test_name TEXT, " +
                    "value REAL, " +
                    "date TEXT, " +
                    "FOREIGN KEY(patient_id) REFERENCES patients(id)" +
                    ");";
            stmt.execute(createLabsTable);

            System.out.println("✅ Database and tables initialized successfully.");

        } catch (Exception e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
        }
    }
}