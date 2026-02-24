package sentinel;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import sentinel.model.*;
import sentinel.util.DataLoader;
import sentinel.util.FlareUpDetector;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main extends Application {

    private Map<String, Patient> patientData;
    private LineChart<Number, Number> cmasChart;
    private LineChart<Number, Number> labChart;
    
    // UI Elements
    private HBox biomarkerLine; 
    private Label patientTitle;
    private Label riskLabel;
    private ListView<String> patientList;
    private FilteredList<String> filteredData;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 14px;");
        Scene scene = new Scene(root, 1200, 800);
        
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Warning: Could not find style.css");
        }
        
        VBox loadingScreen = new VBox(20);
        loadingScreen.setAlignment(Pos.CENTER);
        loadingScreen.getChildren().addAll(new ProgressIndicator(), new Label("Loading Clinical Data..."));
        root.setCenter(loadingScreen);
        
        stage.setTitle("Sentinel - Clinical Dashboard");
        stage.setScene(scene);
        stage.show();

        Task<Map<String, Patient>> loadDataTask = new Task<>() {
            @Override
            protected Map<String, Patient> call() throws Exception {
                return DataLoader.getInstance().loadAllData("27ec48bb-769c-4fe9-af36-2c3e51d4988f");
            }
        };

        loadDataTask.setOnSucceeded(event -> {
            patientData = loadDataTask.getValue();
            buildDashboard(root); 
        });

        loadDataTask.setOnFailed(event -> {
            root.setCenter(new Label("Failed to load data. Please check console logs."));
            loadDataTask.getException().printStackTrace();
        });

        new Thread(loadDataTask).start();
    }

    private void buildDashboard(BorderPane root) {
        TextField searchField = new TextField();
        searchField.setPromptText("Search ID...");
        searchField.setStyle("-fx-padding: 8;");

        patientList = new ListView<>();
        ObservableList<String> masterData = FXCollections.observableArrayList(patientData.keySet());
        filteredData = new FilteredList<>(masterData, p -> true);
        
        SortedList<String> sortedData = new SortedList<>(filteredData);
        patientList.setItems(sortedData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(patientId -> {
                if (newVal == null || newVal.trim().isEmpty()) return true;
                return patientId.toLowerCase().contains(newVal.toLowerCase().trim());
            });
        });

        patientList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) {
                    setText(null);
                } else {
                    Patient p = patientData.get(id);
                    RiskLevel risk = FlareUpDetector.analyze(p);
                    
                    // Reverted back to full, complete IDs
                    setText((risk == RiskLevel.CRITICAL ? "⚠️ " : "   ") + id);
                    
                    if (risk == RiskLevel.CRITICAL) {
                        setTextFill(Color.RED);
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setTextFill(Color.BLACK);
                        setStyle("");
                    }
                }
            }
        });

        VBox sidebar = new VBox(10, new Label("Patient Search"), searchField, patientList);
        sidebar.setPadding(new Insets(10));
        VBox.setVgrow(patientList, Priority.ALWAYS);

        // --- CENTER CHART AREA ---
        NumberAxis xAxis1 = createDateAxis();
        NumberAxis xAxis2 = createDateAxis();
        
        xAxis2.setTickLabelsVisible(false);
        xAxis2.setTickMarkVisible(false);
        xAxis2.setMinorTickVisible(false);
        xAxis2.setLabel(""); 

        NumberAxis yAxis1 = new NumberAxis();
        yAxis1.setLabel("Muscle Strength (CMAS)");
        cmasChart = new LineChart<>(xAxis1, yAxis1);
        cmasChart.setAnimated(false);
        cmasChart.setLegendVisible(true);

        NumberAxis yAxis2 = new NumberAxis();
        yAxis2.setLabel("Biomarker Level (U/L)");
        yAxis2.setSide(Side.RIGHT);
        labChart = new LineChart<>(xAxis2, yAxis2);
        labChart.setAnimated(false);
        labChart.setLegendVisible(true);
        
        labChart.getStyleClass().add("overlay-chart");
        
        labChart.setBackground(Background.EMPTY);
        labChart.setPickOnBounds(false); 
        labChart.setHorizontalGridLinesVisible(false);
        labChart.setVerticalGridLinesVisible(false);

        StackPane chartStack = new StackPane(cmasChart, labChart);
        chartStack.setAlignment(Pos.CENTER);
        chartStack.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        patientTitle = new Label("Select a Patient");
        patientTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        riskLabel = new Label("");
        riskLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        // FIX: Replaced Dropdown with a Horizontal Scrollable Line
        biomarkerLine = new HBox(8);
        biomarkerLine.setAlignment(Pos.CENTER_LEFT);
        
        ScrollPane scrollPane = new ScrollPane(biomarkerLine);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scrollPane.setFitToHeight(true);

        HBox biomarkerBox = new HBox(15, new Label("Biomarkers:"), scrollPane);
        biomarkerBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        
        VBox centerLayout = new VBox(10);
        centerLayout.setPadding(new Insets(20));
        centerLayout.getChildren().addAll(patientTitle, riskLabel, chartStack, new Separator(), biomarkerBox);
        VBox.setVgrow(chartStack, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(sidebar, centerLayout);
        splitPane.setDividerPositions(0.25); 
        
        root.setCenter(splitPane);

        patientList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) loadPatient(newVal);
        });

        patientList.getSelectionModel().select("27ec48bb-769c-4fe9-af36-2c3e51d4988f");
    }

    private NumberAxis createDateAxis() {
        NumberAxis axis = new NumberAxis();
        axis.setLabel("Timeline");
        axis.setForceZeroInRange(false); 
        axis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                return LocalDate.ofEpochDay(object.longValue()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            @Override
            public Number fromString(String string) {
                return LocalDate.parse(string).toEpochDay();
            }
        });
        return axis;
    }

    private void loadPatient(String patientId) {
        Patient p = patientData.get(patientId);
        
        if (patientId.equals("27ec48bb-769c-4fe9-af36-2c3e51d4988f")) {
            patientTitle.setText("Patient ID: " + patientId + " (Demo: CMAS Overlay)");
            patientTitle.setTextFill(Color.DARKBLUE);
        } else {
            patientTitle.setText("Patient ID: " + patientId);
            patientTitle.setTextFill(Color.BLACK);
        }

        RiskLevel risk = FlareUpDetector.analyze(p);
        riskLabel.setText("Risk Status: " + risk);
        if (risk == RiskLevel.CRITICAL) riskLabel.setTextFill(Color.RED);
        else riskLabel.setTextFill(Color.GREEN);

        biomarkerLine.getChildren().clear();

        Set<String> tests = new HashSet<>();
        for (MedicalRecord r : p.getHistory()) {
            if (r instanceof LabResult) tests.add(((LabResult) r).getTestName());
        }

        if (tests.isEmpty()) {
            biomarkerLine.getChildren().add(new Label("No lab data available."));
            updateCharts(p, null);
        } else {
            List<String> sortedTests = new ArrayList<>(tests);
            Collections.sort(sortedTests);
            
            for (int i = 0; i < sortedTests.size(); i++) {
                String test = sortedTests.get(i);
                
                // Style the button to look like clean, clickable text
                Button b = new Button(test);
                b.setStyle("-fx-background-color: transparent; -fx-text-fill: #007acc; -fx-cursor: hand; -fx-font-weight: normal;");
                
                b.setOnAction(e -> {
                    updateCharts(p, test);
                    // Reset all other text buttons
                    biomarkerLine.getChildren().forEach(node -> {
                        if (node instanceof Button) {
                            node.setStyle("-fx-background-color: transparent; -fx-text-fill: #007acc; -fx-cursor: hand; -fx-font-weight: normal;");
                        }
                    });
                    // Highlight the selected one with bold text
                    b.setStyle("-fx-background-color: transparent; -fx-text-fill: black; -fx-cursor: hand; -fx-font-weight: bold; -fx-underline: true;");
                });
                
                biomarkerLine.getChildren().add(b);
                
                // Add the "|" separator between items (except after the last one)
                if (i < sortedTests.size() - 1) {
                    Label separator = new Label("|");
                    separator.setStyle("-fx-text-fill: #a0a0a0;");
                    biomarkerLine.getChildren().add(separator);
                }
            }
            
            // Automatically select the first biomarker to populate the chart
            if (!biomarkerLine.getChildren().isEmpty()) {
                ((Button) biomarkerLine.getChildren().get(0)).fire();
            }
        }
    }

    private void updateCharts(Patient p, String biomarker) {
        long minDate = Long.MAX_VALUE;
        long maxDate = Long.MIN_VALUE;
        for (MedicalRecord r : p.getHistory()) {
            long epoch = r.getDate().toEpochDay();
            if (epoch < minDate) minDate = epoch;
            if (epoch > maxDate) maxDate = epoch;
        }

        if (minDate <= maxDate) {
            NumberAxis x1 = (NumberAxis) cmasChart.getXAxis();
            NumberAxis x2 = (NumberAxis) labChart.getXAxis();
            
            x1.setAutoRanging(false);
            x2.setAutoRanging(false);
            
            double lower = minDate - 30;
            double upper = maxDate + 30;
            
            x1.setLowerBound(lower);
            x1.setUpperBound(upper);
            x2.setLowerBound(lower);
            x2.setUpperBound(upper);
            
            double tickSpacing = Math.max((upper - lower) / 10.0, 1.0);
            x1.setTickUnit(tickSpacing);
            x2.setTickUnit(tickSpacing);
        }

        // --- 1. CMAS CHART ---
        cmasChart.getData().clear();
        XYChart.Series<Number, Number> cmasSeries = new XYChart.Series<>();
        cmasSeries.setName("Muscle Score");
        
        for (MedicalRecord r : p.getHistory()) {
            if (r instanceof CMAS) {
                cmasSeries.getData().add(new XYChart.Data<>(r.getDate().toEpochDay(), ((CMAS) r).getScore()));
            }
        }
        
        if (!cmasSeries.getData().isEmpty()) {
            cmasChart.getData().add(cmasSeries);
            
            for (XYChart.Data<Number, Number> data : cmasSeries.getData()) {
                if (data.getNode() != null) {
                    String dateStr = LocalDate.ofEpochDay(data.getXValue().longValue()).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
                    Tooltip t = new Tooltip(dateStr + "\nScore: " + data.getYValue());
                    Tooltip.install(data.getNode(), t);
                }
            }
        }

        // --- 2. LAB CHART ---
        labChart.getData().clear();
        if (biomarker != null) {
            XYChart.Series<Number, Number> labSeries = new XYChart.Series<>();
            labSeries.setName(biomarker);
            for (MedicalRecord r : p.getHistory()) {
                if (r instanceof LabResult && ((LabResult) r).getTestName().equals(biomarker)) {
                    labSeries.getData().add(new XYChart.Data<>(r.getDate().toEpochDay(), ((LabResult) r).getValue()));
                }
            }
            if (!labSeries.getData().isEmpty()) {
                labChart.getData().add(labSeries);
                labSeries.nodeProperty().addListener((obs, old, newNode) -> {
                    if (newNode != null) newNode.setStyle("-fx-stroke: red; -fx-stroke-width: 2px;");
                });
                
                for (XYChart.Data<Number, Number> data : labSeries.getData()) {
                    if (data.getNode() != null) {
                        String dateStr = LocalDate.ofEpochDay(data.getXValue().longValue()).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
                        Tooltip t = new Tooltip(dateStr + "\nValue: " + data.getYValue());
                        Tooltip.install(data.getNode(), t);
                    }
                }
            }
        }
    }

    public static void main(String[] args) { launch(); }
}