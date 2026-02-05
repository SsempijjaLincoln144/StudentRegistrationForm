package studentregform;

// Import JavaFX classes
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

// Import JDBC + SQL classes
import java.sql.*;
import java.time.*;
import java.util.regex.*;

public class StudentRegForm extends Application {
    // Declare UI components
    private TextField firstNameField, lastNameField, emailField, confirmEmailField;
    private PasswordField passwordField, confirmPasswordField;
    private ComboBox<Integer> yearBox, monthBox, dayBox;
    private ToggleGroup genderGroup;
    private ComboBox<String> deptBox;
    private TextArea outputArea;

    @Override
    public void start(Stage primaryStage) {
        
        
        // Input fields
        firstNameField = new TextField();
        lastNameField = new TextField();
        emailField = new TextField();
        confirmEmailField = new TextField();
        passwordField = new PasswordField();
        confirmPasswordField = new PasswordField();

        // DOB combo boxes
        yearBox = new ComboBox<>();
        monthBox = new ComboBox<>();
        dayBox = new ComboBox<>();
        for (int y = 1960; y <= LocalDate.now().getYear(); y++) yearBox.getItems().add(y);
        for (int m = 1; m <= 12; m++) monthBox.getItems().add(m);
        monthBox.setOnAction(e -> updateDays());
        yearBox.setOnAction(e -> updateDays());

        // Gender radio buttons
        genderGroup = new ToggleGroup();
        RadioButton male = new RadioButton("Male");
        RadioButton female = new RadioButton("Female");
        male.setToggleGroup(genderGroup);
        female.setToggleGroup(genderGroup);

        // Department dropdown
        deptBox = new ComboBox<>();
        deptBox.getItems().addAll("Civil", "CSE", "Electrical", "E&C", "Mechanical");

        // Buttons
        Button submitBtn = new Button("Submit");
        Button cancelBtn = new Button("Cancel");
        submitBtn.setOnAction(e -> handleSubmit());
        cancelBtn.setOnAction(e -> clearForm());

        // Output area
        outputArea = new TextArea();
        outputArea.setEditable(false);

        // Layout
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);
        
        
        grid.add(new Label("First Name:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1);
        grid.add(lastNameField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Confirm Email:"), 0, 3);
        grid.add(confirmEmailField, 1, 3);
        grid.add(new Label("Password:"), 0, 4);
        grid.add(passwordField, 1, 4);
        grid.add(new Label("Confirm Password:"), 0, 5);
        grid.add(confirmPasswordField, 1, 5);
        grid.add(new Label("DOB:"), 0, 6);
        grid.add(yearBox, 1, 6);
        grid.add(monthBox, 2, 6);
        grid.add(dayBox, 3, 6);
        grid.add(new Label("Gender:"), 0, 7);
        grid.add(male, 1, 7);
        grid.add(female, 2, 7);
        grid.add(new Label("Department:"), 0, 8);
        grid.add(deptBox, 1, 8);
        grid.add(submitBtn, 0, 9);
        grid.add(cancelBtn, 1, 9);
        grid.add(new Label("Your Data is Below:"), 0, 10);
        grid.add(outputArea, 0, 11, 4, 4);

        primaryStage.setScene(new Scene(grid, 600, 500));
        primaryStage.setTitle("New Student Registration Form");
        Label formTitle = new Label("New Student Registration Form ");
        formTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
      
        
        primaryStage.show();
    }
    

    // Update days in dayBox
    private void updateDays() {
        dayBox.getItems().clear();
        if (yearBox.getValue() != null && monthBox.getValue() != null) {
            YearMonth ym = YearMonth.of(yearBox.getValue(), monthBox.getValue());
            for (int d = 1; d <= ym.lengthOfMonth(); d++) dayBox.getItems().add(d);
        }
    }

    // Handle Submit
    private void handleSubmit() {
        String fname = firstNameField.getText().trim();
        String lname = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String confirmEmail = confirmEmailField.getText().trim();
        String pass = passwordField.getText().trim();
        String confirmPass = confirmPasswordField.getText().trim();

        if (fname.isEmpty() || lname.isEmpty() || email.isEmpty() || confirmEmail.isEmpty() ||
            pass.isEmpty() || confirmPass.isEmpty() || yearBox.getValue() == null ||
            monthBox.getValue() == null || dayBox.getValue() == null ||
            genderGroup.getSelectedToggle() == null || deptBox.getValue() == null) {
            showError("All fields are required!");
            return;
        }

        if (!email.equals(confirmEmail) || !Pattern.matches("[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$", email)) {
            showError("Invalid or mismatched Email!");
            return;
        }

        if (!pass.equals(confirmPass) || pass.length() < 8 || pass.length() > 20 ||
            !pass.matches(".*[A-Za-z].*") || !pass.matches(".*\\d.*")) {
            showError("Invalid or mismatched password!");
            return;
        }

        LocalDate dob = LocalDate.of(yearBox.getValue(), monthBox.getValue(), dayBox.getValue());
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (age < 16 || age > 60) {
            showError("Age must be between 16 and 60!");
            return;
        }

        String gender = ((RadioButton) genderGroup.getSelectedToggle()).getText().substring(0, 1);
        String dept = deptBox.getValue();
        String id = generateID(LocalDate.now().getYear());

        String record = String.format("ID: %s | %s %s | %s | %s | %s | %s",
                id, fname, lname, gender, dept, dob, email);

        outputArea.appendText(record + "\n");

        saveToDB(id, fname, lname, gender, dept, dob, email, pass);
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        confirmEmailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        yearBox.setValue(null);
        monthBox.setValue(null);
        dayBox.setValue(null);
        genderGroup.selectToggle(null);
        deptBox.setValue(null);
    }

    private String generateID(int year) {
        int counter = 1;
        try (Connection conn = DriverManager.getConnection(
                "jdbc:ucanaccess://C:/Users/LincolnMatthew/OneDrive/Documents/GitHub/StudentRegForm/Students.accdb");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Students WHERE ID LIKE '" + year + "-%'")) {
            if (rs.next()) counter = rs.getInt(1) + 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return String.format("%d-%05d", year, counter);
    }

    private void saveToDB(String id, String fname, String lname, String gender,
                          String dept, LocalDate dob, String email, String pass) {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:ucanaccess://C:/Users/LincolnMatthew/OneDrive/Documents/GitHub/StudentRegForm/Students.accdb");
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO Students (ID, FirstName, LastName, Gender, Department, DOB, Email, Password) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            pstmt.setString(1, id);
            pstmt.setString(2, fname);
            pstmt.setString(3, lname);
            pstmt.setString(4, gender);
            pstmt.setString(5, dept);
            pstmt.setDate(6, java.sql.Date.valueOf(dob));
            pstmt.setString(7, email);
            pstmt.setString(8, pass);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database save failed!");
        }
    }

    //  Main method to launch JavaFX app
    public static void main(String[] args) {
        launch(args);
    }
}