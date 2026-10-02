import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class ComplaintManagementFX extends Application {

    private ObservableList<Complaint> complaints;
    private Stage stage;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        this.complaints = FXCollections.observableArrayList(FileHandler.load());

        stage.setTitle("Complaint Management System");
        showLoginScene();
        stage.show();
    }

    private void showLoginScene() {
        Label title = new Label("Complaint Management System");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.getStyleClass().add("title-label");

        Label subtitle = new Label("Log in as a student to submit a complaint, or as admin to manage them.");
        subtitle.getStyleClass().add("subtitle-label");

        TextField userField = new TextField();
        userField.setPromptText("Email (Enter your email)");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Password (Enter your password)");

        Button loginBtn = new Button("Login");
        loginBtn.setDefaultButton(true);
        loginBtn.getStyleClass().add("primary-button");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        loginBtn.setOnAction(e -> {
            String u = userField.getText().trim();
            String p = passField.getText();

            if (u.isEmpty()) {
                errorLabel.setText("Please enter your email.");
                return;
            }

            if (u.equalsIgnoreCase("admin@gmail.com") && p.equals("admin123")) {
                showAdminScene();
            } else if (u.equals("admin@gmail.com")) {
                errorLabel.setText("Incorrect admin password.");
            } else if (!EMAIL_PATTERN.matcher(u).matches()) {
                errorLabel.setText("Please enter a valid email address.");
            } else {
                showStudentScene(u);
            }
        });

        VBox form = new VBox(12, title, subtitle, userField, passField, loginBtn, errorLabel);
        form.setPadding(new Insets(40));
        form.setMaxWidth(380);
        form.setAlignment(Pos.CENTER_LEFT);
        form.getStyleClass().add("login-card");

        StackPane root = new StackPane(form);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("root-pane");

        setScene(root, 520, 420);
    }

    private void showStudentScene(String userEmail) {
        Label heading = new Label("Student Panel");
        heading.getStyleClass().add("title-label");

        Label who = new Label("Logged in as " + userEmail);
        who.getStyleClass().add("subtitle-label");

        TextField nameField = new TextField();
        nameField.setPromptText("Your full name");

        ComboBox<String> deptBox = new ComboBox<>(FXCollections.observableArrayList(
                "IT", "Hostel", "Academics", "Administration", "Library", "Other"));
        deptBox.setValue("IT");
        deptBox.setMaxWidth(Double.MAX_VALUE);

        TextArea complaintArea = new TextArea();
        complaintArea.setPromptText("Describe your complaint here...");
        complaintArea.setPrefRowCount(4);
        complaintArea.setWrapText(true);

        Button submitBtn = new Button("Submit Complaint");
        submitBtn.getStyleClass().add("primary-button");

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("secondary-button");

        Label statusMsg = new Label();

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.addRow(0, new Label("Name:"), nameField);
        formGrid.addRow(1, new Label("Department:"), deptBox);
        formGrid.add(new Label("Complaint:"), 0, 2);
        formGrid.add(complaintArea, 1, 2);
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(deptBox, Priority.ALWAYS);
        GridPane.setHgrow(complaintArea, Priority.ALWAYS);

        ColumnConstraints c0 = new ColumnConstraints(100);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        formGrid.getColumnConstraints().addAll(c0, c1);

        HBox buttonRow = new HBox(10, submitBtn, logoutBtn);

        VBox topBox = new VBox(10, heading, who, formGrid, buttonRow, statusMsg);
        topBox.setPadding(new Insets(20));

        // Table of this student's own complaints
        Label myComplaintsLabel = new Label("My Complaints");
        myComplaintsLabel.getStyleClass().add("section-label");

        TableView<Complaint> myTable = buildComplaintTable(false);
        FilteredList<Complaint> mine = new FilteredList<>(complaints,
                c -> c.getEmail().equalsIgnoreCase(userEmail));
        myTable.setItems(mine);
        VBox.setVgrow(myTable, Priority.ALWAYS);

        VBox bottomBox = new VBox(8, myComplaintsLabel, myTable);
        bottomBox.setPadding(new Insets(0, 20, 20, 20));
        VBox.setVgrow(myTable, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(bottomBox);
        root.getStyleClass().add("root-pane");

        submitBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String text = complaintArea.getText().trim();

            if (name.isEmpty() || text.isEmpty()) {
                statusMsg.getStyleClass().setAll("error-label");
                statusMsg.setText("Please fill in your name and complaint before submitting.");
                return;
            }

            Complaint c = new Complaint(nextId(), name, userEmail, deptBox.getValue(), text);
            complaints.add(c);
            FileHandler.save(new ArrayList<>(complaints));
            javafx.application.Platform.runLater(myTable::refresh);

            statusMsg.getStyleClass().setAll("success-label");
            statusMsg.setText("Complaint submitted! You can track its status below.");
            nameField.clear();
            complaintArea.clear();
        });

        logoutBtn.setOnAction(e -> showLoginScene());

        setScene(root, 720, 640);
    }

    private void showAdminScene() {
        Label heading = new Label("Admin Panel");
        heading.getStyleClass().add("title-label");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name or email...");

        ComboBox<String> statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                "All", "Pending", "In Progress", "Resolved"));
        statusFilter.setValue("All");

        TableView<Complaint> table = buildComplaintTable(true);

        FilteredList<Complaint> filtered = new FilteredList<>(complaints, c -> true);
        table.setItems(filtered);

        Runnable applyFilter = () -> filtered.setPredicate(c -> {
            String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            boolean matchesQuery = q.isEmpty()
                    || c.getName().toLowerCase().contains(q)
                    || c.getEmail().toLowerCase().contains(q);
            boolean matchesStatus = statusFilter.getValue().equals("All")
                    || c.getStatus().equals(statusFilter.getValue());
            return matchesQuery && matchesStatus;
        });

        searchField.textProperty().addListener((obs, old, val) -> applyFilter.run());
        statusFilter.valueProperty().addListener((obs, old, val) -> applyFilter.run());

        Button progressBtn = new Button("Mark In Progress");
        Button resolveBtn = new Button("Mark Resolved");
        Button replyBtn = new Button("Reply");
        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("danger-button");
        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("secondary-button");

        progressBtn.getStyleClass().add("secondary-button");
        resolveBtn.getStyleClass().add("primary-button");
        replyBtn.getStyleClass().add("secondary-button");

        HBox actionRow = new HBox(10, progressBtn, resolveBtn, replyBtn, deleteBtn, logoutBtn);

        HBox filterRow = new HBox(10, searchField, statusFilter);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox listTab = new VBox(10, filterRow, table, actionRow);
        listTab.setPadding(new Insets(15));
        VBox.setVgrow(table, Priority.ALWAYS);

        Tab complaintsTab = new Tab("Complaints", listTab);
        complaintsTab.setClosable(false);

        Tab dashboardTab = new Tab("Dashboard", buildDashboard());
        dashboardTab.setClosable(false);

        TabPane tabs = new TabPane(complaintsTab, dashboardTab);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == dashboardTab)
                refreshDashboardData();
        });

        progressBtn.setOnAction(e -> updateSelected(table, c -> c.setStatus("In Progress")));
        resolveBtn.setOnAction(e -> updateSelected(table, c -> c.setStatus("Resolved")));
        deleteBtn.setOnAction(e -> {
            Complaint selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                warn("Select a complaint first.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete this complaint from " + selected.getName() + "?");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    complaints.remove(selected);
                    FileHandler.save(new ArrayList<>(complaints));
                }
            });
        });

        replyBtn.setOnAction(e -> {
            Complaint selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                warn("Select a complaint first.");
                return;
            }
            TextInputDialog dialog = new TextInputDialog(selected.getReply());
            dialog.setTitle("Reply to Complaint");
            dialog.setHeaderText("Reply to " + selected.getName() + "'s complaint");
            dialog.setContentText("Reply:");
            dialog.showAndWait().ifPresent(reply -> {
                selected.setReply(reply);
                FileHandler.save(new ArrayList<>(complaints));
                javafx.application.Platform.runLater(table::refresh);
            });
        });

        logoutBtn.setOnAction(e -> showLoginScene());

        BorderPane root = new BorderPane();
        VBox top = new VBox(heading);
        top.setPadding(new Insets(20, 20, 0, 20));
        root.setTop(top);
        root.setCenter(tabs);
        root.getStyleClass().add("root-pane");

        setScene(root, 900, 680);
    }

    private void updateSelected(TableView<Complaint> table, java.util.function.Consumer<Complaint> action) {
        Complaint selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            warn("Select a complaint first.");
            return;
        }
        action.accept(selected);
        FileHandler.save(new ArrayList<>(complaints));
        javafx.application.Platform.runLater(table::refresh);
    }

    private void warn(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    private PieChart dashboardChart;

    private VBox buildDashboard() {
        dashboardChart = new PieChart();
        dashboardChart.setTitle("Complaints by Status");
        refreshDashboardData();

        Label totalLabel = new Label();
        totalLabel.textProperty().bind(
                javafx.beans.binding.Bindings.size(complaints).asString("Total complaints: %d"));
        totalLabel.getStyleClass().add("section-label");

        VBox box = new VBox(10, totalLabel, dashboardChart);
        box.setPadding(new Insets(20));
        VBox.setVgrow(dashboardChart, Priority.ALWAYS);
        return box;
    }

    private void refreshDashboardData() {
        long pending = complaints.stream().filter(c -> c.getStatus().equals("Pending")).count();
        long progress = complaints.stream().filter(c -> c.getStatus().equals("In Progress")).count();
        long resolved = complaints.stream().filter(c -> c.getStatus().equals("Resolved")).count();

        // Only include slices that actually have complaints - a 0-count
        // slice has no angular width, which makes its label collide with
        // the neighbouring slice's label.
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        if (pending > 0)
            data.add(new PieChart.Data("Pending (" + pending + ")", pending));
        if (progress > 0)
            data.add(new PieChart.Data("In Progress (" + progress + ")", progress));
        if (resolved > 0)
            data.add(new PieChart.Data("Resolved (" + resolved + ")", resolved));

        if (dashboardChart != null) {
            dashboardChart.setData(data);
        }
    }

    // ---------------------------------------------------------------
    // SHARED: complaint TableView builder
    // ---------------------------------------------------------------
    private TableView<Complaint> buildComplaintTable(boolean showEmail) {
        TableView<Complaint> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No complaints yet."));

        TableColumn<Complaint, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().getId())));
        idCol.setMaxWidth(60);

        TableColumn<Complaint, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getName()));

        TableColumn<Complaint, String> deptCol = new TableColumn<>("Department");
        deptCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDepartment()));

        TableColumn<Complaint, String> textCol = new TableColumn<>("Complaint");
        textCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getComplaintText()));

        TableColumn<Complaint, String> dateCol = new TableColumn<>("Submitted");
        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDateSubmitted()));

        TableColumn<Complaint, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "Resolved" -> setStyle("-fx-text-fill: #1b8a3d; -fx-font-weight: bold;");
                        case "In Progress" -> setStyle("-fx-text-fill: #b06a00; -fx-font-weight: bold;");
                        default -> setStyle("-fx-text-fill: #b02a2a; -fx-font-weight: bold;");
                    }
                }
            }
        });

        TableColumn<Complaint, String> replyCol = new TableColumn<>("Admin Reply");
        replyCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getReply()));

        if (showEmail) {
            TableColumn<Complaint, String> emailCol = new TableColumn<>("Email");
            emailCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    data.getValue().getEmail()));
            table.getColumns().addAll(idCol, nameCol, emailCol, deptCol, textCol, dateCol, statusCol, replyCol);
        } else {
            table.getColumns().addAll(idCol, deptCol, textCol, dateCol, statusCol, replyCol);
        }

        return table;
    }

    private int nextId() {
        return complaints.stream().mapToInt(Complaint::getId).max().orElse(0) + 1;
    }

    private void setScene(javafx.scene.Parent root, double w, double h) {
        Scene scene = new Scene(root, w, h);
        String css = getClass().getResource("style.css") != null
                ? getClass().getResource("style.css").toExternalForm()
                : null;
        if (css != null)
            scene.getStylesheets().add(css);
        stage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

// =====================================================================
// MODEL
// =====================================================================
class Complaint implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    private final int id;
    private final String name;
    private final String email;
    private final String department;
    private final String complaintText;
    private final String dateSubmitted;
    private String status;
    private String reply;

    Complaint(int id, String name, String email, String department, String complaintText) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.complaintText = complaintText;
        this.dateSubmitted = LocalDateTime.now().format(FMT);
        this.status = "Pending";
        this.reply = "";
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public String getComplaintText() {
        return complaintText;
    }

    public String getDateSubmitted() {
        return dateSubmitted;
    }

    public String getStatus() {
        return status;
    }

    public String getReply() {
        return reply;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    @Override
    public String toString() {
        return name + " : " + complaintText + " | Status: " + status
                + (reply.isEmpty() ? "" : " | Admin Reply: " + reply);
    }
}

class FileHandler {

    private static final String FILE_NAME = "data.dat";

    static void save(ArrayList<Complaint> list) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    static ArrayList<Complaint> load() {
        File file = new File(FILE_NAME);
        if (!file.exists())
            return new ArrayList<>();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (ArrayList<Complaint>) ois.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}