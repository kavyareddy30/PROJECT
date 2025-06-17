import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// Imports for regex pattern matching
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrudGuiApp extends Application {

    private final String url = "jdbc:oracle:thin:@localhost:1521:XE";
    private final String user = "system";
    private final String pass = "300705"; // Reminder: Use a secure way to handle passwords in production.

    private VBox mainContainer;
    private TextArea outputArea;
    private static final String TABLE_TAG_MARKER = "--@table:";

    // Map to hold dynamically created checkboxes and their associated SQL statements
    private final Map<CheckBox, String> statementCheckboxMap = new HashMap<>();

    // --- Define a color theme for easy access ---
    private static final String THEME_BACKGROUND = "#ECEFF1"; // Light Blue-Gray
    private static final String THEME_PANEL_BACKGROUND = "#FFFFFF"; // White
    private static final String THEME_TITLE_COLOR = "#263238"; // Dark Slate Gray
    private static final String THEME_SUBTITLE_COLOR = "#546E7A"; // Lighter Slate Gray
    private static final String THEME_TERMINAL_BACKGROUND = "#2B3E50"; // Dark Blue/Charcoal
    private static final String THEME_TERMINAL_TEXT = "#E0E0E0"; // Light Gray Text
    private static final String THEME_BORDER_COLOR = "#CFD8DC"; // Light border color


    @Override
    public void start(Stage stage) {
        stage.setTitle("Database File Parser");

        // --- TOP-LEVEL CONTROLS ---
        Button createBtn = new Button("CREATE");
        Button insertBtn = new Button("INSERT");
        Button updateBtn = new Button("UPDATE");
        Button deleteBtn = new Button("DELETE");
        Button dropBtn = new Button("DROP");
        Button selectBtn = new Button("SELECT");

        String buttonBaseStyle = "-fx-font-size: 14px; -fx-min-width: 150px; -fx-min-height: 40px; -fx-background-radius: 8; -fx-font-weight: bold;";
        String buttonHoverEffect = "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 5, 0, 0, 1);";

        createBtn.setStyle(buttonBaseStyle + "-fx-background-color: #4CAF50; -fx-text-fill: white;");
        insertBtn.setStyle(buttonBaseStyle + "-fx-background-color: #2196F3; -fx-text-fill: white;");
        updateBtn.setStyle(buttonBaseStyle + "-fx-background-color: #FF9800; -fx-text-fill: white;");
        deleteBtn.setStyle(buttonBaseStyle + "-fx-background-color: #F44336; -fx-text-fill: white;");
        dropBtn.setStyle(buttonBaseStyle + "-fx-background-color: #B71C1C; -fx-text-fill: white;");
        selectBtn.setStyle(buttonBaseStyle + "-fx-background-color: #9C27B0; -fx-text-fill: white;");

        for (Button btn : new Button[]{createBtn, insertBtn, updateBtn, deleteBtn, dropBtn, selectBtn}) {
            btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + buttonHoverEffect));
            btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace(buttonHoverEffect, "")));
        }

        // This HBox holds the operation buttons and is always visible.
        HBox buttonRow = new HBox(15, createBtn, insertBtn, updateBtn, deleteBtn, dropBtn, selectBtn);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.setPadding(new Insets(10, 0, 10, 0));


        // --- SIDE-BY-SIDE CONTENT AREA ---
        mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(15));
        mainContainer.setStyle("-fx-background-color: " + THEME_PANEL_BACKGROUND + "; -fx-background-radius: 8;");
        mainContainer.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.1)));

        ScrollPane leftScrollPane = new ScrollPane(mainContainer);
        leftScrollPane.setFitToWidth(true);
        leftScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setStyle(
                "-fx-font-family: 'Consolas', 'Monospaced', 'Courier New';" +
                        "-fx-control-inner-background: " + THEME_TERMINAL_BACKGROUND + ";" +
                        "-fx-text-fill: " + THEME_TERMINAL_TEXT + ";" +
                        "-fx-border-color: " + THEME_BORDER_COLOR + ";" +
                        "-fx-border-radius: 8;"
        );
        outputArea.setWrapText(true);
        outputArea.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.1)));

        VBox rightPanel = new VBox(5,
                new Label("Application Output") {{
                    setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + THEME_TITLE_COLOR + ";");
                }},
                outputArea);
        VBox.setVgrow(outputArea, Priority.ALWAYS);

        HBox contentArea = new HBox(20, leftScrollPane, rightPanel);
        contentArea.setPadding(new Insets(20, 0, 0, 0));
        HBox.setHgrow(leftScrollPane, Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        // --- MAIN LAYOUT ---
        Label mainTitle = new Label("Database File Parser");
        mainTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + THEME_TITLE_COLOR + ";");

        // The buttonRow is now directly included in the root layout.
        VBox root = new VBox(15, mainTitle, buttonRow, new Separator(), contentArea);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: " + THEME_BACKGROUND + ";");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // --- ACTION HANDLERS (for the operation buttons) ---
        createBtn.setOnAction(e -> showGenericForm("CREATE", "create.txt"));
        insertBtn.setOnAction(e -> showGenericForm("INSERT", "insert.txt"));
        updateBtn.setOnAction(e -> showGenericForm("UPDATE", "update.txt"));
        deleteBtn.setOnAction(e -> showGenericForm("DELETE", "delete.txt"));
        dropBtn.setOnAction(e -> showGenericForm("DROP", "drop.txt"));
        selectBtn.setOnAction(e -> showSelectForm());

        // --- SCENE and STAGE ---
        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);
        stage.show();

        outputArea.setText("🚀 Database Management System Ready!\n");
        mainContainer.getChildren().add(new Label("Select an operation above to begin.") {{
            setStyle("-fx-font-size: 14px; -fx-text-fill: " + THEME_SUBTITLE_COLOR + ";");
        }});
    }

    private void clearMainContainer() {
        mainContainer.getChildren().clear();
        statementCheckboxMap.clear();
    }

    private void showGenericForm(String title, String queryFileName) {
        clearMainContainer();
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + THEME_TITLE_COLOR + ";");
        TextField tableNameField = new TextField();
        tableNameField.setPromptText("Enter target table name (e.g., CUSTOMERS)");
        tableNameField.setMinWidth(300);

        Button loadBtn = new Button("Load Queries from " + queryFileName);
        loadBtn.setStyle("-fx-background-color: " + THEME_SUBTITLE_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");

        VBox statementSelectionContainer = new VBox(10);
        statementSelectionContainer.setPadding(new Insets(10, 0, 0, 0));
        VBox.setVgrow(statementSelectionContainer, Priority.ALWAYS);

        loadBtn.setOnAction(e -> {
            String tableName = tableNameField.getText().trim();
            if (tableName.isEmpty()) {
                outputArea.appendText("❌ Error: Please enter a table name.\n");
                return;
            }
            String fileContent = readQueryFromFile(queryFileName);
            if (fileContent == null) return;

            String queryBlock = findQueryBlockForTable(fileContent, tableName);
            if (queryBlock == null) {
                outputArea.appendText("❌ Error: No query block found for table '" + tableName + "' in " + queryFileName + ".\n"
                        + "   Please ensure the file contains a tag like: " + TABLE_TAG_MARKER + " " + tableName.toUpperCase() + "\n\n");
                return;
            }
            displayStatementsForSelection(queryBlock, statementSelectionContainer);
        });

        Label instructionLabel = new Label("1. Enter a table name to find its query block in " + queryFileName + ".");
        instructionLabel.setStyle("-fx-text-fill: " + THEME_SUBTITLE_COLOR + ";");

        HBox inputRow = new HBox(10, new Label("Target Table:"), tableNameField, loadBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        // Help text label for example tables
        Label examplesLabel = new Label("Example tables: CUSTOMERS, CATEGORIES, PRODUCTS, ORDERS, ORDER_ITEMS");
        examplesLabel.setStyle("-fx-font-style: italic; -fx-text-fill: " + THEME_SUBTITLE_COLOR + "; -fx-font-size: 11px; -fx-padding: 0 0 0 95;");


        mainContainer.getChildren().addAll(
                titleLabel,
                instructionLabel,
                inputRow,
                examplesLabel, // Add the new label to the layout
                new Separator(),
                statementSelectionContainer
        );
    }

    private void displayStatementsForSelection(String queryBlock, VBox container) {
        container.getChildren().clear();
        statementCheckboxMap.clear();

        String[] statements = queryBlock.split(";");
        List<String> validStatements = new ArrayList<>();
        for (String s : statements) {
            if (!s.trim().isEmpty()) validStatements.add(s.trim());
        }

        if (validStatements.isEmpty()) {
            outputArea.appendText("ℹ No executable statements found for the selected table.\n\n");
            return;
        }

        outputArea.appendText("✅ Loaded " + validStatements.size() + " statement(s) for review.\n");
        Label instructionLabel = new Label("2. Select the statements you wish to execute:");
        instructionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + THEME_SUBTITLE_COLOR + ";");
        container.getChildren().add(instructionLabel);

        VBox checkBoxesVBox = new VBox(8);
        checkBoxesVBox.setStyle("-fx-border-color: " + THEME_BORDER_COLOR + "; -fx-border-radius: 5; -fx-padding: 10;");
        for (String sql : validStatements) {
            CheckBox cb = new CheckBox();
            cb.setSelected(true);
            statementCheckboxMap.put(cb, sql);

            TextArea sqlDisplay = new TextArea(sql);
            sqlDisplay.setEditable(false);
            sqlDisplay.setWrapText(true);
            sqlDisplay.setPrefRowCount(2);
            sqlDisplay.setStyle("-fx-font-family: 'Consolas', 'Monospaced';");

            HBox row = new HBox(10, cb, sqlDisplay);
            row.setAlignment(Pos.CENTER_LEFT);
            checkBoxesVBox.getChildren().add(row);
        }

        Button selectAllBtn = new Button("Select All");
        Button deselectAllBtn = new Button("Deselect All");
        HBox selectionButtons = new HBox(10, selectAllBtn, deselectAllBtn);
        selectAllBtn.setOnAction(e -> statementCheckboxMap.keySet().forEach(cb -> cb.setSelected(true)));
        deselectAllBtn.setOnAction(e -> statementCheckboxMap.keySet().forEach(cb -> cb.setSelected(false)));

        Button executeSelectedBtn = new Button("Execute Selected Statements");
        executeSelectedBtn.setStyle("-fx-background-color: #00897B; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 15; -fx-background-radius: 5;");
        executeSelectedBtn.setOnAction(e -> {
            // Build a list of selected queries instead of a single string
            List<String> selectedQueries = new ArrayList<>();
            for (Map.Entry<CheckBox, String> entry : statementCheckboxMap.entrySet()) {
                if (entry.getKey().isSelected()) {
                    selectedQueries.add(entry.getValue());
                }
            }

            if (selectedQueries.isEmpty()) {
                outputArea.appendText("⚠ Warning: No statements were selected to execute.\n\n");
                return;
            }

            outputArea.appendText("▶ Executing " + selectedQueries.size() + " selected statement(s)...\n");
            executeStatements(selectedQueries);
        });

        container.getChildren().addAll(checkBoxesVBox, selectionButtons, new Separator(), executeSelectedBtn);
    }
    
    private void executeStatements(List<String> statements) {
        if (statements == null || statements.isEmpty()) {
            outputArea.appendText("⚠ Warning: Query list was empty. Nothing to execute.\n\n");
            return;
        }

        try (Connection con = DriverManager.getConnection(url, user, pass)) {
            con.setAutoCommit(false);

            try (Statement stmt = con.createStatement()) {
                int executedCount = 0;
                int totalAffectedRows = 0;

                for (String sql : statements) {
                    if (sql.trim().isEmpty()) {
                        continue;
                    }
                    outputArea.appendText("🔍 Executing: " + sql.trim().replaceAll("\\s+", " ") + "\n");
                    try {
                        // This inner try-catch handles errors for a SINGLE statement
                        int affectedRows = stmt.executeUpdate(sql.trim());
                        totalAffectedRows += affectedRows;
                        executedCount++;
                    } catch (SQLException ex) {
                        // Call our new intelligent error handler
                        handleCreateFailure(sql, ex);
                        // Re-throw the exception to trigger the transaction rollback
                        throw ex;
                    }
                }

                con.commit();
                outputArea.appendText("✅ Success! Executed " + executedCount + " statement(s). Total rows affected: " + totalAffectedRows + ".\n\n");
            } catch (SQLException ex) {
                // This outer catch block now handles the rollback
                outputArea.appendText("--- Rolling back changes ---\n\n");
                con.rollback();
            }
        } catch (SQLException ex) {
            outputArea.appendText("❌ Database Connection Error: " + ex.getMessage() + "\n\n");
        }
    }
    
    private void handleCreateFailure(String failedSql, SQLException e) {
        // Oracle's error code for "table or view does not exist" is 942
        if (e.getErrorCode() == 942 && failedSql.toUpperCase().trim().startsWith("CREATE TABLE")) {
            List<String> referencedTables = new ArrayList<>();
            Pattern pattern = Pattern.compile("REFERENCES\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(failedSql);
            while (matcher.find()) {
                referencedTables.add(matcher.group(1)); // group(1) is the captured table name
            }

            outputArea.appendText("❌ Transaction Failed: Could not create table.\n");
            outputArea.appendText("   Reason: A referenced table does not exist. (ORA-00942)\n");
            if (!referencedTables.isEmpty()) {
                outputArea.appendText("   This statement depends on the following table(s): " + referencedTables + "\n");
                outputArea.appendText("   SOLUTION: Please ensure you have created these tables first.\n");
            }
        } else {
            // If it's another error, just print the standard message.
            outputArea.appendText("❌ Transaction Failed: " + e.getMessage() + "\n");
        }
    }

    private void showSelectForm() {
        clearMainContainer();
        Label titleLabel = new Label("SELECT DATA");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + THEME_TITLE_COLOR + ";");
        TextField tableNameField = new TextField();
        tableNameField.setPromptText("Enter target table name");
        tableNameField.setMinWidth(300);
        Button selectDataBtn = new Button("Execute from select.txt");
        selectDataBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        TableView<Map<String, Object>> resultTable = new TableView<>();
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        resultTable.setPlaceholder(new Label("Enter a table name and click the button to see results."));
        VBox.setVgrow(resultTable, Priority.ALWAYS);

        selectDataBtn.setOnAction(e -> {
            String tableName = tableNameField.getText().trim();
            if (tableName.isEmpty()) {
                outputArea.appendText("❌ Error: Please enter a table name.\n");
                return;
            }
            String fileContent = readQueryFromFile("select.txt");
            if (fileContent == null) return;

            String queryBlock = findQueryBlockForTable(fileContent, tableName);
            if (queryBlock == null) {
                outputArea.appendText("❌ Error: No SELECT query for '" + tableName + "' in select.txt.\n\n");
                return;
            }

            String finalSql = "";
            for (String s : queryBlock.split(";")) {
                if (!s.trim().isEmpty()) {
                    finalSql = s.trim();
                    break;
                }
            }

            if (finalSql.isEmpty()) {
                outputArea.appendText("❌ Error: No executable statement found for '" + tableName + "'.\n");
                return;
            }

            outputArea.appendText("🔍 Executing: " + finalSql + "\n");
            try (Connection con = DriverManager.getConnection(url, user, pass);
                 Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(finalSql)) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                resultTable.getItems().clear();
                resultTable.getColumns().clear();

                for (int i = 1; i <= columnCount; i++) {
                    final String columnName = metaData.getColumnName(i);
                    TableColumn<Map<String, Object>, String> column = new TableColumn<>(columnName);
                    column.setCellValueFactory(new MapValueFactory(columnName));
                    resultTable.getColumns().add(column);
                }

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnName(i), rs.getObject(i) != null ? rs.getObject(i).toString() : "NULL");
                    }
                    resultTable.getItems().add(row);
                }
                outputArea.appendText("✅ Retrieved " + resultTable.getItems().size() + " records.\n\n");
            } catch (SQLException ex) {
                outputArea.appendText("❌ Database error: " + ex.getMessage() + "\n\n");
            }
        });

        Label resultsLabel = new Label("Query Results:");
        resultsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + THEME_SUBTITLE_COLOR + ";");
        
        HBox inputRow = new HBox(10, new Label("Target Table:"), tableNameField, selectDataBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        Label examplesLabel = new Label("Example tables: CUSTOMERS, CATEGORIES, PRODUCTS, ORDERS, ORDER_ITEMS");
        examplesLabel.setStyle("-fx-font-style: italic; -fx-text-fill: " + THEME_SUBTITLE_COLOR + "; -fx-font-size: 11px; -fx-padding: 0 0 0 95;");


        VBox selectContainer = new VBox(10,
                titleLabel,
                inputRow,
                examplesLabel,
                new Separator(),
                resultsLabel,
                resultTable);
        VBox.setVgrow(resultTable, Priority.ALWAYS);

        mainContainer.getChildren().add(selectContainer);
        VBox.setVgrow(selectContainer, Priority.ALWAYS);
    }

    private String findQueryBlockForTable(String fileContent, String tableName) {
        String content = fileContent.replaceAll("\r\n", "\n");
        String[] lines = content.split("\n");
        int lineIndex = -1;

        for (int i = 0; i < lines.length; i++) {
            String trimmedLine = lines[i].trim();
            if (trimmedLine.toUpperCase().startsWith(TABLE_TAG_MARKER.toUpperCase())) {
                String extractedTable = trimmedLine.substring(TABLE_TAG_MARKER.length()).trim();
                if (extractedTable.equalsIgnoreCase(tableName)) {
                    lineIndex = i;
                    break;
                }
            }
        }

        if (lineIndex == -1) {
            return null; // Tag not found
        }

        StringBuilder queryBlock = new StringBuilder();
        for (int i = lineIndex + 1; i < lines.length; i++) {
            if (lines[i].trim().toUpperCase().startsWith(TABLE_TAG_MARKER.toUpperCase())) {
                break;
            }
            queryBlock.append(lines[i]).append("\n");
        }

        return queryBlock.toString().trim();
    }

    private String readQueryFromFile(String fileName) {
        try {
            return Files.readString(Paths.get(fileName));
        } catch (IOException e) {
            outputArea.appendText("❌ File Error: Could not read '" + fileName + "'.\n" + e.getMessage() + "\n\n");
            return null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}